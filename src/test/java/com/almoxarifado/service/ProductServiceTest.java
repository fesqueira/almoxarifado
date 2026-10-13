package com.almoxarifado.service;

import com.almoxarifado.database.ProductRepository;
import com.almoxarifado.model.MovementReason;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    private Product buildProduct(String name, int quantity) {
        return new Product(
                UUID.randomUUID(), name, "Descrição",
                BigDecimal.valueOf(5), BigDecimal.valueOf(10),
                quantity, 2, LocalDateTime.now());
    }

    private StockMovement capturedMovement(int expectedQuantity) throws SQLException {
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(productRepository).registerMovement(captor.capture(), eq(expectedQuantity), any());
        return captor.getValue();
    }

    // ---------- registerProduct ----------

    @Test
    void registerProductComNomeValidoESemDuplicataSalvaProduto() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of());

        productService.registerProduct(
                " Chave de Fenda ", "Ferramentas", "Chave Phillips",
                BigDecimal.valueOf(3), BigDecimal.valueOf(8), 20, 5);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("Chave de Fenda", captor.getValue().getName());
        assertEquals("Ferramentas", captor.getValue().getCategory());
    }

    @Test
    void registerProductLancaEstoqueInicialComoMovimentacao() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of());

        productService.registerProduct(
                "Chave de Fenda", "Chave Phillips", BigDecimal.valueOf(3), BigDecimal.valueOf(8), 20, 5);

        StockMovement movement = capturedMovement(20);
        assertEquals(MovementReason.INITIAL_STOCK, movement.getReason());
        assertEquals(20, movement.getQuantity());
    }

    @Test
    void registerProductSemQuantidadeNaoGeraMovimentacao() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of());

        productService.registerProduct(
                "Chave de Fenda", "Chave Phillips", BigDecimal.valueOf(3), BigDecimal.valueOf(8), 0, 5);

        verify(productRepository).save(any());
        verify(productRepository, never()).registerMovement(any(), anyInt(), any());
    }

    @Test
    void registerProductComNomeEmBrancoLancaExcecaoENaoSalva() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> productService.registerProduct(
                "   ", "Descrição", BigDecimal.ONE, BigDecimal.TEN, 1, 1));

        verify(productRepository, never()).save(any());
    }

    @Test
    void registerProductComNomeJaExistenteLancaExcecaoENaoSalva() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of(buildProduct("Martelo", 1)));

        // A comparação ignora maiúsculas/minúsculas
        assertThrows(IllegalArgumentException.class, () -> productService.registerProduct(
                "martelo", "Descrição", BigDecimal.ONE, BigDecimal.TEN, 1, 1));

        verify(productRepository, never()).save(any());
    }

    @Test
    void registerProductComPrecoNegativoLancaExcecao() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> productService.registerProduct(
                "Martelo", "Descrição", BigDecimal.valueOf(-1), BigDecimal.TEN, 1, 1));

        verify(productRepository, never()).save(any());
    }

    // ---------- updateProduct ----------

    @Test
    void updateProductAlteraOsDadosMasNaoAQuantidade() throws SQLException {
        Product product = buildProduct("Martelo", 7);
        when(productRepository.findAll()).thenReturn(List.of(product));

        productService.updateProduct(product.getId(), "Martelo de Unha", "Ferramentas", null,
                BigDecimal.valueOf(6), BigDecimal.valueOf(12), 3);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).update(captor.capture());
        assertEquals("Martelo de Unha", captor.getValue().getName());
        assertEquals("Ferramentas", captor.getValue().getCategory());
        assertNull(captor.getValue().getDescription());
        assertEquals(3, captor.getValue().getMinimumStock());
        assertEquals(7, captor.getValue().getQuantity());
    }

    @Test
    void updateProductComNomeDeOutroProdutoLancaExcecao() throws SQLException {
        Product martelo = buildProduct("Martelo", 7);
        Product alicate = buildProduct("Alicate", 3);
        when(productRepository.findAll()).thenReturn(List.of(martelo, alicate));

        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(
                martelo.getId(), "Alicate", null, null, BigDecimal.ONE, BigDecimal.TEN, 1));

        verify(productRepository, never()).update(any());
    }

    // ---------- verifyExists ----------

    @Test
    void verifyExistsDelegaParaORepositorio() throws SQLException {
        when(productRepository.existsByName("Serrote")).thenReturn(true);

        assertTrue(productService.verifyExists("Serrote"));
        verify(productRepository).existsByName("Serrote");
    }

    // ---------- registerEntry ----------

    @Test
    void registerEntryDeCompraRecalculaOCustoMedio() throws SQLException {
        Product product = buildProduct("Furadeira", 10); // custo atual: 5
        when(productRepository.findAll()).thenReturn(List.of(product));

        productService.registerEntry(product.getId(), 10, MovementReason.PURCHASE, BigDecimal.valueOf(7), "NF 10");

        // (10 x 5 + 10 x 7) / 20 = 6
        ArgumentCaptor<BigDecimal> cost = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(productRepository).registerMovement(movement.capture(), eq(20), cost.capture());
        assertEquals(0, BigDecimal.valueOf(6).compareTo(cost.getValue()));
        assertEquals(0, BigDecimal.valueOf(7).compareTo(movement.getValue().getUnitCost()));
        assertEquals("NF 10", movement.getValue().getNote());
    }

    @Test
    void registerEntryComMotivoDeSaidaLancaExcecao() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> productService.registerEntry(
                UUID.randomUUID(), 1, MovementReason.SALE, null, null));

        verify(productRepository, never()).registerMovement(any(), anyInt(), any());
    }

    // ---------- registerExit ----------

    @Test
    void registerExitDeVendaUsaOPrecoDoCadastroQuandoNaoInformado() throws SQLException {
        Product product = buildProduct("Alicate", 10); // custo 5, venda 10
        when(productRepository.findAll()).thenReturn(List.of(product));

        productService.registerExit(product.getId(), 4, MovementReason.SALE, null, null);

        StockMovement movement = capturedMovement(6);
        assertEquals(0, BigDecimal.valueOf(40).compareTo(movement.getRevenue()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(movement.getProfit()));
    }

    @Test
    void registerExitQueNaoEVendaNaoGeraReceita() throws SQLException {
        Product product = buildProduct("Alicate", 10);
        when(productRepository.findAll()).thenReturn(List.of(product));

        productService.registerExit(product.getId(), 4, MovementReason.LOSS, BigDecimal.valueOf(99), null);

        StockMovement movement = capturedMovement(6);
        assertNull(movement.getUnitPrice());
        assertEquals(0, BigDecimal.ZERO.compareTo(movement.getProfit()));
    }

    @Test
    void registerExitMaiorQueEstoqueLancaExcecaoENaoRegistra() throws SQLException {
        Product product = buildProduct("Alicate", 3);
        when(productRepository.findAll()).thenReturn(List.of(product));

        assertThrows(IllegalStateException.class, () -> productService.registerExit(
                product.getId(), 10, MovementReason.SALE, null, null));

        verify(productRepository, never()).registerMovement(any(), anyInt(), any());
    }

    // ---------- increaseAmount / decreaseAmount ----------

    @Test
    void increaseAmountAtualizaQuantidadeQuandoProdutoExiste() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of(buildProduct("Furadeira", 10)));

        productService.increaseAmount("Furadeira", 5);

        assertEquals(MovementReason.ADJUSTMENT_IN, capturedMovement(15).getReason());
    }

    @Test
    void increaseAmountEhCaseInsensitive() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of(buildProduct("Furadeira", 10)));

        productService.increaseAmount("furadeira", 5);

        assertEquals("Furadeira", capturedMovement(15).getProductName());
    }

    @Test
    void increaseAmountComProdutoInexistenteLancaExcecao() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> productService.increaseAmount("Não Existe", 5));

        verify(productRepository, never()).registerMovement(any(), anyInt(), any());
    }

    @Test
    void decreaseAmountAtualizaQuantidadeQuandoProdutoExiste() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of(buildProduct("Alicate", 10)));

        productService.decreaseAmount("Alicate", 4);

        assertEquals(MovementReason.ADJUSTMENT_OUT, capturedMovement(6).getReason());
    }

    @Test
    void decreaseAmountMaiorQueEstoqueLancaExcecaoENaoAtualiza() throws SQLException {
        when(productRepository.findAll()).thenReturn(List.of(buildProduct("Alicate", 3)));

        // Product.removeStock recusa remover mais do que há em estoque
        assertThrows(IllegalStateException.class, () -> productService.decreaseAmount("Alicate", 10));

        verify(productRepository, never()).registerMovement(any(), anyInt(), any());
    }

    // ---------- deleteProduct ----------

    @Test
    void deleteProductRemovePeloId() throws SQLException {
        Product product = buildProduct("Alicate", 3);
        when(productRepository.findAll()).thenReturn(List.of(product));

        productService.deleteProduct("alicate");

        verify(productRepository).deleteById(product.getId());
    }
}

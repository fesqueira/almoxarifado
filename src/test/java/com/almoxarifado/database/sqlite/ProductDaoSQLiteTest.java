package com.almoxarifado.database.sqlite;

import com.almoxarifado.model.MovementReason;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;

import com.almoxarifado.database.sqlite.ProductDaoSQLite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ProductDaoSQLiteTest {

    private File tempDbFile;
    private ProductDaoSQLite dao;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        tempDbFile = File.createTempFile("almoxarifado-test", ".db");
        tempDbFile.delete(); // queremos só o caminho; o SQLite cria o arquivo sozinho
        System.setProperty("almoxarifado.db.path", tempDbFile.getAbsolutePath());

        dao = new ProductDaoSQLite();
        dao.createSchemaIfNotExists();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("almoxarifado.db.path");
        if (tempDbFile != null) {
            tempDbFile.delete();
        }
    }

    private Product buildProduct(String name) {
        return new Product(
                name,
                "Produto de teste",
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(9.9),
                20,
                5);
    }

    @Test
    void saveESeguidoDeExistsByNameEncontraOProduto() throws SQLException {
        dao.save(buildProduct("Chave Allen"));

        assertTrue(dao.existsByName("Chave Allen"));
        assertFalse(dao.existsByName("Não Cadastrado"));
    }

    @Test
    void findAllRetornaTodosOsCamposCorretamente() throws SQLException {
        Product original = buildProduct("Trena");
        dao.save(original);

        List<Product> products = dao.findAll();

        assertEquals(1, products.size());
        Product loaded = products.getFirst();
        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getDescription(), loaded.getDescription());
        assertEquals(0, original.getPurchasePrice().compareTo(loaded.getPurchasePrice()));
        assertEquals(0, original.getSellingPrice().compareTo(loaded.getSellingPrice()));
        assertEquals(original.getQuantity(), loaded.getQuantity());
        assertEquals(original.getMinimumStock(), loaded.getMinimumStock());
        assertEquals(original.getCreatedAt(), loaded.getCreatedAt());
    }

    @Test
    void updateQuantityAlteraApenasOProdutoCorreto() throws SQLException {
        dao.save(buildProduct("Lixa"));
        dao.save(buildProduct("Broca"));

        dao.updateQuantity("Lixa", 99);

        List<Product> products = dao.findAll();
        int lixaQuantity = products.stream()
                .filter(p -> p.getName().equals("Lixa"))
                .findFirst()
                .orElseThrow()
                .getQuantity();
        int brocaQuantity = products.stream()
                .filter(p -> p.getName().equals("Broca"))
                .findFirst()
                .orElseThrow()
                .getQuantity();

        assertEquals(99, lixaQuantity);
        assertEquals(20, brocaQuantity);
    }

    @Test
    void saveComNomeDuplicadoLancaSQLException() throws SQLException {
        dao.save(buildProduct("Serra"));
        Product duplicado = buildProduct("Serra");

        // A constraint UNIQUE em "name" deve rejeitar o segundo insert
        assertThrows(SQLException.class, () -> dao.save(duplicado));
    }

    @Test
    void findAllComBancoVazioRetornaListaVazia() throws SQLException {
        assertEquals(0, dao.findAll().size());
    }

    @Test
    void updateAlteraOsDadosCadastraisSemMexerNaQuantidade() throws SQLException {
        Product product = buildProduct("Lixa");
        dao.save(product);

        product.setName("Lixa d'água");
        product.setCategory("Abrasivos");
        product.setDescription(null);
        product.setPurchasePrice(BigDecimal.valueOf(5));
        product.setSellingPrice(BigDecimal.valueOf(11));
        product.setMinimumStock(9);
        dao.update(product);

        Product loaded = dao.findAll().getFirst();
        assertEquals("Lixa d'água", loaded.getName());
        assertEquals("Abrasivos", loaded.getCategory());
        assertNull(loaded.getDescription());
        assertEquals(0, BigDecimal.valueOf(5).compareTo(loaded.getPurchasePrice()));
        assertEquals(0, BigDecimal.valueOf(11).compareTo(loaded.getSellingPrice()));
        assertEquals(9, loaded.getMinimumStock());
        assertEquals(20, loaded.getQuantity());
    }

    @Test
    void deleteByIdRemoveApenasOProdutoCorreto() throws SQLException {
        Product lixa = buildProduct("Lixa");
        dao.save(lixa);
        dao.save(buildProduct("Broca"));

        dao.deleteById(lixa.getId());

        List<Product> products = dao.findAll();
        assertEquals(1, products.size());
        assertEquals("Broca", products.getFirst().getName());
    }

    @Test
    void registerMovementGravaOHistoricoEAtualizaSaldoECusto() throws SQLException {
        Product product = buildProduct("Trena");
        product.setCategory("Medição");
        dao.save(product);

        StockMovement sale = new StockMovement(
                product, MovementReason.SALE, 3, BigDecimal.valueOf(4.5), BigDecimal.valueOf(9.9), "NF 12");
        dao.registerMovement(sale, 17, BigDecimal.valueOf(4.75));

        Product loaded = dao.findAll().getFirst();
        assertEquals(17, loaded.getQuantity());
        assertEquals(0, BigDecimal.valueOf(4.75).compareTo(loaded.getPurchasePrice()));

        List<StockMovement> movements = dao.findAllMovements();
        assertEquals(1, movements.size());
        StockMovement stored = movements.getFirst();
        assertEquals(sale.getId(), stored.getId());
        assertEquals(product.getId(), stored.getProductId());
        assertEquals("Trena", stored.getProductName());
        assertEquals("Medição", stored.getCategory());
        assertEquals(MovementReason.SALE, stored.getReason());
        assertEquals(3, stored.getQuantity());
        assertEquals(0, BigDecimal.valueOf(4.5).compareTo(stored.getUnitCost()));
        assertEquals(0, BigDecimal.valueOf(9.9).compareTo(stored.getUnitPrice()));
        assertEquals("NF 12", stored.getNote());
        assertEquals(sale.getCreatedAt(), stored.getCreatedAt());
    }

    @Test
    void movimentacaoSemPrecoDeVendaVoltaComPrecoNulo() throws SQLException {
        Product product = buildProduct("Trena");
        dao.save(product);

        dao.registerMovement(
                new StockMovement(product, MovementReason.LOSS, 1, BigDecimal.valueOf(4.5), null, null),
                19, BigDecimal.valueOf(4.5));

        StockMovement stored = dao.findAllMovements().getFirst();
        assertNull(stored.getUnitPrice());
        assertNull(stored.getNote());
    }

    @Test
    void historicoSobreviveAExclusaoDoProduto() throws SQLException {
        Product product = buildProduct("Trena");
        dao.save(product);
        dao.registerMovement(
                new StockMovement(product, MovementReason.SALE, 1, BigDecimal.valueOf(4.5), BigDecimal.TEN, null),
                19, BigDecimal.valueOf(4.5));

        dao.deleteById(product.getId());

        assertEquals(0, dao.findAll().size());
        assertEquals(1, dao.findAllMovements().size());
    }

    @Test
    void bancoDaVersaoAnteriorGanhaCategoriaSemPerderDados() throws SQLException {
        // Recria a tabela como era antes de existirem categorias e movimentações
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE stock_movements");
            statement.execute("DROP TABLE products");
            statement.execute("""
                    CREATE TABLE products (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        description TEXT,
                        purchase_price NUMERIC NOT NULL,
                        selling_price NUMERIC NOT NULL,
                        quantity INTEGER NOT NULL,
                        minimum_stock INTEGER NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO products VALUES
                    ('7f1f3a60-0000-4000-8000-000000000001', 'Antigo', 'Veio da versão 1', 2.5, 5, 8, 3, 1754000000000)
                    """);
        }

        dao.createSchemaIfNotExists();
        dao.createSchemaIfNotExists(); // rodar de novo não pode falhar

        Product loaded = dao.findAll().getFirst();
        assertEquals("Antigo", loaded.getName());
        assertNull(loaded.getCategory());
        assertEquals(8, loaded.getQuantity());
        assertEquals(0, dao.findAllMovements().size());
    }
}

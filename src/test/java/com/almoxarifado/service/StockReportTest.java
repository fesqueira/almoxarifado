package com.almoxarifado.service;

import com.almoxarifado.model.MovementReason;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StockReportTest {

    private static final LocalDateTime OCTOBER = LocalDateTime.of(2026, 10, 5, 10, 0);
    private static final LocalDateTime SEPTEMBER = LocalDateTime.of(2026, 9, 5, 10, 0);

    private Product product(String name, String category, String cost, String price, int quantity) {
        return new Product(UUID.randomUUID(), name, category, null,
                new BigDecimal(cost), new BigDecimal(price), quantity, 1, OCTOBER);
    }

    private StockMovement movement(Product product, MovementReason reason, int quantity, String price, LocalDateTime when) {
        return new StockMovement(UUID.randomUUID(), product.getId(), product.getName(), product.getCategory(),
                reason, quantity, product.getPurchasePrice(), price == null ? null : new BigDecimal(price), null, when);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "esperado " + expected + ", veio " + actual);
    }

    private StockReport.Line line(StockReport report, String category) {
        return report.lines().stream().filter(l -> l.category().equals(category)).findFirst().orElseThrow();
    }

    @Test
    void somaValorDoEstoquePorCategoriaENoTotal() {
        Product broca = product("Broca", "Ferramentas", "4.00", "10.00", 5);
        Product serra = product("Serra", "Ferramentas", "20.00", "35.00", 2);
        Product luva = product("Luva", "EPI", "3.00", "6.00", 10);

        StockReport report = StockReport.of(List.of(broca, serra, luva), List.of(), null, null);

        StockReport.Line tools = line(report, "Ferramentas");
        assertEquals(2, tools.products());
        assertEquals(7, tools.units());
        assertMoney("60.00", tools.stockCost());
        assertMoney("120.00", tools.stockSaleValue());
        assertMoney("60.00", tools.potentialProfit());

        assertEquals(3, report.total().products());
        assertMoney("90.00", report.total().stockCost());
        assertMoney("180.00", report.total().stockSaleValue());
    }

    @Test
    void lucroConsideraApenasVendas() {
        Product broca = product("Broca", "Ferramentas", "4.00", "10.00", 5);
        List<StockMovement> movements = List.of(
                movement(broca, MovementReason.SALE, 3, "10.00", OCTOBER),
                movement(broca, MovementReason.SALE, 1, "8.00", OCTOBER),
                movement(broca, MovementReason.LOSS, 2, null, OCTOBER),
                movement(broca, MovementReason.INTERNAL_USE, 1, null, OCTOBER),
                movement(broca, MovementReason.PURCHASE, 50, null, OCTOBER));

        StockReport.Line total = StockReport.of(List.of(broca), movements, null, null).total();

        assertEquals(4, total.unitsSold());
        assertMoney("38.00", total.revenue());
        assertMoney("16.00", total.costOfSales());
        assertMoney("22.00", total.profit());
        assertMoney("8.00", total.losses());
        assertMoney("57.9", total.marginPercent());
    }

    @Test
    void periodoFiltraAsVendasMasNaoOEstoque() {
        Product broca = product("Broca", "Ferramentas", "4.00", "10.00", 5);
        List<StockMovement> movements = List.of(
                movement(broca, MovementReason.SALE, 3, "10.00", SEPTEMBER),
                movement(broca, MovementReason.SALE, 1, "10.00", OCTOBER));

        StockReport.Line total = StockReport.of(
                List.of(broca), movements, LocalDateTime.of(2026, 10, 1, 0, 0), null).total();

        assertEquals(1, total.unitsSold());
        assertMoney("6.00", total.profit());
        assertMoney("20.00", total.stockCost());
    }

    @Test
    void vendaDeProdutoExcluidoContinuaNoRelatorio() {
        Product removed = product("Trena", "Medição", "5.00", "9.00", 0);
        List<StockMovement> movements = List.of(movement(removed, MovementReason.SALE, 2, "9.00", OCTOBER));

        StockReport report = StockReport.of(List.of(), movements, null, null);

        assertEquals(0, line(report, "Medição").products());
        assertMoney("8.00", line(report, "Medição").profit());
    }

    @Test
    void produtoSemCategoriaEntraEmSemCategoriaESemVendasNaoTemMargem() {
        Product prego = product("Prego", null, "1.00", "2.00", 4);

        StockReport report = StockReport.of(List.of(prego), List.of(), null, null);

        assertEquals(1, line(report, StockReport.NO_CATEGORY).products());
        assertNull(report.total().marginPercent());
    }
}

package com.almoxarifado.service;

import com.almoxarifado.model.MovementReason;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

// Valores do estoque atual e resultado das vendas de um período, por categoria e no total.
public record StockReport(List<Line> lines, Line total) {

    public static final String NO_CATEGORY = "Sem categoria";

    public record Line(
            String category,
            int products,
            int units,
            BigDecimal stockCost,
            BigDecimal stockSaleValue,
            int unitsSold,
            BigDecimal revenue,
            BigDecimal costOfSales,
            BigDecimal losses
    ) {
        public BigDecimal potentialProfit() {
            return stockSaleValue.subtract(stockCost);
        }

        public BigDecimal profit() {
            return revenue.subtract(costOfSales);
        }

        // Margem do lucro sobre a receita, em %. Retorna null quando não houve venda.
        public BigDecimal marginPercent() {
            if (revenue.signum() <= 0) {
                return null;
            }
            return profit().multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP);
        }

        private Line plus(Line other, String name) {
            return new Line(name,
                    products + other.products,
                    units + other.units,
                    stockCost.add(other.stockCost),
                    stockSaleValue.add(other.stockSaleValue),
                    unitsSold + other.unitsSold,
                    revenue.add(other.revenue),
                    costOfSales.add(other.costOfSales),
                    losses.add(other.losses));
        }
    }

    // from/to nulos significam "sem limite"; o estoque é sempre o atual, o período só filtra vendas e perdas.
    public static StockReport of(
            Collection<Product> products,
            Collection<StockMovement> movements,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Map<String, Line> byCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<UUID, String> currentCategory = new HashMap<>();

        for (Product product : products) {
            String category = categoryName(product.getCategory());
            currentCategory.put(product.getId(), category);
            merge(byCategory, new Line(category, 1, product.getQuantity(),
                    product.getStockCost(), product.getStockSaleValue(),
                    0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        for (StockMovement movement : movements) {
            if (from != null && movement.getCreatedAt().isBefore(from)) {
                continue;
            }
            if (to != null && movement.getCreatedAt().isAfter(to)) {
                continue;
            }
            // Se o produto mudou de categoria, a venda acompanha a categoria atual.
            String category = currentCategory.getOrDefault(
                    movement.getProductId(), categoryName(movement.getCategory()));

            if (movement.isSale()) {
                merge(byCategory, new Line(category, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                        movement.getQuantity(), movement.getRevenue(), movement.getTotalCost(), BigDecimal.ZERO));
            } else if (movement.getReason() == MovementReason.LOSS) {
                merge(byCategory, new Line(category, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                        0, BigDecimal.ZERO, BigDecimal.ZERO, movement.getTotalCost()));
            }
        }

        Line total = empty("Total");
        for (Line line : byCategory.values()) {
            total = total.plus(line, "Total");
        }
        return new StockReport(new ArrayList<>(byCategory.values()), total);
    }

    private static void merge(Map<String, Line> byCategory, Line line) {
        byCategory.merge(line.category(), line, (a, b) -> a.plus(b, a.category()));
    }

    private static Line empty(String name) {
        return new Line(name, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static String categoryName(String category) {
        return category == null || category.isBlank() ? NO_CATEGORY : category.trim();
    }
}

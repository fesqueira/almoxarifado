package com.almoxarifado.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class StockMovement {
    private final UUID id;
    private final UUID productId;
    // Nome e categoria são copiados do produto para o histórico sobreviver à exclusão dele.
    private final String productName;
    private final String category;
    private final MovementReason reason;
    private final int quantity;
    private final BigDecimal unitCost;
    private final BigDecimal unitPrice;
    private final String note;
    private final LocalDateTime createdAt;

    public StockMovement(
            Product product,
            MovementReason reason,
            int quantity,
            BigDecimal unitCost,
            BigDecimal unitPrice,
            String note
    ) {
        this(UUID.randomUUID(), product.getId(), product.getName(), product.getCategory(),
                reason, quantity, unitCost, unitPrice, note, LocalDateTime.now().withNano(0));
    }

    public StockMovement(
            UUID id,
            UUID productId,
            String productName,
            String category,
            MovementReason reason,
            int quantity,
            BigDecimal unitCost,
            BigDecimal unitPrice,
            String note,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.reason = reason;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.unitPrice = unitPrice;
        this.note = note;
        this.createdAt = createdAt;
    }

    public boolean isSale() {
        return reason == MovementReason.SALE;
    }

    public BigDecimal getTotalCost() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    // Receita só existe em vendas; as demais saídas não geram dinheiro.
    public BigDecimal getRevenue() {
        if (!isSale() || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getProfit() {
        if (!isSale()) {
            return BigDecimal.ZERO;
        }
        return getRevenue().subtract(getTotalCost());
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public MovementType getType() {
        return reason.getType();
    }

    public MovementReason getReason() {
        return reason;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

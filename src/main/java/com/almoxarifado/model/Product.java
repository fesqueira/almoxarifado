package com.almoxarifado.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public class Product {
    private final UUID id;
    private String name;
    private String category;
    private String description;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private int quantity;
    private int minimumStock;
    private final LocalDateTime createdAt;

    public Product() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now().withNano(0);
    }

    public Product(
            String name,
            String description,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            int quantity,
            int minimumStock
    ) {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now().withNano(0);

        this.name = name;
        this.description = description;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.minimumStock = minimumStock;
    }

    public Product(
            UUID id,
            String name,
            String description,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            int quantity,
            int minimumStock,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.minimumStock = minimumStock;
        this.createdAt = createdAt;
    }

    public Product(
            UUID id,
            String name,
            String category,
            String description,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            int quantity,
            int minimumStock,
            LocalDateTime createdAt
    ) {
        this(id, name, description, purchasePrice, sellingPrice, quantity, minimumStock, createdAt);
        this.category = category;
    }


    public void addStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Você não pode adicionar números negativos."
            );
        }
        this.quantity += amount;
    }

    public void removeStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Você não pode subtrair números negativos!"
            );
        }
        if (amount > this.quantity) {
            throw new IllegalStateException(
                    "Não é possível remover " + amount + " unidades. Apenas " + this.quantity + " disponiveis."
            );
        }
        this.quantity -= amount;
    }

    public boolean isOutOfStock() {
        return this.quantity <= 0;
    }

    public boolean isLowStock() {
        return this.quantity <= this.minimumStock;
    }

    public BigDecimal getStockCost() {
        return this.purchasePrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    public BigDecimal getStockSaleValue() {
        return this.sellingPrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    public BigDecimal getUnitProfit() {
        return this.sellingPrice.subtract(this.purchasePrice);
    }

    // Margem sobre o preço de venda, em %. Retorna null quando não há preço de venda.
    public BigDecimal getMarginPercent() {
        if (this.sellingPrice.signum() <= 0) {
            return null;
        }
        return getUnitProfit()
                .multiply(BigDecimal.valueOf(100))
                .divide(this.sellingPrice, 1, RoundingMode.HALF_UP);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }

    public String getDescription() {
        return this.description;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public BigDecimal getPurchasePrice() {
        return this.purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return this.sellingPrice;
    }

    public int getMinimumStock() {
        return this.minimumStock;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

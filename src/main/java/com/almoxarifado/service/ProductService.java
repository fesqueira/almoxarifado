package com.almoxarifado.service;

import com.almoxarifado.database.ProductRepository;
import com.almoxarifado.model.MovementReason;
import com.almoxarifado.model.MovementType;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;


public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }


    public List<Product> getProducts() throws SQLException {
        return productRepository.findAll();
    }

    public List<StockMovement> getMovements() throws SQLException {
        return productRepository.findAllMovements();
    }

    public List<String> getCategories() throws SQLException {
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Product product : productRepository.findAll()) {
            if (product.getCategory() != null && !product.getCategory().isBlank()) {
                categories.add(product.getCategory());
            }
        }
        return List.copyOf(categories);
    }

    public boolean verifyExists(String name) throws SQLException {
        return productRepository.existsByName(name);
    }

    public Product registerProduct(
            String name,
            String description,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            int quantity,
            int minimumStock) throws SQLException {

        return registerProduct(name, null, description, purchasePrice, sellingPrice, quantity, minimumStock);
    }

    public Product registerProduct(
            String name,
            String category,
            String description,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            int quantity,
            int minimumStock) throws SQLException {

        validate(name, purchasePrice, sellingPrice, minimumStock);
        if (quantity < 0) {
            throw new IllegalArgumentException("A quantidade inicial não pode ser negativa.");
        }
        if (findByName(name.trim()) != null) {
            throw new IllegalArgumentException("Já existe um produto chamado \"" + name.trim() + "\".");
        }

        // O produto nasce zerado; o estoque inicial entra como movimentação para aparecer no histórico.
        Product product = new Product(
                name.trim(),
                clean(description),
                purchasePrice,
                sellingPrice,
                0,
                minimumStock);
        product.setCategory(clean(category));

        productRepository.save(product);

        if (quantity > 0) {
            product.addStock(quantity);
            productRepository.registerMovement(
                    new StockMovement(product, MovementReason.INITIAL_STOCK, quantity, purchasePrice, null, null),
                    product.getQuantity(),
                    purchasePrice);
        }
        return product;
    }

    public Product updateProduct(
            UUID id,
            String name,
            String category,
            String description,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            int minimumStock) throws SQLException {

        validate(name, purchasePrice, sellingPrice, minimumStock);

        Product product = findById(id);
        Product sameName = findByName(name.trim());
        if (sameName != null && !sameName.getId().equals(id)) {
            throw new IllegalArgumentException("Já existe um produto chamado \"" + name.trim() + "\".");
        }

        product.setName(name.trim());
        product.setCategory(clean(category));
        product.setDescription(clean(description));
        product.setPurchasePrice(purchasePrice);
        product.setSellingPrice(sellingPrice);
        product.setMinimumStock(minimumStock);

        productRepository.update(product);
        return product;
    }

    // unitCost só é usado em compras; nos demais motivos vale o custo atual do produto.
    public Product registerEntry(
            UUID productId,
            int amount,
            MovementReason reason,
            BigDecimal unitCost,
            String note) throws SQLException {

        requireType(reason, MovementType.ENTRY);
        Product product = findById(productId);

        BigDecimal cost = product.getPurchasePrice();
        BigDecimal newPurchasePrice = cost;
        if (reason == MovementReason.PURCHASE && unitCost != null) {
            if (unitCost.signum() < 0) {
                throw new IllegalArgumentException("O custo unitário não pode ser negativo.");
            }
            cost = unitCost;
            newPurchasePrice = averageCost(product, amount, unitCost);
        }

        product.addStock(amount);
        productRepository.registerMovement(
                new StockMovement(product, reason, amount, cost, null, clean(note)),
                product.getQuantity(),
                newPurchasePrice);
        product.setPurchasePrice(newPurchasePrice);
        return product;
    }

    // unitPrice só é usado em vendas; quando nulo vale o preço de venda do cadastro.
    public Product registerExit(
            UUID productId,
            int amount,
            MovementReason reason,
            BigDecimal unitPrice,
            String note) throws SQLException {

        requireType(reason, MovementType.EXIT);
        Product product = findById(productId);

        BigDecimal price = null;
        if (reason == MovementReason.SALE) {
            price = unitPrice != null ? unitPrice : product.getSellingPrice();
            if (price.signum() < 0) {
                throw new IllegalArgumentException("O preço de venda não pode ser negativo.");
            }
        }

        product.removeStock(amount);
        productRepository.registerMovement(
                new StockMovement(product, reason, amount, product.getPurchasePrice(), price, clean(note)),
                product.getQuantity(),
                product.getPurchasePrice());
        return product;
    }

    public void increaseAmount(String name, int amount) throws SQLException {
        Product product = requireByName(name);
        registerEntry(product.getId(), amount, MovementReason.ADJUSTMENT_IN, null, null);
    }

    public void decreaseAmount(String name, int amount) throws SQLException {
        Product product = requireByName(name);
        registerExit(product.getId(), amount, MovementReason.ADJUSTMENT_OUT, null, null);
    }

    public void deleteProduct(String name) throws SQLException {
        deleteProduct(requireByName(name).getId());
    }

    public void deleteProduct(UUID id) throws SQLException {
        productRepository.deleteById(findById(id).getId());
    }

    // Custo médio ponderado entre o que já estava em estoque e o que está chegando.
    public static BigDecimal averageCost(Product product, int amount, BigDecimal unitCost) {
        if (amount <= 0 || product.getQuantity() <= 0) {
            return unitCost;
        }
        BigDecimal total = product.getStockCost().add(unitCost.multiply(BigDecimal.valueOf(amount)));
        return total.divide(BigDecimal.valueOf((long) product.getQuantity() + amount), 2, RoundingMode.HALF_UP);
    }

    private void validate(String name, BigDecimal purchasePrice, BigDecimal sellingPrice, int minimumStock) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto.");
        }
        if (purchasePrice == null || purchasePrice.signum() < 0) {
            throw new IllegalArgumentException("Informe um preço de custo válido.");
        }
        if (sellingPrice == null || sellingPrice.signum() < 0) {
            throw new IllegalArgumentException("Informe um preço de venda válido.");
        }
        if (minimumStock < 0) {
            throw new IllegalArgumentException("O estoque mínimo não pode ser negativo.");
        }
    }

    private void requireType(MovementReason reason, MovementType type) {
        if (reason == null || reason.getType() != type) {
            throw new IllegalArgumentException("Motivo inválido para " + type.getLabel().toLowerCase() + ".");
        }
    }

    private String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private Product findById(UUID id) throws SQLException {
        for (Product product : productRepository.findAll()) {
            if (product.getId().equals(id)) {
                return product;
            }
        }
        throw new IllegalArgumentException("Produto não encontrado.");
    }

    private Product requireByName(String name) throws SQLException {
        Product product = name == null ? null : findByName(name.trim());
        if (product == null) {
            throw new IllegalArgumentException("Produto não encontrado.");
        }
        return product;
    }

    private Product findByName(String name) throws SQLException {
        for (Product product : productRepository.findAll()) {
            if (product.getName().equalsIgnoreCase(name)) {
                return product;
            }
        }
        return null;
    }
}

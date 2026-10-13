package com.almoxarifado.database;

import com.almoxarifado.model.MovementReason;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractProductDao implements ProductRepository{

    protected abstract Connection getConnection() throws SQLException;


    public void save(Product product) throws SQLException {

        String sql = """
                INSERT INTO products
                (id, name, description, purchase_price, selling_price,
                 quantity, minimum_stock, created_at, category)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, product.getId());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setBigDecimal(4, product.getPurchasePrice());
            statement.setBigDecimal(5, product.getSellingPrice());
            statement.setInt(6, product.getQuantity());
            statement.setInt(7, product.getMinimumStock());
            statement.setTimestamp(
                    8,
                    Timestamp.valueOf(product.getCreatedAt())
            );
            statement.setString(9, product.getCategory());

            statement.executeUpdate();
        }
    }

    public boolean existsByName(String name) throws SQLException {

        String sql = """
                SELECT 1
                FROM products
                WHERE name = ?
                LIMIT 1
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<Product> findAll() throws SQLException {

        String sql = """
                SELECT id, name, category, description, purchase_price,
                       selling_price, quantity, minimum_stock, created_at
                FROM products
                ORDER BY name
                """;

        List<Product> products = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Product product = new Product(
                        getUUID(resultSet, "id"),
                        resultSet.getString("name"),
                        resultSet.getString("category"),
                        resultSet.getString("description"),
                        resultSet.getBigDecimal("purchase_price"),
                        resultSet.getBigDecimal("selling_price"),
                        resultSet.getInt("quantity"),
                        resultSet.getInt("minimum_stock"),
                        getCreatedAt(resultSet)
                );

                products.add(product);
            }
        }

        return products;
    }

    public void updateQuantity(String name, int newQuantity)
            throws SQLException {

        String sql = """
                UPDATE products
                SET quantity = ?
                WHERE name = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, newQuantity);
            statement.setString(2, name);

            statement.executeUpdate();
        }
    }

    public void deleteByName(String name) throws SQLException {

        String sql = """
                DELETE FROM products
                WHERE name = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);

            statement.executeUpdate();
        }
    }

    public void update(Product product) throws SQLException {

        String sql = """
                UPDATE products
                SET name = ?, category = ?, description = ?,
                    purchase_price = ?, selling_price = ?, minimum_stock = ?
                WHERE id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, product.getName());
            statement.setString(2, product.getCategory());
            statement.setString(3, product.getDescription());
            statement.setBigDecimal(4, product.getPurchasePrice());
            statement.setBigDecimal(5, product.getSellingPrice());
            statement.setInt(6, product.getMinimumStock());
            statement.setObject(7, product.getId());

            statement.executeUpdate();
        }
    }

    public void deleteById(UUID id) throws SQLException {

        String sql = """
                DELETE FROM products
                WHERE id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            statement.executeUpdate();
        }
    }

    public void registerMovement(StockMovement movement, int newQuantity, BigDecimal newPurchasePrice)
            throws SQLException {

        String insertSql = """
                INSERT INTO stock_movements
                (id, product_id, product_name, category, reason,
                 quantity, unit_cost, unit_price, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String updateSql = """
                UPDATE products
                SET quantity = ?, purchase_price = ?
                WHERE id = ?
                """;

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement insert = connection.prepareStatement(insertSql);
                 PreparedStatement update = connection.prepareStatement(updateSql)) {

                insert.setObject(1, movement.getId());
                insert.setObject(2, movement.getProductId());
                insert.setString(3, movement.getProductName());
                insert.setString(4, movement.getCategory());
                insert.setString(5, movement.getReason().name());
                insert.setInt(6, movement.getQuantity());
                insert.setBigDecimal(7, movement.getUnitCost());
                if (movement.getUnitPrice() == null) {
                    insert.setNull(8, Types.NUMERIC);
                } else {
                    insert.setBigDecimal(8, movement.getUnitPrice());
                }
                insert.setString(9, movement.getNote());
                insert.setTimestamp(10, Timestamp.valueOf(movement.getCreatedAt()));
                insert.executeUpdate();

                update.setInt(1, newQuantity);
                update.setBigDecimal(2, newPurchasePrice);
                update.setObject(3, movement.getProductId());
                update.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public List<StockMovement> findAllMovements() throws SQLException {

        String sql = """
                SELECT id, product_id, product_name, category, reason,
                       quantity, unit_cost, unit_price, note, created_at
                FROM stock_movements
                ORDER BY created_at DESC
                """;

        List<StockMovement> movements = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                StockMovement movement = new StockMovement(
                        getUUID(resultSet, "id"),
                        getUUID(resultSet, "product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("category"),
                        MovementReason.valueOf(resultSet.getString("reason")),
                        resultSet.getInt("quantity"),
                        resultSet.getBigDecimal("unit_cost"),
                        resultSet.getBigDecimal("unit_price"),
                        resultSet.getString("note"),
                        getCreatedAt(resultSet)
                );

                movements.add(movement);
            }
        }

        return movements;
    }

    protected abstract UUID getUUID(ResultSet resultSet, String column)
        throws SQLException;

    protected abstract LocalDateTime getCreatedAt(ResultSet resultSet)
            throws SQLException;
}
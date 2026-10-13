package com.almoxarifado.database.sqlite;

import com.almoxarifado.database.AbstractProductDao;
import com.almoxarifado.database.ProductRepository;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;


public class ProductDaoSQLite extends AbstractProductDao{

    @Override
    protected Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    public void createSchemaIfNotExists() throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS products (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                purchase_price NUMERIC NOT NULL,
                selling_price NUMERIC NOT NULL,
                quantity INTEGER NOT NULL,
                minimum_stock INTEGER NOT NULL,
                created_at TEXT NOT NULL
            )
            """;

        String movementsSql = """
            CREATE TABLE IF NOT EXISTS stock_movements (
                id TEXT PRIMARY KEY,
                product_id TEXT NOT NULL,
                product_name TEXT NOT NULL,
                category TEXT,
                reason TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                unit_cost NUMERIC NOT NULL,
                unit_price NUMERIC,
                note TEXT,
                created_at TEXT NOT NULL
            )
            """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            // Bancos criados antes da versão com categorias não têm a coluna.
            if (!hasColumn(statement, "products", "category")) {
                statement.execute("ALTER TABLE products ADD COLUMN category TEXT");
            }
            statement.execute(movementsSql);
        }
    }

    private boolean hasColumn(Statement statement, String table, String column) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    protected LocalDateTime getCreatedAt(ResultSet resultSet)
            throws SQLException {

        long timestamp = resultSet.getLong("created_at");

        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
    @Override
    protected UUID getUUID(ResultSet resultSet, String column)
            throws SQLException {

        String value = resultSet.getString(column);

        return UUID.fromString(value);
    }
}

package com.almoxarifado.database.postgres;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

import com.almoxarifado.database.AbstractProductDao;
import com.almoxarifado.database.ProductRepository;


public class ProductDaoPostgres extends AbstractProductDao {

    @Override
    protected Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    public void createSchemaIfNotExists() throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS products (
                id UUID PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                purchase_price NUMERIC(12,2) NOT NULL,
                selling_price NUMERIC(12,2) NOT NULL,
                quantity INTEGER NOT NULL,
                minimum_stock INTEGER NOT NULL,
                created_at TIMESTAMP NOT NULL
            )
            """;

        String movementsSql = """
            CREATE TABLE IF NOT EXISTS stock_movements (
                id UUID PRIMARY KEY,
                product_id UUID NOT NULL,
                product_name VARCHAR(255) NOT NULL,
                category VARCHAR(255),
                reason VARCHAR(32) NOT NULL,
                quantity INTEGER NOT NULL,
                unit_cost NUMERIC(12,2) NOT NULL,
                unit_price NUMERIC(12,2),
                note TEXT,
                created_at TIMESTAMP NOT NULL
            )
            """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            // Bancos criados antes da versão com categorias não têm a coluna.
            statement.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS category VARCHAR(255)");
            statement.execute(movementsSql);
        }
    }
    @Override
    protected LocalDateTime getCreatedAt(ResultSet resultSet)
            throws SQLException {

        return resultSet
                .getTimestamp("created_at")
                .toLocalDateTime();
    }
    @Override
    protected UUID getUUID(ResultSet resultSet, String column)
            throws SQLException {

        Object value = resultSet.getObject(column);

        if (value instanceof UUID uuid) {
            return uuid;
        }

        return UUID.fromString(value.toString());
    }
}
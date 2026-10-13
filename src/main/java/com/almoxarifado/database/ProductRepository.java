package com.almoxarifado.database;

import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;


public interface ProductRepository {
    void save(Product product) throws SQLException;

    boolean existsByName(String name) throws SQLException;

    List<Product> findAll() throws SQLException;

    // Atualiza os dados cadastrais; a quantidade só muda por movimentação.
    void update(Product product) throws SQLException;

    void updateQuantity(String name, int newQuantity) throws SQLException;

    void deleteByName(String name) throws SQLException;

    void deleteById(UUID id) throws SQLException;

    // Grava a movimentação e o novo saldo/custo do produto na mesma transação.
    void registerMovement(StockMovement movement, int newQuantity, BigDecimal newPurchasePrice)
            throws SQLException;

    List<StockMovement> findAllMovements() throws SQLException;
}

package com.almoxarifado;

import com.almoxarifado.database.postgres.ProductDaoPostgres;
import com.almoxarifado.service.ProductService;
import com.almoxarifado.ui.ProductGUI;
import com.almoxarifado.ui.Theme;

import javax.swing.SwingUtilities;


public class Main {
    public static void main(String[] args) {
        Theme.setup();
        try {
            ProductDaoPostgres productDao = new ProductDaoPostgres();
            productDao.createSchemaIfNotExists();

            ProductService productService = new ProductService(productDao);

            SwingUtilities.invokeLater(() -> new ProductGUI(productService).setVisible(true));
        } catch (Exception e) {
            ProductGUI.showStartupError(e);
            System.exit(1);
        }
    }
}

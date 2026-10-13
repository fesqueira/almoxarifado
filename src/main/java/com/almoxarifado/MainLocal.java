package com.almoxarifado;

import com.almoxarifado.database.sqlite.ProductDaoSQLite;
import com.almoxarifado.service.ProductService;
import com.almoxarifado.ui.ProductGUI;
import com.almoxarifado.ui.Theme;

import javax.swing.SwingUtilities;


public class MainLocal {
    public static void main(String[] args) {
        Theme.setup();
        try {
            ProductDaoSQLite productDao = new ProductDaoSQLite();
            productDao.createSchemaIfNotExists();

            ProductService productService = new ProductService(productDao);

            SwingUtilities.invokeLater(() -> new ProductGUI(productService).setVisible(true));
        } catch (Exception e) {
            ProductGUI.showStartupError(e);
            System.exit(1);
        }
    }
}

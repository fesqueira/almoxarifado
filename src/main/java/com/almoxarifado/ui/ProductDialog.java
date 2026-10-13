package com.almoxarifado.ui;

import com.almoxarifado.model.Product;
import com.almoxarifado.service.ProductService;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

// Formulário de cadastro e de edição de produto.
class ProductDialog extends FormDialog {

    private final ProductService service;
    private final Product editing;
    private final JTextField name = new JTextField();
    private final JComboBox<String> category = new JComboBox<>();
    private final JTextField description = new JTextField();
    private final JTextField purchasePrice = new JTextField();
    private final JTextField sellingPrice = new JTextField();
    private final JTextField quantity = new JTextField("0");
    private final JTextField minimumStock = new JTextField("0");
    private final JLabel margin = hint(" ");

    ProductDialog(Window owner, ProductService service, Product editing, List<String> categories) {
        super(owner,
                editing == null ? "Novo produto" : "Editar produto",
                editing == null ? "Preencha os dados do produto. Campos com * são obrigatórios."
                        : "Altere os dados e salve. O estoque muda por entradas e saídas.",
                editing == null ? "Cadastrar produto" : "Salvar alterações");
        this.service = service;
        this.editing = editing;

        category.setEditable(true);
        category.addItem("");
        categories.forEach(category::addItem);
        name.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex.: Parafuso sextavado M6");
        description.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Opcional");
        purchasePrice.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0,00");
        sellingPrice.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0,00");
        ((JTextComponent) category.getEditor().getEditorComponent())
                .putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Escolha ou digite uma nova");

        addRow(field("Nome do produto *", name));
        addRow(field("Categoria", category), field("Descrição", description));
        addRow(field("Preço de custo (R$) *", purchasePrice), field("Preço de venda (R$) *", sellingPrice));
        addRow(margin);

        if (editing == null) {
            addRow(field("Quantidade inicial em estoque", quantity),
                    field("Estoque mínimo (aviso de baixa)", minimumStock));
        } else {
            quantity.setText(String.valueOf(editing.getQuantity()));
            quantity.setEnabled(false);
            addRow(field("Estoque atual", quantity), field("Estoque mínimo (aviso de baixa)", minimumStock));

            name.setText(editing.getName());
            category.setSelectedItem(editing.getCategory() == null ? "" : editing.getCategory());
            description.setText(editing.getDescription() == null ? "" : editing.getDescription());
            purchasePrice.setText(Theme.moneyInput(editing.getPurchasePrice()));
            sellingPrice.setText(Theme.moneyInput(editing.getSellingPrice()));
            minimumStock.setText(String.valueOf(editing.getMinimumStock()));
        }
        addRow(hint("O produto fica sinalizado quando o estoque chega ao mínimo ou abaixo dele."));

        onChange(purchasePrice, this::updateMargin);
        onChange(sellingPrice, this::updateMargin);
        updateMargin();
    }

    private void updateMargin() {
        try {
            BigDecimal cost = Theme.parseMoney(purchasePrice.getText(), "");
            BigDecimal price = Theme.parseMoney(sellingPrice.getText(), "");
            BigDecimal profit = price.subtract(cost);
            if (profit.signum() < 0) {
                margin.setForeground(Theme.AMBER);
                margin.setText("Atenção: venda abaixo do custo (prejuízo de "
                        + Theme.money(profit.negate()) + " por unidade).");
            } else {
                margin.setForeground(Theme.GREEN);
                String percent = price.signum() == 0 ? "" : " · margem de " + Theme.percent(
                        profit.multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP));
                margin.setText("Lucro por unidade: " + Theme.money(profit) + percent);
            }
        } catch (IllegalArgumentException e) {
            margin.setForeground(Theme.MUTED);
            margin.setText("Informe custo e venda para ver o lucro por unidade.");
        }
    }

    @Override
    protected void confirm() throws SQLException {
        BigDecimal cost = Theme.parseMoney(purchasePrice.getText(), "Preço de custo");
        BigDecimal price = Theme.parseMoney(sellingPrice.getText(), "Preço de venda");
        int minimum = Theme.parseQuantity(minimumStock.getText(), "Estoque mínimo");
        Object chosen = category.getEditor().getItem();
        String categoryName = chosen == null ? null : chosen.toString();

        if (editing == null) {
            int initial = Theme.parseQuantity(quantity.getText(), "Quantidade inicial");
            service.registerProduct(name.getText(), categoryName, description.getText(), cost, price, initial, minimum);
        } else {
            service.updateProduct(editing.getId(), name.getText(), categoryName, description.getText(),
                    cost, price, minimum);
        }
    }
}

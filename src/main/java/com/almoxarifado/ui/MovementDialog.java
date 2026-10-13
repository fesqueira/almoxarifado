package com.almoxarifado.ui;

import com.almoxarifado.model.MovementReason;
import com.almoxarifado.model.MovementType;
import com.almoxarifado.model.Product;
import com.almoxarifado.service.ProductService;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

// Formulário de entrada ou saída de estoque.
class MovementDialog extends FormDialog {

    private final ProductService service;
    private final MovementType type;
    private final JComboBox<Product> product = new JComboBox<>();
    private final JComboBox<MovementReason> reason = new JComboBox<>();
    private final JTextField quantity = new JTextField();
    private final JTextField unitValue = new JTextField();
    private final JLabel unitValueLabel = Theme.label("", Font.PLAIN, 13.5f, Theme.MUTED);
    private final JPanel unitValueField = field(unitValueLabel, unitValue);
    private final JTextField note = new JTextField();
    private final JLabel stockLine = Theme.label(" ", Font.BOLD, 15f, Theme.INK);
    private final JLabel valueLine = Theme.label(" ", Font.PLAIN, 13.5f, Theme.MUTED);

    MovementDialog(Window owner, ProductService service, MovementType type, List<Product> products, Product preselected) {
        super(owner,
                type == MovementType.ENTRY ? "Registrar entrada" : "Registrar saída",
                type == MovementType.ENTRY ? "Produtos que chegaram ao estoque."
                        : "Produtos que saíram do estoque, por venda ou outro motivo.",
                type == MovementType.ENTRY ? "Registrar entrada" : "Registrar saída");
        this.service = service;
        this.type = type;

        products.forEach(product::addItem);
        product.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                Object text = value instanceof Product p
                        ? p.getName() + "   (" + Theme.number(p.getQuantity()) + " em estoque)"
                        : value;
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        if (preselected != null) {
            for (Product candidate : products) {
                if (candidate.getId().equals(preselected.getId())) {
                    product.setSelectedItem(candidate);
                }
            }
        }
        MovementReason.selectableFor(type).forEach(reason::addItem);
        quantity.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex.: 10");
        note.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Opcional. Ex.: nota fiscal, cliente, fornecedor");

        RoundedPanel summary = new RoundedPanel(new GridLayout(2, 1, 0, Theme.px(4)), 12);
        summary.setBackground(Theme.NAVY);
        summary.setBorder(Theme.pad(12, 16, 12, 16));
        summary.add(stockLine);
        summary.add(valueLine);

        addRow(field("Produto *", product));
        addRow(field("Motivo *", reason), field("Quantidade *", quantity));
        addRow(unitValueField);
        addRow(field("Observação", note));
        addRow(summary);

        product.addActionListener(e -> productChanged());
        reason.addActionListener(e -> productChanged());
        onChange(quantity, this::updateSummary);
        onChange(unitValue, this::updateSummary);
        productChanged();
    }

    private Product product() {
        return (Product) product.getSelectedItem();
    }

    private MovementReason reason() {
        return (MovementReason) reason.getSelectedItem();
    }

    // Compras pedem o custo pago e vendas o preço cobrado; os demais motivos não envolvem valor.
    private void productChanged() {
        MovementReason chosen = reason();
        boolean purchase = chosen == MovementReason.PURCHASE;
        boolean sale = chosen == MovementReason.SALE;
        if (purchase) {
            unitValueLabel.setText("Custo por unidade nesta compra (R$) *");
            unitValue.setText(Theme.moneyInput(product().getPurchasePrice()));
        } else if (sale) {
            unitValueLabel.setText("Preço de venda por unidade (R$) *");
            unitValue.setText(Theme.moneyInput(product().getSellingPrice()));
        }
        boolean wasVisible = unitValueField.getParent().isVisible();
        unitValueField.getParent().setVisible(purchase || sale);
        if (wasVisible != (purchase || sale) && isDisplayable()) {
            layoutDialog();
        }
        updateSummary();
    }

    private void updateSummary() {
        Product chosen = product();
        MovementReason chosenReason = reason();
        int amount;
        try {
            amount = Theme.parseQuantity(quantity.getText(), "");
        } catch (IllegalArgumentException e) {
            amount = 0;
        }
        int after = type == MovementType.ENTRY ? chosen.getQuantity() + amount : chosen.getQuantity() - amount;

        stockLine.setForeground(Theme.INK);
        stockLine.setText("Estoque: " + Theme.number(chosen.getQuantity()) + "  →  " + Theme.number(after));
        valueLine.setForeground(Theme.MUTED);
        valueLine.setText(" ");

        if (after < 0) {
            stockLine.setForeground(Theme.RED);
            stockLine.setText("Só há " + Theme.number(chosen.getQuantity()) + " unidade(s) em estoque.");
            return;
        }
        if (type == MovementType.EXIT && amount > 0 && after <= chosen.getMinimumStock()) {
            valueLine.setForeground(Theme.AMBER);
            valueLine.setText("O produto ficará em baixa (mínimo: " + Theme.number(chosen.getMinimumStock()) + ").");
        }
        if (amount == 0) {
            valueLine.setText("Informe a quantidade para ver o resultado.");
            return;
        }

        BigDecimal units = BigDecimal.valueOf(amount);
        try {
            if (chosenReason == MovementReason.PURCHASE) {
                BigDecimal cost = Theme.parseMoney(unitValue.getText(), "");
                valueLine.setForeground(Theme.MUTED);
                valueLine.setText("Total da compra: " + Theme.money(cost.multiply(units))
                        + "  ·  Novo custo médio: " + Theme.money(ProductService.averageCost(chosen, amount, cost)));
            } else if (chosenReason == MovementReason.SALE) {
                BigDecimal price = Theme.parseMoney(unitValue.getText(), "");
                BigDecimal profit = price.subtract(chosen.getPurchasePrice()).multiply(units);
                valueLine.setForeground(profit.signum() < 0 ? Theme.RED : Theme.GREEN);
                valueLine.setText("Total da venda: " + Theme.money(price.multiply(units))
                        + "  ·  " + (profit.signum() < 0 ? "Prejuízo: " : "Lucro: ") + Theme.money(profit.abs()));
            } else if (chosenReason == MovementReason.LOSS) {
                valueLine.setText("Perda de " + Theme.money(chosen.getPurchasePrice().multiply(units)) + " em custo.");
            }
        } catch (IllegalArgumentException e) {
            // Valor ainda incompleto: o resumo fica só com o estoque.
        }
    }

    @Override
    protected void confirm() throws SQLException {
        Product chosen = product();
        MovementReason chosenReason = reason();
        int amount = Theme.parseQuantity(quantity.getText(), "Quantidade");
        if (amount == 0) {
            throw new IllegalArgumentException("Quantidade: informe um número maior que zero.");
        }

        if (type == MovementType.ENTRY) {
            BigDecimal cost = chosenReason == MovementReason.PURCHASE
                    ? Theme.parseMoney(unitValue.getText(), "Custo por unidade") : null;
            service.registerEntry(chosen.getId(), amount, chosenReason, cost, note.getText());
        } else {
            BigDecimal price = chosenReason == MovementReason.SALE
                    ? Theme.parseMoney(unitValue.getText(), "Preço de venda") : null;
            service.registerExit(chosen.getId(), amount, chosenReason, price, note.getText());
        }
    }
}

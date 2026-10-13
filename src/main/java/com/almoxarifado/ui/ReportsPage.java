package com.almoxarifado.ui;

import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;
import com.almoxarifado.service.StockReport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Valor do estoque e lucro das vendas, no total e por categoria.
class ReportsPage extends JPanel implements AppShell.Page {

    private final JComboBox<Period> period = new JComboBox<>(Period.values());
    private final Kpi stockCost = new Kpi("Valor do estoque (custo)", "layers");
    private final Kpi stockSale = new Kpi("Valor do estoque (preço de venda)", "box");
    private final Kpi potential = new Kpi("Lucro potencial do estoque", "chart");
    private final Kpi revenue = new Kpi("Receita de vendas", "money");
    private final Kpi costOfSales = new Kpi("Custo dos produtos vendidos", "out");
    private final Kpi profit = new Kpi("Lucro obtido com vendas", "check");
    private final JLabel salesTitle = sectionTitle("");
    private final Model model = new Model();
    private final JTable table = new JTable(model);

    private List<Product> products = List.of();
    private List<StockMovement> movements = List.of();

    ReportsPage() {
        super(new BorderLayout());

        period.setPreferredSize(Theme.size(200, 40));
        period.addActionListener(e -> update());
        JLabel periodLabel = Theme.label("Vendas de:", Font.PLAIN, 14f, Theme.MUTED);

        add(AppShell.header("Financeiro", "Quanto vale o estoque e quanto as vendas renderam.",
                periodLabel, period), BorderLayout.NORTH);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(left(sectionTitle("ESTOQUE ATUAL")));
        top.add(Box.createVerticalStrut(Theme.px(8)));
        top.add(row(stockCost, stockSale, potential));
        top.add(Box.createVerticalStrut(Theme.px(16)));
        top.add(left(salesTitle));
        top.add(Box.createVerticalStrut(Theme.px(8)));
        top.add(row(revenue, costOfSales, profit));
        top.add(Box.createVerticalStrut(Theme.px(16)));
        top.add(left(sectionTitle("POR CATEGORIA")));
        top.add(Box.createVerticalStrut(Theme.px(8)));

        configureTable();
        RoundedPanel card = new RoundedPanel(new BorderLayout(), 14);
        card.setBorder(Theme.pad(4, 1, 6, 1));
        card.add(Cells.styledScroll(table), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(card, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private static JLabel sectionTitle(String text) {
        return Theme.label(text, Font.BOLD, 12.5f, Theme.PINK);
    }

    private static JComponent left(JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(component, BorderLayout.WEST);
        return panel;
    }

    private static JPanel row(JComponent... cards) {
        JPanel row = new JPanel(new GridLayout(1, cards.length, Theme.px(16), 0));
        row.setOpaque(false);
        for (JComponent card : cards) {
            row.add(card);
        }
        return row;
    }

    private void configureTable() {
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);

        Cells.Text money = new Cells.Text(SwingConstants.RIGHT, v -> Theme.money((BigDecimal) v));
        Cells.Text signedMoney = new Cells.Text(SwingConstants.RIGHT, v -> Theme.money((BigDecimal) v),
                v -> ((BigDecimal) v).signum() < 0 ? Theme.RED : ((BigDecimal) v).signum() > 0 ? Theme.GREEN : null);
        Cells.Text integer = new Cells.Text(SwingConstants.RIGHT, v -> Theme.number((Integer) v));

        int[] widths = {155, 75, 145, 145, 160, 105, 145, 140, 95};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
            table.getColumnModel().getColumn(column).setCellRenderer(switch (column) {
                case 0 -> new Cells.Text(SwingConstants.LEFT, Object::toString);
                case 1, 5 -> integer;
                case 4, 7 -> signedMoney;
                case 8 -> new Cells.Text(SwingConstants.RIGHT, v -> Theme.percent((BigDecimal) v));
                default -> money;
            });
        }
        Cells.alignHeaders(table, 1, 2, 3, 4, 5, 6, 7, 8);
        table.setRowHeight(Theme.px(42));
    }

    @Override
    public void refresh(List<Product> products, List<StockMovement> movements) {
        this.products = products;
        this.movements = movements;
        update();
    }

    private void update() {
        Period chosen = (Period) period.getSelectedItem();
        StockReport report = StockReport.of(products, movements, chosen.from(), null);
        StockReport.Line total = report.total();

        stockCost.set(Theme.money(total.stockCost()),
                Theme.number(total.units()) + " unidades em " + Theme.number(total.products()) + " produtos");
        stockSale.set(Theme.money(total.stockSaleValue()), "Se todo o estoque for vendido");
        potential.set(Theme.money(total.potentialProfit()), "Venda menos custo do estoque atual");
        potential.setAccent(Theme.PINK, total.potentialProfit().signum() < 0 ? Theme.RED : Theme.INK);

        salesTitle.setText("VENDAS · " + chosen.toString().toUpperCase(Theme.LOCALE));
        revenue.set(Theme.money(total.revenue()), Theme.number(total.unitsSold()) + " unidades vendidas");
        costOfSales.set(Theme.money(total.costOfSales()),
                total.losses().signum() > 0 ? "Perdas e avarias: " + Theme.money(total.losses()) : "Custo do que foi vendido");
        profit.set(Theme.money(total.profit()),
                total.marginPercent() == null ? "Nenhuma venda no período"
                        : "Margem de " + Theme.percent(total.marginPercent()) + " sobre a receita");
        Color profitColor = total.profit().signum() < 0 ? Theme.RED : total.profit().signum() > 0 ? Theme.GREEN : Theme.INK;
        profit.setAccent(profitColor == Theme.INK ? Theme.PINK : profitColor, profitColor);

        // O total vem primeiro para continuar visível quando há muitas categorias.
        model.lines = new ArrayList<>();
        if (!report.lines().isEmpty()) {
            model.lines.add(total);
        }
        model.lines.addAll(report.lines());
        model.fireTableDataChanged();
    }

    private static class Model extends AbstractTableModel implements Cells.RowStyle {
        private static final String[] COLUMNS = {
                "CATEGORIA", "UNID.", "VALOR (CUSTO)", "VALOR (VENDA)", "LUCRO POTENCIAL",
                "VENDIDOS", "RECEITA", "LUCRO OBTIDO", "MARGEM"};
        private List<StockReport.Line> lines = new ArrayList<>();

        @Override
        public int getRowCount() {
            return lines.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            StockReport.Line line = lines.get(row);
            return switch (column) {
                case 0 -> line.category();
                case 1 -> line.units();
                case 2 -> line.stockCost();
                case 3 -> line.stockSaleValue();
                case 4 -> line.potentialProfit();
                case 5 -> line.unitsSold();
                case 6 -> line.revenue();
                case 7 -> line.profit();
                default -> line.marginPercent();
            };
        }

        @Override
        public Color accentFor(int modelRow) {
            return isBold(modelRow) ? Theme.PINK : null;
        }

        // A primeira linha é o total geral.
        @Override
        public boolean isBold(int modelRow) {
            return modelRow == 0;
        }
    }
}

package com.almoxarifado.ui;

import com.almoxarifado.model.MovementType;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;
import com.almoxarifado.service.StockReport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

// Resumo do estoque: indicadores, produtos em baixa e últimas movimentações.
class DashboardPage extends JPanel implements AppShell.Page {

    private static final int MAX_ROWS = 30;

    private final AppShell shell;
    private final Kpi productsKpi = new Kpi("Produtos cadastrados", "box");
    private final Kpi stockKpi = new Kpi("Valor do estoque", "layers");
    private final Kpi profitKpi = new Kpi("Lucro com vendas no mês", "money");
    private final Kpi lowKpi = new Kpi("Produtos em baixa", "alert");
    private final JPanel lowList = newList();
    private final JPanel recentList = newList();

    DashboardPage(AppShell shell) {
        super(new BorderLayout());
        this.shell = shell;

        JButton exitButton = Theme.ghostButton("Registrar saída", "out", Theme.AMBER);
        JButton entryButton = Theme.ghostButton("Registrar entrada", "in", Theme.GREEN);
        JButton newButton = Theme.primaryButton("Novo produto", "plus");
        exitButton.addActionListener(e -> shell.registerMovement(MovementType.EXIT, null));
        entryButton.addActionListener(e -> shell.registerMovement(MovementType.ENTRY, null));
        newButton.addActionListener(e -> shell.newProduct());

        add(AppShell.header("Visão geral", "Como está o seu estoque hoje.",
                exitButton, entryButton, newButton), BorderLayout.NORTH);

        JPanel kpis = new JPanel(new GridLayout(1, 4, Theme.px(16), 0));
        kpis.setOpaque(false);
        kpis.setBorder(Theme.pad(0, 0, 18, 0));
        kpis.add(productsKpi);
        kpis.add(stockKpi);
        kpis.add(profitKpi);
        kpis.add(lowKpi);

        JPanel lists = new JPanel(new GridLayout(1, 2, Theme.px(16), 0));
        lists.setOpaque(false);
        lists.add(card("Atenção ao estoque", "Produtos no estoque mínimo ou abaixo dele", lowList));
        lists.add(card("Últimas movimentações", "Entradas e saídas mais recentes", recentList));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(kpis, BorderLayout.NORTH);
        center.add(lists, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private static JPanel newList() {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        return list;
    }

    private static RoundedPanel card(String title, String subtitle, JPanel list) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), 14);
        card.setBorder(Theme.pad(18, 20, 10, 8));

        JPanel head = new JPanel(new GridLayout(2, 1, 0, Theme.px(3)));
        head.setOpaque(false);
        head.setBorder(Theme.pad(0, 0, 10, 0));
        head.add(Theme.label(title, Font.BOLD, 17f, Theme.INK));
        head.add(Theme.label(subtitle, Font.PLAIN, 13f, Theme.MUTED));

        // O painel extra impede que as linhas estiquem para preencher a altura.
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(list, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh(List<Product> products, List<StockMovement> movements) {
        StockReport.Line total = StockReport.of(products, movements, Period.THIS_MONTH.from(), null).total();
        long categories = products.stream()
                .map(p -> p.getCategory() == null ? "" : p.getCategory().toLowerCase(Theme.LOCALE))
                .distinct().count();
        List<Product> low = products.stream()
                .filter(Product::isLowStock)
                .sorted(Comparator.comparingInt(Product::getQuantity))
                .toList();

        productsKpi.set(Theme.number(products.size()),
                Theme.number(total.units()) + " unidades · " + categories + (categories == 1 ? " categoria" : " categorias"));
        stockKpi.set(Theme.money(total.stockCost()), Theme.money(total.stockSaleValue()) + " a preço de venda");
        profitKpi.set(Theme.money(total.profit()),
                total.unitsSold() == 0 ? "Nenhuma venda neste mês" : Theme.money(total.revenue()) + " em vendas");
        profitKpi.setAccent(Theme.PINK, total.profit().signum() < 0 ? Theme.RED
                : total.profit().signum() > 0 ? Theme.GREEN : Theme.INK);
        lowKpi.set(Theme.number(low.size()), low.isEmpty() ? "Tudo em dia" : "Precisam de reposição");
        lowKpi.setAccent(low.isEmpty() ? Theme.GREEN : Theme.AMBER, low.isEmpty() ? Theme.INK : Theme.AMBER);

        lowList.removeAll();
        if (low.isEmpty()) {
            lowList.add(emptyRow("check", Theme.GREEN, products.isEmpty()
                    ? "Nenhum produto cadastrado ainda."
                    : "Nenhum produto em baixa. Tudo em dia!"));
        }
        low.stream().limit(MAX_ROWS).forEach(product -> lowList.add(lowRow(product)));
        if (low.size() > MAX_ROWS) {
            JButton more = Theme.ghostButton("Ver todos os " + low.size() + " produtos em baixa", null);
            more.addActionListener(e -> shell.showLowStock());
            lowList.add(row(null, "", "", more));
        }

        recentList.removeAll();
        if (movements.isEmpty()) {
            recentList.add(emptyRow("swap", Theme.MUTED, "Nenhuma entrada ou saída registrada ainda."));
        }
        movements.stream().limit(MAX_ROWS).forEach(movement -> recentList.add(movementRow(movement)));

        revalidate();
        repaint();
    }

    private JComponent lowRow(Product product) {
        JButton restock = Theme.ghostButton("Repor", "in", Theme.GREEN);
        restock.addActionListener(e -> shell.registerMovement(MovementType.ENTRY, product));
        return row(null, product.getName(),
                "Em estoque: " + Theme.number(product.getQuantity())
                        + "  ·  Mínimo: " + Theme.number(product.getMinimumStock()),
                Cells.badgeLabel(Cells.stockBadge(product)), restock);
    }

    private JComponent movementRow(StockMovement movement) {
        boolean entry = movement.getType() == MovementType.ENTRY;
        Color color = entry ? Theme.GREEN : Theme.AMBER;
        JLabel amount = Theme.label((entry ? "+" : "−") + Theme.number(movement.getQuantity()),
                Font.BOLD, 16f, color);
        return row(Theme.icon(entry ? "in" : "out", 18, color), movement.getProductName(),
                movement.getReason().getLabel() + "  ·  " + Theme.DATE_TIME.format(movement.getCreatedAt()),
                amount);
    }

    private static JComponent emptyRow(String iconName, Color color, String text) {
        JLabel label = Theme.label(text, Font.PLAIN, 14.5f, Theme.MUTED);
        label.setIcon(Theme.icon(iconName, 20, color));
        label.setIconTextGap(10);
        label.setBorder(Theme.pad(18, 0, 18, 0));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.WEST);
        return panel;
    }

    private static JComponent row(Icon icon, String title, String subtitle, JComponent... trailing) {
        JPanel row = new JPanel(new BorderLayout(Theme.px(12), 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                Theme.pad(11, 0, 11, 12)));

        if (icon != null) {
            row.add(new JLabel(icon), BorderLayout.WEST);
        }
        JPanel text = new JPanel(new GridLayout(2, 1, 0, Theme.px(2)));
        text.setOpaque(false);
        text.add(Theme.label(title, Font.BOLD, 15f, Theme.INK));
        text.add(Theme.label(subtitle, Font.PLAIN, 13f, Theme.MUTED));
        row.add(text, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.px(10), 0));
        right.setOpaque(false);
        for (JComponent component : trailing) {
            right.add(component);
        }
        JPanel aligned = new JPanel(new GridBagLayout());
        aligned.setOpaque(false);
        aligned.add(right);
        row.add(aligned, BorderLayout.EAST);
        return row;
    }
}

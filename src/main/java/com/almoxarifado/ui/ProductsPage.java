package com.almoxarifado.ui;

import com.almoxarifado.model.MovementType;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;
import com.almoxarifado.service.StockReport;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

// Lista de produtos com busca, filtros e as ações de cadastro e estoque.
class ProductsPage extends JPanel implements AppShell.Page {

    private static final String ALL_CATEGORIES = "Todas as categorias";

    private final AppShell shell;
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final TableRowSorter<Model> sorter = new TableRowSorter<>(model);
    private final JTextField search = new JTextField();
    private final JComboBox<String> category = new JComboBox<>();
    private final JCheckBox onlyLow = new JCheckBox("Somente em baixa");
    private final JLabel count = Theme.label("", Font.PLAIN, 13.5f, Theme.MUTED);
    private final CardLayout bodyCards = new CardLayout();
    private final JPanel body = new JPanel(bodyCards);
    private final JButton editButton = Theme.ghostButton("Editar", "edit");
    private final JButton deleteButton = Theme.ghostButton("Excluir", "trash", Theme.RED);
    private boolean refreshing;

    ProductsPage(AppShell shell) {
        super(new BorderLayout());
        this.shell = shell;

        JButton entryButton = Theme.ghostButton("Entrada", "in", Theme.GREEN);
        JButton exitButton = Theme.ghostButton("Saída", "out", Theme.AMBER);
        JButton newButton = Theme.primaryButton("Novo produto", "plus");
        entryButton.setToolTipText("Registrar entrada de estoque");
        exitButton.setToolTipText("Registrar saída ou venda");
        entryButton.addActionListener(e -> shell.registerMovement(MovementType.ENTRY, selected()));
        exitButton.addActionListener(e -> shell.registerMovement(MovementType.EXIT, selected()));
        editButton.addActionListener(e -> shell.editProduct(selected()));
        deleteButton.addActionListener(e -> shell.deleteProduct(selected()));
        newButton.addActionListener(e -> shell.newProduct());

        add(AppShell.header("Produtos", "Cadastro e estoque de cada produto.",
                entryButton, exitButton, editButton, deleteButton, newButton), BorderLayout.NORTH);

        configureTable();

        RoundedPanel card = new RoundedPanel(new BorderLayout(), 14);
        card.setBorder(Theme.pad(6, 1, 6, 1));
        card.add(buildFilters(), BorderLayout.NORTH);
        card.add(Cells.styledScroll(table), BorderLayout.CENTER);

        body.setOpaque(false);
        body.add(card, "table");
        body.add(buildEmptyState(), "empty");
        add(body, BorderLayout.CENTER);

        updateSelectionButtons();
    }

    private JPanel buildFilters() {
        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nome ou descrição");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Theme.icon("search", 17, Theme.MUTED));
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        search.setPreferredSize(Theme.size(320, 40));
        category.setPreferredSize(Theme.size(230, 40));
        onlyLow.setOpaque(false);
        onlyLow.setForeground(Theme.INK);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        category.addActionListener(e -> applyFilter());
        onlyLow.addActionListener(e -> applyFilter());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.px(12), 0));
        left.setOpaque(false);
        left.add(search);
        left.add(category);
        left.add(onlyLow);

        JPanel filters = new JPanel(new BorderLayout());
        filters.setOpaque(false);
        filters.setBorder(Theme.pad(12, 6, 12, 18));
        filters.add(left, BorderLayout.WEST);
        filters.add(count, BorderLayout.EAST);
        return filters;
    }

    private JPanel buildEmptyState() {
        RoundedPanel empty = new RoundedPanel(new GridBagLayout(), 14);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(Theme.icon("box", 44, Theme.PINK));
        JLabel title = Theme.label("Nenhum produto cadastrado", Font.BOLD, 20f, Theme.INK);
        JLabel text = Theme.label("Comece cadastrando o primeiro produto do seu estoque.",
                Font.PLAIN, 14.5f, Theme.MUTED);
        JButton button = Theme.primaryButton("Cadastrar produto", "plus");
        button.addActionListener(e -> shell.newProduct());

        for (JComponent component : new JComponent[]{icon, title, text, button}) {
            component.setAlignmentX(CENTER_ALIGNMENT);
        }
        stack.add(icon);
        stack.add(Box.createVerticalStrut(Theme.px(14)));
        stack.add(title);
        stack.add(Box.createVerticalStrut(Theme.px(6)));
        stack.add(text);
        stack.add(Box.createVerticalStrut(Theme.px(20)));
        stack.add(button);
        empty.add(stack);
        return empty;
    }

    private void configureTable() {
        table.setRowSorter(sorter);
        table.setRowHeight(Theme.px(54));

        Collator collator = Collator.getInstance(Theme.LOCALE);
        sorter.setComparator(0, Comparator.comparing((Product p) -> p.getName(), collator));

        Cells.Text money = new Cells.Text(SwingConstants.RIGHT, v -> Theme.money((BigDecimal) v));

        setRenderer(0, 270, new Cells.TwoLines(SwingConstants.LEFT, v -> {
            Product product = (Product) v;
            return new String[]{product.getName(), product.getDescription()};
        }, null));
        setRenderer(1, 160, new Cells.Text(SwingConstants.LEFT, Object::toString));
        setRenderer(2, 120, money);
        setRenderer(3, 120, money);
        setRenderer(4, 105, new Cells.Text(SwingConstants.RIGHT, v -> Theme.percent((BigDecimal) v),
                v -> ((BigDecimal) v).signum() < 0 ? Theme.RED : null));
        setRenderer(5, 110, new Cells.TwoLines(SwingConstants.RIGHT, v -> {
            Product product = (Product) v;
            return new String[]{Theme.number(product.getQuantity()), "mín. " + Theme.number(product.getMinimumStock())};
        }, v -> {
            Product product = (Product) v;
            return product.isLowStock() ? Cells.stockBadge(product).color() : null;
        }));
        setRenderer(6, 150, money);
        setRenderer(7, 160, new Cells.BadgeCell(v -> Cells.stockBadge((Product) v)));
        Cells.alignHeaders(table, 2, 3, 4, 5, 6);
        sorter.setComparator(5, Comparator.comparingInt(Product::getQuantity));
        sorter.setComparator(7, Comparator.comparingInt(ProductsPage::statusRank));

        table.getSelectionModel().addListSelectionListener(e -> updateSelectionButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selected() != null) {
                    shell.editProduct(selected());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showMenu(e);
            }
        });
    }

    private void showMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        table.setRowSelectionInterval(row, row);
        Product product = selected();

        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("Registrar entrada", "in", () -> shell.registerMovement(MovementType.ENTRY, product)));
        menu.add(menuItem("Registrar saída", "out", () -> shell.registerMovement(MovementType.EXIT, product)));
        menu.addSeparator();
        menu.add(menuItem("Editar produto", "edit", () -> shell.editProduct(product)));
        menu.add(menuItem("Excluir produto", "trash", () -> shell.deleteProduct(product)));
        menu.show(table, e.getX(), e.getY());
    }

    private JMenuItem menuItem(String text, String iconName, Runnable action) {
        JMenuItem item = new JMenuItem(text, Theme.icon(iconName, 16, Theme.MUTED));
        item.addActionListener(e -> action.run());
        return item;
    }

    private void setRenderer(int column, int width, javax.swing.table.TableCellRenderer renderer) {
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
    }

    private static int statusRank(Product product) {
        return product.isOutOfStock() ? 0 : product.isLowStock() ? 1 : 2;
    }

    private Product selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.products.get(table.convertRowIndexToModel(row));
    }

    private void updateSelectionButtons() {
        boolean hasSelection = table.getSelectedRow() >= 0;
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }

    void showOnlyLowStock() {
        search.setText("");
        category.setSelectedIndex(0);
        onlyLow.setSelected(true);
        applyFilter();
    }

    @Override
    public void refresh(List<Product> products, List<StockMovement> movements) {
        Product previous = selected();
        UUID selectedId = previous == null ? null : previous.getId();

        refreshing = true;
        Object selectedCategory = category.getSelectedItem();
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Product product : products) {
            categories.add(categoryOf(product));
        }
        category.removeAllItems();
        category.addItem(ALL_CATEGORIES);
        categories.forEach(category::addItem);
        category.setSelectedItem(selectedCategory != null && categories.contains(selectedCategory.toString())
                ? selectedCategory : ALL_CATEGORIES);
        refreshing = false;

        model.products = new ArrayList<>(products);
        model.fireTableDataChanged();
        applyFilter();
        bodyCards.show(body, products.isEmpty() ? "empty" : "table");

        for (int i = 0; i < model.products.size(); i++) {
            if (model.products.get(i).getId().equals(selectedId)) {
                int viewRow = table.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }
            }
        }
        updateSelectionButtons();
    }

    private void applyFilter() {
        if (refreshing) {
            return;
        }
        String text = search.getText().trim().toLowerCase(Theme.LOCALE);
        Object chosen = category.getSelectedItem();
        boolean low = onlyLow.isSelected();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                Product product = model.products.get(entry.getIdentifier());
                if (low && !product.isLowStock()) {
                    return false;
                }
                if (chosen != null && !ALL_CATEGORIES.equals(chosen)
                        && !categoryOf(product).equalsIgnoreCase(chosen.toString())) {
                    return false;
                }
                String haystack = (product.getName() + " "
                        + (product.getDescription() == null ? "" : product.getDescription()))
                        .toLowerCase(Theme.LOCALE);
                return text.isEmpty() || haystack.contains(text);
            }
        });

        int total = model.getRowCount();
        int shown = table.getRowCount();
        count.setText(shown == total
                ? (total == 1 ? "1 produto" : total + " produtos")
                : shown + " de " + total + " produtos");
    }

    private static String categoryOf(Product product) {
        String value = product.getCategory();
        return value == null || value.isBlank() ? StockReport.NO_CATEGORY : value;
    }

    private static class Model extends AbstractTableModel implements Cells.RowStyle {
        private static final String[] COLUMNS = {
                "PRODUTO", "CATEGORIA", "CUSTO", "VENDA", "MARGEM", "ESTOQUE", "TOTAL (CUSTO)", "SITUAÇÃO"};
        private List<Product> products = new ArrayList<>();

        @Override
        public int getRowCount() {
            return products.size();
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
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0, 5, 7 -> Product.class;
                case 1 -> String.class;
                default -> BigDecimal.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            Product product = products.get(row);
            return switch (column) {
                case 0, 5, 7 -> product;
                case 1 -> categoryOf(product);
                case 2 -> product.getPurchasePrice();
                case 3 -> product.getSellingPrice();
                case 4 -> product.getMarginPercent();
                default -> product.getStockCost();
            };
        }

        @Override
        public Color accentFor(int modelRow) {
            Product product = products.get(modelRow);
            return product.isLowStock() ? Cells.stockBadge(product).color() : null;
        }
    }
}

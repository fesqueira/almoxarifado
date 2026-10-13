package com.almoxarifado.ui;

import com.almoxarifado.model.MovementType;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Histórico de entradas e saídas do estoque.
class MovementsPage extends JPanel implements AppShell.Page {

    private static final String ALL_TYPES = "Entradas e saídas";

    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final TableRowSorter<Model> sorter = new TableRowSorter<>(model);
    private final JTextField search = new JTextField();
    private final JComboBox<Object> type = new JComboBox<>(
            new Object[]{ALL_TYPES, MovementType.ENTRY, MovementType.EXIT});
    private final JComboBox<Period> period = new JComboBox<>(Period.values());
    private final JLabel count = Theme.label("", Font.PLAIN, 13.5f, Theme.MUTED);

    MovementsPage(AppShell shell) {
        super(new BorderLayout());

        JButton exitButton = Theme.ghostButton("Registrar saída", "out", Theme.AMBER);
        JButton entryButton = Theme.primaryButton("Registrar entrada", "in");
        exitButton.addActionListener(e -> shell.registerMovement(MovementType.EXIT, null));
        entryButton.addActionListener(e -> shell.registerMovement(MovementType.ENTRY, null));

        add(AppShell.header("Entradas e saídas", "Tudo o que entrou e saiu do estoque, da mais recente para a mais antiga.",
                exitButton, entryButton), BorderLayout.NORTH);

        configureTable();

        RoundedPanel card = new RoundedPanel(new BorderLayout(), 14);
        card.setBorder(Theme.pad(6, 1, 6, 1));
        card.add(buildFilters(), BorderLayout.NORTH);
        card.add(Cells.styledScroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    private JPanel buildFilters() {
        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por produto ou observação");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Theme.icon("search", 17, Theme.MUTED));
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        search.setPreferredSize(Theme.size(320, 40));
        type.setPreferredSize(Theme.size(200, 40));
        period.setPreferredSize(Theme.size(190, 40));
        period.setSelectedItem(Period.ALL);
        type.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                Object text = value instanceof MovementType movementType
                        ? (movementType == MovementType.ENTRY ? "Somente entradas" : "Somente saídas")
                        : value;
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });

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
        type.addActionListener(e -> applyFilter());
        period.addActionListener(e -> applyFilter());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.px(12), 0));
        left.setOpaque(false);
        left.add(search);
        left.add(type);
        left.add(period);

        JPanel filters = new JPanel(new BorderLayout());
        filters.setOpaque(false);
        filters.setBorder(Theme.pad(12, 6, 12, 18));
        filters.add(left, BorderLayout.WEST);
        filters.add(count, BorderLayout.EAST);
        return filters;
    }

    private void configureTable() {
        table.setRowSorter(sorter);

        Cells.Text text = new Cells.Text(SwingConstants.LEFT, Object::toString);
        Cells.Text money = new Cells.Text(SwingConstants.RIGHT, v -> Theme.money((BigDecimal) v));

        setRenderer(0, 175, new Cells.Text(SwingConstants.LEFT, v -> Theme.DATE_TIME.format((LocalDateTime) v)));
        setRenderer(1, 240, text);
        setRenderer(2, 130, new Cells.BadgeCell(v -> v == MovementType.ENTRY
                ? new Cells.Badge("Entrada", Theme.GREEN)
                : new Cells.Badge("Saída", Theme.AMBER)));
        setRenderer(3, 170, text);
        setRenderer(4, 85, new Cells.Text(SwingConstants.RIGHT,
                v -> ((Integer) v > 0 ? "+" : "−") + Theme.number(Math.abs((Integer) v)),
                v -> (Integer) v > 0 ? Theme.GREEN : Theme.AMBER));
        setRenderer(5, 125, money);
        setRenderer(6, 135, money);
        setRenderer(7, 220, new Cells.Text(SwingConstants.LEFT, Object::toString, v -> Theme.MUTED));
        Cells.alignHeaders(table, 4, 5, 6);
    }

    private void setRenderer(int column, int width, javax.swing.table.TableCellRenderer renderer) {
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
    }

    @Override
    public void refresh(List<Product> products, List<StockMovement> movements) {
        model.movements = new ArrayList<>(movements);
        model.fireTableDataChanged();
        applyFilter();
    }

    private void applyFilter() {
        String text = search.getText().trim().toLowerCase(Theme.LOCALE);
        Object chosenType = type.getSelectedItem();
        Period chosenPeriod = (Period) period.getSelectedItem();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                StockMovement movement = model.movements.get(entry.getIdentifier());
                if (chosenType instanceof MovementType movementType && movement.getType() != movementType) {
                    return false;
                }
                if (chosenPeriod != null && !chosenPeriod.contains(movement.getCreatedAt())) {
                    return false;
                }
                String haystack = (movement.getProductName() + " "
                        + (movement.getNote() == null ? "" : movement.getNote())).toLowerCase(Theme.LOCALE);
                return text.isEmpty() || haystack.contains(text);
            }
        });

        int total = model.getRowCount();
        int shown = table.getRowCount();
        if (total == 0) {
            count.setText("Nenhuma movimentação registrada ainda");
        } else if (shown == total) {
            count.setText(total == 1 ? "1 movimentação" : total + " movimentações");
        } else {
            count.setText(shown + " de " + total + " movimentações");
        }
    }

    private static class Model extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "DATA", "PRODUTO", "TIPO", "MOTIVO", "QTD.", "UNITÁRIO", "TOTAL", "OBSERVAÇÃO"};
        private List<StockMovement> movements = new ArrayList<>();

        @Override
        public int getRowCount() {
            return movements.size();
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
                case 0 -> LocalDateTime.class;
                case 2 -> MovementType.class;
                case 4 -> Integer.class;
                case 5, 6 -> BigDecimal.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            StockMovement movement = movements.get(row);
            // Vendas mostram o preço cobrado; as demais movimentações, o custo.
            BigDecimal unit = movement.isSale() ? movement.getUnitPrice() : movement.getUnitCost();
            return switch (column) {
                case 0 -> movement.getCreatedAt();
                case 1 -> movement.getProductName();
                case 2 -> movement.getType();
                case 3 -> movement.getReason().getLabel();
                case 4 -> movement.getType() == MovementType.ENTRY ? movement.getQuantity() : -movement.getQuantity();
                case 5 -> unit;
                case 6 -> unit.multiply(BigDecimal.valueOf(movement.getQuantity()));
                default -> movement.getNote() == null ? "" : movement.getNote();
            };
        }
    }
}

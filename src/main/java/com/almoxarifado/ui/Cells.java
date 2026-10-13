package com.almoxarifado.ui;

import com.almoxarifado.model.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Function;

// Renderizadores e estilo padrão das tabelas.
final class Cells {

    // Modelos que destacam linhas (ex.: produto em baixa) informam a cor de destaque aqui.
    interface RowStyle {
        Color accentFor(int modelRow);

        default boolean isBold(int modelRow) {
            return false;
        }
    }

    record Badge(String text, Color color) {
    }

    private Cells() {
    }

    static Badge stockBadge(Product product) {
        if (product.isOutOfStock()) {
            return new Badge("Sem estoque", Theme.RED);
        }
        if (product.isLowStock()) {
            return new Badge("Estoque baixo", Theme.AMBER);
        }
        return new Badge("Em dia", Theme.GREEN);
    }

    static JScrollPane styledScroll(JTable table) {
        table.setRowHeight(Theme.px(46));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 12f));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.NAVY_2);
        scroll.setOpaque(false);
        return scroll;
    }

    // Alinha o título das colunas numéricas à direita, do mesmo lado do conteúdo.
    static void alignHeaders(JTable table, int... rightAligned) {
        JTableHeader header = table.getTableHeader();
        TableCellRenderer base = header.getDefaultRenderer();
        header.setDefaultRenderer((t, value, selected, focus, row, column) -> {
            Component component = base.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                int modelColumn = t.convertColumnIndexToModel(column);
                boolean right = Arrays.stream(rightAligned).anyMatch(c -> c == modelColumn);
                label.setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
            }
            return component;
        });
    }

    private static Color background(JTable table, boolean selected, int row) {
        if (selected) {
            return table.getSelectionBackground();
        }
        Color accent = accent(table, row);
        return accent == null ? table.getBackground() : Theme.mix(table.getBackground(), accent, 0.07f);
    }

    private static Color accent(JTable table, int row) {
        if (table.getModel() instanceof RowStyle style) {
            return style.accentFor(table.convertRowIndexToModel(row));
        }
        return null;
    }

    private static boolean bold(JTable table, int row) {
        return table.getModel() instanceof RowStyle style && style.isBold(table.convertRowIndexToModel(row));
    }

    static class Text extends DefaultTableCellRenderer {
        private final Function<Object, String> format;
        private final Function<Object, Color> color;

        Text(int alignment, Function<Object, String> format) {
            this(alignment, format, null);
        }

        Text(int alignment, Function<Object, String> format, Function<Object, Color> color) {
            this.format = format;
            this.color = color;
            setHorizontalAlignment(alignment);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setText(value == null ? "—" : format.apply(value));
            setBorder(Theme.pad(0, 12, 0, 12));
            setBackground(background(table, isSelected, row));
            Color foreground = color == null || value == null ? null : color.apply(value);
            setForeground(foreground != null ? foreground : Theme.INK);
            setFont(table.getFont().deriveFont(bold(table, row) ? Font.BOLD : Font.PLAIN));
            return this;
        }
    }

    // Nome em destaque com a descrição logo abaixo.
    static class TwoLines extends JPanel implements TableCellRenderer {
        private final JLabel title = new JLabel();
        private final JLabel subtitle = new JLabel();
        private final Function<Object, String[]> lines;
        private final Function<Object, Color> color;

        TwoLines(int alignment, Function<Object, String[]> lines, Function<Object, Color> color) {
            super(new GridBagLayout());
            this.lines = lines;
            this.color = color;
            // BoxLayout ignora a segunda linha quando ela está oculta, mantendo o título centralizado.
            JPanel stack = new JPanel();
            stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
            stack.setOpaque(false);
            float side = alignment == SwingConstants.RIGHT ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT;
            title.setAlignmentX(side);
            subtitle.setAlignmentX(side);
            stack.add(title);
            stack.add(subtitle);
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.anchor = alignment == SwingConstants.RIGHT ? GridBagConstraints.EAST : GridBagConstraints.WEST;
            add(stack, c);
            setBorder(Theme.pad(0, 12, 0, 12));
            subtitle.setForeground(Theme.MUTED);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String[] text = lines.apply(value);
            title.setText(text[0]);
            title.setFont(table.getFont().deriveFont(Font.BOLD));
            Color foreground = color == null ? null : color.apply(value);
            title.setForeground(foreground != null ? foreground : Theme.INK);
            subtitle.setText(text[1]);
            subtitle.setFont(Theme.font(Font.PLAIN, 12.5f));
            subtitle.setVisible(text[1] != null && !text[1].isBlank());
            setBackground(background(table, isSelected, row));
            return this;
        }
    }

    // Etiqueta colorida ("Estoque baixo", "Entrada"...).
    static class BadgeCell extends JComponent implements TableCellRenderer {
        private final Function<Object, Badge> badgeOf;
        private Badge badge;
        private Color rowBackground;

        BadgeCell(Function<Object, Badge> badgeOf) {
            this.badgeOf = badgeOf;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            badge = value == null ? null : badgeOf.apply(value);
            rowBackground = background(table, isSelected, row);
            setFont(Theme.font(Font.BOLD, 12.5f));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(rowBackground);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (badge != null) {
                paintBadge(g, badge, getFont(), rowBackground, Theme.px(12), getHeight());
            }
            g.dispose();
        }
    }

    static void paintBadge(Graphics2D g, Badge badge, Font font, Color behind, int x, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int pillHeight = Theme.px(26);
        int pillWidth = metrics.stringWidth(badge.text()) + Theme.px(34);
        int dot = Theme.px(8);
        int y = (height - pillHeight) / 2;
        g.setColor(Theme.mix(behind, badge.color(), 0.18f));
        g.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight);
        g.setColor(badge.color());
        g.fillOval(x + Theme.px(11), y + (pillHeight - dot) / 2, dot, dot);
        g.drawString(badge.text(), x + Theme.px(24), y + (pillHeight - metrics.getHeight()) / 2 + metrics.getAscent());
    }

    // Etiqueta fora de tabelas.
    static JComponent badgeLabel(Badge badge) {
        Font font = Theme.font(Font.BOLD, 12.5f);
        JComponent component = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                paintBadge(g, badge, font, Theme.NAVY_2, 0, getHeight());
                g.dispose();
            }
        };
        int width = component.getFontMetrics(font).stringWidth(badge.text()) + Theme.px(35);
        component.setPreferredSize(new Dimension(width, Theme.px(28)));
        component.setMinimumSize(component.getPreferredSize());
        return component;
    }
}

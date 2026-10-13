package com.almoxarifado.ui;

import javax.swing.*;
import java.awt.*;

// Painel com cantos arredondados e borda fina, usado como "cartão".
public class RoundedPanel extends JPanel {
    private final int arc;
    private Color borderColor = Theme.LINE;

    public RoundedPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = Theme.px(arc);
        setOpaque(false);
        setBackground(Theme.NAVY_2);
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (borderColor != null) {
            g.setColor(borderColor);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g.dispose();
    }
}

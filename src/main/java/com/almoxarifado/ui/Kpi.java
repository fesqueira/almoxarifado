package com.almoxarifado.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Cartão de indicador: rótulo, valor em destaque e uma linha de apoio.
class Kpi extends RoundedPanel {
    private final JLabel icon = new JLabel();
    private final JLabel value = Theme.label("—", Font.BOLD, 25f, Theme.INK);
    private final JLabel detail = Theme.label(" ", Font.PLAIN, 13f, Theme.MUTED);
    private final String iconName;

    Kpi(String label, String iconName) {
        super(new BorderLayout(0, Theme.px(6)), 14);
        this.iconName = iconName;
        setBorder(Theme.pad(15, 20, 15, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.label(label, Font.PLAIN, 13.5f, Theme.MUTED), BorderLayout.CENTER);
        top.add(icon, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
        add(detail, BorderLayout.SOUTH);
        setAccent(Theme.PINK, Theme.INK);
    }

    void set(String valueText, String detailText) {
        value.setText(valueText);
        detail.setText(detailText == null || detailText.isBlank() ? " " : detailText);
    }

    void setAccent(Color iconColor, Color valueColor) {
        icon.setIcon(Theme.icon(iconName, 20, iconColor));
        value.setForeground(valueColor);
    }
}

package com.almoxarifado.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

// Identidade visual (paleta da Simplifica Tech) e utilitários de formatação usados por todas as telas.
public final class Theme {

    public static final Color NAVY = new Color(0x1B1440);
    public static final Color NAVY_2 = new Color(0x241B54);
    public static final Color NAVY_3 = new Color(0x2E2468);
    public static final Color PINK = new Color(0xFF1F8F);
    public static final Color PINK_DIM = new Color(0xC81876);
    public static final Color INK = new Color(0xF3F1FA);
    public static final Color MUTED = new Color(0xA79FCB);
    public static final Color GREEN = new Color(0x3DD68C);
    public static final Color AMBER = new Color(0xFFB648);
    public static final Color RED = new Color(0xFF6B6B);
    public static final Color LINE = mix(NAVY_2, Color.WHITE, 0.09f);
    public static final Color BORDER = mix(NAVY_2, Color.WHITE, 0.18f);

    public static final Locale LOCALE = Locale.of("pt", "BR");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String PRIMARY_STYLE =
            "background:#FF1F8F; foreground:#FFFFFF; hoverBackground:#E01B7E; pressedBackground:#C81876;"
                    + "focusedBackground:#FF1F8F; disabledBackground:#4A3A7A; disabledText:#8F86B8;"
                    + "default.background:#FF1F8F; default.foreground:#FFFFFF; default.hoverBackground:#E01B7E;"
                    + "default.pressedBackground:#C81876; default.focusedBackground:#FF1F8F;"
                    + "borderWidth:0; default.borderWidth:0; arc:10; margin:9,18,9,18; font:semibold";
    private static final String GHOST_STYLE =
            "background:#241B54; foreground:%s; hoverBackground:#2E2468; pressedBackground:#372C78;"
                    + "focusedBackground:#241B54; borderColor:#4B4375; hoverBorderColor:%s; focusedBorderColor:#FF1F8F;"
                    + "disabledBackground:#241B54; disabledBorderColor:#352C63; disabledText:#6F6799;"
                    + "arc:10; margin:8,16,8,16";

    private static final String ZOOM_KEY = "zoom";
    private static final int MIN_ZOOM = 70;
    private static final int MAX_ZOOM = 160;
    private static final int ZOOM_STEP = 10;

    // Fator aplicado a fontes, espaçamentos e tamanhos: escala da tela x ajuste do usuário.
    private static float scale = 1f;
    private static int zoomPercent = 100;

    private Theme() {
    }

    // As telas foram desenhadas para caber em 1400x820. Em telas com mais pixels tudo cresce na mesma
    // proporção, para que o programa ocupe a tela do mesmo jeito em qualquer resolução.
    private static float screenScale() {
        String forced = System.getProperty("almoxarifado.scale");
        if (forced != null) {
            try {
                return Float.parseFloat(forced);
            } catch (NumberFormatException e) {
                // valor inválido: segue com a detecção automática
            }
        }
        if (GraphicsEnvironment.isHeadless()) {
            return 1f;
        }
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        float fit = Math.min(screen.width / 1400f, screen.height / 820f);
        return Math.clamp(fit, 1f, 3f);
    }

    private static Preferences preferences() {
        return Preferences.userNodeForPackage(Theme.class);
    }

    // Ajuste escolhido pelo usuário (1.0 = padrão).
    public static float zoom() {
        return zoomPercent / 100f;
    }

    public static boolean canZoom(int direction) {
        int target = direction == 0 ? 100 : zoomPercent + direction * ZOOM_STEP;
        return target != zoomPercent && target >= MIN_ZOOM && target <= MAX_ZOOM;
    }

    // Muda o ajuste e o guarda para as próximas aberturas. A tela precisa ser refeita para refletir a mudança.
    public static boolean changeZoom(int direction) {
        if (!canZoom(direction)) {
            return false;
        }
        zoomPercent = direction == 0 ? 100 : zoomPercent + direction * ZOOM_STEP;
        preferences().putInt(ZOOM_KEY, zoomPercent);
        return true;
    }

    public static int px(int value) {
        return Math.round(value * scale);
    }

    public static Dimension size(int width, int height) {
        return new Dimension(px(width), px(height));
    }

    public static EmptyBorder pad(int top, int left, int bottom, int right) {
        return new EmptyBorder(px(top), px(left), px(bottom), px(right));
    }

    public static void setup() {
        zoomPercent = Math.clamp(preferences().getInt(ZOOM_KEY, 100), MIN_ZOOM, MAX_ZOOM);
        scale = screenScale() * zoom();

        FlatInterFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@background", "#1B1440",
                "@foreground", "#F3F1FA",
                "@accentColor", "#FF1F8F"));
        FlatDarkLaf.setup();

        // O FlatLaf dimensiona seus próprios componentes e ícones a partir do tamanho desta fonte.
        UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(15f * scale));
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", NAVY_2);
        UIManager.put("TextField.background", NAVY);
        UIManager.put("ComboBox.background", NAVY);
        UIManager.put("ComboBox.buttonBackground", NAVY);
        UIManager.put("TextField.margin", new Insets(7, 10, 7, 10));
        UIManager.put("ComboBox.padding", new Insets(7, 10, 7, 10));
        UIManager.put("ToolTip.background", NAVY_3);

        Color selection = mix(NAVY_2, PINK, 0.24f);
        UIManager.put("Table.background", NAVY_2);
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.selectionBackground", selection);
        UIManager.put("Table.selectionInactiveBackground", selection);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("Table.selectionInactiveForeground", INK);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TableHeader.background", NAVY_2);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.separatorColor", NAVY_2);
        UIManager.put("TableHeader.bottomSeparatorColor", LINE);
        UIManager.put("TableHeader.height", 42);
        UIManager.put("TableHeader.cellMargins", new Insets(0, 12, 0, 8));
    }

    public static Color mix(Color base, Color over, float amount) {
        return new Color(
                Math.round(base.getRed() + (over.getRed() - base.getRed()) * amount),
                Math.round(base.getGreen() + (over.getGreen() - base.getGreen()) * amount),
                Math.round(base.getBlue() + (over.getBlue() - base.getBlue()) * amount));
    }

    public static Icon icon(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(original -> color));
        return icon;
    }

    public static Font font(int style, float size) {
        return UIManager.getFont("defaultFont").deriveFont(style, size * scale);
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    public static JButton primaryButton(String text, String iconName) {
        JButton button = new JButton(text);
        if (iconName != null) {
            button.setIcon(icon(iconName, 18, Color.WHITE));
            button.setDisabledIcon(icon(iconName, 18, new Color(0x8F86B8)));
            button.setIconTextGap(8);
        }
        button.putClientProperty(FlatClientProperties.STYLE, PRIMARY_STYLE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton ghostButton(String text, String iconName) {
        return ghostButton(text, iconName, INK);
    }

    public static JButton ghostButton(String text, String iconName, Color color) {
        JButton button = new JButton(text);
        if (iconName != null) {
            button.setIcon(icon(iconName, 18, color));
            button.setDisabledIcon(icon(iconName, 18, new Color(0x6F6799)));
            button.setIconTextGap(8);
        }
        String hex = String.format("#%06X", color.getRGB() & 0xFFFFFF);
        button.putClientProperty(FlatClientProperties.STYLE, GHOST_STYLE.formatted(hex, hex));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return NumberFormat.getCurrencyInstance(LOCALE).format(value);
    }

    public static String percent(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getNumberInstance(LOCALE);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format.format(value) + "%";
    }

    public static String number(int value) {
        return NumberFormat.getIntegerInstance(LOCALE).format(value);
    }

    // Texto pronto para um campo de valor: "12,50".
    public static String moneyInput(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(LOCALE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(false);
        return format.format(value);
    }

    // Aceita "12,50", "12.50", "1.234,56" e "R$ 12,50". Lança IllegalArgumentException se não for um valor válido.
    public static BigDecimal parseMoney(String text, String fieldName) {
        String clean = text == null ? "" : text.replace("R$", "").replaceAll("[\\s\\u00A0]", "");
        if (clean.contains(",")) {
            clean = clean.replace(".", "").replace(',', '.');
        }
        try {
            BigDecimal value = new BigDecimal(clean);
            if (value.signum() < 0) {
                throw new NumberFormatException();
            }
            return value.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + ": informe um valor em reais, por exemplo 12,50.");
        }
    }

    public static int parseQuantity(String text, String fieldName) {
        try {
            int value = Integer.parseInt(text == null ? "" : text.trim());
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + ": informe um número inteiro, por exemplo 10.");
        }
    }
}

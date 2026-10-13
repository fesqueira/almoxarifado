package com.almoxarifado.ui;

import com.almoxarifado.model.MovementType;
import com.almoxarifado.model.Product;
import com.almoxarifado.model.StockMovement;
import com.almoxarifado.service.ProductService;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Estrutura principal: menu lateral, páginas e as ações compartilhadas entre elas.
public class AppShell extends JPanel {

    interface Page {
        void refresh(List<Product> products, List<StockMovement> movements);
    }

    public static final String DASHBOARD = "dashboard";
    public static final String PRODUCTS = "products";
    public static final String MOVEMENTS = "movements";
    public static final String REPORTS = "reports";

    private final ProductService service;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JToggleButton> navButtons = new LinkedHashMap<>();
    private final List<Page> pages = new ArrayList<>();
    private final ProductsPage productsPage;
    private final RoundedPanel lowStockAlert = new RoundedPanel(new BorderLayout(Theme.px(10), 0), 12);
    private final JLabel lowStockText = Theme.label("", Font.BOLD, 13f, Theme.AMBER);

    private final JLabel zoomText = Theme.label("", Font.PLAIN, 12.5f, Theme.MUTED);

    private List<Product> products = List.of();
    private String currentPage = DASHBOARD;

    public AppShell(ProductService service) {
        super(new BorderLayout());
        this.service = service;
        setBackground(Theme.NAVY);

        this.productsPage = new ProductsPage(this);
        addPage(DASHBOARD, new DashboardPage(this));
        addPage(PRODUCTS, productsPage);
        addPage(MOVEMENTS, new MovementsPage(this));
        addPage(REPORTS, new ReportsPage());

        add(buildSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        showPage(DASHBOARD);
        reload();
    }

    private <T extends JPanel & Page> void addPage(String key, T page) {
        page.setBorder(Theme.pad(30, 34, 28, 34));
        page.setBackground(Theme.NAVY);
        pages.add(page);
        content.add(page, key);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.NAVY_2);
        sidebar.setPreferredSize(Theme.size(250, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.LINE),
                Theme.pad(26, 18, 18, 18)));

        JPanel brand = new JPanel(new BorderLayout(Theme.px(12), 0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                Theme.pad(0, 6, 22, 6)));
        Image logo = new ImageIcon(AppShell.class.getResource("/images/logo.png")).getImage();
        brand.add(new JLabel(new ImageIcon(logo.getScaledInstance(Theme.px(40), -1, Image.SCALE_SMOOTH))), BorderLayout.WEST);
        JPanel names = new JPanel(new GridLayout(2, 1, 0, Theme.px(2)));
        names.setOpaque(false);
        names.add(Theme.label("Almoxarifado", Font.BOLD, 17f, Theme.PINK));
        names.add(Theme.label("CONTROLE DE ESTOQUE", Font.PLAIN, 10f, Theme.MUTED));
        brand.add(names, BorderLayout.CENTER);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(Theme.pad(20, 0, 0, 0));
        ButtonGroup group = new ButtonGroup();
        addNav(nav, group, DASHBOARD, "Visão geral", "home");
        addNav(nav, group, PRODUCTS, "Produtos", "box");
        addNav(nav, group, MOVEMENTS, "Entradas e saídas", "swap");
        addNav(nav, group, REPORTS, "Financeiro", "chart");

        lowStockAlert.setBackground(Theme.mix(Theme.NAVY_2, Theme.AMBER, 0.12f));
        lowStockAlert.setBorderColor(Theme.mix(Theme.NAVY_2, Theme.AMBER, 0.45f));
        lowStockAlert.setBorder(Theme.pad(12, 12, 12, 12));
        lowStockAlert.add(new JLabel(Theme.icon("alert", 20, Theme.AMBER)), BorderLayout.WEST);
        JPanel alertText = new JPanel(new GridLayout(2, 1, 0, Theme.px(2)));
        alertText.setOpaque(false);
        alertText.add(lowStockText);
        alertText.add(Theme.label("Clique para ver", Font.PLAIN, 12f, Theme.MUTED));
        lowStockAlert.add(alertText, BorderLayout.CENTER);
        lowStockAlert.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lowStockAlert.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showLowStock();
            }
        });

        JPanel foot = new JPanel(new BorderLayout(0, Theme.px(14)));
        foot.setOpaque(false);
        foot.add(lowStockAlert, BorderLayout.NORTH);
        foot.add(buildZoomControl(), BorderLayout.CENTER);
        JLabel credit = Theme.label("Desenvolvido por Simplifica Tech", Font.PLAIN, 12f, Theme.MUTED);
        credit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                Theme.pad(14, 6, 0, 6)));
        foot.add(credit, BorderLayout.SOUTH);

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(foot, BorderLayout.SOUTH);
        return sidebar;
    }

    // Ajuste manual do tamanho da interface, somado à escala automática da tela.
    private JPanel buildZoomControl() {
        JButton smaller = zoomButton("A−", "Diminuir (Ctrl −)", -1);
        JButton larger = zoomButton("A+", "Aumentar (Ctrl +)", 1);
        zoomText.setText("Tamanho: " + Math.round(Theme.zoom() * 100) + "%");
        zoomText.setToolTipText("Ctrl 0 volta ao tamanho padrão");

        JPanel buttons = new JPanel(new GridLayout(1, 2, Theme.px(6), 0));
        buttons.setOpaque(false);
        buttons.add(smaller);
        buttons.add(larger);

        JPanel control = new JPanel(new BorderLayout(Theme.px(8), 0));
        control.setOpaque(false);
        control.setBorder(Theme.pad(0, 6, 0, 0));
        control.add(zoomText, BorderLayout.CENTER);
        control.add(buttons, BorderLayout.EAST);

        bindZoomKey(KeyEvent.VK_EQUALS, 1);
        bindZoomKey(KeyEvent.VK_PLUS, 1);
        bindZoomKey(KeyEvent.VK_ADD, 1);
        bindZoomKey(KeyEvent.VK_MINUS, -1);
        bindZoomKey(KeyEvent.VK_SUBTRACT, -1);
        bindZoomKey(KeyEvent.VK_0, 0);
        return control;
    }

    private JButton zoomButton(String text, String tooltip, int direction) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(Theme.font(Font.BOLD, 12.5f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.STYLE,
                "background:#241B54; foreground:#A79FCB; hoverBackground:#2E2468; pressedBackground:#372C78;"
                        + "borderColor:#4B4375; focusWidth:0; arc:8; margin:3,9,3,9");
        button.setEnabled(Theme.canZoom(direction));
        button.addActionListener(e -> changeZoom(direction));
        return button;
    }

    private void bindZoomKey(int keyCode, int direction) {
        registerKeyboardAction(e -> changeZoom(direction),
                KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
    }

    // direction: +1 aumenta, -1 diminui, 0 volta ao padrão.
    private void changeZoom(int direction) {
        if (Theme.changeZoom(direction) && window() instanceof ProductGUI frame) {
            frame.rebuild(currentPage);
        }
    }

    private void addNav(JPanel nav, ButtonGroup group, String key, String text, String iconName) {
        JToggleButton button = new JToggleButton(text, Theme.icon(iconName, 19, Theme.MUTED));
        button.setSelectedIcon(Theme.icon(iconName, 19, Color.WHITE));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(12);
        button.setFont(Theme.font(Font.PLAIN, 15f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.STYLE,
                "background:#241B54; foreground:#A79FCB; hoverBackground:#2E2468; pressedBackground:#372C78;"
                        + "selectedBackground:#FF1F8F; selectedForeground:#FFFFFF; borderWidth:0; focusWidth:0;"
                        + "arc:10; margin:11,12,11,12");
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.px(46)));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.addActionListener(e -> showPage(key));
        group.add(button);
        navButtons.put(key, button);
        nav.add(button);
        nav.add(Box.createVerticalStrut(Theme.px(4)));
    }

    public void showPage(String key) {
        currentPage = key;
        navButtons.get(key).setSelected(true);
        cards.show(content, key);
    }

    // Recarrega os dados do banco e atualiza todas as páginas.
    public void reload() {
        try {
            products = service.getProducts();
            List<StockMovement> movements = service.getMovements();
            for (Page page : pages) {
                page.refresh(products, movements);
            }
            long low = products.stream().filter(Product::isLowStock).count();
            lowStockText.setText(low == 1 ? "1 produto em baixa" : low + " produtos em baixa");
            lowStockAlert.setVisible(low > 0);
        } catch (SQLException e) {
            showError("Erro ao carregar os dados: " + e.getMessage());
        }
    }

    void showLowStock() {
        productsPage.showOnlyLowStock();
        showPage(PRODUCTS);
    }

    void newProduct() {
        openProductDialog(null);
    }

    void editProduct(Product product) {
        openProductDialog(product);
    }

    private void openProductDialog(Product product) {
        try {
            ProductDialog dialog = new ProductDialog(window(), service, product, service.getCategories());
            if (dialog.open()) {
                reload();
            }
        } catch (SQLException e) {
            showError("Erro ao acessar o banco de dados: " + e.getMessage());
        }
    }

    void registerMovement(MovementType type, Product preselected) {
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(window(),
                    "Cadastre um produto antes de registrar entradas ou saídas.",
                    "Nenhum produto cadastrado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (new MovementDialog(window(), service, type, products, preselected).open()) {
            reload();
        }
    }

    void deleteProduct(Product product) {
        Object[] options = {"Excluir produto", "Cancelar"};
        String message = "<html><b>Excluir \"" + escape(product.getName()) + "\"?</b><br><br>"
                + (product.getQuantity() > 0
                ? "Ainda há " + product.getQuantity() + " unidade(s) em estoque, que deixarão de ser contadas.<br>"
                : "")
                + "As vendas já registradas continuam aparecendo no financeiro.</html>";
        int choice = JOptionPane.showOptionDialog(window(), message, "Excluir produto",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice != 0) {
            return;
        }
        try {
            service.deleteProduct(product.getId());
            reload();
        } catch (SQLException e) {
            showError("Erro ao excluir produto: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            reload();
        }
    }

    void showError(String message) {
        JOptionPane.showMessageDialog(window(), message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Cabeçalho padrão das páginas: título, subtítulo e botões de ação à direita.
    static JPanel header(String title, String subtitle, JComponent... actions) {
        JPanel header = new JPanel(new BorderLayout(Theme.px(20), 0));
        header.setOpaque(false);
        header.setBorder(Theme.pad(0, 0, 22, 0));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, Theme.px(4)));
        titles.setOpaque(false);
        titles.add(Theme.label(title, Font.BOLD, 27f, Theme.INK));
        titles.add(Theme.label(subtitle, Font.PLAIN, 14.5f, Theme.MUTED));
        header.add(titles, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.px(10), 0));
        buttons.setOpaque(false);
        for (JComponent action : actions) {
            buttons.add(action);
        }
        JPanel aligned = new JPanel(new GridBagLayout());
        aligned.setOpaque(false);
        aligned.add(buttons);
        header.add(aligned, BorderLayout.EAST);
        return header;
    }
}

package com.almoxarifado.ui;

import com.almoxarifado.service.ProductService;

import javax.swing.*;
import java.awt.*;
import java.util.List;


public class ProductGUI extends JFrame {

    private final ProductService service;

    public ProductGUI(ProductService service) {
        super("Almoxarifado - Controle de Estoque");

        setIconImages(List.of(
                image("/images/icon-32.png"),
                image("/images/icon-64.png"),
                image("/images/icon-256.png")));
        this.service = service;
        setContentPane(new AppShell(service));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fitToScreen();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // O tamanho mínimo acompanha a escala, mas nunca passa do que cabe na tela.
    private void fitToScreen() {
        Rectangle screen = getGraphicsConfiguration().getBounds();
        Dimension minimum = Theme.size(1100, 700);
        setMinimumSize(new Dimension(
                Math.min(minimum.width, screen.width), Math.min(minimum.height, screen.height)));
        if (getExtendedState() == JFrame.NORMAL) {
            Dimension preferred = Theme.size(1280, 800);
            setSize(Math.min(preferred.width, screen.width), Math.min(preferred.height, screen.height));
        }
    }

    // Refaz a tela depois de mudar o tamanho do texto, voltando para a mesma página.
    void rebuild(String page) {
        Theme.setup();
        AppShell shell = new AppShell(service);
        shell.showPage(page);
        setContentPane(shell);
        fitToScreen();
        revalidate();
        repaint();
    }

    private static Image image(String path) {
        return new ImageIcon(ProductGUI.class.getResource(path)).getImage();
    }

    // Erro que impede o programa de abrir: mostra na tela em vez de fechar em silêncio.
    public static void showStartupError(Throwable error) {
        error.printStackTrace();
        String detail = error.getMessage() == null ? error.toString() : error.getMessage();
        JOptionPane.showMessageDialog(null,
                "Não foi possível iniciar o Almoxarifado.\n\n" + detail,
                "Almoxarifado", JOptionPane.ERROR_MESSAGE);
    }
}

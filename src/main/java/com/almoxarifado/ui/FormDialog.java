package com.almoxarifado.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

// Base dos formulários: título, campos, mensagem de erro e botões de confirmar/cancelar.
abstract class FormDialog extends JDialog {

    private static final int WIDTH = 540;

    private final JPanel form = new JPanel();
    private final JLabel error = Theme.label(" ", Font.PLAIN, 13.5f, Theme.RED);
    private boolean confirmed;

    FormDialog(Window owner, String title, String subtitle, String confirmText) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        JPanel content = new JPanel(new BorderLayout(0, Theme.px(18)));
        content.setBorder(Theme.pad(26, 28, 22, 28));
        content.setBackground(Theme.NAVY_2);

        JPanel head = new JPanel(new GridLayout(2, 1, 0, Theme.px(4)));
        head.setOpaque(false);
        head.add(Theme.label(title, Font.BOLD, 21f, Theme.INK));
        head.add(Theme.label(subtitle, Font.PLAIN, 14f, Theme.MUTED));

        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JButton cancel = Theme.ghostButton("Cancelar", null);
        JButton confirm = Theme.primaryButton(confirmText, null);
        cancel.addActionListener(e -> dispose());
        confirm.addActionListener(e -> tryConfirm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.px(10), 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(confirm);

        error.setVisible(false);
        JPanel foot = new JPanel(new BorderLayout(0, Theme.px(14)));
        foot.setOpaque(false);
        foot.add(error, BorderLayout.NORTH);
        foot.add(buttons, BorderLayout.SOUTH);

        content.add(head, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(foot, BorderLayout.SOUTH);
        setContentPane(content);

        getRootPane().setDefaultButton(confirm);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    // Valida e grava. Problemas de preenchimento são sinalizados com IllegalArgumentException/IllegalStateException.
    protected abstract void confirm() throws SQLException;

    // Mostra o formulário e informa se o usuário confirmou.
    boolean open() {
        layoutDialog();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return confirmed;
    }

    void layoutDialog() {
        Container content = getContentPane();
        content.setPreferredSize(null);
        content.setPreferredSize(new Dimension(Theme.px(WIDTH), content.getPreferredSize().height));
        pack();
    }

    private void tryConfirm() {
        try {
            confirm();
            confirmed = true;
            dispose();
        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Erro ao salvar: " + e.getMessage());
        }
    }

    protected void showError(String message) {
        error.setText("<html>" + AppShell.escape(message) + "</html>");
        error.setIcon(Theme.icon("alert", 16, Theme.RED));
        error.setVisible(true);
        layoutDialog();
    }

    // Adiciona uma linha ao formulário; os campos dividem a largura igualmente.
    protected JPanel addRow(JComponent... fields) {
        JPanel row = new JPanel(new GridLayout(1, fields.length, Theme.px(14), 0));
        row.setOpaque(false);
        row.setBorder(Theme.pad(0, 0, 14, 0));
        for (JComponent field : fields) {
            row.add(field);
        }
        row.setAlignmentX(LEFT_ALIGNMENT);
        form.add(row);
        return row;
    }

    protected static JPanel field(String label, JComponent input) {
        return field(Theme.label(label, Font.PLAIN, 13.5f, Theme.MUTED), input);
    }

    protected static JPanel field(JLabel label, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, Theme.px(6)));
        panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        input.setPreferredSize(new Dimension(input.getPreferredSize().width, Theme.px(42)));
        return panel;
    }

    protected static JLabel hint(String text) {
        return Theme.label(text, Font.PLAIN, 13f, Theme.MUTED);
    }

    protected static void onChange(JTextComponent field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }
}

package PaooGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog grafic pentru setarile jocului.
 * folosit atat din meniul principal, cat si din meniul de pauza.
 */
public class SettingsDialog extends JDialog {
    private JCheckBox musicCheck;
    private JCheckBox sfxCheck;
    private JCheckBox cinematicCheck;
    private JCheckBox hitboxCheck;
    private JComboBox<String> difficultyCombo;

    public static void showSettings(Window owner) {
        SettingsDialog dialog = new SettingsDialog(owner);
        dialog.setVisible(true);
    }

    private SettingsDialog(Window owner) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);

        setUndecorated(true);
        setSize(470, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
        setAlwaysOnTop(true);

        setContentPane(createContent());

        // Inchidere cu ESC.
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint background = new GradientPaint(
                        0, 0, new Color(12, 16, 31),
                        0, getHeight(), new Color(31, 24, 45)
                );
                g2.setPaint(background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);

                g2.setColor(new Color(218, 165, 32, 190));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28);

                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-90, -80, 220, 220);
                g2.fillOval(getWidth() - 130, getHeight() - 130, 210, 210);

                g2.dispose();
            }
        };

        root.setOpaque(false);
        root.setBorder(new EmptyBorder(26, 32, 26, 32));

        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createSettingsPanel(), BorderLayout.CENTER);
        root.add(createButtonsPanel(), BorderLayout.SOUTH);

        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SETTINGS");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Serif", Font.BOLD, 40));
        title.setForeground(new Color(238, 232, 215));

        JLabel subtitle = new JLabel("Customize your Aethelgard experience");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(185, 190, 210));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(330, 1));
        sep.setForeground(new Color(218, 165, 32));
        sep.setBackground(new Color(218, 165, 32));

        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(18));
        header.add(sep);
        header.add(Box.createVerticalStrut(20));

        return header;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        musicCheck = createCheckBox("Music", GameSettings.musicEnabled);
        sfxCheck = createCheckBox("Sound Effects", GameSettings.sfxEnabled);
        cinematicCheck = createCheckBox("Cinematic Visual Mode", GameSettings.cinematicMode);
        hitboxCheck = createCheckBox("Show Collision Hitboxes", Game.showHitboxes);

        difficultyCombo = new JComboBox<>(new String[]{"EASY", "NORMAL", "HARD"});
        difficultyCombo.setSelectedItem(GameSettings.difficulty);
        difficultyCombo.setFont(new Font("SansSerif", Font.BOLD, 14));
        difficultyCombo.setForeground(new Color(235, 235, 240));
        difficultyCombo.setBackground(new Color(28, 32, 46));
        difficultyCombo.setFocusable(false);

        panel.add(createSettingRow("Audio", musicCheck));
        panel.add(Box.createVerticalStrut(12));

        panel.add(createSettingRow("Effects", sfxCheck));
        panel.add(Box.createVerticalStrut(12));

        panel.add(createSettingRow("Visual", cinematicCheck));
        panel.add(Box.createVerticalStrut(12));

        panel.add(createSettingRow("Debug", hitboxCheck));
        panel.add(Box.createVerticalStrut(12));

        panel.add(createSettingRow("Difficulty", difficultyCombo));

        return panel;
    }

    private JPanel createSettingRow(String labelText, JComponent control) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 35), 1),
                new EmptyBorder(12, 14, 12, 14)
        ));
        row.setMaximumSize(new Dimension(390, 58));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Serif", Font.BOLD, 17));
        label.setForeground(new Color(230, 225, 215));

        row.add(label, BorderLayout.WEST);
        row.add(control, BorderLayout.EAST);

        return row;
    }

    private JCheckBox createCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setFont(new Font("SansSerif", Font.BOLD, 13));
        checkBox.setForeground(new Color(220, 224, 235));
        checkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return checkBox;
    }

    private JPanel createButtonsPanel() {
        JPanel buttons = new JPanel(new GridLayout(1, 3, 12, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(22, 0, 0, 0));

        JButton saveButton = createButton("SAVE", true);
        JButton resetButton = createButton("RESET", false);
        JButton closeButton = createButton("CLOSE", false);

        saveButton.addActionListener(e -> saveSettings());
        resetButton.addActionListener(e -> resetSettings());
        closeButton.addActionListener(e -> dispose());

        buttons.add(saveButton);
        buttons.add(resetButton);
        buttons.add(closeButton);

        return buttons;
    }

    private JButton createButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (primary) {
            button.setForeground(new Color(18, 20, 28));
            button.setBackground(new Color(218, 165, 32));
            button.setBorder(BorderFactory.createLineBorder(new Color(255, 210, 90), 2));
        } else {
            button.setForeground(new Color(235, 235, 240));
            button.setBackground(new Color(28, 32, 46));
            button.setBorder(BorderFactory.createLineBorder(new Color(145, 150, 170), 2));
        }

        return button;
    }

    private void saveSettings() {
        GameSettings.musicEnabled = musicCheck.isSelected();
        GameSettings.sfxEnabled = sfxCheck.isSelected();
        GameSettings.cinematicMode = cinematicCheck.isSelected();
        GameSettings.difficulty = (String) difficultyCombo.getSelectedItem();

        Game.showHitboxes = hitboxCheck.isSelected();

        JOptionPane.showMessageDialog(
                this,
                "Settings saved successfully.",
                "Aethelgard",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void resetSettings() {
        GameSettings.resetToDefault();

        musicCheck.setSelected(GameSettings.musicEnabled);
        sfxCheck.setSelected(GameSettings.sfxEnabled);
        cinematicCheck.setSelected(GameSettings.cinematicMode);
        hitboxCheck.setSelected(Game.showHitboxes);
        difficultyCombo.setSelectedItem(GameSettings.difficulty);
    }
}
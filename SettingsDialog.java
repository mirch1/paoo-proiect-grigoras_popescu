package PaooGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SettingsDialog extends JDialog {
    public SettingsDialog(Window owner) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBorder(new EmptyBorder(22, 26, 22, 26));
        root.setBackground(new Color(18, 22, 34));

        JLabel title = new JLabel("AETHELGARD SETTINGS", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 22));
        title.setForeground(new Color(232, 220, 180));
        root.add(title, BorderLayout.NORTH);

        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));

        JCheckBox musicBox = createCheckBox("Music enabled", GameSettings.isMusicEnabled());
        JCheckBox soundBox = createCheckBox("Sound effects enabled", GameSettings.isSoundEnabled());


        options.add(musicBox);
        options.add(Box.createVerticalStrut(8));
        options.add(soundBox);
        options.add(Box.createVerticalStrut(8));

        root.add(options, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton cancelButton = createButton("CANCEL");
        JButton saveButton = createButton("SAVE");
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> {
            GameSettings.setMusicEnabled(musicBox.isSelected());
            GameSettings.setSoundEnabled(soundBox.isSelected());
            dispose();
        });
        buttons.add(cancelButton);
        buttons.add(saveButton);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setSize(360, getHeight());
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JCheckBox createCheckBox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setOpaque(false);
        box.setFont(new Font("SansSerif", Font.PLAIN, 15));
        box.setForeground(new Color(230, 230, 236));
        box.setFocusPainted(false);
        return box;
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(new Color(235, 235, 240));
        button.setBackground(new Color(34, 39, 56));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(166, 150, 105), 1));
        button.setPreferredSize(new Dimension(95, 32));
        return button;
    }
}

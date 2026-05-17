package PaooGame;

import PaooGame.Exceptions.DatabaseException;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

/*! \class LeaderboardDialog
    \brief Dialog modal care afiseaza top 10 scoruri din baza de date.

    \details
    Se deschide din MenuWindow la apasarea butonului LEADERBOARD.
    Citeste scorurile prin DatabaseManager.getTopScores(10) si le
    afiseaza intr-un tabel stilizat cu coloanele:
    - Loc (#)
    - Jucator
    - Scor
    - Nivel
    - Data

    Stilul vizual: fundal inchis (#1c2030), text alb, accent auriu
    pentru primele 3 locuri, consistent cu estetica jocului.
 */
public class LeaderboardDialog extends JDialog {

    private static final Color BG_DARK    = new Color(18, 22, 36);
    private static final Color BG_TABLE   = new Color(28, 32, 46);
    private static final Color BG_ALT_ROW = new Color(34, 39, 58);
    private static final Color TEXT_MAIN  = new Color(225, 225, 232);
    private static final Color TEXT_MUTED = new Color(160, 165, 185);
    private static final Color GOLD       = new Color(218, 165, 32);
    private static final Color SILVER     = new Color(192, 192, 200);
    private static final Color BRONZE     = new Color(180, 130, 70);

    // =========================================================================
    //  CONSTRUCTOR
    // =========================================================================

    /*! \fn public LeaderboardDialog(Window owner)
        \brief Construieste si populeaza dialogul de leaderboard.
        \param owner Fereastra parinte peste care apare dialogul (centrat).
     */
    public LeaderboardDialog(Window owner) {
        super(owner, "Leaderboard — Aethelgard", ModalityType.APPLICATION_MODAL);

        setUndecorated(true);
        setBackground(BG_DARK);
        setSize(560, 460);
        setLocationRelativeTo(owner);

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(BG_DARK);
        main.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(80, 85, 110), 2),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        // ---------------------------------------------------------------
        //  HEADER
        // ---------------------------------------------------------------

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(BG_DARK);

        JLabel title = new JLabel("LEADERBOARD", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 28));
        title.setForeground(GOLD);

        JLabel subtitle = new JLabel("Top 10 scoruri — toate sesiunile", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        header.add(title);
        header.add(subtitle);
        main.add(header, BorderLayout.NORTH);

        // ---------------------------------------------------------------
        //  TABEL
        // ---------------------------------------------------------------

        String[] columns = {"#", "Jucator", "Scor", "Nivel", "Data"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        populateModel(model);

        JTable table = new JTable(model);
        table.setBackground(BG_TABLE);
        table.setForeground(TEXT_MAIN);
        table.setFont(new Font("Consolas", Font.PLAIN, 14));
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(50, 55, 80));
        table.setSelectionForeground(TEXT_MAIN);
        table.setFocusable(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(36);
        table.getColumnModel().getColumn(0).setMaxWidth(40);

        /// Renderer custom: primele 3 locuri au culori speciale (aur, argint, bronz).
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {

                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);

                setBackground(row % 2 == 0 ? BG_TABLE : BG_ALT_ROW);

                if      (row == 0) setForeground(GOLD);
                else if (row == 1) setForeground(SILVER);
                else if (row == 2) setForeground(BRONZE);
                else               setForeground(TEXT_MAIN);

                setHorizontalAlignment(col == 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

                if (isSelected) setBackground(new Color(50, 55, 80));

                return this;
            }
        });

        /// Header stilizat consistent cu tema jocului.
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setBackground(new Color(22, 26, 42));
        tableHeader.setForeground(TEXT_MUTED);
        tableHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(false);
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(80, 85, 110)));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_TABLE);
        scroll.setBorder(new LineBorder(new Color(60, 65, 90), 1));
        scroll.getViewport().setBackground(BG_TABLE);

        main.add(scroll, BorderLayout.CENTER);

        // ---------------------------------------------------------------
        //  FOOTER: buton INCHIDE
        // ---------------------------------------------------------------

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(BG_DARK);

        JButton closeBtn = new JButton("INCHIDE");
        closeBtn.setFont(new Font("Serif", Font.BOLD, 15));
        closeBtn.setForeground(TEXT_MAIN);
        closeBtn.setBackground(new Color(28, 32, 46));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(new LineBorder(new Color(160, 165, 185), 2));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(140, 38));

        closeBtn.addActionListener(e -> {
            AudioManager.getInstance().playSoundEffect("res/audio/button_click.wav");
            dispose();
        });

        footer.add(closeBtn);
        main.add(footer, BorderLayout.SOUTH);

        setContentPane(main);
    }

    // =========================================================================
    //  POPULARE MODEL
    // =========================================================================

    /*! \fn private void populateModel(DefaultTableModel model)
        \brief Citeste top 10 scoruri din DatabaseManager si le adauga in model.

        \details
        Daca baza de date nu este disponibila sau nu exista scoruri,
        adauga un rand informativ in locul datelor reale.

        \param model Modelul tabelului Swing in care se adauga randurile.
     */
    private void populateModel(DefaultTableModel model) {
        try {
            List<DatabaseManager.ScoreEntry> scores =
                    DatabaseManager.getInstance().getTopScores(10);

            if (scores.isEmpty()) {
                model.addRow(new Object[]{"—", "Nu exista scoruri inca.", "—", "—", "—"});
                return;
            }

            for (int i = 0; i < scores.size(); i++) {
                DatabaseManager.ScoreEntry e = scores.get(i);
                model.addRow(new Object[]{
                        (i + 1) + ".",
                        e.playerName,
                        e.score,
                        "Nivel " + e.level,
                        e.date
                });
            }

        } catch (DatabaseException ex) {
            model.addRow(new Object[]{"!", "Eroare DB: " + ex.getMessage(), "—", "—", "—"});
            System.out.println("LeaderboardDialog: " + ex.getMessage());
        }
    }

    // =========================================================================
    //  METODA STATICA DE AFISARE
    // =========================================================================

    /*! \fn public static void show(Window owner)
        \brief Creeaza si afiseaza dialogul de leaderboard peste fereastra owner.

        \details Folosire din MenuWindow:
        \code
            LeaderboardDialog.show(this);
        \endcode

        \param owner Fereastra parinte.
     */
    public static void show(Window owner) {
        LeaderboardDialog dialog = new LeaderboardDialog(owner);
        dialog.setVisible(true);
    }
}
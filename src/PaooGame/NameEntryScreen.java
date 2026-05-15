package PaooGame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;

/*! \class NameEntryScreen
    \brief Ecran dedicat introducerii numelui unui jucator nou.

    Apare in doua situatii:
    - La prima rulare a aplicatiei, cand nu exista niciun profil salvat.
    - Cand jucatorul apasa "+ JUCATOR NOU" din ProfileSelectScreen.

    Valideaza numele introdus si, dupa confirmare, creeaza profilul prin
    ProfileManager si deschide MenuWindow.

    Designul grafic este identic cu celelalte ferestre principale ale jocului,
    pentru consistenta vizuala.
*/
public class NameEntryScreen extends JFrame {

    private int screenWidth;   /*!< Latimea ecranului principal in pixeli. */
    private int screenHeight;  /*!< Inaltimea ecranului principal in pixeli. */
    private JTextField nameField; /*!< Campul de text in care jucatorul scrie username-ul. */
    private JLabel errorLabel; /*!< Eticheta de eroare, afisata doar la validare esuata. */

    /*! \fn public NameEntryScreen()
        \brief Constructorul ecranului de introducere a numelui.
        \details Construieste interfata fullscreen cu campul de text, butonul de confirmare
                 si, optional, butonul INAPOI daca exista deja profile.
    */
    public NameEntryScreen() {
        /// Eliminam bara de titlu si marginile ferestrei.
        setUndecorated(true);
        setTitle("Aethelgard — Jucator Nou");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        /// Obtinem monitorul principal si dimensiunile sale reale.
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        /// Eliberam orice fullscreen exclusiv ramas din sesiunea anterioara.
        if (device.getFullScreenWindow() != null) {
            device.setFullScreenWindow(null);
        }

        Rectangle screenBounds = device.getDefaultConfiguration().getBounds();
        screenWidth = screenBounds.width;
        screenHeight = screenBounds.height;
        setBounds(screenBounds);

        /// Cream panoul cu fundal grafic personalizat.
        BackgroundPanel panel = new BackgroundPanel();
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(screenWidth, screenHeight));

        /// --- TITLUL PRINCIPAL ---
        JLabel titleLabel = new JLabel("AETHELGARD", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 52));
        titleLabel.setForeground(new Color(225, 225, 232));
        titleLabel.setBounds(0, screenHeight / 2 - 200, screenWidth, 70);
        panel.add(titleLabel);

        /// --- SUBTITLUL ---
        JLabel subLabel = new JLabel("Rise of the Fallen Crown", SwingConstants.CENTER);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        subLabel.setForeground(new Color(180, 180, 195));
        subLabel.setBounds(0, screenHeight / 2 - 130, screenWidth, 30);
        panel.add(subLabel);

        /// --- ETICHETA DEASUPRA CAMPULUI DE TEXT ---
        JLabel promptLabel = new JLabel("Username:", SwingConstants.CENTER);
        promptLabel.setFont(new Font("Serif", Font.BOLD, 22));
        promptLabel.setForeground(new Color(210, 210, 225));
        promptLabel.setBounds(0, screenHeight / 2 - 60, screenWidth, 36);
        panel.add(promptLabel);

        /// --- CAMPUL DE TEXT ---
        int fieldW = 320;
        int fieldH = 48;
        int fieldX = (screenWidth - fieldW) / 2;
        int fieldY = screenHeight / 2 - 10;

        nameField = new JTextField();
        nameField.setFont(new Font("Serif", Font.PLAIN, 22));
        nameField.setForeground(new Color(235, 235, 240));
        nameField.setBackground(new Color(18, 22, 36));
        nameField.setCaretColor(new Color(200, 200, 215));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(160, 165, 185), 2),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setBounds(fieldX, fieldY, fieldW, fieldH);

        /// Enter confirma direct fara click pe buton.
        nameField.addActionListener(e -> confirmName());
        panel.add(nameField);

        /// --- ETICHETA DE EROARE ---
        errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        errorLabel.setForeground(new Color(210, 80, 80));
        errorLabel.setBounds(0, fieldY + fieldH + 8, screenWidth, 24);
        panel.add(errorLabel);

        /// --- BUTONUL DE CONFIRMARE ---
        JButton confirmBtn = createMenuButton("CONFIRMA");
        confirmBtn.setBounds((screenWidth - 200) / 2, fieldY + fieldH + 44, 200, 50);
        confirmBtn.addActionListener(e -> confirmName());
        panel.add(confirmBtn);

        /// --- BUTONUL INAPOI ---
        JButton backBtn = createTopButton("INAPOI");
        backBtn.setBounds(30, 22, 110, 34);
        backBtn.setVisible(!ProfileManager.getProfiles().isEmpty());
        backBtn.addActionListener(e -> {
            dispose();
            new ProfileSelectScreen().setVisible(true);
        });
        panel.add(backBtn);

        setContentPane(panel);
        validate();

        /// Acordam focus campului de text imediat ce fereastra devine vizibila.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                nameField.requestFocusInWindow();
            }
        });
    }

    // =========================================================================
    // VALIDARE SI CONFIRMARE
    // =========================================================================

    /*! \fn private void confirmName()
        \brief Valideaza numele introdus si creeaza profilul.
        \details Regulile de validare:
                 - Nu poate fi gol.
                 - Minim 2 caractere, maxim 20.
                 - Doar litere, cifre, spatiu, underscore si cratima.
                 - Nu poate coincide cu un nume existent.
    */
    private void confirmName() {
        String name = nameField.getText().trim();

        /// Resetam stilul normal inainte de o noua validare.
        errorLabel.setText("");
        nameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(160, 165, 185), 2),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        if (name.isEmpty()) {
            showError("Username-ul nu poate fi gol!");
            return;
        }

        if (name.length() < 2) {
            showError("Username-ul trebuie sa aiba cel putin 2 caractere.");
            return;
        }

        if (name.length() > 20) {
            showError("Username-ul nu poate depasi 20 de caractere.");
            return;
        }

        if (!name.matches("[a-zA-Z0-9 _\\-]+")) {
            showError("Caractere invalide! Foloseste litere, cifre, spatiu, _ sau -");
            return;
        }

        if (ProfileManager.nameExists(name)) {
            showError("Exista deja un jucator cu acest username!");
            return;
        }

        /// Validarea a trecut: cream profilul si deschidem meniul principal.
        ProfileManager.createProfile(name);
        dispose();
        new MenuWindow().setVisible(true);
    }

    /*! \fn private void showError(String msg)
        \brief Afiseaza un mesaj de eroare si coloreaza bordura campului in rosu.
        \param msg Mesajul de eroare care trebuie afisat.
    */
    private void showError(String msg) {
        errorLabel.setText(msg);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 60, 60), 2),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
    }

    // =========================================================================
    // UTILITARE UI
    // =========================================================================

    /*! \fn private JButton createMenuButton(String text)
        \brief Creeaza un buton principal cu stilul meniului.
        \param text Textul afisat pe buton.
        \return Butonul creat si stilizat.
    */
    private JButton createMenuButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Serif", Font.BOLD, 18));
        b.setForeground(new Color(235, 235, 240));
        b.setBackground(new Color(28, 32, 46));
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(160, 165, 185), 2));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /*! \fn private JButton createTopButton(String text)
        \brief Creeaza un buton mic pentru bara de sus.
        \param text Textul butonului.
        \return Butonul creat si stilizat.
    */
    private JButton createTopButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(new Color(235, 235, 240));
        b.setBackground(new Color(28, 32, 46));
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(160, 165, 185), 2));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // =========================================================================
    // BACKGROUND PANEL
    // =========================================================================

    /*! \class BackgroundPanel
        \brief Panoul care deseneaza fundalul grafic.
        \details Deseneaza cerul de noapte, stelele, luna, muntii si solul.
    */
    class BackgroundPanel extends JPanel {

        /*! \fn protected void paintComponent(Graphics g)
            \brief Deseneaza fundalul ferestrei.
            \param g Contextul grafic primit de la Swing.
        */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            /// Gradient de fundal pentru cerul noptii.
            g2.setPaint(new GradientPaint(
                    new Point2D.Float(0, 0), new Color(8, 12, 24),
                    new Point2D.Float(0, h), new Color(30, 34, 52)));
            g2.fillRect(0, 0, w, h);

            /// Scalare virtuala raportata la 800x600.
            g2.scale((double) w / 800.0, (double) h / 600.0);

            /// Stele.
            g2.setColor(new Color(220, 220, 235, 120));
            int[][] stars = {{70,70},{130,95},{210,60},{290,100},{360,70},{450,95},{520,55},{610,110},{700,80},{745,130}};
            for (int[] s : stars) {
                g2.fillOval(s[0], s[1], 3, 3);
            }

            /// Luna si craterele ei.
            g2.setColor(new Color(232, 232, 225));
            g2.fillOval(545, 70, 72, 72);
            g2.setColor(new Color(195, 195, 188));
            g2.fillOval(565, 88, 10, 10);
            g2.fillOval(586, 97, 8, 8);
            g2.fillOval(555, 108, 7, 7);

            /// Munti principali.
            g2.setColor(new Color(82, 88, 118));
            int[][] mts = {{10,390,145,150,280,390},{170,390,330,100,490,390},{380,390,540,170,680,390},{560,390,720,140,860,390}};
            for (int[] m : mts) {
                Polygon p = new Polygon();
                p.addPoint(m[0], m[1]);
                p.addPoint(m[2], m[3]);
                p.addPoint(m[4], m[5]);
                g2.fillPolygon(p);
            }

            /// Umbrele muntilor.
            g2.setColor(new Color(48, 54, 78, 110));
            int[][] shadows = {{145,150,200,390,280,390},{330,100,385,390,490,390},{540,170,580,390,645,390},{720,140,760,390,830,390}};
            for (int[] s : shadows) {
                Polygon p = new Polygon();
                p.addPoint(s[0], s[1]);
                p.addPoint(s[2], s[3]);
                p.addPoint(s[4], s[5]);
                g2.fillPolygon(p);
            }

            /// Creasta de deal din plan apropiat.
            g2.setColor(new Color(36, 42, 58));
            Polygon ridge = new Polygon();
            ridge.addPoint(0,430);
            ridge.addPoint(90,405);
            ridge.addPoint(180,425);
            ridge.addPoint(280,395);
            ridge.addPoint(380,420);
            ridge.addPoint(470,400);
            ridge.addPoint(560,430);
            ridge.addPoint(660,405);
            ridge.addPoint(760,425);
            ridge.addPoint(800,415);
            ridge.addPoint(800,540);
            ridge.addPoint(0,540);
            g2.fillPolygon(ridge);

            /// Solul de la baza ecranului.
            g2.setColor(new Color(72, 54, 42));
            g2.fillRect(0, 540, 800, 60);

            g2.dispose();
        }
    }
}

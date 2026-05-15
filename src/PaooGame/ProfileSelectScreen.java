package PaooGame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.List;

/*! \class ProfileSelectScreen
    \brief Ecran de selectare a profilului de jucator.

    Afiseaza toate profilurile existente sub forma de carduri interactive.
    In aceasta versiune, selectia profilului se face prin click pe TOT cardul,
    nu doar pe un buton JOACA. Astfel eliminam orice problema de hitbox,
    clipping sau propagare a click-urilor in interiorul JScrollPane-ului.

    Fiecare card contine:
    - iconita decorativa,
    - numele jucatorului,
    - nivelul curent,
    - efect de hover,
    - click pe intreg cardul pentru selectie.

    Butonul STERGE ramane separat in dreapta cardului.
*/
public class ProfileSelectScreen extends JFrame {

    private int screenWidth;          /*!< Latimea ecranului principal in pixeli. */
    private int screenHeight;         /*!< Inaltimea ecranului principal in pixeli. */
    private JPanel profileListPanel;  /*!< Panoul cu lista dinamica de carduri de profil. */

    /*! \fn public ProfileSelectScreen()
        \brief Constructorul ecranului de selectie profil.
        \details Construieste interfata fullscreen cu lista de profile,
                 butonul de creare profil nou si butonul EXIT.
    */
    public ProfileSelectScreen() {
        /// Eliminam bara de titlu si marginile ferestrei pentru fullscreen curat.
        setUndecorated(true);
        setTitle("Aethelgard — Selectie Profil");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        /// Obtinem monitorul principal si dimensiunile sale reale.
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        /// Eliberam orice fullscreen exclusiv ramas din alta fereastra.
        if (device.getFullScreenWindow() != null) {
            device.setFullScreenWindow(null);
        }

        Rectangle screenBounds = device.getDefaultConfiguration().getBounds();
        screenWidth = screenBounds.width;
        screenHeight = screenBounds.height;
        setBounds(screenBounds);

        /// Cream panoul de fundal cu peisajul grafic al jocului.
        JPanel bgPanel = new BackgroundPanel();
        bgPanel.setLayout(null);

        /// --- TITLUL ECRANULUI ---
        JLabel titleLabel = new JLabel("SELECTEAZA JUCATORUL", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 36));
        titleLabel.setForeground(new Color(225, 225, 232));
        titleLabel.setBounds(0, screenHeight / 2 - 240, screenWidth, 50);
        bgPanel.add(titleLabel);

        /// --- LISTA DE PROFILE CU SCROLL ---
        profileListPanel = new JPanel();
        profileListPanel.setLayout(new BoxLayout(profileListPanel, BoxLayout.Y_AXIS));
        profileListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(profileListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));

        int listW = 500;
        int listH = 270;
        scrollPane.setBounds((screenWidth - listW) / 2, screenHeight / 2 - 175, listW, listH);
        bgPanel.add(scrollPane);

        /// Populam lista cu profilurile existente.
        buildProfileList();

        /// --- BUTON JUCATOR NOU ---
        JButton newBtn = createMenuButton("+ JUCATOR NOU");
        newBtn.setBounds((screenWidth - 220) / 2, screenHeight / 2 + 118, 220, 48);
        newBtn.addActionListener(e -> {
            dispose();
            new NameEntryScreen().setVisible(true);
        });
        bgPanel.add(newBtn);

        /// --- BUTON EXIT ---
        JButton exitBtn = createTopButton("EXIT");
        exitBtn.setBounds(screenWidth - 120, 22, 90, 34);
        exitBtn.addActionListener(e -> System.exit(0));
        bgPanel.add(exitBtn);

        setContentPane(bgPanel);
        validate();
    }

    // =========================================================================
    //  CONSTRUIRE LISTA DE PROFILE
    // =========================================================================

    /*! \fn private void buildProfileList()
        \brief Reconstruieste complet lista de profile din ProfileManager.
        \details Afiseaza un mesaj informativ daca nu exista profiluri salvate.
    */
    private void buildProfileList() {
        profileListPanel.removeAll();
        List<PlayerProfile> profiles = ProfileManager.getProfiles();

        if (profiles.isEmpty()) {
            JLabel empty = new JLabel("Nu exista profile. Creeaza unul nou!", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 16));
            empty.setForeground(new Color(160, 160, 175));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            profileListPanel.add(Box.createVerticalStrut(40));
            profileListPanel.add(empty);
        } else {
            for (PlayerProfile p : profiles) {
                profileListPanel.add(createProfileCard(p));
                profileListPanel.add(Box.createVerticalStrut(10));
            }
        }

        profileListPanel.revalidate();
        profileListPanel.repaint();
    }

    // =========================================================================
    //  CARD DE PROFIL
    // =========================================================================

    /*! \fn private JPanel createProfileCard(PlayerProfile profile)
        \brief Creeaza un card complet clickabil pentru un profil.
        \details Intreg cardul functioneaza ca buton de selectie.
                 Daca userul da click oriunde pe card (mai putin pe STERGE),
                 profilul devine activ si se deschide MenuWindow.
        \param profile Profilul pentru care se construieste cardul.
        \return Panoul cardului.
    */
    private JPanel createProfileCard(PlayerProfile profile) {
        final Color normalBg = new Color(22, 26, 42);
        final Color hoverBg  = new Color(34, 40, 62);
        final Color normalBorder = new Color(100, 105, 130);
        final Color hoverBorder  = new Color(180, 165, 110);

        /// Card custom desenat manual pentru control complet al aspectului.
        JPanel card = new JPanel(null) {
            private Color bgColor = normalBg;
            private Color borderColor = normalBorder;

            public void setVisualState(Color bg, Color border) {
                this.bgColor = bg;
                this.borderColor = border;
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                /// Fundalul cardului.
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                /// Conturul cardului.
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2.dispose();
            }
        };

        card.setOpaque(false);
        card.setPreferredSize(new Dimension(470, 78));
        card.setMaximumSize(new Dimension(470, 78));
        card.setMinimumSize(new Dimension(470, 78));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        /// Iconita decorativa din stanga.
        JLabel icon = new JLabel("⚔", SwingConstants.CENTER);
        icon.setFont(new Font("Serif", Font.PLAIN, 28));
        icon.setForeground(new Color(200, 185, 130));
        icon.setBounds(12, 14, 40, 40);
        card.add(icon);

        /// Numele profilului.
        JLabel nameLabel = new JLabel(profile.getName());
        nameLabel.setFont(new Font("Serif", Font.BOLD, 20));
        nameLabel.setForeground(new Color(235, 235, 240));
        nameLabel.setBounds(60, 12, 220, 24);
        card.add(nameLabel);

        /// Nivelul curent al profilului.
        JLabel levelLabel = new JLabel("Nivel " + profile.getLevel());
        levelLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        levelLabel.setForeground(new Color(160, 170, 185));
        levelLabel.setBounds(60, 40, 180, 20);
        card.add(levelLabel);

        /// Mesaj ajutator pentru utilizator: cardul este clickabil integral.
        JLabel hintLabel = new JLabel("Click pentru a selecta", SwingConstants.RIGHT);
        hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hintLabel.setForeground(new Color(170, 175, 190));
        hintLabel.setBounds(250, 14, 130, 18);
        card.add(hintLabel);

        /// Butonul de stergere ramane separat, in dreapta cardului.
        JButton deleteBtn = new JButton("STERGE");
        styleCardButton(deleteBtn, new Color(110, 30, 30), new Color(150, 45, 45));
        deleteBtn.setBounds(385, 22, 74, 32);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Esti sigur ca vrei sa stergi profilul \"" + profile.getName() + "\"?\nProgresul va fi pierdut definitiv.",
                "Sterge profil",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (result == JOptionPane.YES_OPTION) {
                ProfileManager.deleteProfile(profile.getName());
                buildProfileList();
            }
        });
        card.add(deleteBtn);

        /// Mouse listener pe TOT cardul pentru selectie profil.
        MouseAdapter selectListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                try {
                    card.getClass().getMethod("setVisualState", Color.class, Color.class)
                        .invoke(card, hoverBg, hoverBorder);
                } catch (Exception ignored) {}
            }

            @Override
            public void mouseExited(MouseEvent e) {
                try {
                    card.getClass().getMethod("setVisualState", Color.class, Color.class)
                        .invoke(card, normalBg, normalBorder);
                } catch (Exception ignored) {}
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                /// Daca s-a apasat pe butonul de stergere, nu selectam profilul.
                Component clicked = card.getComponentAt(e.getPoint());
                if (clicked == deleteBtn || SwingUtilities.isDescendingFrom(clicked, deleteBtn)) {
                    return;
                }

                /// Selectam profilul si deschidem meniul principal.
                ProfileManager.setActive(profile);
                dispose();
                new MenuWindow().setVisible(true);
            }
        };

        /// Atasam listenerul pe card si pe etichete, ca sa functioneze uniform.
        card.addMouseListener(selectListener);
        icon.addMouseListener(selectListener);
        nameLabel.addMouseListener(selectListener);
        levelLabel.addMouseListener(selectListener);
        hintLabel.addMouseListener(selectListener);

        return card;
    }

    // =========================================================================
    //  UTILITARE UI
    // =========================================================================

    /*! \fn private void styleCardButton(JButton b, Color bg, Color hover)
        \brief Stilizeaza butonul mic din card (STERGE).
        \param b Butonul care trebuie stilizat.
        \param bg Culoarea normala.
        \param hover Culoarea de hover.
    */
    private void styleCardButton(JButton b, Color bg, Color hover) {
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
    }

    /*! \fn private JButton createMenuButton(String text)
        \brief Creeaza un buton principal cu stilul meniului.
        \param text Textul butonului.
        \return Butonul creat.
    */
    private JButton createMenuButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Serif", Font.BOLD, 18));
        b.setForeground(new Color(235, 235, 240));
        b.setBackground(new Color(28, 32, 46));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorder(new LineBorder(new Color(160, 165, 185), 2));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /*! \fn private JButton createTopButton(String text)
        \brief Creeaza un buton mic pentru coltul de sus al ferestrei.
        \param text Textul afisat pe buton.
        \return Butonul creat.
    */
    private JButton createTopButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(new Color(235, 235, 240));
        b.setBackground(new Color(28, 32, 46));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorder(new LineBorder(new Color(160, 165, 185), 2));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // =========================================================================
    //  BACKGROUND PANEL
    // =========================================================================

    /*! \class BackgroundPanel
        \brief Panou care deseneaza fundalul grafic al ecranului.
        \details Deseneaza cerul, stelele, luna, muntii si solul.
    */
    class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            /// Gradient de cer nocturn.
            g2.setPaint(new GradientPaint(
                new Point2D.Float(0, 0), new Color(8, 12, 24),
                new Point2D.Float(0, h), new Color(30, 34, 52)));
            g2.fillRect(0, 0, w, h);

            /// Scalare virtuala la 800x600.
            g2.scale((double) w / 800.0, (double) h / 600.0);

            /// Stele.
            g2.setColor(new Color(220, 220, 235, 120));
            int[][] stars = {{70,70},{130,95},{210,60},{290,100},{360,70},{450,95},{520,55},{610,110},{700,80},{745,130}};
            for (int[] s : stars) g2.fillOval(s[0], s[1], 3, 3);

            /// Luna.
            g2.setColor(new Color(232, 232, 225));
            g2.fillOval(545, 70, 72, 72);
            g2.setColor(new Color(195, 195, 188));
            g2.fillOval(565, 88, 10, 10);
            g2.fillOval(586, 97, 8, 8);
            g2.fillOval(555, 108, 7, 7);

            /// Muntii principali.
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
            ridge.addPoint(0,430); ridge.addPoint(90,405); ridge.addPoint(180,425); ridge.addPoint(280,395);
            ridge.addPoint(380,420); ridge.addPoint(470,400); ridge.addPoint(560,430); ridge.addPoint(660,405);
            ridge.addPoint(760,425); ridge.addPoint(800,415); ridge.addPoint(800,540); ridge.addPoint(0,540);
            g2.fillPolygon(ridge);

            /// Solul.
            g2.setColor(new Color(72, 54, 42));
            g2.fillRect(0, 540, 800, 60);

            g2.dispose();
        }
    }
}

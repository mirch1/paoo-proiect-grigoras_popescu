package PaooGame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Point2D;

/*! \class MenuWindow
    \brief Implementeaza fereastra de meniu principal al jocului Aethelgard.

    Ruleaza ca fereastra fullscreen non-exclusiva (setBounds pe intreg ecranul,
    fara setFullScreenWindow). Acest mod evita problemele grafice care apar
    cand revenim din fereastra de joc dupa un fullscreen exclusiv.

    Contine:
    - Butonul PLAY: porneste un joc nou.
    - Butonul LOAD GAME: incarca jocul salvat al profilului activ.
    - Butonul SCHIMBA JUCATOR: deschide ProfileSelectScreen.
    - Butonul EXIT: inchide aplicatia.
    - Butonul SETTINGS (dreapta-sus): deschide SettingsDialog.
    - Eticheta cu numele si nivelul jucatorului activ (stanga-sus).
    - Clasa interna MenuPanel: deseneaza fundalul animat (cer, stele, luna, munti).
*/
public class MenuWindow extends JFrame {

    private int screenWidth;  /*!< Latimea ecranului curent in pixeli, calculata dinamic la constructie. */
    private int screenHeight; /*!< Inaltimea ecranului curent in pixeli, calculata dinamic la constructie. */

    /*! \fn public MenuWindow()
        \brief Constructorul principal al ferestrei de meniu.
        \details Pasii de constructie:
                 1. Configureaza JFrame (undecorated, fullscreen non-exclusiv).
                 2. Obtine dimensiunile ecranului principal.
                 3. Creeaza MenuPanel si adauga toate elementele UI.
                 4. Pozitioneaza butoanele manual (layout null) centrat pe ecran.
    */
    public MenuWindow() {
        /// Eliminam bara de titlu si marginile ferestrei pentru aspect fullscreen curat.
        setUndecorated(true);

        /// Titlul intern al ferestrei (vizibil in taskbar pe unele sisteme).
        setTitle("Aethelgard: Rise of the Fallen Crown");

        /// La apasarea X, aplicatia se inchide complet.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /// Nu permitem redimensionarea manuala a ferestrei.
        setResizable(false);

        /*
         * Obtinem monitorul principal pe care ruleaza aplicatia.
         *
         * IMPORTANT: Nu mai folosim setFullScreenWindow(this) pentru meniu,
         * deoarece fullscreen-ul exclusiv poate ramane intr-o stare grafica
         * incorecta dupa ce revenim din fereastra de joc (care foloseste Canvas).
         */
        GraphicsDevice device = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();

        /// Daca o fereastra a ramas in fullscreen exclusiv, o eliberam inainte de a continua.
        if (device.getFullScreenWindow() != null) {
            device.setFullScreenWindow(null);
        }

        /// Preluam dimensiunile reale ale ecranului principal.
        Rectangle screenBounds = device.getDefaultConfiguration().getBounds();

        /// Salvam dimensiunile pentru pozitionarea elementelor UI.
        screenWidth  = screenBounds.width;
        screenHeight = screenBounds.height;

        /// Setam fereastra sa acopere intreg ecranul, fara fullscreen exclusiv.
        setBounds(screenBounds);

        /// Cream panoul personalizat al meniului (contine toata logica de desenare).
        MenuPanel panel = new MenuPanel();

        /// Layout null: pozitionam manual toate butoanele cu setBounds().
        panel.setLayout(null);

        /// Dimensiunea preferata a panoului = dimensiunea ecranului.
        panel.setPreferredSize(new Dimension(screenWidth, screenHeight));

        // ---------------------------------------------------------------
        //  Dimensiuni si pozitii comune pentru butoane
        // ---------------------------------------------------------------

        int buttonWidth  = 200; /*!< Latimea comuna a butoanelor principale. */
        int buttonHeight = 52;  /*!< Inaltimea comuna a butoanelor principale. */
        int spacing      = 15;  /*!< Spatiul vertical dintre butoane. */

        /// Coordonata X pentru centrarea butoanelor pe ecran.
        int centerX = (screenWidth - buttonWidth) / 2;

        /// Coordonata Y de inceput a primului buton (usor sub centrul ecranului).
        int startY = screenHeight / 2 + 20;

        // ---------------------------------------------------------------
        //  ETICHETA JUCATOR ACTIV (stanga-sus)
        // ---------------------------------------------------------------

        /*! \brief Afiseaza numele si nivelul profilului activ in coltul stanga-sus.
            \details Vizibila dupa selectarea unui profil in ProfileSelectScreen.
                     Permite jucatorului sa confirme rapid ce profil este activ
                     inainte de a porni jocul. Nu este afisata daca nu exista
                     niciun profil activ (caz ce nu ar trebui sa apara in practica).
        */
        PlayerProfile active = ProfileManager.activeProfile;
        if (active != null) {
            JLabel playerLabel = new JLabel("\u2694 " + active.getName() + " | Nivel " + active.getLevel());
            playerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            playerLabel.setForeground(new Color(200, 195, 170));
            playerLabel.setBounds(20, 20, 360, 28);
            panel.add(playerLabel);
        }

        // ---------------------------------------------------------------
        //  BUTON PLAY
        // ---------------------------------------------------------------

        /// Butonul pentru pornirea unui joc nou (de la Nivelul 1, spawn default).
        JButton playButton = createMenuButton("PLAY");

        /// Pozitionam PLAY pe primul rand de butoane.
        playButton.setBounds(centerX, startY, buttonWidth, buttonHeight);

        /*! \brief Listener buton PLAY: inchide meniul si porneste un joc nou.
            \details Creeaza o noua instanta Game cu loadSavedGame=false,
                     astfel InitGame() va porni intotdeauna din Nivelul 1.
        */
        playButton.addActionListener(e -> {
            /// Redam un efect scurt de click pentru feedback audio.
            AudioManager.getInstance().playSoundEffect("res/audio/button_click.wav");

            /// Oprim muzica de meniu inainte sa pornim jocul.
            AudioManager.getInstance().stopMusic();

            /// Inchidem fereastra de meniu.
            dispose();

            /// Cream jocul nou si pornim bucla principala.
            Game paooGame = new Game("Aethelgard", screenWidth, screenHeight);
            paooGame.StartGame();
        });

        panel.add(playButton);

        // ---------------------------------------------------------------
        //  BUTON LOAD GAME
        // ---------------------------------------------------------------

        /// Butonul pentru incarcarea jocului salvat al profilului activ.
        JButton loadButton = createMenuButton("LOAD GAME");

        /// Pozitionam LOAD GAME pe al doilea rand de butoane.
        loadButton.setBounds(centerX, startY + buttonHeight + spacing, buttonWidth, buttonHeight);

        /*! \brief Listener buton LOAD GAME: verifica existenta salvarii, apoi incarca jocul.
            \details Verifica daca profilul activ are un joc salvat (nivel > 1 sau
                     pozitie nenula). Daca nu exista salvare, afiseaza un mesaj
                     informativ. Daca exista, porneste Game cu loadSavedGame=true,
                     care va restaura nivelul si pozitia din profilul activ.
        */
        loadButton.addActionListener(e -> {
            PlayerProfile activeProfile = ProfileManager.activeProfile;

            /// Verificam daca profilul activ are un joc salvat.
            boolean hasSave = activeProfile != null && SaveManager.hasSaveGame();

            if (!hasSave) {
                /// Nu exista salvare — informam jucatorul si nu facem nimic altceva.
                JOptionPane.showMessageDialog(
                    this,
                    "Nu exista niciun joc salvat pentru " +
                    (activeProfile != null ? activeProfile.getName() : "acest profil") + ".",
                    "Load Game",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            /// Redam un efect scurt de click pentru feedback audio.
            AudioManager.getInstance().playSoundEffect("res/audio/button_click.wav");

            /// Oprim muzica de meniu inainte sa pornim jocul incarcat.
            AudioManager.getInstance().stopMusic();

            /// Exista salvare — inchidem meniul si pornim jocul cu restaurarea progresului.
            dispose();
            Game paooGame = new Game("Aethelgard", screenWidth, screenHeight, true);
            paooGame.StartGame();
        });

        panel.add(loadButton);

        // ---------------------------------------------------------------
        //  BUTON SCHIMBA JUCATOR
        // ---------------------------------------------------------------

        /*! \brief Butonul pentru schimbarea profilului activ fara a iesi din aplicatie.
            \details Inchide MenuWindow si deschide ProfileSelectScreen, unde jucatorul
                     poate selecta un profil diferit sau crea unul nou prin NameEntryScreen.
                     Pozitionat pe randul 2 (index 2), impingand EXIT pe randul 3.
        */
        JButton switchBtn = createMenuButton("SCHIMBA JUCATOR");

        /// Font usor mai mic pentru a incapea textul in latimea butonului standard.
        switchBtn.setFont(new Font("Serif", Font.BOLD, 15));

        /// Randul 2 (dupa LOAD GAME).
        switchBtn.setBounds(centerX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight - 6);

        switchBtn.addActionListener(e -> {

            /// Redam un efect scurt de click pentru feedback audio.
            AudioManager.getInstance().playSoundEffect("res/audio/button_click.wav");

            /// Inchidem meniul si deschidem ecranul de selectie profil.
            dispose();
            new ProfileSelectScreen().setVisible(true);
        });

        panel.add(switchBtn);

        // ---------------------------------------------------------------
        //  BUTON EXIT
        // ---------------------------------------------------------------

        /// Butonul pentru inchiderea completa a aplicatiei.
        JButton exitButton = createMenuButton("EXIT");

        /// Randul 3 (mutat de pe randul 2 pentru a face loc butonului SCHIMBA JUCATOR).
        exitButton.setBounds(centerX, startY + (buttonHeight + spacing) * 3, buttonWidth, buttonHeight - 8);

        /// La apasarea EXIT, aplicatia se inchide complet.
        exitButton.addActionListener(e -> {
            /// Oprim muzica inainte de inchiderea aplicatiei.
            AudioManager.getInstance().stopMusic();

            /// Inchidem complet aplicatia.
            System.exit(0);
        });

        panel.add(exitButton);

        // ---------------------------------------------------------------
        //  BUTON SETTINGS (dreapta-sus)
        // ---------------------------------------------------------------

        /// Butonul de setari pozitionat in coltul dreapta-sus al ecranului.
        JButton settingsButton = createTopButton("SETTINGS");

        /// Pozitionat la 165px de marginea dreapta, la 22px de sus.
        settingsButton.setBounds(screenWidth - 165, 22, 130, 34);

        /*
         * Deschide dialogul de setari cu fereastra de meniu ca owner,
         * pentru a aparea centrat peste meniu si a bloca interactiunea
         * cu meniul cat timp setarile sunt deschise.
         */
        settingsButton.addActionListener(e -> {
            /// Redam un efect scurt de click pentru feedback audio.
            AudioManager.getInstance().playSoundEffect("res/audio/button_click.wav");

            /// Deschidem dialogul de setari peste meniul principal.
            SettingsDialog.showSettings(this);
        });

        panel.add(settingsButton);

        // ---------------------------------------------------------------
        //  FINALIZARE FEREASTRA
        // ---------------------------------------------------------------

        /// Setam panoul ca element principal al ferestrei.
        setContentPane(panel);

        /// Fortam recalcularea layout-ului dupa adaugarea tuturor componentelor.
        validate();

        /*
         * Pornim muzica de meniu.
         *
         * AudioManager verifica intern daca aceeasi melodie ruleaza deja,
         * deci nu va reporni inutil soundtrack-ul daca revenim in meniu.
         */
        AudioManager.getInstance().playMusic("res/audio/menu_theme.wav");
    }

    // =========================================================================
    //  METODE UTILITARE PENTRU BUTOANE
    // =========================================================================

    /*! \fn private JButton createMenuButton(String text)
        \brief Creeaza un buton principal de meniu cu stilul standard al jocului.
        \details Stilul: font Serif Bold 18pt, text alb pe fundal albastru inchis,
                 chenar gri-albastru de 2px, cursor de mana la hover.
        \param text Textul afisat pe buton.
        \return Butonul creat, nestilizat cu pozitie (se face cu setBounds() la apelant).
    */
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Serif", Font.BOLD, 18));
        button.setForeground(new Color(235, 235, 240));
        button.setBackground(new Color(28, 32, 46));
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(new Color(160, 165, 185), 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    /*! \fn private JButton createTopButton(String text)
        \brief Creeaza un buton secundar (folosit pentru SETTINGS) cu font mai mic.
        \details Identic cu createMenuButton() dar cu font SansSerif Bold 13pt,
                 potrivit pentru butoanele de chrome din colturile ferestrei.
        \param text Textul afisat pe buton.
        \return Butonul creat, nestilizat cu pozitie.
    */
    private JButton createTopButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(new Color(235, 235, 240));
        button.setBackground(new Color(28, 32, 46));
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(new Color(160, 165, 185), 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // =========================================================================
    //  CLASA INTERNA MenuPanel
    // =========================================================================

    /*! \class MenuPanel
        \brief Panoul personalizat care deseneaza fundalul artistic al meniului.
        \details Deseneaza (de la spate spre fata):
                 1. Gradient de cer nocturn (sus negru-albastru, jos albastru inchis).
                 2. Stele (10 puncte mici, pozitii fixe) si luna plina cu crateri.
                 3. Munti in doua tonuri: corp principal gri-violet + umbra laterala.
                 4. Creste de deal in planul 2 + tufisuri verzi + sol maro.
                 5. Titlul "AETHELGARD" si subtitlul, centrate pe latimea virtuala de 800px.

        Toate coordonatele interne se raporteaza la o rezolutie virtuala de 800x600.
        Scalarea la rezolutia fizica a monitorului se face prin g2.scale(scaleX, scaleY)
        calculat in functie de dimensiunile reale ale panoului.
    */
    class MenuPanel extends JPanel {

        /*! \fn protected void paintComponent(Graphics g)
            \brief Suprascrie paintComponent pentru a desena fundalul personalizat al meniului.
            \details Metoda este apelata automat de Swing la fiecare repaint().
                     Foloseste Graphics2D cu antialiasing activat si scalare proportionala
                     pentru a se adapta la orice rezolutie de monitor.
            \param g Contextul grafic furnizat de Swing (convertit la Graphics2D intern).
        */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            /// Activam antialiasing pentru margini line la poligoane si text.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            /// Factori de scalare: toate coordonatele virtuale (800x600) → pixeli reali.
            double scaleX = (double) w / 800.0;
            double scaleY = (double) h / 600.0;

            // -----------------------------------------------------------
            //  1. FUNDAL — gradient de cer nocturn (sus: negru-albastru, jos: albastru inchis)
            // -----------------------------------------------------------

            GradientPaint sky = new GradientPaint(
                new Point2D.Float(0, 0), new Color(8,  12, 24),
                new Point2D.Float(0, h), new Color(30, 34, 52)
            );
            g2.setPaint(sky);
            g2.fillRect(0, 0, w, h);

            /// De aici inainte, toate coordonatele sunt virtuale (800x600) — scalarea e aplicata.
            g2.scale(scaleX, scaleY);

            // -----------------------------------------------------------
            //  2. STELE SI LUNA
            // -----------------------------------------------------------

            /// 10 stele mici alb-albastru, semitransparente, pozitii fixe.
            g2.setColor(new Color(220, 220, 235, 120));
            int[][] stars = {
                {70,70}, {130,95}, {210,60}, {290,100}, {360,70},
                {450,95},{520,55}, {610,110},{700,80},  {745,130}
            };
            for (int[] s : stars) {
                g2.fillOval(s[0], s[1], 3, 3);
            }

            /// Luna plina (cerc alb-gri, 72x72px virtual).
            g2.setColor(new Color(232, 232, 225));
            g2.fillOval(545, 70, 72, 72);

            /// Cratere pe luna (trei cercuri gri mai inchis).
            g2.setColor(new Color(195, 195, 188));
            g2.fillOval(565, 88, 10, 10);
            g2.fillOval(586, 97,  8,  8);
            g2.fillOval(555, 108,  7,  7);

            // -----------------------------------------------------------
            //  3. MUNTI (corp + umbra laterala)
            // -----------------------------------------------------------

            /// Corpul principal al muntilor (gri-violet mediu).
            g2.setColor(new Color(82, 88, 118));
            Polygon m1 = new Polygon(); m1.addPoint(10,390);  m1.addPoint(145,150); m1.addPoint(280,390); g2.fillPolygon(m1);
            Polygon m2 = new Polygon(); m2.addPoint(170,390); m2.addPoint(330,100); m2.addPoint(490,390); g2.fillPolygon(m2);
            Polygon m3 = new Polygon(); m3.addPoint(380,390); m3.addPoint(540,170); m3.addPoint(680,390); g2.fillPolygon(m3);
            Polygon m4 = new Polygon(); m4.addPoint(560,390); m4.addPoint(720,140); m4.addPoint(860,390); g2.fillPolygon(m4);

            /// Umbre laterale ale muntilor (gri-violet inchis, semitransparent) — adancime vizuala.
            g2.setColor(new Color(48, 54, 78, 110));
            Polygon s1 = new Polygon(); s1.addPoint(145,150); s1.addPoint(200,390); s1.addPoint(280,390); g2.fillPolygon(s1);
            Polygon s2 = new Polygon(); s2.addPoint(330,100); s2.addPoint(385,390); s2.addPoint(490,390); g2.fillPolygon(s2);
            Polygon s3 = new Polygon(); s3.addPoint(540,170); s3.addPoint(580,390); s3.addPoint(645,390); g2.fillPolygon(s3);
            Polygon s4 = new Polygon(); s4.addPoint(720,140); s4.addPoint(760,390); s4.addPoint(830,390); g2.fillPolygon(s4);

            // -----------------------------------------------------------
            //  4. PLANUL 2 — creste de deal, tufisuri, sol
            // -----------------------------------------------------------

            /// Creste de deal in plan secund (albastru-negru inchis).
            g2.setColor(new Color(36, 42, 58));
            Polygon ridge = new Polygon();
            ridge.addPoint(0,430);   ridge.addPoint(90,405);  ridge.addPoint(180,425);
            ridge.addPoint(280,395); ridge.addPoint(380,420); ridge.addPoint(470,400);
            ridge.addPoint(560,430); ridge.addPoint(660,405); ridge.addPoint(760,425);
            ridge.addPoint(800,415); ridge.addPoint(800,540); ridge.addPoint(0,540);
            g2.fillPolygon(ridge);

            /// Ceata usoara alba (overlay semitransparent) la baza muntilor — efect de distanta.
            g2.setColor(new Color(210, 210, 225, 20));
            g2.fillRoundRect(0, 395, 800, 55, 30, 30);

            /// Tufisuri verzi in stanga si dreapta (semielipse).
            g2.setColor(new Color(68, 88, 66));
            g2.fillArc(-80, 505, 240, 110, 0, 180);
            g2.fillArc(620, 505, 260, 110, 0, 180);

            /// Sol maro inchis la baza (planul cel mai apropiat).
            g2.setColor(new Color(72, 54, 42));
            g2.fillRect(0, 540, 800, 60);

            // -----------------------------------------------------------
            //  5. TITLURI CENTRATE PE LATIMEA VIRTUALA DE 800px
            // -----------------------------------------------------------

            /// TITLUL PRINCIPAL — "AETHELGARD", Serif Bold 58pt, centrat.
            String mainTitle = "AETHELGARD";
            Font   mainFont  = new Font("Serif", Font.BOLD, 58);
            g2.setFont(mainFont);
            FontMetrics fmMain = g2.getFontMetrics(mainFont);

            /// Calculam X pentru centrare exacta pe latimea virtuala de 800.
            int mainTitleX = (800 - fmMain.stringWidth(mainTitle)) / 2;
            int mainTitleY = 201;

            /// Drop shadow (offset +4px) pentru efect de adancime.
            g2.setColor(new Color(15, 18, 30, 200));
            g2.drawString(mainTitle, mainTitleX + 4, mainTitleY + 4);

            /// Titlul propriu-zis in alb-gri.
            g2.setColor(new Color(225, 225, 232));
            g2.drawString(mainTitle, mainTitleX, mainTitleY);

            /// SUBTITLUL — "Rise of the Fallen Crown", SansSerif Plain 22pt, centrat.
            String subTitle = "Rise of the Fallen Crown";
            Font   subFont  = new Font("SansSerif", Font.PLAIN, 22);
            g2.setFont(subFont);
            FontMetrics fmSub = g2.getFontMetrics(subFont);

            int subTitleX = (800 - fmSub.stringWidth(subTitle)) / 2;
            int subTitleY = 244;

            /// Drop shadow subtitlu (offset +2px).
            g2.setColor(new Color(15, 18, 30, 200));
            g2.drawString(subTitle, subTitleX + 2, subTitleY + 2);

            /// Subtitlul propriu-zis.
            g2.setColor(new Color(225, 225, 232));
            g2.drawString(subTitle, subTitleX, subTitleY);

            /// Eliberam contextul grafic creat cu g.create() pentru a preveni memory leak.
            g2.dispose();
        }
    }
}


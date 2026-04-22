package PaooGame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Point2D;

/*! \class MenuWindow
    \brief Implementeaza fereastra de meniu a jocului, ruland in modul Fullscreen.

    Responsabila pentru afisarea meniului principal, preluarea rezolutiei optime a
    ecranului si centrarea dinamica a elementelor grafice indiferent de monitor.
 */
public class MenuWindow extends JFrame {

    private int screenWidth;    /*!< Latimea ecranului curent in pixeli, calculata dinamic.*/
    private int screenHeight;   /*!< Inaltimea ecranului curent in pixeli, calculata dinamic.*/

    /*! \fn public MenuWindow()
        \brief Constructorul ferestrei de meniu.
        Pregateste modul fullscreen si centreaza butoanele de actiune.
     */
    public MenuWindow() {
        /// Ascunderea marginilor ferestrei (OBLIGATORIU pentru fullscreen).
        /// Trebuie apelata inainte ca fereastra sa devina vizibila pentru a evita exceptii.
        setUndecorated(true);

        setTitle("Aethelgard: Rise of the Fallen Crown");

        /// Preluarea rezolutiei dinamice a sistemului de operare (ex: 1920x1080).
        /// Inlocuieste dimensiunile fixe (800x600) pentru a se adapta oricarui monitor.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;

        setSize(screenWidth, screenHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        MenuPanel panel = new MenuPanel();
        panel.setLayout(null);

        /// Calcularea coordonatelor pentru centrarea dinamica a butoanelor.
        /// Axa X este centrata scazand latimea butonului din latimea ecranului.
        int buttonWidth = 200;
        int centerX = (screenWidth - buttonWidth) / 2;
        int startY = screenHeight / 2 + 50;

        JButton playButton = createMenuButton("PLAY");
        playButton.setBounds(centerX, startY, buttonWidth, 52);
        playButton.addActionListener(e -> {
            dispose();
            /// Transmitem noile dimensiuni calculate (rezolutia maxima) catre clasa Game.
            Game paooGame = new Game("Aethelgard", screenWidth, screenHeight);
            paooGame.StartGame();
        });
        panel.add(playButton);

        JButton loadButton = createMenuButton("LOAD GAME");
        loadButton.setBounds(centerX, startY + 65, buttonWidth, 52);
        loadButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Load Game va fi implementat in saptamana 12.")
        );
        panel.add(loadButton);

        JButton exitButton = createMenuButton("EXIT");
        exitButton.setBounds(centerX, startY + 130, buttonWidth, 44);
        exitButton.addActionListener(e -> System.exit(0));
        panel.add(exitButton);

        /// Butonul de setari este ancorat in coltul din dreapta sus, relativ la latimea ecranului.
        JButton settingsButton = createTopButton("SETTINGS");
        settingsButton.setBounds(screenWidth - 165, 22, 130, 34);
        settingsButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Settings este momentan placeholder.")
        );
        panel.add(settingsButton);

        setContentPane(panel);

        /// Activarea modului Full-Screen Exclusive (FSEM).
        /// Interogheaza placa video daca suporta fullscreen si preia controlul monitorului.
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();
        try {
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(this);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                setVisible(true);
            }
        } catch (Exception e) {
            setVisible(true);
        }
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Serif", Font.BOLD,18));
        button.setForeground(new Color(235,235,240));
        button.setBackground(new Color(28,32,46));
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(new Color(160,165,185),2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createTopButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif",Font.BOLD,13));
        button.setForeground(new Color(235,235,240));
        button.setBackground(new Color(28,32,46));
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(new Color(160,165,185),2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    /*! \class MenuPanel
        \brief Panoul in care se deseneaza elementele grafice ale meniului.
     */
    class MenuPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            /// Coeficientul de marire (Scaling Factor).
            /// Calculeaza de cate ori s-a marit ecranul fata de dimensiunea standard de 800x600.
            double scaleX = (double) w / 800.0;
            double scaleY = (double) h / 600.0;

            GradientPaint sky = new GradientPaint(
                    new Point2D.Float(0,0), new Color(8,12,24),
                    new Point2D.Float(0,h), new Color(30,34,52)
            );

            g2.setPaint(sky);
            g2.fillRect(0,0,w,h);

            /// Aplicarea scalarii virtuale.
            /// Toate elementele desenate in continuare vor fi marite automat cu factorii scaleX si scaleY,
            /// pastrand proportiile graficii originale, indiferent de monitor.
            g2.scale(scaleX, scaleY);

            g2.setColor(new Color(220,220,235,120));
            int[][] stars = {
                    {70,70}, {130,95}, {210, 60}, {290,100}, {360,70},
                    {450,95},{520,55}, {610,110}, {700,80}, {745,130}
            };
            for (int[] s : stars) {
                g2.fillOval(s[0],s[1],3,3);
            }
            g2.setColor(new Color(232,232,225));
            g2.fillOval(545,70,72,72);
            g2.setColor(new Color(195,195,188));
            g2.fillOval(565,88,10,10);
            g2.fillOval(586,97,8,8);
            g2.fillOval(555,108,7,7);


            g2.setColor(new Color(82,88,118));
            Polygon m1 = new Polygon();
            m1.addPoint(10,390); m1.addPoint(145,150); m1.addPoint(280,390);
            g2.fillPolygon(m1);

            Polygon m2 = new Polygon();
            m2.addPoint(170,390); m2.addPoint(330,100); m2.addPoint(490,390);
            g2.fillPolygon(m2);

            Polygon m3 = new Polygon();
            m3.addPoint(380,390); m3.addPoint(540,170); m3.addPoint(680,390);
            g2.fillPolygon(m3);

            Polygon m4 = new Polygon();
            m4.addPoint(560,390); m4.addPoint(720,140); m4.addPoint(860,390);
            g2.fillPolygon(m4);

            g2.setColor(new Color(48,54,78,110));
            Polygon s1 = new Polygon();
            s1.addPoint(145,150); s1.addPoint(200,390); s1.addPoint(280,390);
            g2.fillPolygon(s1);

            Polygon s2 = new Polygon();
            s2.addPoint(330,100); s2.addPoint(385,390); s2.addPoint(490,390);
            g2.fillPolygon(s2);

            Polygon s3 = new Polygon();
            s3.addPoint(540,170); s3.addPoint(580,390); s3.addPoint(645,390);
            g2.fillPolygon(s3);

            Polygon s4 = new Polygon();
            s4.addPoint(720,140); s4.addPoint(760,390); s4.addPoint(830,390);
            g2.fillPolygon(s4);

            g2.setColor(new Color(36,42,58));
            Polygon ridge = new Polygon();
            ridge.addPoint(0,430); ridge.addPoint(90,405); ridge.addPoint(180,425);
            ridge.addPoint(280,395); ridge.addPoint(380,420); ridge.addPoint(470,400);
            ridge.addPoint(560,430); ridge.addPoint(660,405); ridge.addPoint(760,425);
            ridge.addPoint(800,415); ridge.addPoint(800,540); ridge.addPoint(0,540);
            g2.fillPolygon(ridge);

            g2.setColor(new Color(210,210,225,20));
            g2.fillRoundRect(0,395,800,55,30,30);

            g2.setColor(new Color(68,88,66));
            g2.fillArc(-80,505,240,110,0,180);
            g2.fillArc(620,505,260,110,0,180);

            g2.setColor(new Color(72,54,42));
            g2.fillRect(0,540,800,60);

            g2.setFont(new Font("Serif", Font.BOLD,58));
            g2.setColor(new Color(225,225,232));
            g2.drawString("AETHELGARD",58,201);

            g2.setFont(new Font("SansSerif", Font.PLAIN,22));
            g2.setColor(new Color(225,225,232));
            g2.drawString("Rise of the Fallen Crown",165,244);

            g2.dispose();
        }
    }
}
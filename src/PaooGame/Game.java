package PaooGame;

import PaooGame.enemies.Enemy;
import PaooGame.enemies.Skeleton;

import PaooGame.GameWindow.GameWindow;
import PaooGame.Graphics.Assets;
import PaooGame.Tiles.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

/*! \class Game
    \brief Clasa principala a proiectului. Gestioneaza Game-Loop-ul, starile jocului si randarea.
 */
public class Game implements Runnable {
    private GameWindow wnd;                /*!< Fereastra in care se va desena tabla jocului.*/
    private boolean runState;           /*!< Flag ce indica starea firului de executie.*/
    private Thread gameThread;         /*!< Referinta catre thread-ul de update si draw.*/
    private BufferStrategy bs;                 /*!< Mecanism pentru organizarea memoriei video (Triple Buffering).*/

    private Map map;                /*!< Referinta catre harta curenta.*/
    private KeyManager keyManager;         /*!< Referinta catre managerul de tastatura.*/
    private Camera camera;             /*!< Referinta catre camera de urmarire a jucatorului.*/

    /// ENTITATILE JOCULUI
    private Player player;             /*!< Referinta catre jucatorul principal.*/
    private Enemy wolf;               /*!< Referinta catre inamicul Lup (Nivel 1).*/
    private Skeleton skeleton;           /*!< Referinta catre inamicul Schelet (Nivel 2).*/

    /// VARIABILE PENTRU NIVELURI SI TRANZITII
    private int currentLevel;       /*!< Numarul nivelului curent.*/
    private int transitionCooldown; /*!< Temporizator pentru a preveni tranzitiile instantanee repetate.*/

    /// DIMENSIUNI LOGICE ALE ECRANULUI
    private final int LOGICAL_WIDTH = 800;  /*!< Latimea logica a ferestrei.*/
    private final int LOGICAL_HEIGHT = 600; /*!< Inaltimea logica a ferestrei.*/

    private Graphics g;                  /*!< Referinta catre contextul grafic.*/

    /// VARIABILE PENTRU PAUZA SI MENIU
    private boolean isPaused = false;       /*!< Flag care indica daca jocul este in pauza.*/
    private int pauseMenuSelection = 0; /*!< Indexul optiunii selectate in meniul de pauza.*/
    private volatile boolean returnToMenuRequested = false;

    /// VARIABILE PENTRU DEBUG MODE (Tasta H)
    public static boolean showHitboxes = false;   /*!< Variabila globala pentru afisarea coliziunilor.*/
    public static int currentFPS = 0;         /*!< Stocheaza numarul de cadre pe secunda calculate.*/

    /// MEMORAREA STARII TASTELOR (Pentru a detecta o singura apasare)
    private boolean lastEscapeState = false;
    private boolean lastUpState = false;
    private boolean lastDownState = false;
    private boolean lastEnterState = false;
    private boolean lastDebugState = false;

    /*! \fn public Game(String title, int width, int height)
        \brief Constructorul clasei Game.
     */
    public Game(String title, int width, int height) {
        wnd = new GameWindow(title, width, height);
        runState = false;
    }

    /*! \fn private void InitGame()
        \brief Initializeaza resursele, fereastra si obiectele de joc.
     */
    private void InitGame() {
        wnd.BuildGameWindow();
        Assets.Init();

        currentLevel = 1;
        transitionCooldown = 0;

        /// Cream managerul de taste si il atasam ferestrei (Canvas-ului)
        keyManager = new KeyManager();
        wnd.GetCanvas().addKeyListener(keyManager);
        wnd.GetCanvas().setFocusable(true);
        wnd.GetCanvas().requestFocus();

        /// Incarcam nivelul initial
        loadLevel(currentLevel);

        /// Initializam camera (dimensiunile trebuie sa corespunda cu LOGICAL_WIDTH / HEIGHT)
        camera = new Camera(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
    }

    /*! \fn public void run()
        \brief Implementarea Game-Loop-ului cu limitare la 60 FPS si contorizare pentru Overlay-ul de Debug.
     */
    public void run() {
        /// Inițializează fereastra, resursele, harta, camera și player-ul.
        InitGame();

        /// Timpul frame-ului anterior.
        long oldTime = System.nanoTime();

        /// Timpul frame-ului curent.
        long curentTime;

        /// Numărul dorit de cadre pe secundă.
        final int framesPerSecond = 60;

        /// Durata unui frame exprimată în nanosecunde.
        final double timeFrame = 1000000000 / framesPerSecond;

        /// Contor pentru FPS.
        int frames = 0;

        /// Timer folosit pentru actualizarea valorii currentFPS o dată pe secundă.
        long timer = System.currentTimeMillis();

        /*
         * Bucla principală a jocului.
         * Cât timp runState este true, jocul actualizează starea și redesenează scena.
         */
        while (runState == true) {
            /// Citim timpul curent.
            curentTime = System.nanoTime();

            /*
             * Dacă a trecut suficient timp pentru un nou frame,
             * actualizăm logica și desenăm scena.
             */
            if ((curentTime - oldTime) > timeFrame) {
                /// Actualizează logica jocului: input, player, hartă, pauză, tranziții etc.
                Update();

                /*
                 * Foarte important:
                 * Dacă în Update() s-a cerut oprirea jocului, nu mai apelăm Draw().
                 *
                 * Motiv:
                 * Dacă jucătorul alege RETURN TO MENU, fereastra jocului urmează să fie
                 * închisă. Dacă Draw() mai încearcă să facă bs.show() pe un Canvas invalid,
                 * apare eroarea:
                 *
                 * java.lang.IllegalStateException: Component must have a valid peer
                 */
                if (!runState) {
                    break;
                }

                /// Desenează frame-ul curent.
                Draw();

                /// Actualizăm timpul frame-ului anterior.
                oldTime = curentTime;

                /// Creștem contorul de frame-uri.
                frames++;
            }

            /*
             * Actualizăm FPS-ul afișat o dată pe secundă.
             */
            if (System.currentTimeMillis() - timer > 1000) {
                currentFPS = frames;
                frames = 0;
                timer += 1000;
            }
        }

        /*
         * Daca jocul s-a oprit deoarece jucatorul a ales RETURN TO MENU,
         * inchidem fereastra jocului si recream meniul principal.
         */
        if (returnToMenuRequested) {
            SwingUtilities.invokeLater(() -> {
                /// Inchidem corect fereastra jocului.
                if (wnd != null) {
                    wnd.CloseWindow();
                }

                /*
                 * Sincronizam toolkit-ul grafic.
                 * Ajuta la evitarea artefactelor vizuale dupa inchiderea ferestrei jocului.
                 */
                Toolkit.getDefaultToolkit().sync();

                /*
                 * Cream meniul intr-un pas Swing separat.
                 * Asa evitam ca meniul sa fie construit in aceeasi stare grafica in care
                 * tocmai s-a inchis fereastra jocului.
                 */
                SwingUtilities.invokeLater(() -> {
                    MenuWindow menu = new MenuWindow();
                    menu.setVisible(true);
                });
            });
        }
    }

    public synchronized void StartGame() {
        if (runState == false) {
            runState = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public synchronized void StopGame() {
        /*
         * Oprește bucla principală a jocului.
         * Metoda este sincronizată deoarece modifică starea thread-ului jocului.
         */
        if (runState == true) {
            /// Setăm runState pe false, ceea ce oprește bucla while din run().
            runState = false;

            /*
             * Dacă StopGame() este apelată chiar din thread-ul jocului,
             * NU avem voie să facem join() pe același thread.
             *
             * Altfel, thread-ul ar aștepta după el însuși și aplicația s-ar putea bloca.
             */
            if (Thread.currentThread() == gameThread) {
                return;
            }

            /// Așteptăm terminarea thread-ului jocului, dacă suntem din alt thread.
            try {
                gameThread.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*! \fn private void Update()
        \brief Actualizeaza logica jocului, verifica intrarile utilizatorului si gestioneaza starea entitatilor.
     */
    private void Update() {
        /// Citim starea tastelor
        keyManager.Update();

        /// 1. GESTIONARE DEBUG MODE (Toggle Hitboxes)
        if (keyManager.debug && !lastDebugState) {
            showHitboxes = !showHitboxes;
        }
        lastDebugState = keyManager.debug;

        /// 2. GESTIONARE INTRARE/IESIRE PAUZA
        if (keyManager.escape && !lastEscapeState) {
            isPaused = !isPaused;
        }
        lastEscapeState = keyManager.escape;

        /// 3. LOGICA MENIULUI DE PAUZA
        if (isPaused) {
            if (keyManager.up && !lastUpState) {
                pauseMenuSelection--;
                if (pauseMenuSelection < 0) pauseMenuSelection = 2;
            }
            if (keyManager.down && !lastDownState) {
                pauseMenuSelection++;
                if (pauseMenuSelection > 2) pauseMenuSelection = 0;
            }
            if (keyManager.enter && !lastEnterState) {
                executePauseMenuAction();
            }

            lastUpState = keyManager.up;
            lastDownState = keyManager.down;
            lastEnterState = keyManager.enter;

            /// Oprim update-ul elementelor de joc pe durata pauzei
            return;
        }

        /// 4. ACTUALIZARE ENTITATI (Cand jocul nu este in pauza)
        if (player != null && map != null) {
            player.Update(keyManager, map);
        }

        /// Inamic Nivel 1
        if (wolf != null && map != null) {
            wolf.Update(map);
        }

        /// Inamic Nivel 2
        if (skeleton != null && map != null) {
            skeleton.Update(map);
        }

        /// Centram camera pe coordonatele jucatorului
        if (camera != null && player != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        /// Gestionam temporizatorul pentru tranzitia intre niveluri
        if (transitionCooldown > 0) {
            transitionCooldown--;
        }

        /// Verificam daca jucatorul a ajuns la finalul nivelului curent
        checkLevelTransition();
    }

    /*! \fn private void executePauseMenuAction()
        \brief Executa logica butonului apasat in meniul de pauza.
     */

    private void executePauseMenuAction() {
        /*
         * Execută acțiunea corespunzătoare opțiunii selectate
         * din meniul de pauză.
         */
        switch (pauseMenuSelection) {
            case 0: /// SETTINGS
                /*
                 * Deschidem dialogul de setări peste fereastra jocului.
                 * Folosim invokeLater deoarece modificările Swing trebuie făcute
                 * pe Event Dispatch Thread.
                 */
                Window owner = SwingUtilities.getWindowAncestor(wnd.GetCanvas());
                SwingUtilities.invokeLater(() -> SettingsDialog.showSettings(owner));
                break;

            case 1: /// RETURN TO MENU
                /*
                 * Cerem oprirea jocului și revenirea în meniul principal.
                 * Nu închidem direct fereastra aici, pentru că suntem în logica jocului.
                 */
                returnToMainMenu();
                break;

            case 2: /// EXIT
            /// Închide complet aplicația.
                System.exit(0);
                break;
        }
    }


    private void returnToMainMenu() {
        /*
         * Marcăm faptul că după oprirea buclei jocului trebuie să revenim
         * în meniul principal.
         */
        returnToMenuRequested = true;

        /*
         * Oprim bucla principală.
         * După aceasta, metoda run() va ieși din while și va crea meniul principal.
         */
        runState = false;
    }


    /*! \fn private void Draw()
        \brief Metoda responsabila de randarea tuturor elementelor grafice pe ecran.
     */
    private void Draw() {
        if (wnd == null || wnd.GetCanvas() == null || !wnd.GetCanvas().isDisplayable()) {
            return;
        }

        bs = wnd.GetCanvas().getBufferStrategy();
        if (bs == null) {
            try {
                wnd.GetCanvas().createBufferStrategy(3);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        g = bs.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) g;

        /// Antialiasing pentru un text clar si margini netede
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        /// Sistem de scalare: Adaptam randarea la dimensiunea fizica a ferestrei
        double scaleX = (double) wnd.GetWndWidth() / LOGICAL_WIDTH;
        double scaleY = (double) wnd.GetWndHeight() / LOGICAL_HEIGHT;
        g2d.scale(scaleX, scaleY);

        /// Fundal negru de baza (previne glitch-urile grafice la marginile hartii)
        g.setColor(new Color(10, 15, 10));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        /// Calculam un offset in cazul in care harta este mai mica decat ecranul pentru a o centra
        int offsetX = 0;
        int offsetY = 0;
        if (map != null) {
            if (map.getPixelWidth() < LOGICAL_WIDTH) offsetX = (LOGICAL_WIDTH - map.getPixelWidth()) / 2;
            if (map.getPixelHeight() < LOGICAL_HEIGHT) offsetY = (LOGICAL_HEIGHT - map.getPixelHeight()) / 2;
        }

        /// 1. Desenam Harta
        if (map != null && camera != null) {
            map.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 2. Desenam Inamicii (pe straturi inferioare jucatorului)
        if (wolf != null && camera != null) {
            wolf.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }
        if (skeleton != null && camera != null) {
            skeleton.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 3. Desenam Jucatorul (deasupra entitatilor)
        if (player != null && camera != null) {
            player.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 4. Desenam foreground-ul hartii, adica obiectele care trebuie sa apara peste player.
        if (map != null && camera != null) {
            map.DrawForeground(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }
        /// 5. Desenam Meniul de Pauza (Overlay Semi-Transparent)
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

            String[] options = {"SETTINGS", "RETURN TO MENU", "EXIT"};
            Font font = new Font("Serif", Font.BOLD, 36);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics(font);

            int startY = 250;
            for (int i = 0; i < options.length; i++) {
                int textWidth = fm.stringWidth(options[i]);
                int x = (LOGICAL_WIDTH - textWidth) / 2;
                int y = startY + (i * 70);

                if (i == pauseMenuSelection) {
                    g2d.setColor(new Color(218, 165, 32));
                    String selText = "> " + options[i] + " <";
                    int selWidth = fm.stringWidth(selText);
                    g2d.drawString(selText, (LOGICAL_WIDTH - selWidth) / 2, y);
                } else {
                    g2d.setColor(new Color(180, 180, 190));
                    g2d.drawString(options[i], x, y);
                }
            }
        }
        if (GameSettings.cinematicMode) {
            drawCinematicVignette(g2d);
        }
        /// 6. OVERLAY DEVELOPER (FPS si Mod Debug) - Desenat ultimul, sa fie peste orice
        if (showHitboxes) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString("FPS: " + currentFPS, 15, 30);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2d.drawString("Mod Debug: ON", 15, 50);
        }

        if (runState && wnd.GetCanvas().isDisplayable()) {
            bs.show();
        }

        g.dispose();
    }

    private void drawCinematicVignette(Graphics2D g2d) {
        // Benzi cinematice sus-jos
        g2d.setColor(new Color(0, 0, 0, 70));
        g2d.fillRect(0, 0, LOGICAL_WIDTH, 38);
        g2d.fillRect(0, LOGICAL_HEIGHT - 38, LOGICAL_WIDTH, 38);

        // Umbra laterala stanga
        GradientPaint leftShadow = new GradientPaint(
                0, 0, new Color(0, 0, 0, 85),
                90, 0, new Color(0, 0, 0, 0)
        );
        g2d.setPaint(leftShadow);
        g2d.fillRect(0, 0, 90, LOGICAL_HEIGHT);

        // Umbra laterala dreapta
        GradientPaint rightShadow = new GradientPaint(
                LOGICAL_WIDTH, 0, new Color(0, 0, 0, 85),
                LOGICAL_WIDTH - 90, 0, new Color(0, 0, 0, 0)
        );
        g2d.setPaint(rightShadow);
        g2d.fillRect(LOGICAL_WIDTH - 90, 0, 90, LOGICAL_HEIGHT);
    }

    /*! \fn private void loadLevel(int level)
       \brief Instanteaza si reseteaza harta si entitatile corespunzatoare noului nivel.
    */

    private void setPlayerSpawn(int tileCol, int tileRow) {
        /*
         * Calculează poziția de spawn a player-ului pornind de la coordonate de tile.
         *
         * tileCol = coloana din hartă
         * tileRow = rândul din hartă
         *
         * Conversia este:
         * poziție în pixeli = poziție tile * dimensiune tile
         */

        /// Calculăm coordonata X în pixeli.
        float spawnX = tileCol * Tile.TILE_WIDTH + (Tile.TILE_WIDTH - player.GetWidth()) / 2.0f;

        /// Calculăm coordonata Y în pixeli.
        float spawnY = tileRow * Tile.TILE_HEIGHT + (Tile.TILE_HEIGHT - player.GetHeight()) / 2.0f;

        /// Mutăm player-ul la noua poziție.
        player.setPosition(spawnX, spawnY);

        /*
         * Recentram camera pe player după schimbarea nivelului.
         * Fără acest pas, camera poate rămâne raportată la poziția din nivelul anterior.
         */
        if (camera != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        /*
         * Curățăm tastele apăsate.
         *
         * Motiv:
         * Dacă player-ul intră în nivelul următor ținând apăsată o tastă,
         * jocul poate aplica imediat încă o deplasare după spawn.
         * De aceea părea că personajul apare mai încolo decât ar trebui.
         */
        if (keyManager != null) {
            keyManager.Clear();
        }
    }

    private void loadLevel(int level) {
        currentLevel = level;

        /// Resetam toti inamicii curenti la tranzitie pentru a curata memoria
        wolf = null;
        skeleton = null;

        if (level == 1) {
            /// Încărcăm harta primului nivel.
            map = new Map(
                    "res/maps/level1_base.png",
                    "res/maps/level1_foreground.png",
                    "res/maps/harta_primul_nivel.tmx"
            );

            /*
             * Dacă player-ul nu există încă, îl creăm.
             * Poziția inițială va fi apoi setată prin setPlayerSpawn().
             */
            if (player == null) {
                player = new Player(10 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT);
            }

            /// Spawn-ul player-ului în nivelul 1.
            setPlayerSpawn(10, 14);

            /// Instantiem doar inamicul corespunzator Nivelului 1
            wolf = new Enemy(15 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT, player);
        } else if (level == 2) {
            /// Încărcăm harta celui de-al doilea nivel.
            map = new Map("res/maps/level2.txt");

            /// Spawn-ul player-ului în nivelul 2.
            setPlayerSpawn(9, 13);

            /// spawn pointul scheletului (Coloana 5, Randul 6)
            int liberX = 5;
            int liberY = 6;

            skeleton = new Skeleton(liberX * Tile.TILE_WIDTH, liberY * Tile.TILE_HEIGHT, player);
        } else if (level == 3) {
            /// Încărcăm harta celui de-al treilea nivel.
            map = new Map("res/maps/level3.txt");
            setPlayerSpawn(9, 13);

            /// In Nivelul 3 jucatorul va intalni provocari noi (Inamici setati temporar pe null)
        }

        transitionCooldown = 30;
    }

    /*! \fn private void checkLevelTransition()
        \brief Verifica pozitia jucatorului fata de coordonatele iesirii nivelului curent.
     */
    private void checkLevelTransition() {
        if (transitionCooldown > 0) return;

        /*
         * Pentru tranzitii folosim pozitia picioarelor, nu centrul sprite-ului.
         * Altfel trecerea se activeaza prea devreme, pentru ca sprite-ul playerului
         * este desenat mai sus decat hitbox-ul real.
         */
        int playerFeetX = (int) player.GetFeetCenterX();
        int playerFeetY = (int) player.GetFeetBottomY();

        if (currentLevel == 1) {
            /*
             * Nivelul 1 nu mai trece in dungeon prin poarta principala.
             * Tranzitia se face doar cand picioarele player-ului ajung
             * pe tile-ul marcat in layer-ul TransitionToDungeon.
             */
            if (map.isTransitionAtPixel(playerFeetX, playerFeetY)) {
                loadLevel(2);
            }
        }
        else if (currentLevel == 2) {
            /// Iesire Nivel 2: ramane logica veche catre nivelul 3.
            int iesireX_Stanga = 8;
            int iesireX_Dreapta = 12;

            int playerCenterX = (int) player.GetX() + player.GetWidth() / 2;

            if (player.GetY() <= 20 &&
                    playerCenterX >= iesireX_Stanga * Tile.TILE_WIDTH &&
                    playerCenterX <= iesireX_Dreapta * Tile.TILE_WIDTH) {
                loadLevel(3);
            }
        }
    }
}



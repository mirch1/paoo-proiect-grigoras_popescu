package PaooGame;

import PaooGame.GameWindow.GameWindow;
import PaooGame.Graphics.Assets;
import PaooGame.Tiles.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

/*! \class Game
    \brief Clasa principala a proiectului. Gestioneaza Game-Loop-ul, starile jocului si randarea.
 */
public class Game implements Runnable
{
    private GameWindow      wnd;                /*!< Fereastra in care se va desena tabla jocului.*/
    private boolean         runState;           /*!< Flag ce indica starea firului de executie.*/
    private Thread          gameThread;         /*!< Referinta catre thread-ul de update si draw.*/
    private BufferStrategy  bs;                 /*!< Mecanism pentru organizarea memoriei video (Triple Buffering).*/

    private Map             map;                /*!< Referinta catre harta curenta.*/
    private KeyManager      keyManager;         /*!< Referinta catre managerul de tastatura.*/
    private Player          player;             /*!< Referinta catre jucator.*/
    private Enemy           testEnemy;          /*!< Referinta catre inamicul de tip Lup.*/
    private Camera          camera;             /*!< Referinta catre camera de urmarire.*/

    private int             currentLevel;       /*!< Numarul nivelului curent.*/
    private int             transitionCooldown; /*!< Temporizator pentru tranzitiile intre nivele.*/

    private final int       LOGICAL_WIDTH = 800;  /*!< Latimea logica a ferestrei.*/
    private final int       LOGICAL_HEIGHT = 600; /*!< Inaltimea logica a ferestrei.*/

    private Graphics        g;                  /*!< Referinta catre contextul grafic.*/

    // --- VARIABILE PENTRU PAUZA SI DEBUG ---
    private boolean         isPaused = false;   /*!< Flag pentru starea de pauza.*/
    private int             pauseMenuSelection = 0; /*!< Indexul optiunii selectate in meniul de pauza.*/

    public static boolean   showHitboxes = false; /*!< Variabila globala pentru afisarea coliziunilor (Debug).*/
    public static int       currentFPS = 0; /*!< Numarul de cadre pe secunda (Frames Per Second).*/

    private boolean         lastEscapeState = false; /*!< Retine starea ESC din cadrul anterior.*/
    private boolean         lastUpState = false;     /*!< Retine starea UP din cadrul anterior.*/
    private boolean         lastDownState = false;   /*!< Retine starea DOWN din cadrul anterior.*/
    private boolean         lastEnterState = false;  /*!< Retine starea ENTER din cadrul anterior.*/
    private boolean         lastDebugState = false;  /*!< Retine starea H din cadrul anterior.*/

    /*! \fn public Game(String title, int width, int height)
        \brief Constructorul clasei Game.
     */
    public Game(String title, int width, int height)
    {
        wnd = new GameWindow(title, width, height);
        runState = false;
    }

    /*! \fn private void InitGame()
        \brief Initializeaza resursele, fereastra si obiectele de joc.
     */
    private void InitGame()
    {
        wnd.BuildGameWindow();
        Assets.Init();

        currentLevel = 1;
        transitionCooldown = 0;

        /// Mai intai cream keyManager-ul pentru a-l putea transmite
        keyManager = new KeyManager();
        wnd.GetCanvas().addKeyListener(keyManager);
        wnd.GetCanvas().setFocusable(true);
        wnd.GetCanvas().requestFocus();

        /// Incarcam nivelul initial
        loadLevel(currentLevel);

        camera = new Camera(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
    }

    /*! \fn public void run()
        \brief Implementarea Game-Loop-ului cu contorizare FPS.
     */
    public void run()
    {
        InitGame();
        long oldTime = System.nanoTime();
        long curentTime;

        final int framesPerSecond   = 60;
        final double timeFrame      = 1000000000 / framesPerSecond;

        /// Variabile pentru contorizarea FPS-ului
        int frames = 0;
        long timer = System.currentTimeMillis();

        while (runState == true)
        {
            curentTime = System.nanoTime();
            if((curentTime - oldTime) > timeFrame)
            {
                Update();
                Draw();
                oldTime = curentTime;
                frames++; /// Am desenat un cadru nou
            }

            /// Daca a trecut o secunda, salvam FPS-ul si resetam contorul
            if (System.currentTimeMillis() - timer > 1000) {
                currentFPS = frames;
                frames = 0;
                timer += 1000;
            }
        }
    }

    public synchronized void StartGame()
    {
        if(runState == false)
        {
            runState = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public synchronized void StopGame()
    {
        if(runState == true)
        {
            runState = false;
            try { gameThread.join(); }
            catch(InterruptedException ex) { ex.printStackTrace(); }
        }
    }

    /*! \fn private void Update()
        \brief Actualizeaza logica jocului, meniului si starii de debug.
     */
    private void Update()
    {
        keyManager.Update();

        /// 1. GESTIONARE DEBUG MODE (Toggle Hitboxes)
        if (keyManager.debug && !lastDebugState) {
            showHitboxes = !showHitboxes;
        }
        lastDebugState = keyManager.debug;

        /// 2. GESTIONARE INTRARE/IESIRE PAUZA (Toggle Pause)
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

            return; /// Blocam restul update-ului daca suntem in pauza
        }

        /// 4. ACTUALIZAREA JOCULUI (CAND NU ESTE IN PAUZA)
        if (player != null && map != null) {
            player.Update(keyManager, map);
        }

        if (testEnemy != null && map != null) {
            testEnemy.Update(map);
        }

        if (camera != null && player != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        if (transitionCooldown > 0) {
            transitionCooldown--;
        }

        checkLevelTransition();
    }

    /*! \fn private void executePauseMenuAction()
        \brief Executa actiunea corespunzatoare butonului selectat in pauza.
     */
    private void executePauseMenuAction() {
        switch (pauseMenuSelection) {
            case 0: /// SETTINGS (Momentan inactiv)
                break;
            case 1: /// RETURN TO MENU
                StopGame();
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(wnd.GetCanvas());
                if (frame != null) frame.dispose();
                MenuWindow menu = new MenuWindow();
                menu.setVisible(true);
                break;
            case 2: /// EXIT
                System.exit(0);
                break;
        }
    }

    /*! \fn private void Draw()
        \brief Randarea elementelor grafice.
     */
    private void Draw()
    {
        bs = wnd.GetCanvas().getBufferStrategy();
        if(bs == null)
        {
            try { wnd.GetCanvas().createBufferStrategy(3); return; }
            catch (Exception e) { e.printStackTrace(); }
        }

        g = bs.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) g;

        /// Activam antialiasing pentru text si grafici fine
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        /// Scalare pentru rezolutia ferestrei
        double scaleX = (double) wnd.GetWndWidth() / LOGICAL_WIDTH;
        double scaleY = (double) wnd.GetWndHeight() / LOGICAL_HEIGHT;
        g2d.scale(scaleX, scaleY);

        /// Fundal negru/inchis
        g.setColor(new Color(10, 15, 10));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        int offsetX = 0;
        int offsetY = 0;

        if(map != null) {
            if(map.getPixelWidth() < LOGICAL_WIDTH) offsetX = (LOGICAL_WIDTH - map.getPixelWidth()) / 2;
            if(map.getPixelHeight() < LOGICAL_HEIGHT) offsetY = (LOGICAL_HEIGHT - map.getPixelHeight()) / 2;
        }

        /// 1. Desenare Harta
        if (map != null && camera != null) {
            map.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 2. Desenare Inamic (sub player)
        if (testEnemy != null && camera != null) {
            testEnemy.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 3. Desenare Player
        if (player != null && camera != null) {
            player.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 4. Desenare Meniu Pauza (Overlay intunecat)
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

        /// =========================================
        /// 5. OVERLAY DEVELOPER (FPS si Hitbox-uri)
        /// Se deseneaza PESTE TOT (chiar si peste pauza sau joc) cand H este apasat
        /// =========================================
        if (showHitboxes) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString("FPS: " + currentFPS, 15, 30);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2d.drawString("Mod Debug: ON", 15, 50);
        }

        bs.show();
        g.dispose();
    }

    /*! \fn private void loadLevel(int level)
        \brief Incarca harta si pozitioneaza entitatile pentru nivelul respectiv.
     */
    private void loadLevel(int level) {
        currentLevel = level;
        if (level == 1) {
            map = new Map("res/maps/level1.txt");

            /// Initializare Player
            if (player == null) player = new Player(10 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT);
            else player.setPosition(10 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT);

            /// Initializare Lup (Inamic)
            if (testEnemy == null) testEnemy = new Enemy(15 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT, player);
            else testEnemy.setPosition(15 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT);
        }
        else if (level == 2) {
            map = new Map("res/maps/level2.txt");
            player.setPosition(9 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT);
            testEnemy.setPosition(5 * Tile.TILE_WIDTH, 5 * Tile.TILE_HEIGHT);
        }
        transitionCooldown = 30;
    }

    private void checkLevelTransition() {
        if (transitionCooldown > 0) return;
        int feetX = (int) player.GetX() + player.GetWidth() / 2;
        int feetY = (int) player.GetY() + player.GetHeight() - 2;
        if (currentLevel == 1) {
            if (feetY <= 0 && feetX >= 8 * Tile.TILE_WIDTH && feetX <= 12 * Tile.TILE_WIDTH) loadLevel(2);
        }
    }

    public int GetWndWidth() { return wnd.GetWndWidth(); }
    public int GetWndHeight() { return wnd.GetWndHeight(); }
}
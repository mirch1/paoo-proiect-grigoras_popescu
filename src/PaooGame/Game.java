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
public class Game implements Runnable
{
    private GameWindow      wnd;                /*!< Fereastra in care se va desena tabla jocului.*/
    private boolean         runState;           /*!< Flag ce indica starea firului de executie.*/
    private Thread          gameThread;         /*!< Referinta catre thread-ul de update si draw.*/
    private BufferStrategy  bs;                 /*!< Mecanism pentru organizarea memoriei video (Triple Buffering).*/

    private Map             map;                /*!< Referinta catre harta curenta.*/
    private KeyManager      keyManager;         /*!< Referinta catre managerul de tastatura.*/
    private Camera          camera;             /*!< Referinta catre camera de urmarire a jucatorului.*/

    /// ENTITATILE JOCULUI
    private Player          player;             /*!< Referinta catre jucatorul principal.*/
    private Enemy           wolf;               /*!< Referinta catre inamicul Lup (Nivel 1).*/
    private Skeleton        skeleton;           /*!< Referinta catre inamicul Schelet (Nivel 2).*/

    /// VARIABILE PENTRU NIVELURI SI TRANZITII
    private int             currentLevel;       /*!< Numarul nivelului curent.*/
    private int             transitionCooldown; /*!< Temporizator pentru a preveni tranzitiile instantanee repetate.*/

    /// DIMENSIUNI LOGICE ALE ECRANULUI
    private final int       LOGICAL_WIDTH = 800;  /*!< Latimea logica a ferestrei.*/
    private final int       LOGICAL_HEIGHT = 600; /*!< Inaltimea logica a ferestrei.*/

    private Graphics        g;                  /*!< Referinta catre contextul grafic.*/

    /// VARIABILE PENTRU PAUZA SI MENIU
    private boolean         isPaused = false;       /*!< Flag care indica daca jocul este in pauza.*/
    private int             pauseMenuSelection = 0; /*!< Indexul optiunii selectate in meniul de pauza.*/

    /// VARIABILE PENTRU DEBUG MODE (Tasta H)
    public static boolean   showHitboxes = false;   /*!< Variabila globala pentru afisarea coliziunilor.*/
    public static int       currentFPS = 0;         /*!< Stocheaza numarul de cadre pe secunda calculate.*/

    /// MEMORAREA STARII TASTELOR (Pentru a detecta o singura apasare)
    private boolean         lastEscapeState = false;
    private boolean         lastUpState = false;
    private boolean         lastDownState = false;
    private boolean         lastEnterState = false;
    private boolean         lastDebugState = false;

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
    public void run()
    {
        InitGame();
        long oldTime = System.nanoTime();
        long curentTime;

        final int framesPerSecond   = 60;
        final double timeFrame      = 1000000000 / framesPerSecond;

        /// Variabile locale pentru a calcula FPS-ul in timp real
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
                frames++; /// Am randat inca un cadru
            }

            /// Daca a trecut fix o secunda, actualizam variabila statica si resetam
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
        \brief Actualizeaza logica jocului, verifica intrarile utilizatorului si gestioneaza starea entitatilor.
     */
    private void Update()
    {
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
        switch (pauseMenuSelection) {
            case 0: /// SETTINGS
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
        \brief Metoda responsabila de randarea tuturor elementelor grafice pe ecran.
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
        if(map != null) {
            if(map.getPixelWidth() < LOGICAL_WIDTH) offsetX = (LOGICAL_WIDTH - map.getPixelWidth()) / 2;
            if(map.getPixelHeight() < LOGICAL_HEIGHT) offsetY = (LOGICAL_HEIGHT - map.getPixelHeight()) / 2;
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

        /// 4. Desenam Meniul de Pauza (Overlay Semi-Transparent)
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

        /// 5. OVERLAY DEVELOPER (FPS si Mod Debug) - Desenat ultimul, sa fie peste orice
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
        \brief Instanteaza si reseteaza harta si entitatile corespunzatoare noului nivel.
     */
    private void loadLevel(int level) {
        currentLevel = level;

        /// Resetam toti inamicii curenti la tranzitie pentru a curata memoria
        wolf = null;
        skeleton = null;

        if (level == 1) {
            map = new Map("res/maps/level1.txt");

            if (player == null) player = new Player(10 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT);
            else player.setPosition(10 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT);

            /// Instantiem doar inamicul corespunzator Nivelului 1
            wolf = new Enemy(15 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT, player);
        }
        else if (level == 2) {
            map = new Map("res/maps/level2.txt");
            player.setPosition(9 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT);

            /// spawn pointul scheletului (Coloana 5, Randul 6)
            int liberX = 5;
            int liberY = 6;

            skeleton = new Skeleton(liberX * Tile.TILE_WIDTH, liberY * Tile.TILE_HEIGHT, player);
        }
        else if (level == 3) {
            map = new Map("res/maps/level3.txt");
            player.setPosition(5 * Tile.TILE_WIDTH, 10 * Tile.TILE_HEIGHT);

            /// In Nivelul 3 jucatorul va intalni provocari noi (Inamici setati temporar pe null)
        }

        transitionCooldown = 30;
    }

    /*! \fn private void checkLevelTransition()
        \brief Verifica pozitia jucatorului fata de coordonatele iesirii nivelului curent.
     */
    private void checkLevelTransition() {
        if (transitionCooldown > 0) return;

        /// Aflam coordonata centrului jucatorului pe axa X pentru a verifica alinierea pe carare
        int playerCenterX = (int) player.GetX() + player.GetWidth() / 2;

        if (currentLevel == 1) {
            /// Daca jucatorul a atins latura de sus (Y <= 20) in intervalul de X stabilit
            if (player.GetY() <= 20 && playerCenterX >= 8 * Tile.TILE_WIDTH && playerCenterX <= 12 * Tile.TILE_WIDTH) {
                loadLevel(2);
            }
        }
        else if (currentLevel == 2) {
            /// Iesire Nivel 2: Ajusteaza cifrele (ex. 8 si 12) la coordonatele cararii din level2.txt
            int iesireX_Stanga = 8;
            int iesireX_Dreapta = 12;

            if (player.GetY() <= 20 && playerCenterX >= iesireX_Stanga * Tile.TILE_WIDTH && playerCenterX <= iesireX_Dreapta * Tile.TILE_WIDTH) {
                loadLevel(3);
            }
        }
    }

    public int GetWndWidth() { return wnd.GetWndWidth(); }
    public int GetWndHeight() { return wnd.GetWndHeight(); }
}
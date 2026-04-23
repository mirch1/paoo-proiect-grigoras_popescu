package PaooGame;

import PaooGame.GameWindow.GameWindow;
import PaooGame.Graphics.Assets;
import PaooGame.Tiles.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

/*! \class Game
    \brief Clasa principala a intregului proiect. Implementeaza Game-Loop (Update -> Draw).
 */
public class Game implements Runnable
{
    private GameWindow      wnd;                /*!< Fereastra in care se va desena tabla jocului.*/
    private boolean         runState;           /*!< Flag ce indica starea firului de executie.*/
    private Thread          gameThread;         /*!< Referinta catre thread-ul de update si draw al ferestrei.*/
    private BufferStrategy  bs;                 /*!< Referinta catre un mecanism cu care se organizeaza memoria complexa pentru un canvas.*/

    private Map             map;                /*!< Referinta catre harta curenta a nivelului.*/
    private KeyManager      keyManager;         /*!< Referinta catre managerul de evenimente de tastatura.*/
    private Player          player;             /*!< Referinta catre personajul controlat de utilizator.*/
    private Camera          camera;             /*!< Referinta catre camera care urmareste jucatorul.*/

    private int             currentLevel;       /*!< Retine numarul nivelului curent incarcat.*/
    private int             transitionCooldown; /*!< Temporizator pentru a preveni tranzitiile accidentale repetate intre nivele.*/

    private final int       LOGICAL_WIDTH = 800;  /*!< Latimea logica/virtuala a jocului (spatiul in care functioneaza coliziunile).*/
    private final int       LOGICAL_HEIGHT = 600; /*!< Inaltimea logica/virtuala a jocului.*/

    private Graphics        g;                  /*!< Referinta catre contextul grafic.*/

    // --- VARIABILE NOI PENTRU MENIUL DE PAUZA ---
    private boolean isPaused = false;           /*!< Flag ce indica daca jocul se afla in starea de pauza.*/
    private int pauseMenuSelection = 0;         /*!< Indexul optiunii curente selectate in meniul de pauza (0=Settings, 1=Return, 2=Exit).*/

    private boolean lastEscapeState = false;    /*!< Retine starea tastei Escape din cadrul anterior pentru a evita declansarea repetata.*/
    private boolean lastUpState = false;        /*!< Retine starea tastei Up din cadrul anterior pentru navigarea in meniu.*/
    private boolean lastDownState = false;      /*!< Retine starea tastei Down din cadrul anterior pentru navigarea in meniu.*/
    private boolean lastEnterState = false;     /*!< Retine starea tastei Enter din cadrul anterior pentru validarea selectiei.*/

    /*! \fn public Game(String title, int width, int height)
        \brief Constructor de initializare al clasei Game.
     */
    public Game(String title, int width, int height)
    {
        /// Obiectul GameWindow este creat folosind dimensiunile reale primite din meniu.
        wnd = new GameWindow(title, width, height);
        /// Resetarea flagului runState ce indica starea firului de executie
        runState = false;
    }

    /*! \fn private void InitGame()
        \brief  Metoda construieste fereastra jocului, initializeaza aseturile, listenerul de tastatura etc.
     */
    private void InitGame()
    {
        /// Construieste fereastra jocului (cu rezolutia pastrata in wnd).
        wnd.BuildGameWindow();
        /// Se incarca toate elementele grafice (dale, sprite-uri)
        Assets.Init();

        currentLevel = 1;
        transitionCooldown = 0;
        loadLevel(currentLevel);

        keyManager = new KeyManager();
        wnd.GetCanvas().addKeyListener(keyManager);
        wnd.GetCanvas().setFocusable(true);
        wnd.GetCanvas().requestFocus();

        /// Camera va calcula totul pe baza spatiului virtual de 800x600,
        /// ramanand independenta de rezolutia fizica a monitorului.
        camera = new Camera(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
    }

    /*! \fn public void run()
        \brief Functia ce va rula in thread-ul creat, implementand Game-Loop-ul clasic.
     */
    public void run()
    {
        InitGame();
        long oldTime = System.nanoTime();
        long curentTime;

        final int framesPerSecond   = 60;
        final double timeFrame      = 1000000000 / framesPerSecond;

        while (runState == true)
        {
            curentTime = System.nanoTime();
            if((curentTime - oldTime) > timeFrame)
            {
                Update();
                Draw();
                oldTime = curentTime;
            }
        }
    }

    /*! \fn public synchronized void StartGame()
        \brief Initializeaza si porneste thread-ul principal al jocului.
     */
    public synchronized void StartGame()
    {
        if(runState == false)
        {
            runState = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    /*! \fn public synchronized void StopGame()
        \brief Opreste in siguranta thread-ul principal al jocului.
     */
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
        \brief Actualizeaza starea elementelor din joc (input, logica de pauza, pozitii, coliziuni).
     */
    private void Update()
    {
        /// Preluam noile apasari de taste
        keyManager.Update();

        /// 1. VERIFICAREA STARII DE PAUZA (Tasta Esc)
        /// Comparam starea curenta a tastei Esc cu starea din cadrul anterior
        /// pentru a asigura tranzitia o singura data la apasare (debounce).
        if (keyManager.escape && !lastEscapeState) {
            isPaused = !isPaused;
        }
        lastEscapeState = keyManager.escape;

        /// 2. LOGICA MENIULUI DE PAUZA
        /// Daca jocul este in pauza, procesam strict navigarea prin meniul suprapus
        if (isPaused) {
            /// Navigare în sus (bucla la capat)
            if (keyManager.up && !lastUpState) {
                pauseMenuSelection--;
                if (pauseMenuSelection < 0) pauseMenuSelection = 2;
            }
            /// Navigare in jos (bucla la capat)
            if (keyManager.down && !lastDownState) {
                pauseMenuSelection++;
                if (pauseMenuSelection > 2) pauseMenuSelection = 0;
            }
            /// Confirmarea selectiei
            if (keyManager.enter && !lastEnterState) {
                executePauseMenuAction();
            }

            /// Salvam starile curente pentru cadrul urmator
            lastUpState = keyManager.up;
            lastDownState = keyManager.down;
            lastEnterState = keyManager.enter;

            /// Returnam fortat pentru a opri executia restului metodei Update.
            /// Acest lucru "ingheata" jucatorul si harta la pozitiile lor curente.
            return;
        }

        /// 3. ACTUALIZAREA JOCULUI NORMAL (Cand isPaused este false)
        if (player != null && map != null) {
            player.Update(keyManager, map);
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
        \brief Executa logica corespunzatoare optiunii selectate in meniul de pauza.
     */
    private void executePauseMenuAction() {
        switch (pauseMenuSelection) {
            case 0: /// SETTINGS
                System.out.println("Setari accesate din meniul de pauza.");
                break;
            case 1: /// RETURN TO MENU

                /// Identifica si distruge fereastra de joc curenta pentru eliberarea memoriei
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(wnd.GetCanvas());
                if (frame != null) frame.dispose();

                /// Instanteaza si afiseaza noul Meniu Principal
                MenuWindow menu = new MenuWindow();
                menu.setVisible(true);
                break;
            case 2: /// EXIT
                System.exit(0);
                break;
        }
    }

    /*! \fn private void Draw()
        \brief Randarea tuturor elementelor grafice (harta, entitati, UI).
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

        /// Activarea antialiasing-ului pentru fonturi fine in meniul de pauza
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        /// Adaptarea randarii pentru modul Fullscreen (Scalare Virtuala).
        /// Functia scale() dilata fiecare element desenat cu acest coeficient.
        double scaleX = (double) wnd.GetWndWidth() / LOGICAL_WIDTH;
        double scaleY = (double) wnd.GetWndHeight() / LOGICAL_HEIGHT;
        g2d.scale(scaleX, scaleY);

        /// Se deseneaza fundalul folosind dimensiunile logice
        g.setColor(new Color(18,28,18));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        int offsetX = 0;
        int offsetY = 0;

        /// Centrarea hartii se face relativ la spatiul virtual prestabilit
        if(map != null) {
            if(map.getPixelWidth() < LOGICAL_WIDTH) {
                offsetX = (LOGICAL_WIDTH - map.getPixelWidth()) / 2;
            }
            if(map.getPixelHeight() < LOGICAL_HEIGHT) {
                offsetY = (LOGICAL_HEIGHT - map.getPixelHeight()) / 2;
            }
        }

        if (map != null && camera != null) {
            map.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        if (player != null && camera != null) {
            player.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// DESENAREA OVERLAY-ULUI PENTRU MENIUL DE PAUZA
        /// Acesta se suprapune intotdeauna peste player si harta
        if (isPaused) {
            /// Desenarea filtrului transparent intunecat
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

            /// Pregatirea elementelor de tip text
            String[] options = {"SETTINGS", "RETURN TO MENU", "EXIT"};
            Font font = new Font("Serif", Font.BOLD, 36);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics(font);

            int startY = 250;

            /// Randarea si centrarea optiunilor din meniul de pauza
            for (int i = 0; i < options.length; i++) {
                int textWidth = fm.stringWidth(options[i]);
                int x = (LOGICAL_WIDTH - textWidth) / 2;
                int y = startY + (i * 70);

                if (i == pauseMenuSelection) {
                    /// Aplicam un stil diferit (Culoare aurie si decoratiuni) opțiunii selectate
                    g2d.setColor(new Color(218, 165, 32));
                    String selectedText = "►  " + options[i] + "  ◄";
                    int selWidth = fm.stringWidth(selectedText);
                    g2d.drawString(selectedText, (LOGICAL_WIDTH - selWidth) / 2, y);
                } else {
                    /// Optiunile neselectate sunt randate intr-un gri estompat
                    g2d.setColor(new Color(180, 180, 190));
                    g2d.drawString(options[i], x, y);
                }
            }
        }

        bs.show();
        g.dispose();
    }

    /*! \fn private void loadLevel(int level)
        \brief Incarca harta corespunzatoare si initializeaza player-ul la punctul de start.
     */
    private void loadLevel(int level) {
        currentLevel = level;

        if (level == 1) {
            map = new Map("res/maps/level1.txt");
            if (player == null) {
                player = new Player(10 * Tile.TILE_WIDTH + 8, 14 * Tile.TILE_HEIGHT);
            } else {
                player.setPosition(10 * Tile.TILE_WIDTH + 8, 14 * Tile.TILE_HEIGHT);
            }
        }
        else if (level == 2) {
            map = new Map("res/maps/level2.txt");
            if (player == null) {
                player = new Player(9 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT);
            } else {
                player.setPosition(9 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT);
            }
        }
        else if (level == 3) {
            map = new Map("res/maps/level3.txt");
            if (player == null) {
                player = new Player(9 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT);
            } else {
                player.setPosition(9 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT);
            }
        }

        transitionCooldown = 20;
    }

    /*! \fn private void checkLevelTransition()
        \brief Verifica daca player-ul a ajuns la zona de final de nivel.
     */
    private void checkLevelTransition() {
        if (transitionCooldown > 0) return;

        int feetX = (int) player.GetX() + player.GetWidth() / 2;
        int feetY = (int) player.GetY() + player.GetHeight() - 2;

        if (currentLevel == 1) {
            if (feetY <= 0 && feetX >= 8 * Tile.TILE_WIDTH && feetX <= 12 * Tile.TILE_WIDTH) loadLevel(2);
        }
        else if (currentLevel == 2) {
            if (feetY <= 0 && feetX >= 8 * Tile.TILE_WIDTH && feetX <= 12 * Tile.TILE_WIDTH) loadLevel(3);
        }
    }
}
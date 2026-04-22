package PaooGame;

import PaooGame.GameWindow.GameWindow;
import PaooGame.Graphics.Assets;
import PaooGame.Tiles.Tile;

import java.awt.*;
import java.awt.image.BufferStrategy;

/*! \class Game
    \brief Clasa principala a intregului proiect. Implementeaza Game - Loop (Update -> Draw)
 */
public class Game implements Runnable
{
    private GameWindow      wnd;        /*!< Fereastra in care se va desena tabla jocului.*/
    private boolean         runState;   /*!< Flag ce starea firului de executie.*/
    private Thread          gameThread; /*!< Referinta catre thread-ul de update si draw al ferestrei.*/
    private BufferStrategy  bs;         /*!< Referinta catre un mecanism cu care se organizeaza memoria complexa pentru un canvas.*/
    private Map             map;
    private KeyManager      keyManager;
    private Player          player;
    private Camera          camera;
    private int             currentLevel;
    private int             transitionCooldown;

    private final int       LOGICAL_WIDTH = 800;  /*!< Latimea logica/virtuala a jocului (spatiul in care functioneaza coliziunile).*/
    private final int       LOGICAL_HEIGHT = 600; /*!< Inaltimea logica/virtuala a jocului.*/

    private Graphics        g;          /*!< Referinta catre un context grafic.*/
    private Tile tile; /*!< variabila membra temporara. */

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

    /*! \fn private void init()
        \brief  Metoda construieste fereastra jocului, initializeaza aseturile, listenerul de tastatura etc.
     */
    private void InitGame()
    {
        /// Construieste fereastra jocului (cu rezolutia pastrata in wnd).
        wnd.BuildGameWindow();
        /// Se incarca toate elementele grafice (dale)
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
        \brief Functia ce va rula in thread-ul creat.
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

    /*! \fn public synchronized void start()
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

    /*! \fn public synchronized void stop()
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
     */
    private void Update()
    {
        keyManager.Update();

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

    /*! \fn private void Draw()
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

        /// Adaptarea randarii pentru modul Fullscreen (Scalare Virtuala).
        /// Calculeaza raportul dintre rezolutia fizica a ecranului si cea virtuala a jocului.
        /// Functia scale() dilata fiecare element desenat cu acest coeficient.
        Graphics2D g2d = (Graphics2D) g;
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

        bs.show();
        g.dispose();
    }

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

    private void checkLevelTransition() {
        if (transitionCooldown > 0) {
            return;
        }

        int feetX = (int) player.GetX() + player.GetWidth() / 2;
        int feetY = (int) player.GetY() + player.GetHeight() - 2;

        if (currentLevel == 1) {
            if (feetY <= 0 && feetX >= 8 * Tile.TILE_WIDTH && feetX <= 12 * Tile.TILE_WIDTH) {
                loadLevel(2);
            }
        }
        else if (currentLevel == 2) {
            if (feetY <= 0 && feetX >= 8 * Tile.TILE_WIDTH && feetX <= 12 * Tile.TILE_WIDTH) {
                loadLevel(3);
            }
        }
    }
}
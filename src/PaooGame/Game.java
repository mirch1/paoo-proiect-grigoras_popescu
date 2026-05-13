package PaooGame;

import PaooGame.enemies.Enemy;
import PaooGame.enemies.Skeleton;
import PaooGame.enemies.Spider;

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
    private GameWindow wnd;
    private boolean    runState;
    private Thread     gameThread;
    private BufferStrategy bs;

    private Map        map;
    private KeyManager keyManager;
    private Camera     camera;

    private Player   player;
    private Enemy    wolf;
    private Skeleton skeleton;
    private Spider   spider;

    private int currentLevel;
    private int transitionCooldown;

    private final int LOGICAL_WIDTH  = 800;
    private final int LOGICAL_HEIGHT = 600;

    private Graphics g;

    private boolean          isPaused              = false;
    private int              pauseMenuSelection    = 0;
    private volatile boolean returnToMenuRequested = false;

    public static boolean showHitboxes = false;
    public static int     currentFPS   = 0;

    private boolean lastEscapeState = false;
    private boolean lastUpState     = false;
    private boolean lastDownState   = false;
    private boolean lastEnterState  = false;
    private boolean lastDebugState  = false;

    private boolean       loadSavedGame   = false;
    private SaveGameState savedGameState  = null;

    /*! \fn public Game(String title, int width, int height)
        \brief Constructor pentru joc nou.
     */
    public Game(String title, int width, int height) {
        this(title, width, height, false);
    }

    /*! \fn public Game(String title, int width, int height, boolean loadSavedGame)
        \brief Constructor cu optiunea de incarcare a unei salvari.
     */
    public Game(String title, int width, int height, boolean loadSavedGame) {
        wnd = new GameWindow(title, width, height);
        runState = false;
        this.loadSavedGame = loadSavedGame;
    }

    /*! \fn private void InitGame()
        \brief Initializeaza fereastra, resursele, camera si primul nivel.
     */
    private void InitGame() {
        wnd.BuildGameWindow();
        Assets.Init();
        currentLevel       = 1;
        transitionCooldown = 0;

        keyManager = new KeyManager();
        wnd.GetCanvas().addKeyListener(keyManager);
        wnd.GetCanvas().setFocusable(true);
        wnd.GetCanvas().requestFocus();

        camera = new Camera(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        if (loadSavedGame) {
            savedGameState = SaveManager.loadGame();
            if (savedGameState != null) {
                loadLevel(savedGameState.getLevel());
                if (player != null) {
                    player.setPosition(savedGameState.getPlayerX(), savedGameState.getPlayerY());
                    if (camera != null && map != null) camera.CenterOnPlayer(player, map);
                }
            } else {
                currentLevel = 1;
                loadLevel(currentLevel);
            }
        } else {
            currentLevel = 1;
            loadLevel(currentLevel);
        }
    }

    /*! \fn public void run()
        \brief Game-Loop principal: 60 FPS, contorizare FPS pentru overlay-ul de debug.
     */
    public void run() {
        InitGame();

        long   oldTime = System.nanoTime();
        long   curentTime;
        final  double timeFrame = 1000000000.0 / 60;
        int    frames  = 0;
        long   timer   = System.currentTimeMillis();

        while (runState) {
            curentTime = System.nanoTime();
            if ((curentTime - oldTime) > timeFrame) {
                Update();
                if (!runState) break;
                Draw();
                oldTime = curentTime;
                frames++;
            }
            if (System.currentTimeMillis() - timer > 1000) {
                currentFPS = frames;
                frames     = 0;
                timer     += 1000;
            }
        }

        if (returnToMenuRequested) {
            SwingUtilities.invokeLater(() -> {
                if (wnd != null) wnd.CloseWindow();
                Toolkit.getDefaultToolkit().sync();
                SwingUtilities.invokeLater(() -> {
                    MenuWindow menu = new MenuWindow();
                    menu.setVisible(true);
                });
            });
        }
    }

    /*! \fn public synchronized void StartGame()
        \brief Porneste thread-ul principal al jocului.
     */
    public synchronized void StartGame() {
        if (!runState) {
            runState   = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    /*! \fn public synchronized void StopGame()
        \brief Opreste thread-ul principal. Nu apeleaza join() pe acelasi thread (deadlock prevention).
     */
    public synchronized void StopGame() {
        if (runState) {
            runState = false;
            if (Thread.currentThread() == gameThread) return;
            try { gameThread.join(); }
            catch (InterruptedException ex) { ex.printStackTrace(); }
        }
    }

    // =========================================================================
    //  UPDATE
    // =========================================================================

    /*! \fn private void Update()
        \brief Actualizeaza logica jocului: input, pauza, entitati, camera, combat, tranzitii.
     */
    private void Update() {
        keyManager.Update();

        /// 1. DEBUG MODE — tasta H.
        if (keyManager.debug && !lastDebugState) showHitboxes = !showHitboxes;
        lastDebugState = keyManager.debug;

        /// 2. PAUZA — tasta ESC.
        if (keyManager.escape && !lastEscapeState) isPaused = !isPaused;
        lastEscapeState = keyManager.escape;

        /// 3. MENIU PAUZA — navigare si selectie.
        if (isPaused) {
            if (keyManager.up && !lastUpState) {
                pauseMenuSelection--;
                if (pauseMenuSelection < 0) pauseMenuSelection = 3;
            }
            if (keyManager.down && !lastDownState) {
                pauseMenuSelection++;
                if (pauseMenuSelection > 3) pauseMenuSelection = 0;
            }
            if (keyManager.enter && !lastEnterState) executePauseMenuAction();

            lastUpState    = keyManager.up;
            lastDownState  = keyManager.down;
            lastEnterState = keyManager.enter;
            return; /// Nu actualizam logica jocului cat timp suntem in pauza.
        }

        /// 4. ACTUALIZARE ENTITATI.
        if (player   != null && map != null) player.Update(keyManager, map);
        if (wolf     != null && map != null) wolf.Update(map);
        if (skeleton != null && map != null) skeleton.Update(map);
        if (spider   != null && map != null) spider.Update(map);

        /// 5. CAMERA.
        if (camera != null && player != null && map != null)
            camera.CenterOnPlayer(player, map);

        /// 6. TIMER TRANZITIE.
        if (transitionCooldown > 0) transitionCooldown--;

        /// 7. TRANZITIE NIVEL.
        checkLevelTransition();

        /// 8. COMBAT.
        if (player != null) checkCombat();
    }

    // =========================================================================
    //  COMBAT
    // =========================================================================

    /*! \fn private void checkCombat()
        \brief Verifica coliziunile de combat si aplica damage.
        \details Logica de atac:
                 1. Atacul jucatorului: hitbox-ul de atac (sabie) loveste inamicii vii.
                 2. Damage de contact: inamicii ale caror getFeetRect() se suprapun cu al
                    jucatorului aplica damage — NUMAI la contact fizic real, nu la distanta.
                 3. Proiectilul Spider: panza activa la mai putin de 16px de jucator.
                 4. Respawn la moartea jucatorului.
     */
    private void checkCombat() {
        if (player == null || player.isDead()) return;

        Rectangle playerAtk  = player.getAttackHitbox();
        Rectangle playerFeet = player.getFeetRect();

        /// 1. ATACUL JUCATORULUI — hitbox-ul sabiei loveste inamicii.
        if (playerAtk != null) {
            if (wolf     != null && !wolf.isDead()     && playerAtk.intersects(wolf.getFeetRect()))
                wolf.takeDamage(Player.ATTACK_DAMAGE);
            if (skeleton != null && !skeleton.isDead() && playerAtk.intersects(skeleton.getFeetRect()))
                skeleton.takeDamage(Player.ATTACK_DAMAGE);
            if (spider   != null && !spider.isDead()   && playerAtk.intersects(spider.getFeetRect()))
                spider.takeDamage(Player.ATTACK_DAMAGE);
        }

        /// 2. DAMAGE DE CONTACT — inamicii aplica damage NUMAI cand hitbox-urile se ating fizic.
        if (wolf     != null && !wolf.isDead()     && playerFeet.intersects(wolf.getFeetRect()))
            player.takeDamage(Enemy.ATTACK_DAMAGE);
        if (skeleton != null && !skeleton.isDead() && playerFeet.intersects(skeleton.getFeetRect()))
            player.takeDamage(Skeleton.ATTACK_DAMAGE);

        /// 3. PROIECTIL SPIDER — panza activa la distanta mica de jucator.
        if (spider != null && !spider.isDead() && spider.isWebActive()) {
            float wx   = spider.getWebX();
            float wy   = spider.getWebY();
            float px   = player.GetFeetCenterX();
            float py   = player.GetFeetBottomY();
            double dist = Math.sqrt((wx - px) * (wx - px) + (wy - py) * (wy - py));
            if (dist < 16) {
                player.takeDamage(Spider.WEB_DAMAGE);
                spider.deactivateWeb();
            }
        }

        /// 4. RESPAWN — jucatorul a murit, reincarcam nivelul curent.
        if (player.isDead()) loadLevel(currentLevel);
    }

    // =========================================================================
    //  MENIU PAUZA
    // =========================================================================

    /*! \fn private void executePauseMenuAction()
        \brief Executa actiunea selectata in meniul de pauza.
     */
    private void executePauseMenuAction() {
        switch (pauseMenuSelection) {
            case 0:
                Window owner = SwingUtilities.getWindowAncestor(wnd.GetCanvas());
                SwingUtilities.invokeLater(() -> SettingsDialog.showSettings(owner));
                break;
            case 1:
                saveCurrentGame(true);
                break;
            case 2:
                returnToMainMenu();
                break;
            case 3:
                System.exit(0);
                break;
        }
    }

    /*! \fn private void saveCurrentGame(boolean showMessage)
        \brief Salveaza starea curenta a jocului pe disc.
        \param showMessage Daca true, afiseaza un dialog de confirmare.
     */
    private void saveCurrentGame(boolean showMessage) {
        if (player == null) return;
        SaveManager.saveGame(currentLevel, player.GetX(), player.GetY());
        if (showMessage) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null,
                    "Jocul a fost salvat cu succes.", "Save Game",
                    JOptionPane.INFORMATION_MESSAGE)
            );
        }
    }

    /*! \fn private void returnToMainMenu()
        \brief Marcheaza cererea de revenire in meniu si opreste bucla.
     */
    private void returnToMainMenu() {
        returnToMenuRequested = true;
        runState = false;
    }

    // =========================================================================
    //  DRAW
    // =========================================================================

    /*! \fn private void Draw()
        \brief Randeaza toate elementele grafice in ordinea corecta:
               Harta → Inamici → Jucator → HUD → Foreground → Bare HP → Pauza → Debug.
        \details Barele HP ale inamicilor sunt randate DUPA DrawForeground() — apar deasupra copacilor.
     */
    private void Draw() {
        if (wnd == null || wnd.GetCanvas() == null || !wnd.GetCanvas().isDisplayable()) return;

        bs = wnd.GetCanvas().getBufferStrategy();
        if (bs == null) {
            try { wnd.GetCanvas().createBufferStrategy(3); return; }
            catch (Exception e) { e.printStackTrace(); }
        }

        g = bs.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        /// Scalare la dimensiunea fizica a ferestrei.
        double scaleX = (double) wnd.GetWndWidth()  / LOGICAL_WIDTH;
        double scaleY = (double) wnd.GetWndHeight() / LOGICAL_HEIGHT;
        g2d.scale(scaleX, scaleY);

        /// Fundal negru de baza.
        g.setColor(new Color(10, 15, 10));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        /// Offset de centrare daca harta e mai mica decat ecranul.
        int offsetX = 0, offsetY = 0;
        if (map != null) {
            if (map.getPixelWidth()  < LOGICAL_WIDTH)  offsetX = (LOGICAL_WIDTH  - map.getPixelWidth())  / 2;
            if (map.getPixelHeight() < LOGICAL_HEIGHT) offsetY = (LOGICAL_HEIGHT - map.getPixelHeight()) / 2;
        }

        /// 1. HARTA (fundal: sol, ziduri, apa etc.)
        if (map != null && camera != null)
            map.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /// 2. INAMICI — sprite-uri, fara bara de viata (randata la pasul 6).
        if (wolf     != null && camera != null) wolf.Draw(g,     (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (skeleton != null && camera != null) skeleton.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (spider   != null && camera != null) spider.Draw(g,   (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /// 3. JUCATOR.
        if (player != null && camera != null)
            player.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /// 4. HUD — bara mare de HP a jucatorului in coltul stanga-jos.
        if (player != null && !player.isDead()) {
            int   hudX  = 20;
            int   hudY  = LOGICAL_HEIGHT - 30;
            int   hudW  = 150;
            int   hudH  = 12;
            float ratio = (float) player.getCurrentHp() / player.getMaxHp();

            g2d.setColor(new Color(20, 20, 20, 200));
            g2d.fillRoundRect(hudX - 2, hudY - 2, hudW + 4, hudH + 4, 6, 6);

            g2d.setColor(new Color(100, 10, 10, 220));
            g2d.fillRoundRect(hudX, hudY, hudW, hudH, 4, 4);

            Color hudColor = ratio > 0.6f ? new Color(50,  200, 50)
                           : ratio > 0.3f ? new Color(220, 190, 20)
                           :                new Color(210,  40, 40);
            g2d.setColor(hudColor);
            g2d.fillRoundRect(hudX, hudY, (int)(hudW * ratio), hudH, 4, 4);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 11));
            g2d.drawString("HP: " + player.getCurrentHp() + " / " + player.getMaxHp(),
                    hudX + hudW + 8, hudY + 10);
        }

        /// 5. FOREGROUND (copaci, acoperisuri) — PESTE sprites, SUB barele de viata ale inamicilor.
        if (map != null && camera != null)
            map.DrawForeground(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /// 6. BARE HP INAMICI — randate DUPA foreground, deci DEASUPRA copacilor.
        ///    drawHealthBarOnly() nu face nimic daca inamicul e mort sau are HP plin.
        if (camera != null) {
            if (wolf     != null) wolf.drawHealthBarOnly(    g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (skeleton != null) skeleton.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (spider   != null) spider.drawHealthBarOnly(  g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 7. MENIU PAUZA.
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

            String[] options = {"SETTINGS", "SAVE GAME", "RETURN TO MENU", "EXIT"};
            Font font = new Font("Serif", Font.BOLD, 36);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics(font);
            int startY = 250;
            for (int i = 0; i < options.length; i++) {
                int tw = fm.stringWidth(options[i]);
                int x  = (LOGICAL_WIDTH - tw) / 2;
                int y  = startY + (i * 70);
                if (i == pauseMenuSelection) {
                    g2d.setColor(new Color(218, 165, 32));
                    String sel = "> " + options[i] + " <";
                    g2d.drawString(sel, (LOGICAL_WIDTH - fm.stringWidth(sel)) / 2, y);
                } else {
                    g2d.setColor(new Color(180, 180, 190));
                    g2d.drawString(options[i], x, y);
                }
            }
        }

        if (GameSettings.cinematicMode) drawCinematicVignette(g2d);

        /// 8. OVERLAY DEBUG (FPS + Mod Debug) — desenat ultimul, deasupra tuturor.
        if (showHitboxes) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString("FPS: " + currentFPS, 15, 30);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2d.drawString("Mod Debug: ON", 15, 50);
        }

        if (runState && wnd.GetCanvas().isDisplayable()) bs.show();
        g.dispose();
    }

    /*! \fn private void drawCinematicVignette(Graphics2D g2d)
        \brief Aplica benzi cinematice sus-jos si umbre laterale.
     */
    private void drawCinematicVignette(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 70));
        g2d.fillRect(0, 0, LOGICAL_WIDTH, 38);
        g2d.fillRect(0, LOGICAL_HEIGHT - 38, LOGICAL_WIDTH, 38);

        g2d.setPaint(new GradientPaint(0, 0, new Color(0,0,0,85), 90, 0, new Color(0,0,0,0)));
        g2d.fillRect(0, 0, 90, LOGICAL_HEIGHT);

        g2d.setPaint(new GradientPaint(LOGICAL_WIDTH, 0, new Color(0,0,0,85), LOGICAL_WIDTH-90, 0, new Color(0,0,0,0)));
        g2d.fillRect(LOGICAL_WIDTH - 90, 0, 90, LOGICAL_HEIGHT);
    }

    // =========================================================================
    //  NIVELURI
    // =========================================================================

    /*! \fn private void setPlayerSpawn(int tileCol, int tileRow)
        \brief Pozitioneaza player-ul la coordonatele de tile si reseteaza inputul.
     */
    private void setPlayerSpawn(int tileCol, int tileRow) {
        if (player == null) return;
        float spawnX = tileCol * Tile.TILE_WIDTH  + (Tile.TILE_WIDTH  - player.GetWidth())  / 2.0f;
        float spawnY = tileRow * Tile.TILE_HEIGHT + (Tile.TILE_HEIGHT - player.GetHeight()) / 2.0f;
        player.setPosition(spawnX, spawnY);
        if (camera != null && map != null) camera.CenterOnPlayer(player, map);
        if (keyManager != null) keyManager.Clear();
    }

    /*! \fn private void loadLevel(int level)
        \brief Incarca harta si entitatile corespunzatoare nivelului primit.
     */
    private void loadLevel(int level) {
        currentLevel = level;
        wolf     = null;
        skeleton = null;
        spider   = null;

        if (player == null) player = new Player(0, 0);

        if (level == 1) {
            map = new Map("res/maps/level1_base.png",
                          "res/maps/level1_foreground.png",
                          "res/maps/harta_primul_nivel.tmx");
            setPlayerSpawn(10, 14);
            wolf = new Enemy(15 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT, player);

        } else if (level == 2) {
            map = new Map("res/maps/level2_base.png",
                          "res/maps/level2_foreground.png",
                          "res/maps/harta_nivel2_dungeon.tmx");
            setPlayerSpawn(9, 13);
            skeleton = new Skeleton(9 * Tile.TILE_WIDTH, 8 * Tile.TILE_HEIGHT, player);
            spider   = new Spider(  9 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT, player);

        } else if (level == 3) {
            map = new Map("res/maps/level3_base.png",
                          "res/maps/level3_foreground.png",
                          "res/maps/harta_nivel3_the_great_hall.tmx");
            setPlayerSpawn(9, 13);
            /// Inamicii pentru Nivelul 3 se adauga aici cand sunt implementati.
        }

        transitionCooldown = 30;
    }

    /*! \fn private void checkLevelTransition()
        \brief Verifica daca jucatorul a ajuns la zona de tranzitie a nivelului curent.
     */
    private void checkLevelTransition() {
        if (transitionCooldown > 0) return;

        int px = (int) player.GetFeetCenterX();
        int py = (int) player.GetFeetBottomY();

        if (currentLevel == 1) {
            if (map.isTransitionAtPixel(px, py)
             || map.isTransitionAtPixel(px - 8, py)
             || map.isTransitionAtPixel(px + 8, py)) {
                loadLevel(2);
                saveCurrentGame(false);
            }
        } else if (currentLevel == 2) {
            if (map.isTransitionAtPixel(px, py)
             || map.isTransitionAtPixel(px - 8, py)
             || map.isTransitionAtPixel(px + 8, py)) {
                loadLevel(3);
                saveCurrentGame(false);
            }
        }
    }
}

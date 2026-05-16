package PaooGame;

import PaooGame.enemies.Enemy;
import PaooGame.enemies.Guardian;
import PaooGame.enemies.Skeleton;
import PaooGame.enemies.Spider;
import java.util.ArrayList;
import java.util.List;
import PaooGame.GameWindow.GameWindow;
import PaooGame.Graphics.Assets;
import PaooGame.Tiles.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

/*! \class Game
    \brief Clasa principala a jocului.

    \details
    Aceasta clasa coordoneaza intreaga rulare a jocului:
    - initializeaza fereastra si resursele;
    - porneste game-loop-ul la aproximativ 60 FPS;
    - actualizeaza inputul, entitatile si camera;
    - deseneaza harta, personajele, meniul de pauza si HUD-ul;
    - gestioneaza salvarea si tranzitiile intre niveluri.

    Fisierul a fost rescris folosind exact numele de fisiere din folderul tau `res/maps`:
    - level1_base.png
    - level1_foreground.png
    - level2_base.png
    - level2_foreground.png
    - level3_village_base.png
    - level3_village_foreground.png
    - level3_base.png
    - level3_foreground.png
    - harta_primul_nivel.tmx
    - harta_nivel2_dungeon.tmx
    - harta_nivel3_village.tmx
    - harta_nivel3_the_great_hall.tmx
*/
public class Game implements Runnable {

    /// Fereastra principala a jocului.
    private GameWindow wnd;

    /// Arata daca jocul ruleaza in acest moment.
    private boolean runState;

    /// Thread-ul folosit pentru bucla principala a jocului.
    private Thread gameThread;

    /// BufferStrategy pentru randare fluida.
    private BufferStrategy bs;

    /// Harta nivelului curent.
    private Map map;

    /// Managerul de input de la tastatura.
    private KeyManager keyManager;

    /// Camera care urmareste jucatorul in lume.
    private Camera camera;

    /// Jucatorul principal.
    private Player player;

    /// Haita de lupi din Nivelul 1 (4 lupi cu pathfinding individual).
    private List<Enemy> wolves = new ArrayList<>();

    /// Gardienii statici care pazesc intrarea gresita spre Nivelul 2.
    /// Daca jucatorul ii ataca, aplica damage instant fatal si afiseaza WRONG ENTRANCE.
    private List<Guardian> guardians = new ArrayList<>();

    /// Flag setat true cand jucatorul a incercat sa atace un gardian.
    private boolean wrongEntranceDeath = false;

    /// Inamic specific nivelului 2.
    private Skeleton skeleton;

    /// Al doilea inamic specific nivelului 2.
    private Spider spider;

    /// Nivelul curent incarcat.
    private int currentLevel;

    /// Cooldown scurt pentru a preveni tranzitii multiple instant.
    private int transitionCooldown;

    /// Latimea logica a scenei de joc.
    private final int LOGICALWIDTH = 800;

    /// Inaltimea logica a scenei de joc.
    private final int LOGICALHEIGHT = 600;

    /// Contextul grafic folosit la desenare.
    private Graphics g;

    /// Arata daca jocul este in pauza.
    private boolean isPaused = false;

    /// Optiunea curenta selectata in meniul de pauza.
    private int pauseMenuSelection = 0;

    /// Arata daca ecranul de moarte este activ.
    private boolean isDeathScreen = false;

    /// Optiunea curenta selectata in ecranul de moarte.
    private int deathMenuSelection = 0;

    /// Nivelul in care jucatorul a murit.
    private int deathLevel = 1;

    /// Marcheaza revenirea in meniul principal dupa oprirea jocului.
    private volatile boolean returnToMenuRequested = false;

    /// Activeaza overlay-ul de debug.
    public static boolean showHitboxes = false;

    /// FPS-ul curent calculat o data pe secunda.
    public static int currentFPS = 0;

    /// Stari anterioare ale tastelor pentru detectia apasarilor unice.
    private boolean lastEscapeState = false;
    private boolean lastUpState = false;
    private boolean lastDownState = false;
    private boolean lastEnterState = false;
    private boolean lastDebugState = false;

    /// Daca este true, jocul incearca sa incarce progresul salvat.
    private boolean loadSavedGame = false;

    /// Obiectul care retine starea citita din save file.
    private SaveGameState savedGameState = null;

    /*! \fn public Game(String title, int width, int height)
        \brief Constructor pentru joc nou.

        \param title Titlul ferestrei.
        \param width Latimea ferestrei.
        \param height Inaltimea ferestrei.
    */
    public Game(String title, int width, int height) {
        this(title, width, height, false);
    }

    /*! \fn public Game(String title, int width, int height, boolean loadSavedGame)
        \brief Constructor general al jocului.

        \param title Titlul ferestrei.
        \param width Latimea ferestrei.
        \param height Inaltimea ferestrei.
        \param loadSavedGame Daca este true, jocul porneste dintr-o salvare.
    */
    public Game(String title, int width, int height, boolean loadSavedGame) {
        wnd = new GameWindow(title, width, height);
        runState = false;
        this.loadSavedGame = loadSavedGame;
    }

    /*! \fn private void InitGame()
        \brief Initializeaza jocul si resursele de baza.

        \details
        Se construieste fereastra, se incarca asset-urile, se initializeaza
        inputul si camera, iar apoi se incarca fie salvarea existenta,
        fie primul nivel al jocului.
    */
    private void InitGame() {
        wnd.BuildGameWindow();
        Assets.Init();

        currentLevel = 1;
        transitionCooldown = 0;

        keyManager = new KeyManager();
        wnd.GetCanvas().addKeyListener(keyManager);
        wnd.GetCanvas().setFocusable(true);
        wnd.GetCanvas().requestFocus();

        camera = new Camera(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        if (loadSavedGame) {
            savedGameState = SaveManager.loadGame();
            if (savedGameState != null) {
                loadLevel(savedGameState.getLevel());
                if (player != null) {
                    player.setPosition(savedGameState.getPlayerX(), savedGameState.getPlayerY());
                }
                if (camera != null && map != null && player != null) {
                    camera.CenterOnPlayer(player, map);
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
        \brief Game-loop-ul principal.

        \details
        Ruleaza cat timp `runState` este activ si incearca sa mentina
        actualizarea si randarea la aproximativ 60 FPS.
    */
    @Override
    public void run() {
        InitGame();

        long oldTime = System.nanoTime();
        long currentTime;
        final double timeFrame = 1000000000.0 / 60.0;

        int frames = 0;
        long timer = System.currentTimeMillis();

        while (runState) {
            currentTime = System.nanoTime();

            if ((currentTime - oldTime) > timeFrame) {
                Update();
                if (!runState) {
                    break;
                }

                Draw();
                oldTime = currentTime;
                frames++;
            }

            if (System.currentTimeMillis() - timer >= 1000) {
                currentFPS = frames;
                frames = 0;
                timer += 1000;
            }
        }

        if (returnToMenuRequested) {
            SwingUtilities.invokeLater(() -> {
                if (wnd != null) {
                    wnd.CloseWindow();
                }
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
            runState = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    /*! \fn public synchronized void StopGame()
        \brief Opreste jocul si asteapta terminarea thread-ului.
    */
    public synchronized void StopGame() {
        if (runState) {
            runState = false;
        }

        if (Thread.currentThread() == gameThread) {
            return;
        }

        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /*! \fn private void Update()
        \brief Actualizeaza starea jocului pentru un frame.

        \details
        Trateaza inputul, meniul de pauza, miscarea entitatilor,
        combat-ul, camera si tranzitiile intre niveluri.
    */
    private void Update() {
        keyManager.Update();

        if (isDeathScreen) {
            updateDeathScreen();
            return;
        }

        /// Comutare mod debug.
        if (keyManager.debug && !lastDebugState) {
            showHitboxes = !showHitboxes;
        }
        lastDebugState = keyManager.debug;

        /// Comutare pauza.
        if (keyManager.escape && !lastEscapeState) {
            isPaused = !isPaused;
        }
        lastEscapeState = keyManager.escape;

        /// Daca jocul este in pauza, actualizam doar navigarea prin meniu.
        if (isPaused) {
            if (keyManager.up && !lastUpState) {
                pauseMenuSelection--;
                if (pauseMenuSelection < 0) {
                    pauseMenuSelection = 3;
                }
            }

            if (keyManager.down && !lastDownState) {
                pauseMenuSelection++;
                if (pauseMenuSelection > 3) {
                    pauseMenuSelection = 0;
                }
            }

            if (keyManager.enter && !lastEnterState) {
                executePauseMenuAction();
            }

            lastUpState = keyManager.up;
            lastDownState = keyManager.down;
            lastEnterState = keyManager.enter;
            return;
        }

        /// Actualizare entitati.
        if (player != null && map != null) {
            player.Update(keyManager, map);
        }
        /// Actualizam fiecare lup din haita independent.
        for (Enemy wolf : wolves) {
            if (!wolf.isDead() && map != null) {
                wolf.Update(map);
            }
        }
        /// Gardienii sunt statici dar tickuiesc timer-ele interne.
        for (Guardian guardian : guardians) {
            if (map != null) {
                guardian.Update(map);
            }
        }
        if (skeleton != null && map != null) {
            skeleton.Update(map);
        }
        if (spider != null && map != null) {
            spider.Update(map);
        }

        /// Camera urmareste jucatorul activ.
        if (camera != null && player != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        /// Scadem cooldown-ul pentru schimbarea de nivel.
        if (transitionCooldown > 0) {
            transitionCooldown--;
        }

        /// Verificam tranzitiile si combat-ul.
        checkLevelTransition();
        if (player != null) {
            checkCombat();
        }

        lastUpState = keyManager.up;
        lastDownState = keyManager.down;
        lastEnterState = keyManager.enter;
    }

    // =========================================================================
    // COMBAT
    // =========================================================================

    /*! \fn private void checkCombat()
        \brief Verifica interactiunile de lupta dintre jucator si inamici.
    */
    private void checkCombat() {
        if (player == null || player.isDead()) {
            return;
        }

        Rectangle playerAtk = player.getAttackHitbox();
        Rectangle playerFeet = player.getFeetRect();

        /// Atacul jucatorului loveste doar inamicii vii aflati in raza sabiei.
        if (playerAtk != null) {
            /// Sabia jucatorului loveste fiecare lup din haita.
            for (Enemy wolf : wolves) {
                if (!wolf.isDead() && playerAtk.intersects(wolf.getFeetRect())) {
                    wolf.takeDamage(Player.ATTACK_DAMAGE);
                }
            }
            /// Daca jucatorul loveste un gardian — acesta aplica damage instant fatal
            /// si seteaza flagul wrongEntranceDeath pentru ecranul de moarte special.
            for (Guardian guardian : guardians) {
                if (playerAtk.intersects(guardian.getFeetRect())) {
                    wrongEntranceDeath = true;
                    guardian.reactToPlayerAttack();
                }
            }
            if (skeleton != null && !skeleton.isDead() && playerAtk.intersects(skeleton.getFeetRect())) {
                skeleton.takeDamage(Player.ATTACK_DAMAGE);
            }
            if (spider != null && !spider.isDead() && playerAtk.intersects(spider.getFeetRect())) {
                spider.takeDamage(Player.ATTACK_DAMAGE);
            }
        }

        /// Damage de contact cu fiecare lup din haita.
        for (Enemy wolf : wolves) {
            if (!wolf.isDead() && playerFeet.intersects(wolf.getFeetRect())) {
                player.takeDamage(Enemy.ATTACK_DAMAGE);
            }
        }
        if (skeleton != null && !skeleton.isDead() && playerFeet.intersects(skeleton.getFeetRect())) {
            player.takeDamage(Skeleton.ATTACK_DAMAGE);
        }

        /// Proiectilul paianjenului loveste la distanta mica fata de jucator.
        if (spider != null && !spider.isDead() && spider.isWebActive()) {
            float wx = spider.getWebX();
            float wy = spider.getWebY();
            float px = player.GetFeetCenterX();
            float py = player.GetFeetBottomY();
            double dist = Math.sqrt((wx - px) * (wx - px) + (wy - py) * (wy - py));

            if (dist < 16) {
                player.takeDamage(Spider.WEB_DAMAGE);
                spider.deactivateWeb();
            }
        }

        /// Daca jucatorul moare, afisam ecranul de retry.
        if (player.isDead()) {
            openDeathScreen();
        }
    }

    // =========================================================================
    // ECRAN DE MOARTE
    // =========================================================================

    /*! \fn private void openDeathScreen()
        \brief Opreste gameplay-ul si afiseaza meniul de moarte.
    */
    private void openDeathScreen() {
        deathLevel = currentLevel;
        deathMenuSelection = 0;
        isDeathScreen = true;
        isPaused = false;

        if (keyManager != null) {
            keyManager.Clear();
        }

        lastUpState = false;
        lastDownState = false;
        lastEnterState = false;
        lastEscapeState = false;
    }

    /*! \fn private void updateDeathScreen()
        \brief Gestioneaza navigarea in meniul afisat dupa moarte.
    */
    private void updateDeathScreen() {
        if (keyManager.up && !lastUpState) {
            deathMenuSelection--;
            if (deathMenuSelection < 0) {
                deathMenuSelection = 1;
            }
        }

        if (keyManager.down && !lastDownState) {
            deathMenuSelection++;
            if (deathMenuSelection > 1) {
                deathMenuSelection = 0;
            }
        }

        if (keyManager.enter && !lastEnterState) {
            executeDeathMenuAction();
        }

        lastUpState = keyManager.up;
        lastDownState = keyManager.down;
        lastEnterState = keyManager.enter;
    }

    /*! \fn private void executeDeathMenuAction()
        \brief Executa TRY AGAIN sau EXIT GAME.
    */
    private void executeDeathMenuAction() {
        if (deathMenuSelection == 0) {
            isDeathScreen = false;
            wrongEntranceDeath = false;
            player = new Player(0, 0);
            loadLevel(deathLevel);

            if (keyManager != null) {
                keyManager.Clear();
            }

            lastUpState = false;
            lastDownState = false;
            lastEnterState = false;
        } else {
            System.exit(0);
        }
    }

    // =========================================================================
    // MENIU PAUZA
    // =========================================================================

    /*! \fn private void executePauseMenuAction()
        \brief Executa actiunea selectata in meniul de pauza.
    */
    private void executePauseMenuAction() {
        switch (pauseMenuSelection) {
            case 0 -> {
                Window owner = SwingUtilities.getWindowAncestor(wnd.GetCanvas());
                SwingUtilities.invokeLater(() -> SettingsDialog.showSettings(owner));
            }
            case 1 -> saveCurrentGame(true);
            case 2 -> returnToMainMenu();
            case 3 -> System.exit(0);
        }
    }

    /*! \fn private void saveCurrentGame(boolean showMessage)
        \brief Salveaza progresul curent.

        \param showMessage Daca este true, afiseaza mesaj de confirmare.
    */
    private void saveCurrentGame(boolean showMessage) {
        if (player == null) {
            return;
        }

        SaveManager.saveGame(currentLevel, player.GetX(), player.GetY());

        if (showMessage) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Jocul a fost salvat cu succes.",
                    "Save Game",
                    JOptionPane.INFORMATION_MESSAGE
            ));
        }
    }

    /*! \fn private void returnToMainMenu()
        \brief Cere revenirea in meniul principal si opreste jocul.
    */
    private void returnToMainMenu() {
        returnToMenuRequested = true;
        runState = false;
    }

    // =========================================================================
    // DRAW
    // =========================================================================

    /*! \fn private void Draw()
        \brief Randeaza cadrul curent al jocului.

        \details
        Ordinea de desenare este:
        1. harta de baza,
        2. inamicii,
        3. jucatorul,
        4. foreground-ul,
        5. barele HP ale inamicilor,
        6. meniul de pauza,
        7. overlay-ul de debug,
        8. HUD-ul jucatorului.

        HUD-ul este desenat ultimul pentru a ramane permanent deasupra copacilor
        si a elementelor de foreground.
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
                return;
            }
        }

        g = bs.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        /// Scalare din rezolutia logica in rezolutia reala a ferestrei.
        double scaleX = (double) wnd.GetWndWidth() / LOGICALWIDTH;
        double scaleY = (double) wnd.GetWndHeight() / LOGICALHEIGHT;
        g2d.scale(scaleX, scaleY);

        /// Curatam fundalul cu o culoare inchisa.
        g.setColor(new Color(10, 15, 10));
        g.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        /// Offset pentru centrare atunci cand harta este mai mica decat viewport-ul logic.
        int offsetX = 0;
        int offsetY = 0;
        if (map != null) {
            if (map.getPixelWidth() < LOGICALWIDTH) {
                offsetX = (LOGICALWIDTH - map.getPixelWidth()) / 2;
            }
            if (map.getPixelHeight() < LOGICALHEIGHT) {
                offsetY = (LOGICALHEIGHT - map.getPixelHeight()) / 2;
            }
        }

        /// 1. Harta de baza.
        if (map != null && camera != null) {
            map.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 2. Inamicii — haita de lupi, gardieni, skeleton, spider.
        for (Enemy wolf : wolves) {
            if (!wolf.isDead() && camera != null) {
                wolf.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
        }
        /// Gardienii se deseneaza intotdeauna (sunt nemuritori).
        for (Guardian guardian : guardians) {
            if (camera != null) {
                guardian.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
        }
        if (skeleton != null && camera != null) {
            skeleton.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }
        if (spider != null && camera != null) {
            spider.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 3. Jucatorul.
        if (player != null && camera != null) {
            player.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 4. Foreground-ul hartii.
        if (map != null && camera != null) {
            map.DrawForeground(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 5. Barele de viata ale inamicilor, desenate peste foreground.
        if (camera != null) {
            /// Barele de viata ale lupilor din haita.
            for (Enemy wolf : wolves) {
                wolf.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
            if (skeleton != null) {
                skeleton.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
            if (spider != null) {
                spider.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
        }

        /// 6. Meniul de pauza.
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

            String[] options = {"SETTINGS", "SAVE GAME", "RETURN TO MENU", "EXIT"};
            Font font = new Font("Serif", Font.BOLD, 36);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics(font);
            int startY = 250;

            for (int i = 0; i < options.length; i++) {
                int tw = fm.stringWidth(options[i]);
                int x = (LOGICALWIDTH - tw) / 2;
                int y = startY + i * 70;

                if (i == pauseMenuSelection) {
                    g2d.setColor(new Color(218, 165, 32));
                    String sel = "> " + options[i] + " <";
                    g2d.drawString(sel, (LOGICALWIDTH - fm.stringWidth(sel)) / 2, y);
                } else {
                    g2d.setColor(new Color(180, 180, 190));
                    g2d.drawString(options[i], x, y);
                }
            }
        }

        /// 7. Overlay debug.
        if (showHitboxes) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString("FPS: " + currentFPS, 15, 30);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2d.drawString("Mod Debug: ON", 15, 50);
        }

        /// 8. HUD-ul jucatorului se deseneaza ultimul, ca sa fie mereu vizibil.
        if (player != null && !player.isDead()) {
            int hudX = 20;
            int hudY = LOGICALHEIGHT - 30;
            int hudW = 150;
            int hudH = 12;
            float ratio = (float) player.getCurrentHp() / player.getMaxHp();

            g2d.setColor(new Color(20, 20, 20, 200));
            g2d.fillRoundRect(hudX - 2, hudY - 2, hudW + 4, hudH + 4, 6, 6);

            g2d.setColor(new Color(100, 10, 10, 220));
            g2d.fillRoundRect(hudX, hudY, hudW, hudH, 4, 4);

            Color hudColor = ratio > 0.6f
                    ? new Color(50, 200, 50)
                    : ratio > 0.3f
                    ? new Color(220, 190, 20)
                    : new Color(210, 40, 40);

            g2d.setColor(hudColor);
            g2d.fillRoundRect(hudX, hudY, (int) (hudW * ratio), hudH, 4, 4);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 11));
            g2d.drawString(
                    "HP: " + player.getCurrentHp() + " / " + player.getMaxHp(),
                    hudX + hudW + 8,
                    hudY + 10
            );
        }

        if (isDeathScreen) {
            drawDeathScreen(g2d);
        }

        if (runState && wnd.GetCanvas().isDisplayable()) {
            bs.show();
        }
        g.dispose();
    }

    /*! \fn private void drawDeathScreen(Graphics2D g2d)
        \brief Deseneaza fereastra neagra de moarte peste joc.
    */
    private void drawDeathScreen(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        String title = "YOU DIED";
        Font titleFont = new Font("Serif", Font.BOLD, 72);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
        int titleX = (LOGICALWIDTH - titleMetrics.stringWidth(title)) / 2;
        int titleY = 210;

        g2d.setColor(new Color(120, 0, 0));
        g2d.drawString(title, titleX + 3, titleY + 3);
        g2d.setColor(new Color(210, 35, 35));
        g2d.drawString(title, titleX, titleY);

        /// Daca jucatorul a atacat gardienii, afisam mesaj special WRONG ENTRANCE.
        if (wrongEntranceDeath) {
            String wrongMsg = "WRONG ENTRANCE";
            Font wrongFont = new Font("Serif", Font.BOLD, 36);
            g2d.setFont(wrongFont);
            FontMetrics wrongMetrics = g2d.getFontMetrics(wrongFont);
            int wrongX = (LOGICALWIDTH - wrongMetrics.stringWidth(wrongMsg)) / 2;
            int wrongY = titleY + 58;

            /// Shadow subtil pentru lizibilitate.
            g2d.setColor(new Color(80, 40, 0));
            g2d.drawString(wrongMsg, wrongX + 2, wrongY + 2);
            /// Text portocaliu — culoare diferita de rosu pentru a iesi in evidenta.
            g2d.setColor(new Color(218, 130, 30));
            g2d.drawString(wrongMsg, wrongX, wrongY);
        }

        String[] options = {"TRY AGAIN", "EXIT GAME"};
        Font optionFont = new Font("Serif", Font.BOLD, 34);
        g2d.setFont(optionFont);
        FontMetrics optionMetrics = g2d.getFontMetrics(optionFont);
        int startY = 330;

        for (int i = 0; i < options.length; i++) {
            String text = options[i];
            if (i == deathMenuSelection) {
                text = "> " + text + " <";
                g2d.setColor(new Color(218, 165, 32));
            } else {
                g2d.setColor(new Color(170, 170, 180));
            }

            int x = (LOGICALWIDTH - optionMetrics.stringWidth(text)) / 2;
            int y = startY + i * 66;
            g2d.drawString(text, x, y);
        }
    }

    // =========================================================================
    // NIVELURI
    // =========================================================================

    /*! \fn private void setPlayerSpawn(int tileCol, int tileRow)
        \brief Pozitioneaza jucatorul pe harta pe baza coordonatelor de tile.

        \param tileCol Coloana tile-ului de spawn.
        \param tileRow Linia tile-ului de spawn.
    */
    private void setPlayerSpawn(int tileCol, int tileRow) {
        if (player == null) {
            return;
        }

        float spawnX = tileCol * Tile.TILE_WIDTH + Tile.TILE_WIDTH / 2.0f - player.GetWidth() / 2.0f;
        float spawnY = tileRow * Tile.TILE_HEIGHT + Tile.TILE_HEIGHT / 2.0f - player.GetHeight() / 2.0f;

        player.setPosition(spawnX, spawnY);

        if (camera != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }
        if (keyManager != null) {
            keyManager.Clear();
        }
    }

    /*! \fn private void loadLevel(int level)
        \brief Incarca nivelul specificat si fisierele sale vizuale/logice.

        \details
        Atentie: aici sunt folosite exact numele de fisiere pe care le-ai atasat
        din folderul `maps`, inclusiv extensia `.png` pentru imaginile de baza
        si foreground.

        \param level Nivelul care trebuie incarcat.
    */
    private void loadLevel(int level) {
        currentLevel = level;
        wolves.clear();
        guardians.clear();
        skeleton = null;
        spider = null;
        wrongEntranceDeath = false;

        if (player == null) {
            player = new Player(0, 0);
        }

        if (level == 1) {
            map = new Map(
                    "res/maps/level1_base.png",
                    "res/maps/level1_foreground.png",
                    "res/maps/harta_primul_nivel.tmx"
            );
            setPlayerSpawn(10, 14);

            // Haita de 4 lupi — pozitii verificate libere pe harta 24x18
            wolves.clear();
            wolves.add(new Enemy(5  * Tile.TILE_WIDTH,  5 * Tile.TILE_HEIGHT, player));
            wolves.add(new Enemy(14 * Tile.TILE_WIDTH,  9 * Tile.TILE_HEIGHT, player));
            wolves.add(new Enemy(8  * Tile.TILE_WIDTH, 12 * Tile.TILE_HEIGHT, player));
            wolves.add(new Enemy(18 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT, player));

            // 2 gardieni sub tranzitia spre dungeon (col 19-20, rand 2)
            // Tranzitia reala e la randul 0-1, gardienii blocheaza accesul de sub ea
            guardians.clear();
            guardians.add(new Guardian(19 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player, false));
            guardians.add(new Guardian(20 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player, true));

        } else if (level == 2) {
            map = new Map(
                    "res/maps/level2_base.png",
                    "res/maps/level2_foreground.png",
                    "res/maps/harta_nivel2_dungeon.tmx"
            );
            setPlayerSpawn(19, 24);
            skeleton = new Skeleton(11 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT, player);
            spider = new Spider(29 * Tile.TILE_WIDTH, 19 * Tile.TILE_HEIGHT, player);

        } else if (level == 3) {
            map = new Map(
                    "res/maps/level3_village_base.png",
                    "res/maps/level3_village_foreground.png",
                    "res/maps/harta_nivel3_village.tmx"
            );
            setPlayerSpawn(24, 31);

        } else if (level == 4) {
            map = new Map(
                    "res/maps/level3_base.png",
                    "res/maps/level3_foreground.png",
                    "res/maps/harta_nivel3_the_great_hall.tmx"
            );
            setPlayerSpawn(9, 13);
        }

        transitionCooldown = 30;
    }

    /*! \fn private void checkLevelTransition()
        \brief Verifica daca jucatorul a ajuns intr-o zona de tranzitie.
    */
    private void checkLevelTransition() {
        if (transitionCooldown > 0 || player == null || map == null) {
            return;
        }

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
        } else if (currentLevel == 3) {
            if (map.isTransitionAtPixel(px, py)
                    || map.isTransitionAtPixel(px - 8, py)
                    || map.isTransitionAtPixel(px + 8, py)) {
                loadLevel(4);
                saveCurrentGame(false);
            }
        }
    }
}


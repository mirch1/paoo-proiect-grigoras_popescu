package PaooGame;

import PaooGame.enemies.Enemy;
import PaooGame.enemies.Skeleton;
import PaooGame.enemies.Spider;
import PaooGame.enemies.EnemyFactory;
import PaooGame.GameWindow.GameWindow;
import PaooGame.Graphics.Assets;
import PaooGame.Tiles.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import PaooGame.Exceptions.InvalidGameStateException;

/*! \class Game
    \brief Clasa principala a jocului.

    \details
    Aceasta clasa coordoneaza intreaga rulare a jocului:
    - initializeaza fereastra si resursele;
    - porneste game-loop-ul la aproximativ 60 FPS;
    - actualizeaza inputul, entitatile si camera;
    - deseneaza harta, personajele, meniul de pauza si HUD-ul;
    - gestioneaza salvarea si tranzitiile intre niveluri.

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

    /// Inamic specific nivelului 1.
    private Enemy wolf;

    /// Inamic specific nivelului 2.
    private Skeleton skeleton;

    /// Al doilea inamic specific nivelului 2.
    private Spider spider;

    private java.util.List<NPC> npcs = new java.util.ArrayList<>();

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

    /*! \brief Retine daca jocul este in stare de lupta.

    \details
    Este folosit pentru a schimba soundtrack-ul doar o singura data
    cand jucatorul intra sau iese din raza unui inamic.
 */
    private boolean inBattle = false;

    /// Optiunea curenta selectata in meniul de pauza.
    private int pauseMenuSelection = 0;

    /// Arata daca ecranul de moarte este activ.
    private boolean isDeathScreen = false;

    /// Optiunea curenta selectata in ecranul de moarte.
    private int deathMenuSelection = 0;

    /// Nivelul in care jucatorul a murit.
    private int deathLevel = 1;

    private String deathSubtitle = "";

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

    // =========================================================================
// AUDIO
// =========================================================================

    /*! \fn private void updateLevelMusic()
        \brief Porneste soundtrack-ul potrivit pentru nivelul curent.

        \details
        Aceasta metoda este apelata cand se incarca un nivel nou.
        Muzica de battle nu este pornita aici, ci separat, in functie de apropierea
        jucatorului de inamici.
     */
    private void updateLevelMusic() {
        /// Cand incarcam un nivel nou, iesim automat din starea de lupta.
        inBattle = false;

        switch (currentLevel) {
            case 1 -> AudioManager.getInstance().playMusic("res/audio/forest_theme.wav");
            case 2 -> AudioManager.getInstance().playMusic("res/audio/dungeon_theme.wav");
            case 3 -> AudioManager.getInstance().playMusic("res/audio/village_theme.wav");
            case 4 -> AudioManager.getInstance().playMusic("res/audio/great_hall_theme.wav");
            default -> AudioManager.getInstance().playMusic("res/audio/forest_theme.wav");
        }
    }

    /*! \fn private boolean isEnemyCloseForBattle(Entity enemy, float radius)
        \brief Verifica daca un inamic este suficient de aproape pentru a porni muzica de lupta.

        \param enemy Inamicul verificat.
        \param radius Raza de detectie pentru battle music.
        \return true daca inamicul este viu si aproape de jucator.
     */
    private boolean isEnemyCloseForBattle(Entity enemy, float radius) {
        if (player == null || enemy == null || enemy.isDead()) {
            return false;
        }

        float dx = player.GetFeetCenterX() - enemy.GetFeetCenterX();
        float dy = player.GetFeetBottomY() - enemy.GetFeetBottomY();

        double distance = Math.sqrt(dx * dx + dy * dy);

        /*
         * Pornim muzica de battle fie daca inamicul este aproape,
         * fie daca hitbox-urile se intersecteaza deja.
         */
        return distance <= radius || player.getFeetRect().intersects(enemy.getFeetRect());
    }

    /*! \fn private boolean shouldPlayBattleMusic()
        \brief Verifica daca in nivelul curent exista un inamic activ aproape de jucator.

        \return true daca trebuie pornita muzica de lupta.
     */
    private boolean shouldPlayBattleMusic() {
        return isEnemyCloseForBattle(wolf, 220)
                || isEnemyCloseForBattle(skeleton, 220)
                || isEnemyCloseForBattle(spider, 220);
    }

    /*! \fn private void updateBattleMusic()
        \brief Schimba muzica intre tema nivelului si tema de lupta.

        \details
        Daca jucatorul se apropie de un inamic, pornim battle_theme.
        Cand nu mai exista inamic viu aproape, revenim la muzica nivelului curent.
     */
    private void updateBattleMusic() {
        if (isDeathScreen || player == null || player.isDead()) {
            inBattle = false;
            return;
        }

        boolean shouldBeInBattle = shouldPlayBattleMusic();

        if (shouldBeInBattle && !inBattle) {
            inBattle = true;
            AudioManager.getInstance().playMusic("res/audio/battle_theme.wav");
        }

        if (!shouldBeInBattle && inBattle) {
            inBattle = false;
            updateLevelMusic();
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
        if (wolf != null && map != null) {
            wolf.Update(map);
        }
        if (skeleton != null && map != null) {
            skeleton.Update(map);
        }
        if (spider != null && map != null) {
            spider.Update(map);
        }

        if (map != null) {
            for (NPC npc : npcs) {
                npc.Update(map);
            }
        }

        /// Camera urmareste jucatorul activ.
        if (camera != null && player != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        /// Scadem cooldown-ul pentru schimbarea de nivel.
        if (transitionCooldown > 0) {
            transitionCooldown--;
        }

        if (isBadEntranceAttempt()) {
            openDeathScreen("BAD ENTRANCE");
            return;
        }

        /// Verificam tranzitiile si combat-ul.
        checkLevelTransition();
        if (player != null) {
            checkCombat();

            /*
             * Actualizam soundtrack-ul de battle dupa combat.
             * Daca jucatorul este aproape de un inamic viu, pornim muzica de lupta.
             * Daca se indeparteaza sau inamicul moare, revenim la tema nivelului.
             */
            updateBattleMusic();
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
            if (wolf != null && !wolf.isDead() && playerAtk.intersects(wolf.getFeetRect())) {
                wolf.takeDamage(Player.ATTACK_DAMAGE);
            }
            if (skeleton != null && !skeleton.isDead() && playerAtk.intersects(skeleton.getFeetRect())) {
                skeleton.takeDamage(Player.ATTACK_DAMAGE);
            }
            if (spider != null && !spider.isDead() && playerAtk.intersects(spider.getFeetRect())) {
                spider.takeDamage(Player.ATTACK_DAMAGE);
            }

            for (NPC npc : npcs) {
                if (npc.canBeAttacked() && playerAtk.intersects(npc.getFeetRect())) {
                    npc.takeDamage(Player.ATTACK_DAMAGE);
                }
            }
        }

        /// Damage de contact.
        if (wolf != null && !wolf.isDead() && playerFeet.intersects(wolf.getFeetRect())) {
            player.takeDamage(Enemy.ATTACK_DAMAGE);
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

        for (NPC npc : npcs) {
            if (npc.canDamagePlayer(player)) {
                player.takeDamage(NPC.GUARD_ATTACK_DAMAGE);
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

    private void openDeathScreen() {
        openDeathScreen("");
    }

    private void openDeathScreen(String subtitle) {
        deathLevel = currentLevel;
        deathMenuSelection = 0;
        deathSubtitle = subtitle;
        isDeathScreen = true;
        isPaused = false;

        AudioManager.getInstance().stopMusic();
        inBattle = false;

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
            player = new Player(0, 0);
            loadLevel(deathLevel);

            if (keyManager != null) {
                keyManager.Clear();
            }

            lastUpState = false;
            lastDownState = false;
            lastEnterState = false;
        } else {
            AudioManager.getInstance().stopMusic();
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
        /// Feedback audio scurt pentru selectia din meniul de pauza.
        AudioManager.getInstance().playSoundEffect("res/audio/button_click.wav");

        switch (pauseMenuSelection) {
            case 0 -> {
                Window owner = SwingUtilities.getWindowAncestor(wnd.GetCanvas());
                SwingUtilities.invokeLater(() -> SettingsDialog.showSettings(owner));
            }

            case 1 -> saveCurrentGame(true);

            case 2 -> returnToMainMenu();

            case 3 -> {
                AudioManager.getInstance().stopMusic();
                System.exit(0);
            }
        }
    }


    /*! \fn private void saveCurrentGame(boolean showMessage)
    \brief Salveaza progresul curent.

    \param showMessage Daca este true, afiseaza mesaj de confirmare.
*/
private void saveCurrentGame(boolean showMessage) {
    try {
        if (player == null) {
            throw new InvalidGameStateException(
                    "Nu se poate salva jocul: playerul nu exista."
            );
        }

        SaveManager.saveGame(currentLevel, player.GetX(), player.GetY());

        /// Redam un efect sonor scurt pentru salvare.
        AudioManager.getInstance().playSoundEffect("res/audio/save_theme.wav");

        if (showMessage) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Jocul a fost salvat cu succes.",
                    "Save Game",
                    JOptionPane.INFORMATION_MESSAGE
            ));
        }

    } catch (InvalidGameStateException e) {
        System.out.println(e.getMessage());

        if (showMessage) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    "Save Game Error",
                    JOptionPane.ERROR_MESSAGE
            ));
        }
    }
}
    
    /*! \fn private void returnToMainMenu()
        \brief Cere revenirea in meniul principal si opreste jocul.
    */
    private void returnToMainMenu() {
        /*
         * Oprim muzica nivelului / battle inainte de revenirea la meniu.
         * MenuWindow va porni din nou menu_theme.wav in constructor.
         */
        AudioManager.getInstance().stopMusic();
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

        /// 2. Inamicii.
        if (wolf != null && camera != null) {
            wolf.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }
        if (skeleton != null && camera != null) {
            skeleton.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }
        if (spider != null && camera != null) {
            spider.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        if (camera != null) {
            for (NPC npc : npcs) {
                npc.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
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
            if (wolf != null) {
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

        int startY = 330;

        if (deathSubtitle != null && !deathSubtitle.isEmpty()) {
            Font subtitleFont = new Font("Serif", Font.BOLD, 34);
            g2d.setFont(subtitleFont);
            FontMetrics subtitleMetrics = g2d.getFontMetrics(subtitleFont);

            int subtitleX = (LOGICALWIDTH - subtitleMetrics.stringWidth(deathSubtitle)) / 2;
            int subtitleY = titleY + 52;

            g2d.setColor(new Color(218, 165, 32));
            g2d.drawString(deathSubtitle, subtitleX, subtitleY);

            startY = 370;
        }

        String[] options = {"TRY AGAIN", "EXIT GAME"};
        Font optionFont = new Font("Serif", Font.BOLD, 34);
        g2d.setFont(optionFont);
        FontMetrics optionMetrics = g2d.getFontMetrics(optionFont);

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
        wolf = null;
        skeleton = null;
        spider = null;
        npcs.clear();

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
            wolf = EnemyFactory.createWolf(
                    15 * Tile.TILE_WIDTH,
                    14 * Tile.TILE_HEIGHT,
                    player
            );
            npcs.add(new NPC(11 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_spear_small.png"));
            npcs.add(new NPC(15 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_shield_small.png"));

        } else if (level == 2) {
            map = new Map(
                    "res/maps/level2_base.png",
                    "res/maps/level2_foreground.png",
                    "res/maps/harta_nivel2_dungeon.tmx"
            );
            setPlayerSpawn(19, 24);
            skeleton = EnemyFactory.createSkeleton(
                    11 * Tile.TILE_WIDTH,
                    13 * Tile.TILE_HEIGHT,
                    player
            );

            spider = EnemyFactory.createSpider(
                    29 * Tile.TILE_WIDTH,
                    19 * Tile.TILE_HEIGHT,
                    player
            );

        } else if (level == 3) {
            map = new Map(
                    "res/maps/level3_village_base.png",
                    "res/maps/level3_village_foreground.png",
                    "res/maps/harta_nivel3_village.tmx"
            );
            setPlayerSpawn(24, 31);
            // Garzi la poarta castelului
            npcs.add(new NPC(20 * Tile.TILE_WIDTH, 7 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_spear_small.png"));
            npcs.add(new NPC(28 * Tile.TILE_WIDTH, 7 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_shield_small.png"));

            // Sateni prin sat
            npcs.add(new NPC(17 * Tile.TILE_WIDTH, 24 * Tile.TILE_HEIGHT, player, NPC.NPCType.VILLAGER, "/textures/npc_village_blacksmith_small.png"));
            npcs.add(new NPC(33 * Tile.TILE_WIDTH, 25 * Tile.TILE_HEIGHT, player, NPC.NPCType.VILLAGER, "/textures/npc_village_peasant_woman_small.png"));
            npcs.add(new NPC(12 * Tile.TILE_WIDTH, 18 * Tile.TILE_HEIGHT, player, NPC.NPCType.VILLAGER, "/textures/npc_village_elder_lantern_small.png"));
            npcs.add(new NPC(28 * Tile.TILE_WIDTH, 20 * Tile.TILE_HEIGHT, player, NPC.NPCType.VILLAGER, "/textures/npc_village_merchant_traveler_small.png"));

        } else if (level == 4) {
            map = new Map(
                    "res/maps/level3_base.png",
                    "res/maps/level3_foreground.png",
                    "res/maps/harta_nivel3_the_great_hall.tmx"
            );
            setPlayerSpawn(9, 13);
            npcs.add(new NPC(6 * Tile.TILE_WIDTH, 10 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(13 * Tile.TILE_WIDTH, 10 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(7 * Tile.TILE_WIDTH, 12 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(12 * Tile.TILE_WIDTH, 12 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
        }

        transitionCooldown = 30;

        /*
         * Dupa incarcarea nivelului, pornim muzica potrivita.
         * Astfel, cand trecem Forest -> Dungeon -> Village -> Great Hall,
         * soundtrack-ul se schimba automat.
         */
        updateLevelMusic();
    }


    private boolean isBadEntranceAttempt() {
        if (currentLevel != 1 || player == null) {
            return false;
        }

        int tileCol = (int) (player.GetFeetCenterX() / Tile.TILE_WIDTH);
        int tileRow = (int) (player.GetFeetBottomY() / Tile.TILE_HEIGHT);

        return tileRow <= 3 && tileCol >= 11 && tileCol <= 15;
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




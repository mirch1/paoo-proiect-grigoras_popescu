package PaooGame;

import PaooGame.enemies.Enemy;
import PaooGame.enemies.Skeleton;
import PaooGame.enemies.Spider;
import PaooGame.enemies.Malakar;
import PaooGame.enemies.EnemyFactory;
import PaooGame.Exceptions.DatabaseException;
import PaooGame.Exceptions.InvalidGameStateException;
import PaooGame.GameWindow.GameWindow;
import PaooGame.Graphics.Assets;
import PaooGame.Tiles.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

/*!
 * \class Game
 * \brief Clasa principala a jocului.
 *
 * \details
 * Aceasta clasa coordoneaza intreaga rulare a jocului:
 * - initializeaza fereastra si resursele;
 * - porneste game-loop-ul la aproximativ 60 FPS;
 * - actualizeaza inputul, entitatile si camera;
 * - deseneaza harta, personajele, meniul de pauza si HUD-ul;
 * - gestioneaza salvarea (inclusiv in baza de date) si tranzitiile intre niveluri.
 */
public class Game implements Runnable {

    // =========================================================================
    //  CAMPURI PRINCIPALE
    // =========================================================================

    /// Fereastra principala a jocului.
    private GameWindow wnd;

    /// Arata daca jocul ruleaza in acest moment.
    private boolean runState;

    /// Thread-ul folosit pentru bucla principala a jocului.
    private Thread gameThread;

    /// BufferStrategy pentru randare fluida (triple buffering).
    private BufferStrategy bs;

    /// Harta nivelului curent.
    private Map map;

    /// Managerul de input de la tastatura.
    private KeyManager keyManager;

    /// Camera care urmareste jucatorul in lume.
    private Camera camera;

    /// Jucatorul principal.
    private Player player;

    // =========================================================================
    //  INAMICI
    // =========================================================================

    /// Inamici de tip lup pentru nivelul 1.
    private Enemy wolf;
    private Enemy wolf2;
    private Enemy wolf3;

    /// Inamici de tip schelet pentru nivelul 2.
    private Skeleton skeleton;
    private Skeleton skeleton2;
    private Skeleton skeleton3;

    /// Inamic de tip paianjen pentru nivelul 2.
    private Spider spider;

    /*!
     * \brief Boss-ul final din nivelul 4.
     * \details
     * Malakar apare exclusiv in Great Hall si este tratat separat de NPC-urile
     * obisnuite, deoarece are logica proprie de update, combat, animatii si boss bar.
     */
    private Malakar malakar;

    /*!
     * \brief NPC-ul magician din nivelul 2.
     * \details
     * Este un NPC pasiv, animat, cu care jucatorul poate interactiona
     * pentru a primi un dialog contextual in dungeon.
     */
    private WizardNPC dungeonWizard;

    /// Lista de NPC-uri din nivelul curent (garzi, sateni, garzi regale).
    private java.util.List<NPC> npcs = new java.util.ArrayList<>();

    // =========================================================================
    //  STARE JOC
    // =========================================================================

    /// Nivelul curent incarcat (1-4).
    private int currentLevel;

    /// Cooldown scurt pentru a preveni tranzitii multiple instant la schimbarea de nivel.
    private int transitionCooldown;

    /// Latimea logica a scenei de joc (rezolutie virtuala interna).
    private final int LOGICALWIDTH = 800;

    /// Inaltimea logica a scenei de joc (rezolutie virtuala interna).
    private final int LOGICALHEIGHT = 600;

    /// Contextul grafic folosit la desenare.
    private Graphics g;

    /// Arata daca jocul este in pauza (meniu pauza activ).
    private boolean isPaused = false;

    /*!
     * \brief Retine daca jocul este in stare de lupta.
     * \details
     * Este folosit pentru a schimba soundtrack-ul doar o singura data
     * atunci cand jucatorul intra sau iese din raza unui inamic.
     */
    private boolean inBattle = false;

    /// Optiunea curenta selectata in meniul de pauza (0-3).
    private int pauseMenuSelection = 0;

    /// Arata daca ecranul de moarte (YOU DIED) este activ.
    private boolean isDeathScreen = false;

    /// Optiunea curenta selectata in ecranul de moarte (0 = TRY AGAIN, 1 = EXIT GAME).
    private int deathMenuSelection = 0;

    /// Nivelul in care jucatorul a murit — folosit pentru retry pe acelasi nivel.
    private int deathLevel = 1;

    /// Subtitlul afisat pe ecranul de moarte (ex. "BAD ENTRANCE").
    private String deathSubtitle = "";

    /// Marcheaza revenirea in meniul principal dupa oprirea jocului.
    private volatile boolean returnToMenuRequested = false;

    /// Activeaza overlay-ul de debug (hitbox-uri + FPS).
    public static boolean showHitboxes = false;

    /// FPS-ul curent calculat o data pe secunda.
    public static int currentFPS = 0;

    // =========================================================================
    //  INPUT - EDGE DETECTION
    // =========================================================================

    /// Stari anterioare ale tastelor pentru detectia apasarilor unice (edge detection).
    private boolean lastEscapeState = false;
    private boolean lastUpState = false;
    private boolean lastDownState = false;
    private boolean lastEnterState = false;
    private boolean lastDebugState = false;

    /*!
     * \brief Starea anterioara a tastei SPACE.
     * \details Folosita pentru edge detection in dialogBox (apasare unica).
     */
    private boolean lastSpaceState = false;

    // =========================================================================
    //  SAVE / PROFIL / UI
    // =========================================================================

    /// Daca este true, jocul incearca sa incarce progresul salvat la initializare.
    private boolean loadSavedGame = false;

    /// Poțiunea specială din village, necesară înainte de Malakar.
    private boolean villagePotionUsed = false;

    /// Tile-ul de unde jucătorul poate interacționa cu poțiunea.
    private static final int VILLAGE_POTION_TRIGGER_TILE_COL = 43;
    private static final int VILLAGE_POTION_TRIGGER_TILE_ROW = 30;

    /// Tile-ul unde poțiunea este desenată efectiv in village.
    /// O desenăm mai spre colț, ca să pară ascunsă în tufele din dreapta-jos.
    private static final int VILLAGE_POTION_DRAW_TILE_COL = 44;
    private static final int VILLAGE_POTION_DRAW_TILE_ROW = 30;

    /// Indică dacă jucătorul a vorbit cu vrăjitorul din dungeon.
    private boolean talkedToDungeonWizard = false;

    /// Obiectul care retine starea citita din save file la Load Game.
    private SaveGameState savedGameState = null;

    /// Scorul acumulat in sesiunea curenta de joc.
    private int score = 0;

    /*!
     * \brief Caseta de dialog pentru naratiunea jocului.
     * \details Afiseaza texte la intrarea in niveluri sau la evenimente speciale.
     * Jocul este pauzat cat timp dialogul este activ.
     */
    private DialogBox dialogBox = new DialogBox();

    /*!
     * \brief Retine daca dialogul de introducere al fiecarui nivel a fost afisat.
     * \details
     * Index 1 = nivel 1, 2 = nivel 2, 3 = nivel 3, 4 = nivel 4.
     * Previne reafisarea aceluiasi dialog la reintrarea in nivel (ex. TRY AGAIN).
     */
    private boolean[] levelDialogShown = new boolean[5];

    // =========================================================================
    //  CONSTRUCTORI
    // =========================================================================

    public Game(String title, int width, int height) {
        this(title, width, height, false);
    }

    public Game(String title, int width, int height, boolean loadSavedGame) {
        wnd = new GameWindow(title, width, height);
        runState = false;
        this.loadSavedGame = loadSavedGame;
    }

    // =========================================================================
    //  INITIALIZARE
    // =========================================================================

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

                applyDefeatedEnemies(savedGameState);
                score = savedGameState.getScore();

                if (camera != null && map != null && player != null) {
                    camera.CenterOnPlayer(player, map);
                }

                if (savedGameState.getLevel() >= 1 && savedGameState.getLevel() <= 4) {
                    levelDialogShown[savedGameState.getLevel()] = true;
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

    // =========================================================================
    //  GAME LOOP
    // =========================================================================

    @Override
    public void run() {
        InitGame();

        long oldTime = System.nanoTime();
        long currentTime;
        final double timeFrame = 1_000_000_000.0 / 60.0;

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

    public synchronized void StartGame() {
        if (!runState) {
            runState = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public synchronized void StopGame() {
        if (runState) {
            runState = false;

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
    }

    // =========================================================================
    //  SCOR
    // =========================================================================

    public void addScore(int points) {
        score += points;

        PlayerProfile profile = ProfileManager.getActive();
        if (profile != null) {
            profile.setBestScore(score);
        }
    }

    public int getScore() {
        return score;
    }

    // =========================================================================
    //  AUDIO
    // =========================================================================

    private void updateLevelMusic() {
        inBattle = false;

        switch (currentLevel) {
            case 1 -> AudioManager.getInstance().playMusic("res/audio/forest_theme.wav");
            case 2 -> AudioManager.getInstance().playMusic("res/audio/dungeon_theme.wav");
            case 3 -> AudioManager.getInstance().playMusic("res/audio/village_theme.wav");
            case 4 -> AudioManager.getInstance().playMusic("res/audio/great_hall_theme.wav");
            default -> AudioManager.getInstance().playMusic("res/audio/forest_theme.wav");
        }
    }

    private boolean isEnemyCloseForBattle(Entity enemy, float radius) {
        if (player == null || enemy == null || enemy.isDead()) {
            return false;
        }

        float dx = player.GetFeetCenterX() - enemy.GetFeetCenterX();
        float dy = player.GetFeetBottomY() - enemy.GetFeetBottomY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        return distance <= radius || player.getFeetRect().intersects(enemy.getFeetRect());
    }

    private boolean shouldPlayBattleMusic() {
        if (isEnemyCloseForBattle(wolf, 220) ||
                isEnemyCloseForBattle(wolf2, 220) ||
                isEnemyCloseForBattle(wolf3, 220) ||
                isEnemyCloseForBattle(skeleton, 220) ||
                isEnemyCloseForBattle(skeleton2, 220) ||
                isEnemyCloseForBattle(skeleton3, 220) ||
                isEnemyCloseForBattle(spider, 220) ||
                isEnemyCloseForBattle(malakar, 260)) {
            return true;
        }

        for (NPC npc : npcs) {
            if (npc != null && npc.isGuardActive() && isEnemyCloseForBattle(npc, 220)) {
                return true;
            }
        }

        return false;
    }

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

    // =========================================================================
    //  UPDATE
    // =========================================================================

    private void Update() {
        keyManager.Update();

        if (isDeathScreen) {
            updateDeathScreen();
            return;
        }

        if (dialogBox.isActive()) {
            boolean currentSpaceState = keyManager.space;

            if (currentSpaceState && !lastSpaceState) {
                dialogBox.hide();
            }

            lastSpaceState = currentSpaceState;
            return;
        }

        if (keyManager.debug && !lastDebugState) {
            showHitboxes = !showHitboxes;
        }
        lastDebugState = keyManager.debug;

        if (keyManager.escape && !lastEscapeState) {
            isPaused = !isPaused;
        }
        lastEscapeState = keyManager.escape;

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

        if (player != null && map != null) {
            player.Update(keyManager, map);
        }

        if (wolf != null && map != null) wolf.Update(map);
        if (wolf2 != null && map != null) wolf2.Update(map);
        if (wolf3 != null && map != null) wolf3.Update(map);

        if (skeleton != null && map != null) skeleton.Update(map);
        if (skeleton2 != null && map != null) skeleton2.Update(map);
        if (skeleton3 != null && map != null) skeleton3.Update(map);

        if (spider != null && map != null) {
            spider.Update(map);
        }

        /*!
         * \brief Update separat pentru boss.
         * \details
         * Malakar are AI proprie si nu face parte din lista generica de NPC-uri,
         * asa ca trebuie actualizat explicit in game loop.
         */
        if (malakar != null && map != null) {
            malakar.Update(map);
        }

        if (dungeonWizard != null) {
            dungeonWizard.Update();
        }

        if (map != null) {
            for (NPC npc : npcs) {
                npc.Update(map);
            }
        }

        if (keyManager.enter && !lastEnterState) {
            if (dungeonWizard != null && dungeonWizard.canInteract()) {
                talkedToDungeonWizard = true;
                dungeonWizard.showDialog(dialogBox);
                lastEnterState = keyManager.enter;
                return;
            }

            if (currentLevel == 3 && tryDrinkVillagePotion()) {
                lastEnterState = keyManager.enter;
                return;
            }

            if (currentLevel == 3 && tryTalkToVillageNPC()) {
                lastEnterState = keyManager.enter;
                return;
            }
        }

        if (camera != null && player != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        if (transitionCooldown > 0) {
            transitionCooldown--;
        }

        if (isBadEntranceAttempt()) {
            openDeathScreen("BAD ENTRANCE");
            return;
        }

        checkLevelTransition();
        if (player != null) {
            checkCombat();
        }

        updateBattleMusic();

        lastUpState = keyManager.up;
        lastDownState = keyManager.down;
        lastEnterState = keyManager.enter;
    }

    // =========================================================================
    //  DIALOG NARATIV
    // =========================================================================

    private void showLevelDialog(int level) {
        if (level < 1 || level > 4) {
            return;
        }

        if (levelDialogShown[level]) {
            return;
        }

        levelDialogShown[level] = true;

        switch (level) {
            case 1 -> dialogBox.show(
                    "Chapter I  —  The Cursed Forest",
                    "The kingdom of Aethelgard has fallen into darkness.",
                    "You are its last loyal knight. The key to the ruins awaits.",
                    "Beware — the forest is not as empty as it seems.",
                    "[ SPACE ]  Continue"
            );

            case 2 -> dialogBox.show(
                    "Chapter II  —  Beneath the Castle",
                    "The dungeon holds secrets older than the kingdom itself.",
                    "A spirit guardian blocks the path forward.",
                    "Find the artifact that opens the Great Hall.",
                    "[ SPACE ]  Continue"
            );

            case 3 -> dialogBox.show(
                    "Chapter III  —  The Cursed Village",
                    "The village surrounding the castle has been corrupted.",
                    "Shadows stir between the houses. Trust no one.",
                    "The gates to the Great Hall lie ahead.",
                    "[ SPACE ]  Continue"
            );

            case 4 -> dialogBox.show(
                    "Chapter IV  —  The Great Hall",
                    "This is where it ends.",
                    "The fallen crown lies beyond the royal guards.",
                    "For Aethelgard. Reclaim what was lost.",
                    "[ SPACE ]  Continue"
            );

            default -> {
            }
        }
    }

    // =========================================================================
    //  COMBAT
    // =========================================================================

    private void checkCombat() {
        if (player == null || player.isDead()) {
            return;
        }

        Rectangle playerAtk = player.getAttackHitbox();
        Rectangle playerFeet = player.getFeetRect();

        int playerAttackDamage = GameSettings.getPlayerDamage(Player.ATTACK_DAMAGE);

        int wolfAttackDamage = GameSettings.getEnemyDamage(Enemy.ATTACK_DAMAGE);
        int skeletonAttackDamage = GameSettings.getEnemyDamage(Skeleton.ATTACK_DAMAGE);
        int spiderWebDamage = GameSettings.getEnemyDamage(Spider.WEB_DAMAGE);
        int guardAttackDamage = GameSettings.getEnemyDamage(NPC.GUARD_ATTACK_DAMAGE);

        /*!
         * \brief Damage pentru boss Malakar.
         * \details
         * Folosim doua valori separate, deoarece boss-ul are doua tipuri de atac.
         * In versiunea actuala, Attack1 este aplicat la contact direct,
         * iar Attack2 este pregatit pentru extindere ulterioara.
         */
        int malakarAttack1Damage = GameSettings.getEnemyDamage(Malakar.ATTACK1_DAMAGE);
        int malakarAttack2Damage = GameSettings.getEnemyDamage(Malakar.ATTACK2_DAMAGE);

        if (playerAtk != null) {
            if (wolf != null && !wolf.isDead() && playerAtk.intersects(wolf.getFeetRect())) {
                wolf.takeDamage(playerAttackDamage);
            }
            if (wolf2 != null && !wolf2.isDead() && playerAtk.intersects(wolf2.getFeetRect())) {
                wolf2.takeDamage(playerAttackDamage);
            }
            if (wolf3 != null && !wolf3.isDead() && playerAtk.intersects(wolf3.getFeetRect())) {
                wolf3.takeDamage(playerAttackDamage);
            }

            if (skeleton != null && !skeleton.isDead() && playerAtk.intersects(skeleton.getFeetRect())) {
                skeleton.takeDamage(playerAttackDamage);
            }
            if (skeleton2 != null && !skeleton2.isDead() && playerAtk.intersects(skeleton2.getFeetRect())) {
                skeleton2.takeDamage(playerAttackDamage);
            }
            if (skeleton3 != null && !skeleton3.isDead() && playerAtk.intersects(skeleton3.getFeetRect())) {
                skeleton3.takeDamage(playerAttackDamage);
            }

            if (spider != null && !spider.isDead() && playerAtk.intersects(spider.getFeetRect())) {
                spider.takeDamage(playerAttackDamage);
            }

            /*!
             * \brief Playerul poate lovi boss-ul daca hitbox-ul de atac il intersecteaza.
             * \details
             * Malakar foloseste tot getFeetRect(), astfel ramane compatibil cu
             * sistemul deja existent de combat din joc.
             */
            if (malakar != null && !malakar.isDead() && playerAtk.intersects(malakar.getFeetRect())) {
                malakar.takeDamage(playerAttackDamage);
            }

            for (NPC npc : npcs) {
                if (npc != null && npc.canBeAttacked() && playerAtk.intersects(npc.getFeetRect())) {
                    npc.takeDamage(playerAttackDamage);
                }
            }
        }

        if (wolf != null && !wolf.isDead() && playerFeet.intersects(wolf.getFeetRect())) {
            player.takeDamage(wolfAttackDamage);
        }
        if (wolf2 != null && !wolf2.isDead() && playerFeet.intersects(wolf2.getFeetRect())) {
            player.takeDamage(wolfAttackDamage);
        }
        if (wolf3 != null && !wolf3.isDead() && playerFeet.intersects(wolf3.getFeetRect())) {
            player.takeDamage(wolfAttackDamage);
        }

        if (skeleton != null && !skeleton.isDead() && playerFeet.intersects(skeleton.getFeetRect())) {
            player.takeDamage(skeletonAttackDamage);
        }
        if (skeleton2 != null && !skeleton2.isDead() && playerFeet.intersects(skeleton2.getFeetRect())) {
            player.takeDamage(skeletonAttackDamage);
        }
        if (skeleton3 != null && !skeleton3.isDead() && playerFeet.intersects(skeleton3.getFeetRect())) {
            player.takeDamage(skeletonAttackDamage);
        }

        /*!
         * \brief Atacul de baza al lui Malakar.
         * \details
         * In aceasta varianta minima, boss-ul loveste prin contact direct.
         * Daca vrei mai tarziu, poti separa momentul exact al impactului pe frame-ul
         * de animatie sau poti activa Attack2 conditionat de starea boss-ului.
         */
        if (malakar != null && !malakar.isDead() && playerFeet.intersects(malakar.getFeetRect())) {
            if (malakar.isUsingAttack2()) {
                player.takeDamage(malakarAttack2Damage);
            } else {
                player.takeDamage(malakarAttack1Damage);
            }
        }

        if (spider != null && !spider.isDead() && spider.isWebActive()) {
            float wx = spider.getWebX();
            float wy = spider.getWebY();
            float px = player.GetFeetCenterX();
            float py = player.GetFeetBottomY();

            double dist = Math.sqrt((wx - px) * (wx - px) + (wy - py) * (wy - py));

            if (dist < 16) {
                player.takeDamage(spiderWebDamage);
                spider.deactivateWeb();
            }
        }

        for (NPC npc : npcs) {
            if (npc != null && npc.canDamagePlayer(player)) {
                player.takeDamage(guardAttackDamage);
            }
        }

        if (wolf != null && wolf.isDead() && !wolf.isScoreAwarded()) {
            addScore(GameSettings.SCORE_WOLF_KILLED);
            wolf.setScoreAwarded(true);
        }
        if (wolf2 != null && wolf2.isDead() && !wolf2.isScoreAwarded()) {
            addScore(GameSettings.SCORE_WOLF_KILLED);
            wolf2.setScoreAwarded(true);
        }
        if (wolf3 != null && wolf3.isDead() && !wolf3.isScoreAwarded()) {
            addScore(GameSettings.SCORE_WOLF_KILLED);
            wolf3.setScoreAwarded(true);
        }

        if (skeleton != null && skeleton.isDead() && !skeleton.isScoreAwarded()) {
            addScore(GameSettings.SCORE_SKELETON_KILLED);
            skeleton.setScoreAwarded(true);
        }
        if (skeleton2 != null && skeleton2.isDead() && !skeleton2.isScoreAwarded()) {
            addScore(GameSettings.SCORE_SKELETON_KILLED);
            skeleton2.setScoreAwarded(true);
        }
        if (skeleton3 != null && skeleton3.isDead() && !skeleton3.isScoreAwarded()) {
            addScore(GameSettings.SCORE_SKELETON_KILLED);
            skeleton3.setScoreAwarded(true);
        }

        if (spider != null && spider.isDead() && !spider.isScoreAwarded()) {
            addScore(GameSettings.SCORE_SPIDER_KILLED);
            spider.setScoreAwarded(true);
        }

        /*!
         * \brief Acordam scor pentru boss o singura data.
         */
        if (malakar != null && malakar.isDead() && !malakar.isScoreAwarded()) {
            addScore(500);
            malakar.setScoreAwarded(true);
        }

        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (npc != null && npc.isDefeated() && !npc.isScoreAwarded()) {
                addScore(GameSettings.SCORE_GUARDIAN_KILLED);
                npc.setScoreAwarded(true);
            }
        }

        if (player.isDead()) {
            openDeathScreen();
        }
    }

    // =========================================================================
    //  ECRAN DE MOARTE
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
        lastSpaceState = false;
    }

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

    private void executeDeathMenuAction() {
        if (deathMenuSelection == 0) {
            score = 0;
            isDeathScreen = false;

            if (deathLevel >= 1 && deathLevel <= 4) {
                levelDialogShown[deathLevel] = false;
            }

            player = new Player(0, 0);
            loadLevel(deathLevel);

            if (keyManager != null) {
                keyManager.Clear();
            }

            lastUpState = false;
            lastDownState = false;
            lastEnterState = false;
            lastSpaceState = false;
        } else {
            AudioManager.getInstance().stopMusic();

            try {
                DatabaseManager.getInstance().closeConnection();
            } catch (Exception ignored) {
            }

            System.exit(0);
        }
    }

    // =========================================================================
    //  MENIU PAUZA
    // =========================================================================

    private void executePauseMenuAction() {
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

                try {
                    DatabaseManager.getInstance().closeConnection();
                } catch (Exception ignored) {
                }

                System.exit(0);
            }
            default -> {
            }
        }
    }

    // =========================================================================
    //  SALVARE
    // =========================================================================

    private void appendDefeatedId(StringBuilder builder, String id) {
        if (builder.length() > 0) {
            builder.append(",");
        }
        builder.append(id);
    }

    private String getDefeatedEnemiesForSave() {
        StringBuilder defeated = new StringBuilder();

        if (wolf != null && wolf.isDead()) appendDefeatedId(defeated, "wolf");
        if (wolf2 != null && wolf2.isDead()) appendDefeatedId(defeated, "wolf2");
        if (wolf3 != null && wolf3.isDead()) appendDefeatedId(defeated, "wolf3");

        if (skeleton != null && skeleton.isDead()) appendDefeatedId(defeated, "skeleton");
        if (skeleton2 != null && skeleton2.isDead()) appendDefeatedId(defeated, "skeleton2");
        if (skeleton3 != null && skeleton3.isDead()) appendDefeatedId(defeated, "skeleton3");

        if (spider != null && spider.isDead()) appendDefeatedId(defeated, "spider");

        /*!
         * \brief Salvam starea boss-ului separat.
         * \details
         * Daca Malakar a fost invins, il marcam explicit in save pentru ca la
         * Load Game sa nu mai reapara in level 4.
         */
        if (malakar != null && malakar.isDead()) appendDefeatedId(defeated, "malakar");

        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (npc != null && npc.isDefeated()) {
                appendDefeatedId(defeated, "npc" + i);
            }
        }

        if (villagePotionUsed) {
            appendDefeatedId(defeated, "moonrootPotionUsed");
        }

        return defeated.toString();
    }

    private void applyDefeatedEnemies(SaveGameState state) {
        if (state == null) {
            return;
        }

        if (wolf != null && state.isEnemyDefeated("wolf")) wolf.forceDead();
        if (wolf2 != null && state.isEnemyDefeated("wolf2")) wolf2.forceDead();
        if (wolf3 != null && state.isEnemyDefeated("wolf3")) wolf3.forceDead();

        if (skeleton != null && state.isEnemyDefeated("skeleton")) skeleton.forceDead();
        if (skeleton2 != null && state.isEnemyDefeated("skeleton2")) skeleton2.forceDead();
        if (skeleton3 != null && state.isEnemyDefeated("skeleton3")) skeleton3.forceDead();

        if (spider != null && state.isEnemyDefeated("spider")) spider.forceDead();

        if (state.isEnemyDefeated("moonrootPotionUsed")) {
            villagePotionUsed = true;

            if (player != null) {
                player.drinkMoonrootPotion();
            }
        }
        /*!
         * \brief Restauram starea boss-ului dupa Load Game.
         * \details
         * Daca Malakar era deja invins in save, il fortam direct in starea dead.
         */
        if (malakar != null && state.isEnemyDefeated("malakar")) malakar.forceDead();

        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (npc != null && state.isEnemyDefeated("npc" + i)) {
                npc.forceDefeatedState();
            }
        }
    }

    private void saveCurrentGame(boolean showMessage) {
        try {
            if (player == null) {
                throw new InvalidGameStateException(
                        "Nu se poate salva jocul: playerul nu exista."
                );
            }

            SaveManager.saveGame(
                    currentLevel,
                    player.GetX(),
                    player.GetY(),
                    getDefeatedEnemiesForSave(),
                    score
            );

            try {
                PlayerProfile profile = ProfileManager.getActive();
                String name = (profile != null) ? profile.getName() : "Anonim";

                DatabaseManager.getInstance().insertScore(name, score, currentLevel);

                System.out.println(
                        "DB: scor salvat — " + name + " | " + score + " pts | nivel " + currentLevel
                );
            } catch (DatabaseException dbEx) {
                System.out.println("DB: eroare la salvarea scorului — " + dbEx.getMessage());
            }

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

    private void returnToMainMenu() {
        AudioManager.getInstance().stopMusic();
        returnToMenuRequested = true;
        runState = false;
    }

    // =========================================================================
    //  DRAW
    // =========================================================================

    private void drawCinematicOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 35));
        g2d.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0, LOGICALWIDTH, 42);
        g2d.fillRect(0, LOGICALHEIGHT - 42, LOGICALWIDTH, 42);
    }


    private Rectangle getVillagePotionRect() {
        return new Rectangle(
                VILLAGE_POTION_TRIGGER_TILE_COL * Tile.TILE_WIDTH,
                VILLAGE_POTION_TRIGGER_TILE_ROW * Tile.TILE_HEIGHT,
                Tile.TILE_WIDTH,
                Tile.TILE_HEIGHT
        );
    }

    private boolean isPlayerNearVillagePotion() {
        if (player == null || villagePotionUsed) {
            return false;
        }

        Rectangle potionArea = getVillagePotionRect();
        potionArea.grow(30, 30);

        return potionArea.intersects(player.getFeetRect());
    }


    private void drawVillagePotion(Graphics2D g2d, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (currentLevel != 3 || villagePotionUsed) {
            return;
        }

        int worldX = VILLAGE_POTION_DRAW_TILE_COL * Tile.TILE_WIDTH + 24;
        int worldY = VILLAGE_POTION_DRAW_TILE_ROW * Tile.TILE_HEIGHT + 24;


        int screenX = offsetX + worldX - cameraX;
        int screenY = offsetY + worldY - cameraY;

        // Umbră
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillOval(screenX - 4, screenY + 12, 16, 6);

        // Sticluță
        g2d.setColor(new Color(55, 10, 75));
        g2d.fillRoundRect(screenX, screenY, 9, 16, 4, 4);

        // Lichid
        g2d.setColor(new Color(120, 45, 170));
        g2d.fillRoundRect(screenX + 2, screenY + 7, 5, 8, 3, 3);

        // Dop
        g2d.setColor(new Color(120, 85, 45));
        g2d.fillRect(screenX + 2, screenY - 3, 5, 4);

        // O mică licărire, ca să poată fi observată doar dacă te uiți atent.
        g2d.setColor(new Color(210, 170, 240, 130));
        g2d.drawLine(screenX + 7, screenY + 3, screenX + 9, screenY + 1);
    }

    private boolean tryDrinkVillagePotion() {
        if (currentLevel != 3 || player == null || villagePotionUsed) {
            return false;
        }

        if (!isPlayerNearVillagePotion()) {
            return false;
        }

        villagePotionUsed = true;
        player.drinkMoonrootPotion();
        addScore(100);

        dialogBox.show(
                "Hidden Moonroot Potion",
                "Behind the overgrown bushes, you find a small vial pulsing with pale violet light.",
                "You drink it, and a cold strength spreads through your body.",
                "Your life force has increased. Malakar's curse will not break you so easily now.",
                "[ SPACE ] Continue"
        );

        return true;
    }

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

        double scaleX = (double) wnd.GetWndWidth() / LOGICALWIDTH;
        double scaleY = (double) wnd.GetWndHeight() / LOGICALHEIGHT;
        g2d.scale(scaleX, scaleY);

        g.setColor(new Color(10, 15, 10));
        g.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

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

        if (map != null && camera != null) {
            map.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        if (wolf != null && camera != null) wolf.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (wolf2 != null && camera != null) wolf2.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (wolf3 != null && camera != null) wolf3.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        if (skeleton != null && camera != null)
            skeleton.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (skeleton2 != null && camera != null)
            skeleton2.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (skeleton3 != null && camera != null)
            skeleton3.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        if (spider != null && camera != null)
            spider.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /*!
         * \brief Desenam boss-ul explicit in scena.
         * \details
         * Malakar nu este NPC, deci trebuie randat separat fata de lista generica npcs.
         */
        if (malakar != null && camera != null)
            malakar.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        if (dungeonWizard != null && camera != null) {
            dungeonWizard.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        if (camera != null) {
            for (NPC npc : npcs) {
                npc.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
        }

        if (camera != null) {
            drawVillagePotion(
                    g2d,
                    (int) camera.GetX(),
                    (int) camera.GetY(),
                    offsetX,
                    offsetY
            );
        }

        if (player != null && camera != null) {
            player.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        if (map != null && camera != null) {
            map.DrawForeground(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        if (GameSettings.cinematicMode && !Game.showHitboxes) {
            drawCinematicOverlay(g2d);
        }

        if (camera != null) {
            if (wolf != null) wolf.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (wolf2 != null) wolf2.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (wolf3 != null) wolf3.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

            if (skeleton != null)
                skeleton.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (skeleton2 != null)
                skeleton2.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (skeleton3 != null)
                skeleton3.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

            if (spider != null)
                spider.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

            /*!
             * \brief Bara mica de HP deasupra boss-ului.
             */
            if (malakar != null) {
                malakar.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            }
        }

        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

            String[] options = {"SETTINGS", "SAVE GAME", "RETURN TO MENU", "EXIT"};
            Font pauseFont = new Font("Serif", Font.BOLD, 36);
            g2d.setFont(pauseFont);
            FontMetrics fm = g2d.getFontMetrics(pauseFont);
            int startPauseY = 250;

            for (int i = 0; i < options.length; i++) {
                int x = (LOGICALWIDTH - fm.stringWidth(options[i])) / 2;
                int y = startPauseY + i * 70;

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

        if (showHitboxes) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString("FPS: " + currentFPS, 15, 30);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2d.drawString("Mod Debug: ON", 15, 50);
        }

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

        if (dungeonWizard != null && dungeonWizard.canInteract() && !dialogBox.isActive() && !isPaused && !isDeathScreen) {
            g2d.setFont(new Font("Serif", Font.BOLD, 16));
            g2d.setColor(new Color(255, 230, 170));
            g2d.drawString("[ ENTER ] Talk", 315, 520);
        }

        if (currentLevel == 3 && isPlayerNearVillagePotion() && !dialogBox.isActive() && !isPaused && !isDeathScreen) {
            g2d.setFont(new Font("Serif", Font.BOLD, 16));
            g2d.setColor(new Color(255, 230, 170));
            g2d.drawString("[ ENTER ] Inspect", 305, 520);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 14));
        g2d.drawString("SCORE: " + score, LOGICALWIDTH - 155, 25);

        /*!
         * \brief Bara mare speciala de boss.
         * \details
         * O afisam doar in nivelul 4 si doar cat timp Malakar este viu.
         */
        if (malakar != null && !malakar.isDead() && currentLevel == 4) {
            malakar.drawBossBar(g2d, LOGICALWIDTH);
        }

        if (isDeathScreen) {
            drawDeathScreen(g2d);

            Font scoreFont = new Font("Serif", Font.BOLD, 28);
            g2d.setFont(scoreFont);
            g2d.setColor(new Color(180, 180, 190));

            String scoreText = "SCOR FINAL: " + score;
            FontMetrics sm = g2d.getFontMetrics(scoreFont);
            g2d.drawString(scoreText, (LOGICALWIDTH - sm.stringWidth(scoreText)) / 2, 285);
        }

        dialogBox.draw(g2d, LOGICALWIDTH, LOGICALHEIGHT);

        if (runState && wnd.GetCanvas().isDisplayable()) {
            bs.show();
        }

        g.dispose();
    }

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
            Font subFont = new Font("Serif", Font.BOLD, 34);
            g2d.setFont(subFont);

            FontMetrics subMetrics = g2d.getFontMetrics(subFont);
            int subX = (LOGICALWIDTH - subMetrics.stringWidth(deathSubtitle)) / 2;
            int subY = titleY + 52;

            g2d.setColor(new Color(218, 165, 32));
            g2d.drawString(deathSubtitle, subX, subY);

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
    //  NIVELURI
    // =========================================================================

    /**
     * Pozitioneaza jucatorul pe harta in centrul unui tile.
     *
     * @param tileCol coloana tile-ului de spawn
     * @param tileRow linia tile-ului de spawn
     */
    private void setPlayerSpawn(int tileCol, int tileRow) {
        // Daca jucatorul nu exista inca, nu avem ce pozitiona.
        if (player == null) {
            return;
        }

        // Calculam coordonatele astfel incat sprite-ul jucatorului sa fie centrat pe tile-ul dorit.
        float spawnX = tileCol * Tile.TILE_WIDTH+ Tile.TILE_WIDTH/ 2.0f - player.GetWidth() / 2.0f;
        float spawnY = tileRow * Tile.TILE_HEIGHT+ Tile.TILE_HEIGHT/ 2.0f - player.GetHeight() / 2.0f;

        // Aplicam pozitia noua a jucatorului.
        player.setPosition(spawnX, spawnY);

        // Daca avem camera si harta, recenter camera pe jucator dupa spawn.
        if (camera != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        // Curatam inputul pentru a evita miscari sau tranzitii accidentale imediat dupa spawn.
        if (keyManager != null) {
            keyManager.Clear();
        }
    }

    /**
     * Incarca nivelul cerut si reinitializeaza entitatile specifice acelui nivel.
     * Scorul NU este resetat aici.
     *
     * @param level nivelul care trebuie incarcat
     */
    /**
     * Incarca nivelul cerut si reinitializeaza entitatile specifice acelui nivel.
     * Scorul NU este resetat aici.
     *
     * @param level nivelul care trebuie incarcat
     */
    private void loadLevel(int level) {
        // Retinem nivelul curent.
        currentLevel = level;

        // Resetam toti inamicii si boss-ul pentru a reincarca starea nivelului de la zero.
        wolf = null;
        wolf2 = null;
        wolf3 = null;

        skeleton = null;
        skeleton2 = null;
        skeleton3 = null;

        spider = null;
        malakar = null;

        // Golim lista de NPC-uri a nivelului precedent.
        npcs.clear();

        // Resetam si NPC-ul special din dungeon.
        dungeonWizard = null;

        // Daca player-ul nu exista inca, il cream.
        if (player == null) {
            player = new Player(0, 0);
        }

        // =========================
        // NIVELUL 1 - FOREST
        // =========================
        if (level == 1) {
            map = new Map("res/maps/level1_base.png",
                    "res/maps/level1_foreground.png",
                    "res/maps/harta_primul_nivel.tmx");

            // Pozitia initiala a jucatorului in nivelul 1.
            setPlayerSpawn(10, 14);

            // Inamicii de pe nivelul 1.
            wolf = EnemyFactory.createWolf(
                    15 * Tile.TILE_WIDTH,
                    14 * Tile.TILE_HEIGHT,
                    player
            );

            wolf2 = EnemyFactory.createWolf(
                    6 * Tile.TILE_WIDTH,
                    11 * Tile.TILE_HEIGHT,
                    player
            );

            wolf3 = EnemyFactory.createWolf(
                    18 * Tile.TILE_WIDTH,
                    8 * Tile.TILE_HEIGHT,
                    player
            );
            // Garzile de la poarta satului.
            npcs.add(new NPC(11 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_spear_small.png"));
            npcs.add(new NPC(15 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_shield_small.png"));
        }

        // =========================
        // NIVELUL 2 - DUNGEON
        // =========================
        else if (level == 2) {
            map = new Map("res/maps/level2_base.png",
                    "res/maps/level2_foreground.png",
                    "res/maps/harta_nivel2_dungeon.tmx");

            // Pozitia initiala a jucatorului in dungeon.
            setPlayerSpawn(19, 24);

            // Inamicii specifici dungeon-ului.
            // Inamicii specifici dungeon-ului.
            skeleton = EnemyFactory.createSkeleton(
                    11 * Tile.TILE_WIDTH,
                    13 * Tile.TILE_HEIGHT,
                    player
            );

            skeleton2 = EnemyFactory.createSkeleton(
                    20 * Tile.TILE_WIDTH,
                    13 * Tile.TILE_HEIGHT,
                    player
            );

            skeleton3 = EnemyFactory.createSkeleton(
                    10 * Tile.TILE_WIDTH,
                    23 * Tile.TILE_HEIGHT,
                    player
            );

            spider = EnemyFactory.createSpider(
                    29 * Tile.TILE_WIDTH,
                    19 * Tile.TILE_HEIGHT,
                    player
            );

            // NPC-ul special animat din nivelul 2.
            dungeonWizard = new WizardNPC(
                    14 * Tile.TILE_WIDTH,
                    18 * Tile.TILE_HEIGHT,
                    player,
                    "/textures/Idle_CrazyMagician.png",
                    "The Imprisoned Wizard",
                    "Knight... listen carefully. Malakar is far stronger than he seems.",
                    "The passage ahead leads to the corrupted village beneath his shadow.",
                    "There is a Moonroot Potion hidden there. Find it before entering the Great Hall.",
                    "Drink it, and your body will resist part of Malakar's curse.",
                    "[ SPACE ] Continue"
            );
        }

        // =========================
        // NIVELUL 3 - VILLAGE
        // =========================
        else if (level == 3) {
            map = new Map("res/maps/level3_village_base.png",
                    "res/maps/level3_village_foreground.png",
                    "res/maps/harta_nivel3_village.tmx");

            // Pozitia initiala a jucatorului in sat.
            setPlayerSpawn(24, 31);

            // Garzi la poarta castelului.
            npcs.add(new NPC(20 * Tile.TILE_WIDTH, 7 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_spear_small.png"));
            npcs.add(new NPC(28 * Tile.TILE_WIDTH, 7 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.GUARD, "/textures/npc_village_gate_guard_shield_small.png"));

            // Sateni raspanditi prin sat.
            npcs.add(new NPC(17 * Tile.TILE_WIDTH, 24 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.VILLAGER, "/textures/npc_village_blacksmith_small.png"));
            npcs.add(new NPC(33 * Tile.TILE_WIDTH, 25 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.VILLAGER, "/textures/npc_village_peasant_woman_small.png"));
            npcs.add(new NPC(12 * Tile.TILE_WIDTH, 18 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.VILLAGER, "/textures/npc_village_elder_lantern_small.png"));
            npcs.add(new NPC(28 * Tile.TILE_WIDTH, 20 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.VILLAGER, "/textures/npc_village_merchant_traveler_small.png"));
        }

        // =========================
        // NIVELUL 4 - GREAT HALL
        // =========================
        else if (level == 4) {
            map = new Map("res/maps/level3_base.png",
                    "res/maps/level3_foreground.png",
                    "res/maps/harta_nivel3_the_great_hall.tmx");

            // Pozitia initiala a jucatorului in Great Hall.
            setPlayerSpawn(9, 13);

            // Cele patru garzi regale ostile din sala mare.
            npcs.add(new NPC(6 * Tile.TILE_WIDTH, 10 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(13 * Tile.TILE_WIDTH, 10 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(7 * Tile.TILE_WIDTH, 12 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(12 * Tile.TILE_WIDTH, 12 * Tile.TILE_HEIGHT, player,
                    NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));

            /*!
             * \brief Spawn explicit pentru boss-ul final Malakar.
             * \details
             * Boss-ul nu face parte din lista generica de NPC-uri, deci trebuie
             * creat separat aici. Restul logicii (Update, Draw, combat, boss bar,
             * save/load) exista deja in Game.java si va functiona automat dupa
             * aceasta initializare.
             *
             * Coordonatele pot fi ajustate ulterior daca vrei o pozitionare mai
             * dramatica in sala, dar acest spawn il face sa apara imediat pe nivel.
             */
            malakar = new Malakar(2 * Tile.TILE_WIDTH, 8 * Tile.TILE_HEIGHT, player);
        }

        // Punem un mic cooldown pentru a evita tranzitii multiple instant dupa incarcare.
        transitionCooldown = 30;

        // Pornim muzica potrivita pentru nivelul nou incarcat.
        updateLevelMusic();

        // Afisam dialogul de introducere al nivelului, daca nu a fost deja afisat in sesiunea curenta.
        showLevelDialog(level);
    }


    private boolean canInteractWithNPC(NPC npc) {
        if (npc == null || player == null) {
            return false;
        }

        Rectangle interactionArea = npc.getFeetRect();
        interactionArea.grow(55, 55);

        return interactionArea.intersects(player.getFeetRect());
    }

    private boolean tryTalkToVillageNPC() {
        if (currentLevel != 3 || npcs == null || npcs.isEmpty()) {
            return false;
        }

        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);

            if (npc != null && canInteractWithNPC(npc)) {
                showVillageNPCDialog(i);
                return true;
            }
        }

        return false;
    }

    private void showVillageNPCDialog(int npcIndex) {
        switch (npcIndex) {
            case 0, 1 -> dialogBox.show(
                    "Castle Guard",
                    "The Great Hall is sealed by fear and dark magic.",
                    "Do not face Malakar without protection.",
                    "Speak with the villagers. Someone knows where the potion is hidden.",
                    "[ SPACE ] Continue"
            );

            case 2 -> dialogBox.show(
                    "Blacksmith",
                    "Your blade is strong, knight, but steel alone will not defeat Malakar.",
                    "The old stories speak of a Moonroot Potion hidden in this village.",
                    "[ SPACE ] Continue"
            );

            case 3 -> dialogBox.show(
                    "Village Woman",
                    "I saw a strange vial near the lower houses.",
                    "It glowed like moonlight, but no one here dared touch it.",
                    "[ SPACE ] Continue"
            );

            case 4 -> dialogBox.show(
                    "Village Elder",
                    "The potion will not kill Malakar, but it may keep you alive long enough.",
                    "Drink it before entering the Great Hall.",
                    "[ SPACE ] Continue"
            );

            case 5 -> dialogBox.show(
                    "Merchant",
                    "The Moonroot Potion was hidden away from Malakar's eyes.",
                    "Search the south-eastern edge of the village, near the bushes behind the last house.",
                    "You will not see it unless you truly look for it.",
                    "[ SPACE ] Continue"
            );

            default -> dialogBox.show(
                    "Villager",
                    "Malakar's shadow has reached every home.",
                    "Find the potion before you go further.",
                    "[ SPACE ] Continue"
            );
        }
    }

    /**
     * Detecteaza daca jucatorul incearca sa intre prin zona interzisa
     * din partea de sus a nivelului 1.
     *
     * @return true daca jucatorul face o tentativa de intrare ilegala
     */
    private boolean isBadEntranceAttempt() {
        // Verificarea este relevanta doar in nivelul 1 si doar daca player-ul exista.
        if (currentLevel != 1 || player == null) {
            return false;
        }

        // Determinam tile-ul pe care se afla picioarele jucatorului.
        int tileCol = (int) (player.GetFeetCenterX() / Tile.TILE_WIDTH);
        int tileRow = (int) (player.GetFeetBottomY() / Tile.TILE_HEIGHT);

        // Zona interzisa este in partea superioara a hartii, intre coloanele 11 si 15 inclusiv.
        return tileRow <= 3 && tileCol >= 11 && tileCol <= 15;
    }

    /**
     * Verifica daca jucatorul a ajuns intr-o zona de tranzitie si,
     * daca da, incarca nivelul urmator si salveaza progresul.
     */
    private void checkLevelTransition() {
        // Nu facem tranzitie daca exista cooldown, player-ul lipseste sau harta nu e incarcata.
        if (transitionCooldown > 0 || player == null || map == null) {
            return;
        }

        // Folosim coordonatele picioarelor jucatorului pentru detectia tranzitiei.
        int px = (int) player.GetFeetCenterX();
        int py = (int) player.GetFeetBottomY();

        // Tranzitia din nivelul 1 in nivelul 2.
        if (currentLevel == 1) {
            if (map.isTransitionAtPixel(px, py)
                    || map.isTransitionAtPixel(px - 8, py)
                    || map.isTransitionAtPixel(px + 8, py)) {
                loadLevel(2);
                saveCurrentGame(false);
            }
        }

        // Tranzitia din nivelul 2 in nivelul 3.
        else if (currentLevel == 2) {
            if (map.isTransitionAtPixel(px, py)
                    || map.isTransitionAtPixel(px - 8, py)
                    || map.isTransitionAtPixel(px + 8, py)) {

                if (!talkedToDungeonWizard) {
                    dialogBox.show(
                            "The Sealed Passage",
                            "A cold force blocks your path.",
                            "You feel that someone in this dungeon still knows the way forward.",
                            "Find the imprisoned wizard before leaving for the village.",
                            "[ SPACE ] Continue"
                    );

                    transitionCooldown = 30;
                    return;
                }

                loadLevel(3);
                saveCurrentGame(false);
            }
        }

        // Tranzitia din nivelul 3 in nivelul 4.
        else if (currentLevel == 3) {
            if (map.isTransitionAtPixel(px, py)
                    || map.isTransitionAtPixel(px - 8, py)
                    || map.isTransitionAtPixel(px + 8, py)) {

                if (!villagePotionUsed) {
                    dialogBox.show(
                            "The Great Hall",
                            "A dark force pushes you back.",
                            "You remember the wizard's warning: you need the Moonroot Potion first.",
                            "[ SPACE ] Continue"
                    );
                    transitionCooldown = 30;
                    return;
                }

                loadLevel(4);
                saveCurrentGame(false);
            }
        }
    }
}

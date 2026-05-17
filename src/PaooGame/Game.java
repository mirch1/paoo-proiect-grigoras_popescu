package PaooGame;

import PaooGame.enemies.Enemy;
import PaooGame.enemies.Skeleton;
import PaooGame.enemies.Spider;
import PaooGame.enemies.EnemyFactory;
import PaooGame.Exceptions.DatabaseException;
import PaooGame.Exceptions.InvalidGameStateException;
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
    - gestioneaza salvarea (inclusiv in baza de date) si tranzitiile intre niveluri.
*/
public class Game implements Runnable {

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

    /// Inamic specific nivelului 1 (lup).
    private Enemy wolf;

    /// Inamic specific nivelului 2 (schelet).
    private Skeleton skeleton;

    /// Al doilea inamic specific nivelului 2 (paianjen).
    private Spider spider;

    /// Lista de NPC-uri din nivelul curent (garzi, sateni, garzi regale).
    private java.util.List<NPC> npcs = new java.util.ArrayList<>();

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

    /*! \brief Retine daca jocul este in stare de lupta.

        \details
        Este folosit pentru a schimba soundtrack-ul doar o singura data
        cand jucatorul intra sau iese din raza unui inamic.
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

    /// Stari anterioare ale tastelor pentru detectia apasarilor unice (edge detection).
    private boolean lastEscapeState = false;
    private boolean lastUpState     = false;
    private boolean lastDownState   = false;
    private boolean lastEnterState  = false;
    private boolean lastDebugState  = false;

    /*! \brief Starea anterioara a tastei SPACE.
        \details Folosita pentru edge detection in dialogBox (apasare unica).
    */
    private boolean lastSpaceState = false;

    /// Daca este true, jocul incearca sa incarce progresul salvat la initializare.
    private boolean loadSavedGame = false;

    /// Obiectul care retine starea citita din save file la Load Game.
    private SaveGameState savedGameState = null;

    /// Scorul acumulat in sesiunea curenta de joc.
    private int score = 0;

    /*! \brief Caseta de dialog pentru naratiunea jocului.
        \details Afiseaza texte la intrarea in niveluri sau la evenimente speciale.
                 Jocul este pauzat cat timp dialogul este activ.
    */
    private DialogBox dialogBox = new DialogBox();

    /*! \brief Retine daca dialogul de introducere al fiecarui nivel a fost afisat.
        \details Index 1 = nivel 1, 2 = nivel 2, 3 = nivel 3, 4 = nivel 4.
                 Previne reafisarea aceluiasi dialog la reintrarea in nivel (ex. TRY AGAIN).
    */
    private boolean[] levelDialogShown = new boolean[5];

    // =========================================================================
    //  CONSTRUCTORI
    // =========================================================================

    /*! \fn public Game(String title, int width, int height)
        \brief Constructor pentru joc nou (fara save).

        \param title  Titlul ferestrei.
        \param width  Latimea ferestrei.
        \param height Inaltimea ferestrei.
    */
    public Game(String title, int width, int height) {
        this(title, width, height, false);
    }

    /*! \fn public Game(String title, int width, int height, boolean loadSavedGame)
        \brief Constructor general al jocului.

        \param title         Titlul ferestrei.
        \param width         Latimea ferestrei.
        \param height        Inaltimea ferestrei.
        \param loadSavedGame Daca este true, jocul porneste dintr-o salvare existenta.
    */
    public Game(String title, int width, int height, boolean loadSavedGame) {
        wnd = new GameWindow(title, width, height);
        runState = false;
        this.loadSavedGame = loadSavedGame;
    }

    // =========================================================================
    //  INITIALIZARE
    // =========================================================================

    /*! \fn private void InitGame()
        \brief Initializeaza jocul si resursele de baza.

        \details
        Se construieste fereastra, se incarca asset-urile, se initializeaza
        inputul si camera, iar apoi se incarca fie salvarea existenta,
        fie primul nivel al jocului.

        La initializare, DatabaseManager este pornit implicit la prima
        salvare/incarcare de scor. Nu este nevoie de apel explicit aici.
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

        camera = new Camera(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        if (loadSavedGame) {
            savedGameState = SaveManager.loadGame();
            if (savedGameState != null) {
                loadLevel(savedGameState.getLevel());
                if (player != null) {
                    player.setPosition(savedGameState.getPlayerX(), savedGameState.getPlayerY());
                }

                /*
                 * Dupa ce nivelul a fost incarcat si inamicii au fost creati,
                 * restauram starea inamicilor invinsi din salvare.
                 */
                applyDefeatedEnemies(savedGameState);

                /// Restauram scorul din save — jucatorul continua de unde a ramas.
                score = savedGameState.getScore();

                if (camera != null && map != null && player != null) {
                    camera.CenterOnPlayer(player, map);
                }

                /*
                 * La Load Game, marcam dialogul nivelului incarcat ca deja afisat,
                 * pentru a nu-l afisa din nou la continuarea sesiunii salvate.
                 */
                if (savedGameState.getLevel() >= 1 && savedGameState.getLevel() <= 4) {
                    levelDialogShown[savedGameState.getLevel()] = true;
                }

            } else {
                /// Save-ul nu a putut fi citit — pornim un joc nou de la nivelul 1.
                currentLevel = 1;
                loadLevel(currentLevel);
            }
        } else {
            /// Joc nou — pornim intotdeauna de la nivelul 1.
            currentLevel = 1;
            loadLevel(currentLevel);
        }
    }

    // =========================================================================
    //  GAME LOOP
    // =========================================================================

    /*! \fn public void run()
        \brief Game-loop-ul principal.

        \details
        Ruleaza cat timp runState este activ si incearca sa mentina
        actualizarea si randarea la aproximativ 60 FPS.
        La finalul buclei, daca returnToMenuRequested este true,
        inchide fereastra de joc si redeschide meniul principal.
    */
    @Override
    public void run() {
        InitGame();

        long oldTime = System.nanoTime();
        long currentTime;
        final double timeFrame = 1_000_000_000.0 / 60.0;

        int  frames = 0;
        long timer  = System.currentTimeMillis();

        while (runState) {
            currentTime = System.nanoTime();
            if ((currentTime - oldTime) > timeFrame) {
                Update();
                if (!runState) break;
                Draw();
                oldTime = currentTime;
                frames++;
            }
            if (System.currentTimeMillis() - timer >= 1000) {
                currentFPS = frames;
                frames     = 0;
                timer     += 1000;
            }
        }

        /// La iesirea din bucla, daca s-a cerut revenirea la meniu, facem tranzitia.
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
        \brief Opreste jocul si asteapta terminarea thread-ului.
    */
    public synchronized void StopGame() {
        if (runState) {
            runState = false;
            if (Thread.currentThread() == gameThread) return;
            try {
                if (gameThread != null) gameThread.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    // =========================================================================
    //  SCOR
    // =========================================================================

    /*! \fn public void addScore(int points)
        \brief Adauga puncte la scorul curent si actualizeaza best score-ul in profil.

        \param points Numarul de puncte de adaugat.
    */
    public void addScore(int points) {
        score += points;

        /// Actualizam best score-ul in profilul activ daca scorul curent il depaseste.
        PlayerProfile profile = ProfileManager.getActive();
        if (profile != null) {
            profile.setBestScore(score);
        }
    }

    /*! \fn public int getScore()
        \brief Returneaza scorul curent al sesiunii.
        \return Scorul acumulat pana in acest moment.
    */
    public int getScore() { return score; }

    // =========================================================================
    //  AUDIO
    // =========================================================================

    /*! \fn private void updateLevelMusic()
        \brief Porneste soundtrack-ul potrivit pentru nivelul curent.

        \details
        Aceasta metoda este apelata cand se incarca un nivel nou.
        Muzica de battle nu este pornita aici, ci separat, in functie
        de apropierea jucatorului de inamici.
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
        \brief Verifica daca un inamic este suficient de aproape pentru battle music.

        \param enemy  Inamicul verificat.
        \param radius Raza de detectie in pixeli.
        \return true daca inamicul este viu si in raza jucatorului.
    */
    private boolean isEnemyCloseForBattle(Entity enemy, float radius) {
        if (player == null || enemy == null || enemy.isDead()) return false;

        float  dx       = player.GetFeetCenterX() - enemy.GetFeetCenterX();
        float  dy       = player.GetFeetBottomY() - enemy.GetFeetBottomY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        /*
         * Pornim muzica de battle fie daca inamicul este in raza,
         * fie daca hitbox-urile se intersecteaza deja (combat activ).
         */
        return distance <= radius || player.getFeetRect().intersects(enemy.getFeetRect());
    }

    /*! \fn private boolean shouldPlayBattleMusic()
        \brief Verifica daca in nivelul curent exista un inamic activ aproape de jucator.

        \details
        Verifica atat inamicii clasici (lup, schelet, paianjen),
        cat si NPC-urile ostile (gardienii regali din Great Hall).

        \return true daca trebuie pornita muzica de lupta.
    */
    private boolean shouldPlayBattleMusic() {
        if (isEnemyCloseForBattle(wolf,     220) ||
            isEnemyCloseForBattle(skeleton, 220) ||
            isEnemyCloseForBattle(spider,   220)) {
            return true;
        }

        /*
         * In Great Hall, cavalerii/gardienii sunt in lista npcs,
         * nu in variabilele wolf/skeleton/spider.
         * De aceea trebuie verificati separat.
         */
        for (NPC npc : npcs) {
            if (npc != null && npc.isGuardActive() && isEnemyCloseForBattle(npc, 220)) {
                return true;
            }
        }
        return false;
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

    // =========================================================================
    //  UPDATE
    // =========================================================================

    /*! \fn private void Update()
        \brief Actualizeaza starea jocului pentru un frame.

        \details
        Trateaza inputul, dialogul narativ, meniul de pauza, miscarea entitatilor,
        combat-ul, camera si tranzitiile intre niveluri.

        Ordinea de prioritate:
        1. Death screen (cel mai prioritar — blocheaza tot)
        2. DialogBox activ (blocheaza update-ul jocului, doar SPACE e procesat)
        3. Meniu pauza (blocheaza update entitatile)
        4. Update normal al jocului
    */
    private void Update() {
        keyManager.Update();

        /// 1. Ecranul de moarte are prioritate maxima.
        if (isDeathScreen) {
            updateDeathScreen();
            return;
        }

        /// 2. Cat timp un dialog narativ este activ, jocul este pauzat.
        ///    Doar tasta SPACE este procesata pentru a avansa/inchide dialogul.
        if (dialogBox.isActive()) {
            boolean currentSpaceState = keyManager.space;
            if (currentSpaceState && !lastSpaceState) {
                dialogBox.hide();
            }
            lastSpaceState = currentSpaceState;
            return;
        }

        /// Comutare mod debug cu tasta dedicata.
        if (keyManager.debug && !lastDebugState) {
            showHitboxes = !showHitboxes;
        }
        lastDebugState = keyManager.debug;

        /// Comutare pauza cu ESC.
        if (keyManager.escape && !lastEscapeState) {
            isPaused = !isPaused;
        }
        lastEscapeState = keyManager.escape;

        /// Daca jocul este in pauza, actualizam doar navigarea prin meniu.
        if (isPaused) {
            if (keyManager.up && !lastUpState) {
                pauseMenuSelection--;
                if (pauseMenuSelection < 0) pauseMenuSelection = 3;
            }
            if (keyManager.down && !lastDownState) {
                pauseMenuSelection++;
                if (pauseMenuSelection > 3) pauseMenuSelection = 0;
            }
            if (keyManager.enter && !lastEnterState) {
                executePauseMenuAction();
            }
            lastUpState    = keyManager.up;
            lastDownState  = keyManager.down;
            lastEnterState = keyManager.enter;
            return;
        }

        /// Actualizare entitati.
        if (player != null && map != null) player.Update(keyManager, map);
        if (wolf     != null && map != null) wolf.Update(map);
        if (skeleton != null && map != null) skeleton.Update(map);
        if (spider   != null && map != null) spider.Update(map);
        if (map != null) {
            for (NPC npc : npcs) npc.Update(map);
        }

        /// Camera urmareste jucatorul activ.
        if (camera != null && player != null && map != null) {
            camera.CenterOnPlayer(player, map);
        }

        /// Scadem cooldown-ul pentru schimbarea de nivel.
        if (transitionCooldown > 0) transitionCooldown--;

        /// Detectam tentativele de intrare ilegala pe harta (Bad Entrance).
        if (isBadEntranceAttempt()) {
            openDeathScreen("BAD ENTRANCE");
            return;
        }

        /// Verificam tranzitiile si combat-ul.
        checkLevelTransition();
        if (player != null) checkCombat();

        /*
         * Actualizam soundtrack-ul de battle dupa combat.
         * Daca jucatorul este aproape de un inamic viu, pornim muzica de lupta.
         * Daca se indeparteaza sau inamicul moare, revenim la tema nivelului.
         */
        updateBattleMusic();

        lastUpState    = keyManager.up;
        lastDownState  = keyManager.down;
        lastEnterState = keyManager.enter;
    }

    // =========================================================================
    //  DIALOG NARATIV
    // =========================================================================

    /*! \fn private void showLevelDialog(int level)
        \brief Afiseaza dialogul narativ de introducere pentru nivelul specificat.

        \details
        Dialogul este afisat o singura data per sesiune de joc pentru fiecare nivel.
        La Load Game, nivelul restaurat isi marcheaza automat dialogul ca afisat,
        astfel incat sa nu deranjeze jucatorul care continua o sesiune salvata.
        Jocul este pauzat automat cat timp dialogul este activ (tratat in Update()).

        \param level Nivelul pentru care se afiseaza dialogul (1-4).
    */
    private void showLevelDialog(int level) {
        if (level < 1 || level > 4) return;
        if (levelDialogShown[level]) return;

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
        }
    }

    // =========================================================================
    //  COMBAT
    // =========================================================================

    /*! \fn private void checkCombat()
        \brief Verifica interactiunile de lupta dintre jucator si inamici.

        \details
        Damage-ul este ajustat in functie de dificultatea aleasa in Settings.
        Pe EASY, playerul loveste mai tare si primeste mai putin damage.
        Pe HARD, playerul loveste mai slab si primeste mai mult damage.

        Scorul este acordat o singura data per inamic (flag isScoreAwarded).
    */
    private void checkCombat() {
        if (player == null || player.isDead()) return;

        Rectangle playerAtk  = player.getAttackHitbox();
        Rectangle playerFeet = player.getFeetRect();

        int playerAttackDamage   = GameSettings.getPlayerDamage(Player.ATTACK_DAMAGE);
        int wolfAttackDamage     = GameSettings.getEnemyDamage(Enemy.ATTACK_DAMAGE);
        int skeletonAttackDamage = GameSettings.getEnemyDamage(Skeleton.ATTACK_DAMAGE);
        int spiderWebDamage      = GameSettings.getEnemyDamage(Spider.WEB_DAMAGE);
        int guardAttackDamage    = GameSettings.getEnemyDamage(NPC.GUARD_ATTACK_DAMAGE);

        /// Jucatorul ataca inamicii cu hitbox-ul de atac.
        if (playerAtk != null) {
            if (wolf     != null && !wolf.isDead()     && playerAtk.intersects(wolf.getFeetRect()))
                wolf.takeDamage(playerAttackDamage);
            if (skeleton != null && !skeleton.isDead() && playerAtk.intersects(skeleton.getFeetRect()))
                skeleton.takeDamage(playerAttackDamage);
            if (spider   != null && !spider.isDead()   && playerAtk.intersects(spider.getFeetRect()))
                spider.takeDamage(playerAttackDamage);
            for (NPC npc : npcs) {
                if (npc != null && npc.canBeAttacked() && playerAtk.intersects(npc.getFeetRect()))
                    npc.takeDamage(playerAttackDamage);
            }
        }

        /// Inamicii lovesc jucatorul prin contact direct (coliziune picioare).
        if (wolf     != null && !wolf.isDead()     && playerFeet.intersects(wolf.getFeetRect()))
            player.takeDamage(wolfAttackDamage);
        if (skeleton != null && !skeleton.isDead() && playerFeet.intersects(skeleton.getFeetRect()))
            player.takeDamage(skeletonAttackDamage);

        /// Paianjenul loveste prin proiectil (panza).
        if (spider != null && !spider.isDead() && spider.isWebActive()) {
            float wx = spider.getWebX(), wy = spider.getWebY();
            float px = player.GetFeetCenterX(), py = player.GetFeetBottomY();
            double dist = Math.sqrt((wx - px) * (wx - px) + (wy - py) * (wy - py));
            if (dist < 16) {
                player.takeDamage(spiderWebDamage);
                spider.deactivateWeb();
            }
        }

        /// Gardienii NPC lovesc jucatorul daca sunt in raza de atac.
        for (NPC npc : npcs) {
            if (npc != null && npc.canDamagePlayer(player))
                player.takeDamage(guardAttackDamage);
        }

        /// Acorda scor o singura data la moartea fiecarui inamic.
        if (wolf     != null && wolf.isDead()     && !wolf.isScoreAwarded()) {
            addScore(GameSettings.SCORE_WOLF_KILLED);
            wolf.setScoreAwarded(true);
        }
        if (skeleton != null && skeleton.isDead() && !skeleton.isScoreAwarded()) {
            addScore(GameSettings.SCORE_SKELETON_KILLED);
            skeleton.setScoreAwarded(true);
        }
        if (spider   != null && spider.isDead()   && !spider.isScoreAwarded()) {
            addScore(GameSettings.SCORE_SPIDER_KILLED);
            spider.setScoreAwarded(true);
        }

        /// Acorda scor pentru NPC-urile ostile eliminate (gardienii regali din Great Hall).
        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (npc != null && npc.isDefeated() && !npc.isScoreAwarded()) {
                addScore(GameSettings.SCORE_GUARDIAN_KILLED);
                npc.setScoreAwarded(true);
            }
        }

        /// Daca jucatorul moare, afisam ecranul de retry.
        if (player.isDead()) {
            openDeathScreen();
        }
    }

    // =========================================================================
    //  ECRAN DE MOARTE
    // =========================================================================

    /*! \fn private void openDeathScreen()
        \brief Deschide ecranul de moarte fara subtitlu.
    */
    private void openDeathScreen() {
        openDeathScreen("");
    }

    /*! \fn private void openDeathScreen(String subtitle)
        \brief Deschide ecranul de moarte cu subtitlul specificat.

        \param subtitle Textul afisat sub "YOU DIED" (ex. "BAD ENTRANCE").
    */
    private void openDeathScreen(String subtitle) {
        deathLevel         = currentLevel;
        deathMenuSelection = 0;
        deathSubtitle      = subtitle;
        isDeathScreen      = true;
        isPaused           = false;

        AudioManager.getInstance().stopMusic();
        inBattle = false;

        if (keyManager != null) keyManager.Clear();
        lastUpState     = false;
        lastDownState   = false;
        lastEnterState  = false;
        lastEscapeState = false;
        lastSpaceState  = false;
    }

    /*! \fn private void updateDeathScreen()
        \brief Gestioneaza navigarea in meniul afisat dupa moarte.

        \details
        Navigare cu sus/jos, confirmare cu Enter.

        FIX: "score = 0" a fost ELIMINAT din aceasta metoda.
        Anterior era plasat in afara if-ului de Enter, deci se executa la
        FIECARE FRAME cat timp ecranul de moarte era activ, resetand scorul
        la 0 inainte de orice actiune a jucatorului.
        Acum scorul este resetat EXCLUSIV in executeDeathMenuAction(),
        doar la selectia TRY AGAIN (deathMenuSelection == 0).
    */
    private void updateDeathScreen() {
        if (keyManager.up && !lastUpState) {
            deathMenuSelection--;
            if (deathMenuSelection < 0) deathMenuSelection = 1;
        }
        if (keyManager.down && !lastDownState) {
            deathMenuSelection++;
            if (deathMenuSelection > 1) deathMenuSelection = 0;
        }
        if (keyManager.enter && !lastEnterState) {
            executeDeathMenuAction();
        }
        lastUpState    = keyManager.up;
        lastDownState  = keyManager.down;
        lastEnterState = keyManager.enter;
    }

    /*! \fn private void executeDeathMenuAction()
        \brief Executa actiunea selectata in ecranul de moarte.

        \details
        TRY AGAIN (deathMenuSelection == 0):
        - Reseteaza scorul la 0 — sesiunea noua incepe curat.
          FIX: score = 0 este acum AICI, nu in updateDeathScreen() unde
          era executat la fiecare frame, corupand scorul inainte de orice save.
        - Reinitializeaza jucatorul si reincarca nivelul in care a murit.
        - Reseteaza flagul de dialog al nivelului pentru a reafisa introducerea.

        EXIT GAME (deathMenuSelection == 1):
        - Opreste muzica, inchide conexiunea DB corect, iese din aplicatie.
    */
    private void executeDeathMenuAction() {
        if (deathMenuSelection == 0) {
            /// TRY AGAIN — resetam scorul: noua sesiune incepe de la 0.
            score = 0;
            isDeathScreen = false;

            /*
             * Resetam dialogul nivelului pentru ca jucatorul sa il revada
             * la TRY AGAIN — face experienta mai imersiva la retry.
             * Comenteaza linia de mai jos daca preferi sa NU se reafiseze.
             */
            if (deathLevel >= 1 && deathLevel <= 4) {
                levelDialogShown[deathLevel] = false;
            }

            player = new Player(0, 0);
            loadLevel(deathLevel);
            if (keyManager != null) {
                keyManager.Clear();
                lastUpState    = false;
                lastDownState  = false;
                lastEnterState = false;
                lastSpaceState = false;
            }
        } else {
            /// EXIT GAME — oprim muzica, inchidem DB corect, iesim.
            AudioManager.getInstance().stopMusic();
            try { DatabaseManager.getInstance().closeConnection(); } catch (Exception ignored) {}
            System.exit(0);
        }
    }

    // =========================================================================
    //  MENIU PAUZA
    // =========================================================================

    /*! \fn private void executePauseMenuAction()
        \brief Executa actiunea selectata in meniul de pauza.

        \details
        Optiunile sunt: SETTINGS (0), SAVE GAME (1), RETURN TO MENU (2), EXIT (3).
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
                try { DatabaseManager.getInstance().closeConnection(); } catch (Exception ignored) {}
                System.exit(0);
            }
        }
    }

    // =========================================================================
    //  SALVARE
    // =========================================================================

    /*! \fn private void appendDefeatedId(StringBuilder builder, String id)
        \brief Adauga un ID de inamic invins in lista salvata.

        \param builder StringBuilder-ul in care se construieste lista.
        \param id      ID-ul inamicului invins (ex. "wolf", "npc0").
    */
    private void appendDefeatedId(StringBuilder builder, String id) {
        if (builder.length() > 0) builder.append(",");
        builder.append(id);
    }

    /*! \fn private String getDefeatedEnemiesForSave()
        \brief Construieste lista inamicilor invinsi din nivelul curent.

        \details
        Lista este salvata in fisierul .properties.
        Exemple: "wolf", "skeleton", "spider", "npc0,npc1".

        \return Lista de ID-uri separate prin virgula.
    */
    private String getDefeatedEnemiesForSave() {
        StringBuilder defeated = new StringBuilder();

        if (wolf     != null && wolf.isDead())     appendDefeatedId(defeated, "wolf");
        if (skeleton != null && skeleton.isDead()) appendDefeatedId(defeated, "skeleton");
        if (spider   != null && spider.isDead())   appendDefeatedId(defeated, "spider");

        /*
         * NPC-urile sunt salvate dupa pozitia lor in lista.
         * In Great Hall, npc0, npc1, npc2, npc3 sunt gardienii regali.
         */
        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (npc != null && npc.isDefeated()) appendDefeatedId(defeated, "npc" + i);
        }

        return defeated.toString();
    }

    /*! \fn private void applyDefeatedEnemies(SaveGameState state)
        \brief Restaureaza starea inamicilor invinsi dupa Load Game.

        \details
        Dupa incarcarea nivelului, inamicii sunt creati din nou.
        Aceasta metoda ii marcheaza drept morti/invinsi pe cei care existau
        in lista salvata, astfel incat nu mai apar pe harta.

        \param state Starea incarcata din save file.
    */
    private void applyDefeatedEnemies(SaveGameState state) {
        if (state == null) return;

        if (wolf     != null && state.isEnemyDefeated("wolf"))     wolf.forceDead();
        if (skeleton != null && state.isEnemyDefeated("skeleton")) skeleton.forceDead();
        if (spider   != null && state.isEnemyDefeated("spider"))   spider.forceDead();

        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (npc != null && state.isEnemyDefeated("npc" + i)) npc.forceDefeatedState();
        }
    }

    /*! \fn private void saveCurrentGame(boolean showMessage)
        \brief Salveaza progresul curent in fisierul .properties SI in baza de date.

        \details
        Salveaza nivelul, pozitia, inamicii invinsi si scorul in SaveManager.
        Simultan, insereaza scorul curent in baza de date SQLite prin
        DatabaseManager, pentru a aparea in leaderboard.
        Eroarea de DB este tratata non-fatal: jocul continua chiar daca
        baza de date nu este disponibila.

        \param showMessage Daca este true, afiseaza mesaj de confirmare Swing.
    */
    private void saveCurrentGame(boolean showMessage) {
        try {
            if (player == null) {
                throw new InvalidGameStateException(
                    "Nu se poate salva jocul: playerul nu exista."
                );
            }

            /// Salvam in fisierul .properties (save clasic).
            SaveManager.saveGame(
                currentLevel,
                player.GetX(),
                player.GetY(),
                getDefeatedEnemiesForSave(),
                score
            );

            /// Salvam scorul si in baza de date SQLite pentru leaderboard.
            try {
                PlayerProfile profile = ProfileManager.getActive();
                String name = (profile != null) ? profile.getName() : "Anonim";
                DatabaseManager.getInstance().insertScore(name, score, currentLevel);
                System.out.println("DB: scor salvat — " + name + " | " + score + " pts | nivel " + currentLevel);
            } catch (DatabaseException dbEx) {
                /// Eroare non-fatala: jocul continua chiar daca DB nu e disponibil.
                System.out.println("DB: eroare la salvarea scorului — " + dbEx.getMessage());
            }

            /// Redam un efect sonor scurt pentru confirmare vizuala a salvarii.
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
         * Oprim muzica nivelului/battle inainte de revenirea la meniu.
         * MenuWindow va porni din nou menu_theme.wav in constructor.
         */
        AudioManager.getInstance().stopMusic();
        returnToMenuRequested = true;
        runState = false;
    }

    // =========================================================================
    //  DRAW
    // =========================================================================

    /*! \fn private void drawCinematicOverlay(Graphics2D g2d)
        \brief Deseneaza un efect vizual cinematic peste joc.

        \details
        Activat din Settings prin GameSettings.cinematicMode.
        Nu afecteaza logica jocului, doar atmosfera vizuala.
        Adauga doua benzi negre (sus si jos) si o umbra subtila.
    */
    private void drawCinematicOverlay(Graphics2D g2d) {
        /// Umbra subtila peste scena pentru adancime vizuala.
        g2d.setColor(new Color(0, 0, 0, 35));
        g2d.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        /// Benzi cinematice sus si jos (stilul letterbox).
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0,                  LOGICALWIDTH, 42);
        g2d.fillRect(0, LOGICALHEIGHT - 42, LOGICALWIDTH, 42);
    }

    /*! \fn private void Draw()
        \brief Randeaza cadrul curent al jocului.

        \details
        Ordinea de desenare este:
        1. harta de baza,
        2. inamicii,
        3. jucatorul,
        4. foreground-ul hartii,
        5. barele HP ale inamicilor,
        6. meniul de pauza,
        7. overlay-ul de debug,
        8. HUD-ul jucatorului (scor + bara HP),
        9. ecranul de moarte (daca e activ),
        10. dialogul narativ (ultimul — mereu deasupra tuturor).

        HUD-ul si dialogul sunt desenate ultimele pentru a ramane
        mereu vizibile deasupra hartii si elementelor de foreground.
    */
    private void Draw() {
        if (wnd == null || wnd.GetCanvas() == null || !wnd.GetCanvas().isDisplayable()) return;

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

        /// Scalare din rezolutia logica (800x600) in rezolutia reala a ferestrei.
        double scaleX = (double) wnd.GetWndWidth()  / LOGICALWIDTH;
        double scaleY = (double) wnd.GetWndHeight() / LOGICALHEIGHT;
        g2d.scale(scaleX, scaleY);

        /// Curatam fundalul cu o culoare inchisa.
        g.setColor(new Color(10, 15, 10));
        g.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        /// Offset pentru centrare atunci cand harta este mai mica decat viewport-ul logic.
        int offsetX = 0, offsetY = 0;
        if (map != null) {
            if (map.getPixelWidth()  < LOGICALWIDTH)  offsetX = (LOGICALWIDTH  - map.getPixelWidth())  / 2;
            if (map.getPixelHeight() < LOGICALHEIGHT) offsetY = (LOGICALHEIGHT - map.getPixelHeight()) / 2;
        }

        /// 1. Harta de baza.
        if (map != null && camera != null)
            map.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /// 2. Inamicii.
        if (wolf     != null && camera != null) wolf.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (skeleton != null && camera != null) skeleton.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (spider   != null && camera != null) spider.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        if (camera != null) {
            for (NPC npc : npcs) npc.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 3. Jucatorul.
        if (player != null && camera != null)
            player.Draw(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /// 4. Foreground-ul hartii (copaci, acoperisuri etc.) — desenat peste personaje.
        if (map != null && camera != null)
            map.DrawForeground(g, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);

        /// Efect vizual cinematic (benzi + umbra). Nu se afiseaza peste debug.
        if (GameSettings.cinematicMode && !Game.showHitboxes) {
            drawCinematicOverlay(g2d);
        }

        /// 5. Barele de viata ale inamicilor, desenate peste foreground pentru vizibilitate.
        if (camera != null) {
            if (wolf     != null) wolf.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (skeleton != null) skeleton.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
            if (spider   != null) spider.drawHealthBarOnly(g2d, (int) camera.GetX(), (int) camera.GetY(), offsetX, offsetY);
        }

        /// 6. Meniul de pauza (overlay negru semitransparent + optiuni).
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

            String[] options   = {"SETTINGS", "SAVE GAME", "RETURN TO MENU", "EXIT"};
            Font     pauseFont = new Font("Serif", Font.BOLD, 36);
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

        /// 7. Overlay debug: FPS + mesaj "Mod Debug: ON".
        if (showHitboxes) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString("FPS: " + currentFPS, 15, 30);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2d.drawString("Mod Debug: ON", 15, 50);
        }

        /// 8. HUD-ul jucatorului: bara HP + scor curent (desenat ultimul — mereu vizibil).
        if (player != null && !player.isDead()) {
            int   hudX  = 20;
            int   hudY  = LOGICALHEIGHT - 30;
            int   hudW  = 150;
            int   hudH  = 12;
            float ratio = (float) player.getCurrentHp() / player.getMaxHp();

            /// Fundal bara HP.
            g2d.setColor(new Color(20, 20, 20, 200));
            g2d.fillRoundRect(hudX - 2, hudY - 2, hudW + 4, hudH + 4, 6, 6);

            /// Fundal rosu inchis (hp pierdut).
            g2d.setColor(new Color(100, 10, 10, 220));
            g2d.fillRoundRect(hudX, hudY, hudW, hudH, 4, 4);

            /// Bara HP colorata: verde > 60%, galben > 30%, rosu <= 30%.
            Color hudColor = ratio > 0.6f
                ? new Color(50,  200, 50)
                : ratio > 0.3f
                    ? new Color(220, 190, 20)
                    : new Color(210, 40,  40);

            g2d.setColor(hudColor);
            g2d.fillRoundRect(hudX, hudY, (int) (hudW * ratio), hudH, 4, 4);

            /// Text HP: "HP: X / Y" la dreapta barei.
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 11));
            g2d.drawString("HP: " + player.getCurrentHp() + " / " + player.getMaxHp(),
                hudX + hudW + 8, hudY + 10);
        }

        /// Scorul curent in coltul dreapta-sus al ecranului.
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 14));
        g2d.drawString("SCORE: " + score, LOGICALWIDTH - 155, 25);

        /// 9. Ecranul de moarte (YOU DIED) + scorul final al sesiunii terminate.
        if (isDeathScreen) {
            drawDeathScreen(g2d);

            Font scoreFont = new Font("Serif", Font.BOLD, 28);
            g2d.setFont(scoreFont);
            g2d.setColor(new Color(180, 180, 190));
            String scoreText = "SCOR FINAL: " + score;
            FontMetrics sm = g2d.getFontMetrics(scoreFont);
            g2d.drawString(scoreText, (LOGICALWIDTH - sm.stringWidth(scoreText)) / 2, 285);
        }

        /// 10. Dialogul narativ — desenat ultimul, mereu deasupra tuturor elementelor.
        dialogBox.draw(g2d, LOGICALWIDTH, LOGICALHEIGHT);

        if (runState && wnd.GetCanvas().isDisplayable()) bs.show();
        g.dispose();
    }

    /*! \fn private void drawDeathScreen(Graphics2D g2d)
        \brief Deseneaza ecranul negru de moarte cu optiunile TRY AGAIN / EXIT GAME.

        \param g2d Contextul grafic 2D curent.
    */
    private void drawDeathScreen(Graphics2D g2d) {
        /// Fundal negru complet.
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, LOGICALWIDTH, LOGICALHEIGHT);

        /// Titlul "YOU DIED" cu drop shadow rosu.
        String title     = "YOU DIED";
        Font   titleFont = new Font("Serif", Font.BOLD, 72);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
        int titleX = (LOGICALWIDTH - titleMetrics.stringWidth(title)) / 2;
        int titleY = 210;

        g2d.setColor(new Color(120, 0, 0));
        g2d.drawString(title, titleX + 3, titleY + 3);
        g2d.setColor(new Color(210, 35, 35));
        g2d.drawString(title, titleX, titleY);

        int startY = 330;

        /// Subtitlul optional (ex. "BAD ENTRANCE") in auriu sub titlu.
        if (deathSubtitle != null && !deathSubtitle.isEmpty()) {
            Font        subFont    = new Font("Serif", Font.BOLD, 34);
            g2d.setFont(subFont);
            FontMetrics subMetrics = g2d.getFontMetrics(subFont);
            int subX = (LOGICALWIDTH - subMetrics.stringWidth(deathSubtitle)) / 2;
            int subY = titleY + 52;
            g2d.setColor(new Color(218, 165, 32));
            g2d.drawString(deathSubtitle, subX, subY);
            startY = 370;
        }

        /// Optiunile meniului de moarte: TRY AGAIN / EXIT GAME.
        String[] options    = {"TRY AGAIN", "EXIT GAME"};
        Font     optionFont = new Font("Serif", Font.BOLD, 34);
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

    /*! \fn private void setPlayerSpawn(int tileCol, int tileRow)
        \brief Pozitioneaza jucatorul pe harta pe baza coordonatelor de tile.

        \param tileCol Coloana tile-ului de spawn.
        \param tileRow Linia tile-ului de spawn.
    */
    private void setPlayerSpawn(int tileCol, int tileRow) {
        if (player == null) return;
        float spawnX = tileCol * Tile.TILE_WIDTH  + Tile.TILE_WIDTH  / 2.0f - player.GetWidth()  / 2.0f;
        float spawnY = tileRow * Tile.TILE_HEIGHT + Tile.TILE_HEIGHT / 2.0f - player.GetHeight() / 2.0f;
        player.setPosition(spawnX, spawnY);
        if (camera != null && map != null) camera.CenterOnPlayer(player, map);
        if (keyManager != null) keyManager.Clear();
    }

    /*! \fn private void loadLevel(int level)
        \brief Incarca nivelul specificat si fisierele sale vizuale/logice.

        \details
        Reinitializeaza toti inamicii si NPC-urile pentru nivelul nou.
        Porneste muzica potrivita prin updateLevelMusic().
        Afiseaza dialogul narativ de introducere al nivelului (o singura data per sesiune).
        Scorul NU este resetat aici — resetul se face exclusiv in
        executeDeathMenuAction() la TRY AGAIN.

        \param level Nivelul care trebuie incarcat (1-4).
    */
    private void loadLevel(int level) {
        currentLevel = level;
        wolf         = null;
        skeleton     = null;
        spider       = null;
        npcs.clear();

        if (player == null) player = new Player(0, 0);

        if (level == 1) {
            map = new Map(
                "res/maps/level1_base.png",
                "res/maps/level1_foreground.png",
                "res/maps/harta_primul_nivel.tmx"
            );
            setPlayerSpawn(10, 14);
            wolf = EnemyFactory.createWolf(15 * Tile.TILE_WIDTH, 14 * Tile.TILE_HEIGHT, player);
            /// Garzi la poarta satului in nivelul 1.
            npcs.add(new NPC(11 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD,   "/textures/npc_village_gate_guard_spear_small.png"));
            npcs.add(new NPC(15 * Tile.TILE_WIDTH, 2 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD,   "/textures/npc_village_gate_guard_shield_small.png"));

        } else if (level == 2) {
            map = new Map(
                "res/maps/level2_base.png",
                "res/maps/level2_foreground.png",
                "res/maps/harta_nivel2_dungeon.tmx"
            );
            setPlayerSpawn(19, 24);
            skeleton = EnemyFactory.createSkeleton(11 * Tile.TILE_WIDTH, 13 * Tile.TILE_HEIGHT, player);
            spider   = EnemyFactory.createSpider  (29 * Tile.TILE_WIDTH, 19 * Tile.TILE_HEIGHT, player);

        } else if (level == 3) {
            map = new Map(
                "res/maps/level3_village_base.png",
                "res/maps/level3_village_foreground.png",
                "res/maps/harta_nivel3_village.tmx"
            );
            setPlayerSpawn(24, 31);
            /// Garzi la poarta castelului.
            npcs.add(new NPC(20 * Tile.TILE_WIDTH,  7 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD,    "/textures/npc_village_gate_guard_spear_small.png"));
            npcs.add(new NPC(28 * Tile.TILE_WIDTH,  7 * Tile.TILE_HEIGHT, player, NPC.NPCType.GUARD,    "/textures/npc_village_gate_guard_shield_small.png"));
            /// Sateni prin sat.
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
            /// Patru garzi regale in Great Hall.
            npcs.add(new NPC( 6 * Tile.TILE_WIDTH, 10 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(13 * Tile.TILE_WIDTH, 10 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC( 7 * Tile.TILE_WIDTH, 12 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
            npcs.add(new NPC(12 * Tile.TILE_WIDTH, 12 * Tile.TILE_HEIGHT, player, NPC.NPCType.ROYAL_GUARD, "/textures/npc_great_hall_royal_guard_small.png"));
        }

        transitionCooldown = 30;

        /*
         * Dupa incarcarea nivelului, pornim muzica potrivita.
         * Forest -> Dungeon -> Village -> Great Hall: soundtrack-ul se schimba automat.
         */
        updateLevelMusic();

        /*
         * Afisam dialogul narativ de introducere al nivelului.
         * showLevelDialog() verifica intern daca dialogul a mai fost afisat
         * in aceasta sesiune si il ignora daca da.
         */
        showLevelDialog(level);
    }

    /*! \fn private boolean isBadEntranceAttempt()
        \brief Detecteaza tentativele de intrare ilegala in zona restricionata de pe nivelul 1.

        \return true daca jucatorul incearca sa treaca prin zona interzisa.
    */
    private boolean isBadEntranceAttempt() {
        if (currentLevel != 1 || player == null) return false;
        int tileCol = (int) (player.GetFeetCenterX() / Tile.TILE_WIDTH);
        int tileRow = (int) (player.GetFeetBottomY() / Tile.TILE_HEIGHT);
        return tileRow <= 3 && tileCol >= 11 && tileCol <= 15;
    }

    /*! \fn private void checkLevelTransition()
        \brief Verifica daca jucatorul a ajuns intr-o zona de tranzitie si incarca nivelul urmator.

        \details
        La fiecare tranzitie, progresul este salvat automat (fara mesaj de confirmare).
        Zona de tranzitie este definita prin tile-uri speciale in fisierul TMX al hartii.
        Scorul este pastrat intre niveluri — nu se reseteaza la tranzitie.
    */
    private void checkLevelTransition() {
        if (transitionCooldown > 0 || player == null || map == null) return;

        int px = (int) player.GetFeetCenterX();
        int py = (int) player.GetFeetBottomY();

        if (currentLevel == 1) {
            if (map.isTransitionAtPixel(px, py) ||
                map.isTransitionAtPixel(px - 8, py) ||
                map.isTransitionAtPixel(px + 8, py)) {
                loadLevel(2);
                saveCurrentGame(false);
            }
        } else if (currentLevel == 2) {
            if (map.isTransitionAtPixel(px, py) ||
                map.isTransitionAtPixel(px - 8, py) ||
                map.isTransitionAtPixel(px + 8, py)) {
                loadLevel(3);
                saveCurrentGame(false);
            }
        } else if (currentLevel == 3) {
            if (map.isTransitionAtPixel(px, py) ||
                map.isTransitionAtPixel(px - 8, py) ||
                map.isTransitionAtPixel(px + 8, py)) {
                loadLevel(4);
                saveCurrentGame(false);
            }
        }
    }
}

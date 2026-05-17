package PaooGame;

/*! \class GameSettings
    \brief Retine setarile globale ale jocului.

    \details
    Momentan setarile sunt tinute in memorie.
    Ulterior, valorile pot fi salvate in baza de date SQLite.
 */
public class GameSettings {
    public static boolean musicEnabled  = true;
    public static boolean sfxEnabled    = true;
    public static boolean cinematicMode = true;

    /*! \brief Volumul muzicii de fundal (0 = mut, 100 = maxim). */
    public static int musicVolume = 100;

    /*! \brief Volumul efectelor sonore (0 = mut, 100 = maxim). */
    public static int sfxVolume = 100;

    /*! \brief Dificultatea jocului: EASY, NORMAL sau HARD. */
    public static String difficulty = "NORMAL";

    // =========================================================================
    //  CONSTANTE SCOR
    // =========================================================================

    /*! \brief Puncte acordate la eliminarea unui Lup (Nivel 1). */
    public static final int SCORE_WOLF_KILLED     = 100;

    /*! \brief Puncte acordate la eliminarea unui Schelet (Nivel 2). */
    public static final int SCORE_SKELETON_KILLED = 150;

    /*! \brief Puncte acordate la eliminarea unui Paianjen (Nivel 2). */
    public static final int SCORE_SPIDER_KILLED   = 200;

    /*! \brief Puncte acordate la eliminarea Gardianului Royal (Nivel 4). */
    public static final int SCORE_GUARDIAN_KILLED = 300;

    /*! \brief Puncte acordate la trecerea intr-un nivel nou. */
    public static final int SCORE_LEVEL_COMPLETE  = 300;

    /*! \brief Puncte acordate la finalizarea completa a jocului. */
    public static final int SCORE_GAME_COMPLETE   = 500;

    // =========================================================================
    //  METODE
    // =========================================================================

    /*! \fn public static void resetToDefault()
        \brief Reseteaza setarile la valorile implicite.
     */
    public static void resetToDefault() {
        musicEnabled  = true;
        sfxEnabled    = true;
        cinematicMode = true;
        difficulty    = "NORMAL";
        Game.showHitboxes = false;
    }

    /*! \fn public static int getPlayerDamage(int baseDamage)
        \brief Returneaza damage-ul jucatorului in functie de dificultate.

        \details
        Pe EASY jucatorul loveste putin mai tare.
        Pe HARD jucatorul loveste putin mai slab.

        \param baseDamage Damage-ul de baza al jucatorului.
        \return Damage-ul ajustat conform dificultatii.
     */
    public static int getPlayerDamage(int baseDamage) {
        float multiplier;
        switch (difficulty) {
            case "EASY":  multiplier = 1.25f; break;
            case "HARD":  multiplier = 0.85f; break;
            default:      multiplier = 1.0f;  break;
        }
        return Math.max(1, Math.round(baseDamage * multiplier));
    }

    /*! \fn public static int getEnemyDamage(int baseDamage)
        \brief Returneaza damage-ul inamicilor in functie de dificultate.

        \details
        Pe EASY inamicii dau mai putin damage.
        Pe HARD inamicii dau mai mult damage.

        \param baseDamage Damage-ul de baza al inamicului.
        \return Damage-ul ajustat conform dificultatii.
     */
    public static int getEnemyDamage(int baseDamage) {
        float multiplier;
        switch (difficulty) {
            case "EASY":  multiplier = 0.70f; break;
            case "HARD":  multiplier = 1.35f; break;
            default:      multiplier = 1.0f;  break;
        }
        return Math.max(1, Math.round(baseDamage * multiplier));
    }
}
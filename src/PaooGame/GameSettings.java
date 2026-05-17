package PaooGame;

/*! \class GameSettings
    \brief Retine setarile globale ale jocului.

    \details
    Momentan setarile sunt tinute in memorie.
    Ulterior, colegul poate salva aceste valori in baza de date.
 */
public class GameSettings {
    public static boolean musicEnabled = true;
    public static boolean sfxEnabled = true;
    public static boolean cinematicMode = true;

    /*! \brief Volumul muzicii de fundal (0 = mut, 100 = maxim). */
    public static int musicVolume = 100;

    /*! \brief Volumul efectelor sonore (0 = mut, 100 = maxim). */
    public static int sfxVolume = 100;

    /*! \brief Dificultatea jocului: EASY, NORMAL sau HARD. */
    public static String difficulty = "NORMAL";

    /*! \fn public static void resetToDefault()
        \brief Reseteaza setarile la valorile implicite.
     */
    public static void resetToDefault() {
        musicEnabled = true;
        sfxEnabled = true;
        cinematicMode = true;
        difficulty = "NORMAL";

        Game.showHitboxes = false;
    }

    /*! \fn public static int getPlayerDamage(int baseDamage)
        \brief Returneaza damage-ul jucatorului in functie de dificultate.

        \details
        Pe EASY jucatorul loveste putin mai tare.
        Pe HARD jucatorul loveste putin mai slab.
     */
    public static int getPlayerDamage(int baseDamage) {
        float multiplier;

        switch (difficulty) {
            case "EASY":
                multiplier = 1.25f;
                break;

            case "HARD":
                multiplier = 0.85f;
                break;

            case "NORMAL":
            default:
                multiplier = 1.0f;
                break;
        }

        return Math.max(1, Math.round(baseDamage * multiplier));
    }

    /*! \fn public static int getEnemyDamage(int baseDamage)
        \brief Returneaza damage-ul inamicilor in functie de dificultate.

        \details
        Pe EASY inamicii dau mai putin damage.
        Pe HARD inamicii dau mai mult damage.
     */
    public static int getEnemyDamage(int baseDamage) {
        float multiplier;

        switch (difficulty) {
            case "EASY":
                multiplier = 0.70f;
                break;

            case "HARD":
                multiplier = 1.35f;
                break;

            case "NORMAL":
            default:
                multiplier = 1.0f;
                break;
        }

        return Math.max(1, Math.round(baseDamage * multiplier));
    }
}

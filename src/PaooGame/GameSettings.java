package PaooGame;

/**
 *  setarile globale ale jocului.
 * Momentan sunt salvate doar in memorie, dar ulterior pot fi salvate in baza de date.
 */
public class GameSettings {
    public static boolean musicEnabled = true;
    public static boolean sfxEnabled = true;
    public static boolean cinematicMode = true;

    /*! \brief Volumul muzicii de fundal (0.0 = mut, 1.0 = maxim). */
    public static int musicVolume = 100;

    /*! \brief Volumul efectelor sonore (0.0 = mut, 1.0 = maxim). */
    public static int sfxVolume   = 100;

    public static String difficulty = "NORMAL";

    public static void resetToDefault() {
        musicEnabled = true;
        sfxEnabled = true;
        cinematicMode = true;
        difficulty = "NORMAL";


        Game.showHitboxes = false;
    }
}
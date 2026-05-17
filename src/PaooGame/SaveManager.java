package PaooGame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/*! \class SaveManager
    \brief Gestioneaza salvarea si incarcarea progresului jocului.

    \details
    Salveaza progresul separat pentru fiecare profil de jucator.
    Fiecare jucator are propriul fisier de save in folderul res/saves:

    - res/saves/savegame_mirch.properties
    - res/saves/savegame_mario.properties

    Campurile salvate:
    - playerName      — numele profilului activ
    - level           — nivelul curent
    - playerX         — coordonata X a jucatorului
    - playerY         — coordonata Y a jucatorului
    - defeatedEnemies — inamicii invinsi, separati prin virgula
    - score           — scorul acumulat pana la salvare
 */
public class SaveManager {

    /*! \brief Folderul in care se salveaza fisierele de progres. */
    private static final String SAVE_FOLDER = "res/saves";

    /*! \brief Fisier vechi de compatibilitate, folosit doar daca nu exista profil activ. */
    private static final String LEGACY_SAVE_FILE = SAVE_FOLDER + "/savegame.properties";

    // =========================================================================
    //  UTILITARE
    // =========================================================================

    /*! \fn private static String sanitizeProfileName(String name)
        \brief Transforma numele profilului intr-un nume sigur de fisier.

        \details
        Inlocuieste caracterele care nu sunt litere, cifre, underscore sau cratima.
        Exemplu: "Ana Maria" devine "Ana_Maria".

        \param name Numele profilului.
        \return Numele curatat pentru utilizare in path de fisier.
     */
    private static String sanitizeProfileName(String name) {
        if (name == null || name.trim().isEmpty()) return "default";
        return name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    /*! \fn private static File getSaveFileForActiveProfile()
        \brief Returneaza fisierul de save pentru profilul activ.

        \details
        Daca exista un profil activ, salvarea este per-jucator.
        Daca nu exista profil activ, se foloseste fisierul legacy de compatibilitate.

        \return Fisierul de save corespunzator profilului activ.
     */
    private static File getSaveFileForActiveProfile() {
        PlayerProfile activeProfile = ProfileManager.getActive();
        if (activeProfile == null) return new File(LEGACY_SAVE_FILE);

        String safeName = sanitizeProfileName(activeProfile.getName());
        return new File(SAVE_FOLDER + "/savegame_" + safeName + ".properties");
    }

    // =========================================================================
    //  SALVARE
    // =========================================================================

    /*! \fn public static void saveGame(int level, float playerX, float playerY)
        \brief Salveaza nivelul si pozitia jucatorului fara inamici si fara scor.
        \details Apel de compatibilitate cu versiunile vechi ale proiectului.
     */
    public static void saveGame(int level, float playerX, float playerY) {
        saveGame(level, playerX, playerY, "", 0);
    }

    /*! \fn public static void saveGame(int level, float playerX, float playerY, String defeatedEnemies)
        \brief Salveaza progresul fara scor.
        \details Apel de compatibilitate pentru varianta fara sistem de scor.
     */
    public static void saveGame(int level, float playerX, float playerY,
                                String defeatedEnemies) {
        saveGame(level, playerX, playerY, defeatedEnemies, 0);
    }

    /*! \fn public static void saveGame(int level, float playerX, float playerY, String defeatedEnemies, int score)
        \brief Salveaza progresul complet al jocului pentru profilul activ.

        \details
        Salveaza: nivelul curent, pozitia playerului, inamicii invinsi si scorul.

        \param level            Nivelul curent.
        \param playerX          Pozitia X a jucatorului.
        \param playerY          Pozitia Y a jucatorului.
        \param defeatedEnemies  Lista inamicilor invinsi, separati prin virgula.
        \param score            Scorul acumulat pana la salvare.
     */
    public static void saveGame(int level, float playerX, float playerY,
                                String defeatedEnemies, int score) {
        try {
            File folder = new File(SAVE_FOLDER);
            if (!folder.exists()) folder.mkdirs();

            PlayerProfile activeProfile = ProfileManager.getActive();
            File saveFile = getSaveFileForActiveProfile();

            Properties properties = new Properties();

            properties.setProperty(
                    "playerName",
                    activeProfile != null ? activeProfile.getName() : "default"
            );
            properties.setProperty("level",           String.valueOf(level));
            properties.setProperty("playerX",         String.valueOf(playerX));
            properties.setProperty("playerY",         String.valueOf(playerY));
            properties.setProperty("defeatedEnemies", defeatedEnemies != null ? defeatedEnemies : "");
            properties.setProperty("score",           String.valueOf(score));

            try (FileOutputStream output = new FileOutputStream(saveFile)) {
                properties.store(output, "Aethelgard Save Game");
            }

            /// Actualizam profilul activ cu nivelul si pozitia curenta.
            ProfileManager.saveProgress(level, playerX, playerY);

            /// Actualizam best score-ul in profil daca scorul curent este mai mare.
            if (activeProfile != null) {
                activeProfile.setBestScore(score);
                ProfileManager.saveActiveProfile();
            }

            System.out.println("Joc salvat cu succes pentru profilul: "
                    + (activeProfile != null ? activeProfile.getName() : "default"));

        } catch (Exception e) {
            System.out.println("Eroare la salvare: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  INCARCARE
    // =========================================================================

    /*! \fn public static SaveGameState loadGame()
        \brief Incarca progresul salvat pentru profilul activ.

        \details
        Citeste fisierul .properties corespunzator profilului activ si
        construieste un obiect SaveGameState cu toate datele restaurate,
        inclusiv scorul si inamicii invinsi.

        \return Obiectul SaveGameState cu datele salvate, sau null daca nu exista save.
     */
    public static SaveGameState loadGame() {
        File saveFile = getSaveFileForActiveProfile();

        if (!saveFile.exists()) {
            System.out.println("Nu exista save pentru profilul activ.");
            return null;
        }

        try (FileInputStream input = new FileInputStream(saveFile)) {
            Properties properties = new Properties();
            properties.load(input);

            int    level           = Integer.parseInt(properties.getProperty("level",   "1"));
            float  playerX         = Float.parseFloat(properties.getProperty("playerX", "0"));
            float  playerY         = Float.parseFloat(properties.getProperty("playerY", "0"));
            String defeatedEnemies = properties.getProperty("defeatedEnemies", "");

            /// Compatibilitate cu save-uri vechi care nu aveau campul score.
            int score = Integer.parseInt(properties.getProperty("score", "0"));

            System.out.println("Joc incarcat: nivel=" + level
                    + ", score=" + score
                    + ", playerX=" + playerX + ", playerY=" + playerY);

            return new SaveGameState(level, playerX, playerY, defeatedEnemies, score);

        } catch (Exception e) {
            System.out.println("Eroare la incarcare: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================
    //  VERIFICARE / STERGERE
    // =========================================================================

    /*! \fn public static boolean hasSave()
        \brief Verifica daca exista un save pentru profilul activ.
        \return true daca fisierul de save exista pe disc.
     */
    public static boolean hasSave() {
        return getSaveFileForActiveProfile().exists();
    }

    /*! \fn public static void deleteSave()
        \brief Sterge fisierul de save al profilului activ.
     */
    public static void deleteSave() {
        File saveFile = getSaveFileForActiveProfile();
        if (saveFile.exists()) {
            saveFile.delete();
            System.out.println("Save sters pentru profilul activ.");
        }
    }
}
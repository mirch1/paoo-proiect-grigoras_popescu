package PaooGame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/*! \class SaveManager
    \brief Gestioneaza salvarea si incarcarea progresului jocului.

    \details
    Versiunea aceasta salveaza progresul separat pentru fiecare profil de jucator.
    Astfel, fiecare jucator are propriul fisier de save in folderul res/saves.

    Exemplu:
    - res/saves/savegame_mirch.properties
    - res/saves/savegame_mario.properties
*/

public class SaveManager {

    /*! \brief Folderul in care se salveaza fisierele de progres. */
    private static final String SAVE_FOLDER = "res/saves";

    /*! \brief Fisier vechi de compatibilitate, folosit doar daca nu exista profil activ. */
    private static final String LEGACY_SAVE_FILE = SAVE_FOLDER + "/savegame.properties";

    // =========================================================================
    //  UTILITARE PENTRU FISIERUL DE SAVE
    // =========================================================================

    /*! \fn private static String sanitizeProfileName(String name)
        \brief Transforma numele profilului intr-un nume sigur de fisier.

        \details
        Inlocuieste caracterele care nu sunt litere, cifre, underscore sau cratima.
        \param name Numele profilului.
        \return Numele curatat pentru fisier.
     */
    private static String sanitizeProfileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "default";
        }

        return name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    /*! \fn private static File getSaveFileForActiveProfile()
        \brief Returneaza fisierul de save pentru profilul activ.

        \details
        Daca exista un profil activ, salvarea este per-jucator.
        Daca nu exista profil activ, folosim fisierul vechi de compatibilitate.

        \return Fisierul in care se salveaza / din care se incarca progresul.
     */
    private static File getSaveFileForActiveProfile() {
        PlayerProfile activeProfile = ProfileManager.getActive();

        if (activeProfile == null) {
            return new File(LEGACY_SAVE_FILE);
        }

        String safeName = sanitizeProfileName(activeProfile.getName());
        return new File(SAVE_FOLDER + "/savegame_" + safeName + ".properties");
    }

    // =========================================================================
    //  SALVARE
    // =========================================================================

    /*! \fn public static void saveGame(int level, float playerX, float playerY)
        \brief Salveaza nivelul curent si pozitia player-ului pentru profilul activ.

        \details
        Salveaza datele in doua locuri:
        1. in fisierul individual al profilului activ;
        2. in obiectul PlayerProfile, ca meniul sa stie corect ca exista salvare.

        \param level Nivelul curent.
        \param playerX Pozitia X a jucatorului.
        \param playerY Pozitia Y a jucatorului.
     */
    public static void saveGame(int level, float playerX, float playerY) {
        try {
            File folder = new File(SAVE_FOLDER);

            /// Daca folderul res/saves nu exista, il cream.
            if (!folder.exists()) {
                folder.mkdirs();
            }

            PlayerProfile activeProfile = ProfileManager.getActive();
            File saveFile = getSaveFileForActiveProfile();

            Properties properties = new Properties();

            /// Salvam si numele profilului, util pentru verificari/debug.
            properties.setProperty(
                    "playerName",
                    activeProfile != null ? activeProfile.getName() : "default"
            );

            properties.setProperty("level", String.valueOf(level));
            properties.setProperty("playerX", String.valueOf(playerX));
            properties.setProperty("playerY", String.valueOf(playerY));

            try (FileOutputStream output = new FileOutputStream(saveFile)) {
                properties.store(output, "Aethelgard Save Game");
            }

            /*
             * Actualizam si profilul activ.
             * Altfel meniul nu stie ca jucatorul acesta are progres salvat.
             */
            ProfileManager.saveProgress(level, playerX, playerY);

            System.out.println("Joc salvat cu succes pentru profilul: "
                    + (activeProfile != null ? activeProfile.getName() : "default"));

        } catch (Exception e) {
            System.out.println("Eroare la salvarea jocului!");
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  INCARCARE
    // =========================================================================

    /*! \fn public static SaveGameState loadGame()
        \brief Incarca progresul salvat pentru profilul activ.

        \details
        Ordinea este:
        1. Cauta fisierul individual al profilului activ.
        2. Daca nu exista, foloseste progresul salvat in PlayerProfile.
        3. Daca nu exista profil activ, incearca fisierul vechi savegame.properties.

        \return Starea salvata sau null daca nu exista salvare.
     */
    public static SaveGameState loadGame() {
        try {
            PlayerProfile activeProfile = ProfileManager.getActive();
            File saveFile = getSaveFileForActiveProfile();

            /// 1. Incarcam din fisierul individual al profilului activ.
            if (saveFile.exists()) {
                return readSaveFile(saveFile);
            }

            /*
             * 2. Daca nu exista fisier individual, folosim datele din profil.
             * Asta ajuta si pentru profilurile deja existente in profiles.dat.
             */
            if (activeProfile != null) {
                boolean hasProgress =
                        activeProfile.getLevel() > 1
                                || activeProfile.getPlayerX() != 0f
                                || activeProfile.getPlayerY() != 0f;

                if (hasProgress) {
                    return new SaveGameState(
                            activeProfile.getLevel(),
                            activeProfile.getPlayerX(),
                            activeProfile.getPlayerY()
                    );
                }

                /// Profil activ exista, dar nu are progres salvat.
                return null;
            }

            /*
             * 3. Compatibilitate veche:
             * Folosim savegame.properties doar daca nu exista profil activ.
             * Nu vrem sa incarcam din save-ul global cand un jucator este selectat.
             */
            File legacyFile = new File(LEGACY_SAVE_FILE);
            if (legacyFile.exists()) {
                return readSaveFile(legacyFile);
            }

            return null;

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea jocului!");
            e.printStackTrace();
            return null;
        }
    }

    /*! \fn private static SaveGameState readSaveFile(File file)
        \brief Citeste efectiv nivelul si pozitia dintr-un fisier .properties.

        \param file Fisierul de save.
        \return Obiectul SaveGameState citit din fisier.
     */
    private static SaveGameState readSaveFile(File file) throws Exception {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        }

        int level = Integer.parseInt(properties.getProperty("level"));
        float playerX = Float.parseFloat(properties.getProperty("playerX"));
        float playerY = Float.parseFloat(properties.getProperty("playerY"));

        return new SaveGameState(level, playerX, playerY);
    }

    // =========================================================================
    //  VERIFICARE SAVE
    // =========================================================================

    /*! \fn public static boolean hasSaveGame()
        \brief Verifica daca profilul activ are o salvare.

        \details
        Verifica atat fisierul individual al profilului, cat si datele salvate
        in PlayerProfile.

        \return true daca exista salvare pentru jucatorul activ.
     */
    public static boolean hasSaveGame() {
        PlayerProfile activeProfile = ProfileManager.getActive();

        File saveFile = getSaveFileForActiveProfile();
        if (saveFile.exists()) {
            return true;
        }

        if (activeProfile != null) {
            return activeProfile.getLevel() > 1
                    || activeProfile.getPlayerX() != 0f
                    || activeProfile.getPlayerY() != 0f;
        }

        File legacyFile = new File(LEGACY_SAVE_FILE);
        return legacyFile.exists();
    }
}

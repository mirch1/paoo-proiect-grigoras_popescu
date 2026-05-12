package PaooGame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/*! \class SaveManager
    \brief Gestioneaza salvarea si incarcarea progresului jocului.
 */
public class SaveManager {
    private static final String SAVE_FOLDER = "res/saves";
    private static final String SAVE_FILE = SAVE_FOLDER + "/savegame.properties";

    /*! \fn public static void saveGame(int level, float playerX, float playerY)
        \brief Salveaza nivelul curent si pozitia player-ului.
     */
    public static void saveGame(int level, float playerX, float playerY) {
        try {
            File folder = new File(SAVE_FOLDER);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            Properties properties = new Properties();

            properties.setProperty("level", String.valueOf(level));
            properties.setProperty("playerX", String.valueOf(playerX));
            properties.setProperty("playerY", String.valueOf(playerY));

            FileOutputStream output = new FileOutputStream(SAVE_FILE);
            properties.store(output, "Aethelgard Save Game");
            output.close();

            System.out.println("Joc salvat cu succes!");
        } catch (Exception e) {
            System.out.println("Eroare la salvarea jocului!");
            e.printStackTrace();
        }
    }

    /*! \fn public static SaveGameState loadGame()
        \brief Incarca progresul salvat.
     */
    public static SaveGameState loadGame() {
        try {
            File file = new File(SAVE_FILE);

            if (!file.exists()) {
                return null;
            }

            Properties properties = new Properties();

            FileInputStream input = new FileInputStream(file);
            properties.load(input);
            input.close();

            int level = Integer.parseInt(properties.getProperty("level"));
            float playerX = Float.parseFloat(properties.getProperty("playerX"));
            float playerY = Float.parseFloat(properties.getProperty("playerY"));

            return new SaveGameState(level, playerX, playerY);
        } catch (Exception e) {
            System.out.println("Eroare la incarcarea jocului!");
            e.printStackTrace();
            return null;
        }
    }

    /*! \fn public static boolean hasSaveGame()
        \brief Verifica daca exista o salvare.
     */
    public static boolean hasSaveGame() {
        File file = new File(SAVE_FILE);
        return file.exists();
    }
}
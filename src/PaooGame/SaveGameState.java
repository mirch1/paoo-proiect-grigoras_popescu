package PaooGame;

import java.util.HashSet;
import java.util.Set;

/*! \class SaveGameState
    \brief Retine datele necesare pentru restaurarea progresului jocului.

    \details
    Pe langa nivel si pozitia playerului, clasa retine inamicii invinsi
    si scorul acumulat pana la momentul salvarii.
    Astfel, dupa Load Game, inamicii omorati anterior nu mai reapar ca vii,
    iar scorul este restaurat corect.
 */
public class SaveGameState {
    private int   level;
    private float playerX;
    private float playerY;

    /*! \brief Scorul acumulat pana la momentul salvarii. */
    private int score;

    /*! \brief ID-urile inamicilor invinsi, salvate din fisierul de save. */
    private Set<String> defeatedEnemies;

    // =========================================================================
    //  CONSTRUCTORI
    // =========================================================================

    /*! \fn public SaveGameState(int level, float playerX, float playerY)
        \brief Constructor de compatibilitate fara scor si fara inamici invinsi.
    */
    public SaveGameState(int level, float playerX, float playerY) {
        this(level, playerX, playerY, "", 0);
    }

    /*! \fn public SaveGameState(int level, float playerX, float playerY, String defeatedEnemiesText)
        \brief Constructor de compatibilitate fara scor.
    */
    public SaveGameState(int level, float playerX, float playerY, String defeatedEnemiesText) {
        this(level, playerX, playerY, defeatedEnemiesText, 0);
    }

    /*! \fn public SaveGameState(int level, float playerX, float playerY, String defeatedEnemiesText, int score)
        \brief Constructor complet cu scor.

        \param level               Nivelul curent al jocului.
        \param playerX             Coordonata X a jucatorului.
        \param playerY             Coordonata Y a jucatorului.
        \param defeatedEnemiesText Lista inamicilor invinsi, separati prin virgula.
        \param score               Scorul acumulat pana la salvare.
     */
    public SaveGameState(int level, float playerX, float playerY,
                         String defeatedEnemiesText, int score) {
        this.level   = level;
        this.playerX = playerX;
        this.playerY = playerY;
        this.score   = score;
        this.defeatedEnemies = new HashSet<>();

        if (defeatedEnemiesText != null && !defeatedEnemiesText.trim().isEmpty()) {
            for (String id : defeatedEnemiesText.split(",")) {
                if (id != null && !id.trim().isEmpty()) {
                    defeatedEnemies.add(id.trim());
                }
            }
        }
    }

    // =========================================================================
    //  GETTERI
    // =========================================================================

    /*! \fn public int getLevel()      \brief Returneaza nivelul salvat.            */
    public int getLevel()     { return level; }

    /*! \fn public float getPlayerX()  \brief Returneaza coordonata X salvata.      */
    public float getPlayerX() { return playerX; }

    /*! \fn public float getPlayerY()  \brief Returneaza coordonata Y salvata.      */
    public float getPlayerY() { return playerY; }

    /*! \fn public int getScore()
        \brief Returneaza scorul salvat.
        \return Scorul acumulat pana la momentul salvarii.
     */
    public int getScore() { return score; }

    /*! \fn public void setScore(int score)
        \brief Seteaza scorul in starea de save.
        \param score Scorul curent al sesiunii.
     */
    public void setScore(int score) { this.score = score; }

    /*! \fn public boolean isEnemyDefeated(String enemyId)
        \brief Verifica daca un anumit inamic era deja invins la momentul salvarii.
        \param enemyId ID-ul inamicului de verificat.
        \return true daca inamicul a fost deja invins.
     */
    public boolean isEnemyDefeated(String enemyId) {
        return defeatedEnemies.contains(enemyId);
    }

    /*! \fn public Set<String> getDefeatedEnemies()
        \brief Returneaza setul complet de ID-uri ale inamicilor invinsi.
     */
    public Set<String> getDefeatedEnemies() { return defeatedEnemies; }
}
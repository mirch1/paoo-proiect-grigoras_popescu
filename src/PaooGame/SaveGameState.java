package PaooGame;

import java.util.HashSet;
import java.util.Set;

/*! \class SaveGameState
    \brief Retine datele necesare pentru restaurarea progresului jocului.

    \details
    Pe langa nivel si pozitia playerului, clasa retine si inamicii invinsi.
    Astfel, dupa Load Game, inamicii omorati anterior nu mai reapar ca fiind vii.
 */
public class SaveGameState {
    private int level;
    private float playerX;
    private float playerY;

    /*! \brief ID-urile inamicilor invinsi, salvate din fisierul de save. */
    private Set<String> defeatedEnemies;

    public SaveGameState(int level, float playerX, float playerY) {
        this(level, playerX, playerY, "");
    }

    public SaveGameState(int level, float playerX, float playerY, String defeatedEnemiesText) {
        this.level = level;
        this.playerX = playerX;
        this.playerY = playerY;
        this.defeatedEnemies = new HashSet<>();

        if (defeatedEnemiesText != null && !defeatedEnemiesText.trim().isEmpty()) {
            String[] ids = defeatedEnemiesText.split(",");

            for (String id : ids) {
                if (id != null && !id.trim().isEmpty()) {
                    defeatedEnemies.add(id.trim());
                }
            }
        }
    }

    public int getLevel() {
        return level;
    }

    public float getPlayerX() {
        return playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    /*! \fn public boolean isEnemyDefeated(String enemyId)
        \brief Verifica daca un anumit inamic era deja invins la momentul salvarii.
     */
    public boolean isEnemyDefeated(String enemyId) {
        return defeatedEnemies.contains(enemyId);
    }

    public Set<String> getDefeatedEnemies() {
        return defeatedEnemies;
    }
}

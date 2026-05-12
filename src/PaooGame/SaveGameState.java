package PaooGame;

/*! \class SaveGameState
    \brief Retine datele minime necesare pentru restaurarea progresului jocului.
 */
public class SaveGameState {
    private int level;
    private float playerX;
    private float playerY;

    public SaveGameState(int level, float playerX, float playerY) {
        this.level = level;
        this.playerX = playerX;
        this.playerY = playerY;
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
}
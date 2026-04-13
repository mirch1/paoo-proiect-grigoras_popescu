package PaooGame;

public class Camera {
    private float x;
    private float y;

    private final int screenWidth;
    private final int screenHeight;

    public Camera(float x, float y, int screenWidth, int screenHeight) {
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void CenterOnPlayer(Player player, Map map) {
        x = player.GetX() - screenWidth / 2.0f + player.GetWidth() / 2.0f;
        y = player.GetY() - screenHeight / 2.0f + player.GetHeight() / 2.0f;

        float maxX = map.getPixelWidth() - screenWidth;
        float maxY = map.getPixelHeight() - screenHeight;

        if (maxX < 0) {
            maxX = 0;
        }
        if (maxY < 0) {
            maxY = 0;
        }

        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (x > maxX) {
            x = maxX;
        }
        if (y > maxY) {
            y = maxY;
        }
    }

    public float GetX() {
        return x;
    }

    public float GetY() {
        return y;
    }
}

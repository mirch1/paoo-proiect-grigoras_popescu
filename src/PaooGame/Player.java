package PaooGame;

import PaooGame.Tiles.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Player {
    private float x;
    private float y;

    private final int width;
    private final int height;
    private final float speed;
    private final int feetOffsetX = 9;
    private final int feetOffsetY = 47;
    private final int feetWidth = 10;
    private final int feetHeight = 6;

    private BufferedImage idleFrame;

    public Player(float x, float y) {
        this.x = x;
        this.y = y;


        this.width = 32;
        this.height = 32;
        this.speed = 4.0f;

        try {
            BufferedImage spriteSheet = ImageIO.read(new File("res/textures/_Idle.png"));

            int frameWidth = 120;
            int frameHeight = 80;


            int frameIndex = 4;

            idleFrame = spriteSheet.getSubimage(
                    frameIndex * frameWidth,
                    0,
                    frameWidth,
                    frameHeight
            );

        } catch (Exception e) {
            idleFrame = null;
            e.printStackTrace();
        }
    }

    public void Update(KeyManager keyManager, Map map) {
        float xMove = 0;
        float yMove = 0;

        if (keyManager.up)    yMove -= speed;
        if (keyManager.down)  yMove += speed;
        if (keyManager.left)  xMove -= speed;
        if (keyManager.right) xMove += speed;

        if (xMove != 0) {
            float newX = x + xMove;
            if (CanMoveTo(newX, y, map)) {
                x = newX;
            }
        }

        if (yMove != 0) {
            float newY = y + yMove;
            if (CanMoveTo(x, newY, map)) {
                y = newY;
            }
        }
    }

    private boolean CanMoveTo(float testX, float testY, Map map) {
        float left = testX + feetOffsetX;
        float right = testX + feetOffsetX + feetWidth - 1;
        float top = testY + feetOffsetY;
        float bottom = testY + feetOffsetY + feetHeight - 1;

        if (left < 0 || top < 0 ||
                right >= map.getPixelWidth() ||
                bottom >= map.getPixelHeight()) {
            return false;
        }

        return !map.isSolidAtPixel(left, top) &&
                !map.isSolidAtPixel(right, top) &&
                !map.isSolidAtPixel(left, bottom) &&
                !map.isSolidAtPixel(right, bottom);
    }

    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY +  (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(0,0,0,60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);
        if (idleFrame != null) {

            int drawWidth = 78;
            int drawHeight = 82;


            int drawX = screenX - 22;
            int drawY = screenY - 28;

            g2.drawImage(idleFrame, drawX, drawY, drawWidth, drawHeight, null);
        } else {

            g2.setColor(new Color(180, 190, 220));
            g2.fillRect(screenX, screenY, width, height);

            g2.setColor(new Color(40, 40, 60));
            g2.drawRect(screenX, screenY, width, height);
        }
    }

    public float GetX() {
        return x;
    }

    public float GetY() {
        return y;
    }

    public int GetWidth() {
        return width;
    }

    public int GetHeight() {
        return height;
    }
}
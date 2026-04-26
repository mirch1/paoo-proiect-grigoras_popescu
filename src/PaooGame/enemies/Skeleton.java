package PaooGame.enemies;

import PaooGame.Animation;
import PaooGame.Entity;
import PaooGame.Game;
import PaooGame.Map;
import PaooGame.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/*! \class Skeleton
    \brief Implementeaza un inamic de tip Schelet (Nivel 2), mostenind clasa abstracta Entity.
 */
public class Skeleton extends Entity {

    private Animation animIdle;
    private Animation animRun;

    private boolean isMoving = false;
    private boolean facingRight = true;

    private Player targetPlayer;
    private final int aggroRadius = 250; /*!< Raza de detectare marita pentru a vedea jucatorul mai usor */

    /*! \fn public Skeleton(float x, float y, Player targetPlayer)
        \brief Constructorul de initializare pentru inamicul Schelet.
     */
    public Skeleton(float x, float y, Player targetPlayer) {
        super(x, y, 32, 32);

        this.speed = 1.5f;
        this.targetPlayer = targetPlayer;

        /// --- AJUSTARE HITBOX COLIZIUNI ---
        this.feetOffsetX = 8;
        this.feetOffsetY = 24;
        this.feetWidth = 16;
        this.feetHeight = 8;

        try {
            /// 1. INCARCARE ANIMATIE MERS
            BufferedImage walkSheet = ImageIO.read(new File("res/textures/Skeleton_walk-Sheet.png"));
            int numFramesWalk = 5;
            int walkFrameWidth = walkSheet.getWidth() / numFramesWalk;
            int walkFrameHeight = walkSheet.getHeight();

            BufferedImage[] framesWalk = new BufferedImage[numFramesWalk];
            for(int i = 0; i < framesWalk.length; i++) {
                framesWalk[i] = walkSheet.getSubimage(i * walkFrameWidth, 0, walkFrameWidth, walkFrameHeight);
            }
            animRun = new Animation(120, framesWalk);

            /// 2. INCARCARE ANIMATIE REPAUS
            BufferedImage idleSheet = ImageIO.read(new File("res/textures/Skeleton_idle-Sheet.png"));
            int numFramesIdle = 5;
            int idleFrameWidth = idleSheet.getWidth() / numFramesIdle;
            int idleFrameHeight = idleSheet.getHeight();

            BufferedImage[] framesIdle = new BufferedImage[numFramesIdle];
            for(int i = 0; i < framesIdle.length; i++) {
                framesIdle[i] = idleSheet.getSubimage(i * idleFrameWidth, 0, idleFrameWidth, idleFrameHeight);
            }
            animIdle = new Animation(200, framesIdle);

        } catch (Exception e) {
            System.out.println("Eroare critica: Nu s-au putut incarca fisierele pentru Skeleton!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(Map map)
        \brief Logica de urmarire si validare a miscarii.
     */
    public void Update(Map map) {
        float distance = (float) Math.sqrt(Math.pow(targetPlayer.GetX() - this.x, 2) + Math.pow(targetPlayer.GetY() - this.y, 2));

        float xMove = 0;
        float yMove = 0;

        if (distance < aggroRadius && distance > 5) {
            if (targetPlayer.GetX() < this.x - speed) {
                xMove -= speed;
                facingRight = false;
            } else if (targetPlayer.GetX() > this.x + speed) {
                xMove += speed;
                facingRight = true;
            }

            if (targetPlayer.GetY() < this.y - speed) {
                yMove -= speed;
            } else if (targetPlayer.GetY() > this.y + speed) {
                yMove += speed;
            }
        }

        isMoving = (xMove != 0 || yMove != 0);

        if (isMoving) {
            if (animRun != null) animRun.tick();
        } else {
            if (animIdle != null) animIdle.tick();
        }

        if (xMove != 0) {
            float newX = x + xMove;
            if (CanMoveTo(newX, y, map)) x = newX;
        }
        if (yMove != 0) {
            float newY = y + yMove;
            if (CanMoveTo(x, newY, map)) y = newY;
        }
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        /// Umbra 2.5D
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 16, 8);

        if (animIdle != null && animRun != null) {
            int renderWidth = 24;
            int renderHeight = 24;

            int drawX = screenX + 4;
            int drawY = screenY + 8;

            BufferedImage currentFrame = isMoving ? animRun.getCurrentFrame() : animIdle.getCurrentFrame();

            if (facingRight) {
                g2.drawImage(currentFrame, drawX, drawY, renderWidth, renderHeight, null);
            } else {
                g2.drawImage(currentFrame,
                        drawX + renderWidth, drawY, drawX, drawY + renderHeight,
                        0, 0, currentFrame.getWidth(), currentFrame.getHeight(), null);
            }
        } else {
            g2.setColor(Color.RED);
            g2.fillRect(screenX, screenY, width, height);
        }

        /// DEBUG HITBOX
        if (Game.showHitboxes) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);
            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }
}
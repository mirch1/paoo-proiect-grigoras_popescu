package PaooGame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/*! \class Player
    \brief Implementeaza personajul principal controlat de jucator, mostenind clasa de baza Entity.
 */
public class Player extends Entity {

    private Animation animIdle;         /*!< Animatia pentru starea de repaus (idle).*/
    private Animation animRun;          /*!< Animatia pentru starea de miscare (run).*/
    private Animation animTurnAround;   /*!< Animatia de tranzitie cand jucatorul schimba directia.*/

    private boolean isMoving = false;   /*!< Flag care indica daca jucatorul este in miscare.*/
    private boolean isTurning = false;  /*!< Flag care indica daca jucatorul se afla intr-o tranzitie de intoarcere.*/
    private boolean facingRight = true; /*!< Flag care retine directia in care este orientat jucatorul.*/
    private int turnTimer = 0;          /*!< Temporizator pentru a sustine durata animatiei de intoarcere.*/

    /*! \fn public Player(float x, float y)
        \brief Constructorul de initializare al clasei Player.
     */
    public Player(float x, float y) {
        super(x, y, 32, 32);

        this.speed = 4.0f;

        /// --- AJUSTARE HITBOX COLIZIUNI ---
        this.feetOffsetX = 8;
        this.feetOffsetY = 20;
        this.feetWidth = 16;
        this.feetHeight = 12;

        try {
            int frameWidth = 120;
            int frameHeight = 80;

            BufferedImage sheetIdle = ImageIO.read(new File("res/textures/_Idle.png"));
            BufferedImage[] framesIdle = new BufferedImage[10];
            for(int i = 0; i < framesIdle.length; i++) {
                framesIdle[i] = sheetIdle.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            animIdle = new Animation(100, framesIdle);

            BufferedImage sheetRun = ImageIO.read(new File("res/textures/_Run.png"));
            BufferedImage[] framesRun = new BufferedImage[10];
            for(int i = 0; i < framesRun.length; i++) {
                framesRun[i] = sheetRun.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            animRun = new Animation(80, framesRun);

            BufferedImage sheetTurnAround = ImageIO.read(new File("res/textures/_TurnAround.png"));
            BufferedImage[] framesTurnAround = new BufferedImage[3];
            for(int i = 0; i < framesTurnAround.length; i++) {
                framesTurnAround[i] = sheetTurnAround.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            animTurnAround = new Animation(80, framesTurnAround);

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea imaginilor pentru Player!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(KeyManager keyManager, Map map)
     */
    public void Update(KeyManager keyManager, Map map) {
        float xMove = 0;
        float yMove = 0;
        boolean wasFacingRight = facingRight;

        if (keyManager.up)    yMove -= speed;
        if (keyManager.down)  yMove += speed;
        if (keyManager.left) {
            xMove -= speed;
            facingRight = false;
        }
        if (keyManager.right) {
            xMove += speed;
            facingRight = true;
        }

        if (wasFacingRight != facingRight && xMove != 0) {
            isTurning = true;
            turnTimer = 15;
        }

        if (turnTimer > 0) turnTimer--;
        else isTurning = false;

        isMoving = (xMove != 0 || yMove != 0);

        if (isTurning) {
            if (animTurnAround != null) animTurnAround.tick();
        } else if (isMoving) {
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

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);

        if (animIdle != null && animRun != null && animTurnAround != null) {
            int drawX = screenX - 22;
            int drawY = screenY - 50;

            BufferedImage currentFrame;

            if (isTurning) currentFrame = animTurnAround.getCurrentFrame();
            else if (isMoving) currentFrame = animRun.getCurrentFrame();
            else currentFrame = animIdle.getCurrentFrame();

            if (facingRight) {
                g2.drawImage(currentFrame, drawX, drawY, 78, 82, null);
            } else {
                g2.drawImage(currentFrame,
                        drawX + 78, drawY, drawX, drawY + 82,
                        0, 0, currentFrame.getWidth(), currentFrame.getHeight(), null);
            }
        } else {
            g2.setColor(new Color(180, 190, 220));
            g2.fillRect(screenX, screenY, width, height);
        }

        /// =========================================
        /// DEBUG: AFISARE HITBOX PENTRU COLIZIUNI
        /// =========================================
        if (Game.showHitboxes) {
            /// Cutia verde: Bounding Box-ul general (Spatiul virtual al personajului)
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);

            /// Cutia rosie: Hitbox-ul real (Picioarele care se lovesc de harta)
            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }
}
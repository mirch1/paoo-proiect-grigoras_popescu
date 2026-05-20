package PaooGame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/*! \class Player
    \brief Implementeaza personajul principal controlat de jucator, mostenind clasa Entity.
    \details Gestioneaza inputul de tastatura, animatiile (idle/run/turn/attack),
             miscarea cu coliziuni si un hitbox de atac activ pe durata animatiei de sabie.
             Flash-ul rosu la damage a fost eliminat.
 */
public class Player extends Entity {

    private Animation animIdle;         /*!< Animatia de repaus.                          */
    private Animation animRun;          /*!< Animatia de alergare.                        */
    private Animation animTurnAround;   /*!< Animatia de intoarcere.                      */
    private Animation animAttack;       /*!< Animatia de atac cu sabia.                   */

    private boolean isMoving    = false; /*!< true daca jucatorul se misca.              */
    private boolean isTurning   = false; /*!< true in tranzitia de intoarcere.           */
    private boolean isAttacking = false; /*!< true pe durata animatiei de atac.          */
    private boolean facingRight = true;  /*!< Directia curenta a jucatorului.            */
    private int     turnTimer   = 0;     /*!< Durata animatiei de intoarcere in cadre.   */
    private int     attackTimer = 0;     /*!< Durata animatiei de atac in cadre.         */

    /// HP maxim al jucatorului.
    private static final int PLAYER_MAX_HP = 100;

    /// Damage aplicat inamicilor la fiecare lovitura a jucatorului.
    public static final int ATTACK_DAMAGE = 20;

    /*! \fn public Player(float x, float y)
        \brief Constructor — initializeaza HP si incarca toate animatiile.
     */
    public Player(float x, float y) {
        super(x, y, 32, 32);

        this.speed     = 4.0f;
        this.maxHp     = PLAYER_MAX_HP;
        this.currentHp = PLAYER_MAX_HP;

        this.feetOffsetX = 10;
        this.feetOffsetY = 24;
        this.feetWidth   = 12;
        this.feetHeight  = 6;

        try {
            int frameWidth  = 120;
            int frameHeight = 80;

            /// 1. ANIMATIA IDLE
            BufferedImage sheetIdle    = ImageIO.read(new File("res/textures/_Idle.png"));
            BufferedImage[] framesIdle = new BufferedImage[10];
            for (int i = 0; i < framesIdle.length; i++)
                framesIdle[i] = sheetIdle.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            animIdle = new Animation(100, framesIdle);

            /// 2. ANIMATIA RUN
            BufferedImage sheetRun    = ImageIO.read(new File("res/textures/_Run.png"));
            BufferedImage[] framesRun = new BufferedImage[10];
            for (int i = 0; i < framesRun.length; i++)
                framesRun[i] = sheetRun.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            animRun = new Animation(80, framesRun);

            /// 3. ANIMATIA TURN-AROUND
            BufferedImage sheetTurn    = ImageIO.read(new File("res/textures/_TurnAround.png"));
            BufferedImage[] framesTurn = new BufferedImage[3];
            for (int i = 0; i < framesTurn.length; i++)
                framesTurn[i] = sheetTurn.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            animTurnAround = new Animation(80, framesTurn);

            /// 4. ANIMATIA ATTACK
            BufferedImage sheetAttack    = ImageIO.read(new File("res/textures/_Attack.png"));
            BufferedImage[] framesAttack = new BufferedImage[4];
            for (int i = 0; i < framesAttack.length; i++)
                framesAttack[i] = sheetAttack.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            animAttack = new Animation(70, framesAttack);

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea imaginilor pentru Player!");
            e.printStackTrace();
        }
    }


    public void drinkMoonrootPotion() {
        this.maxHp = 180;
        this.currentHp = 180;
    }
    
    /*! \fn public void Update(KeyManager keyManager, Map map)
        \brief Actualizeaza inputul, miscare, animatii si timer-ele de HP.
     */
    public void Update(KeyManager keyManager, Map map) {
        if (isDead) return;

        tickTimers();

        float   xMove          = 0;
        float   yMove          = 0;
        boolean wasFacingRight = facingRight;

        if (keyManager.up)    yMove -= speed;
        if (keyManager.down)  yMove += speed;
        if (keyManager.left)  { xMove -= speed; facingRight = false; }
        if (keyManager.right) { xMove += speed; facingRight = true;  }

        /// Declanseaza atacul la apasarea SPACE, o singura data per apasare.
        if (keyManager.attack && !isAttacking) {
            isAttacking = true;
            attackTimer = 18;
        }

        if (isAttacking) {
            attackTimer--;
            if (attackTimer <= 0) isAttacking = false;
        }


        /// Animatia de intoarcere la schimbarea directiei.
        if (wasFacingRight != facingRight && xMove != 0) {
            isTurning = true;
            turnTimer = 15;
        }
        if (turnTimer > 0) turnTimer--;
        else               isTurning = false;

        /// Pe durata atacului jucatorul sta pe loc.
        if (isAttacking) { xMove = 0; yMove = 0; }

        isMoving = (xMove != 0 || yMove != 0);

        /// Selectia animatiei: Atac > Intoarcere > Alergare > Idle
        if      (isAttacking) { if (animAttack     != null) animAttack.tick();     }
        else if (isTurning)   { if (animTurnAround != null) animTurnAround.tick(); }
        else if (isMoving)    { if (animRun        != null) animRun.tick();        }
        else                  { if (animIdle       != null) animIdle.tick();       }

        /// Miscare cu verificare coliziuni pe fiecare axa separat.
        if (xMove != 0) { float nx = x + xMove; if (CanMoveTo(nx, y,  map)) x = nx; }
        if (yMove != 0) { float ny = y + yMove; if (CanMoveTo(x,  ny, map)) y = ny; }
    }

    /*! \fn public Rectangle getAttackHitbox()
        \brief Returneaza hitbox-ul de atac, activ doar pe durata animatiei de sabie.
        \details Hitbox-ul este plasat in fata jucatorului (stanga sau dreapta).
                 Returneaza null daca jucatorul nu ataca.
        \return Rectangle in coordonate de world sau null.
     */
    public Rectangle getAttackHitbox() {
        if (!isAttacking) return null;

        int atkWidth  = 28;
        int atkHeight = 20;
        int atkY      = (int)(y + feetOffsetY) - atkHeight / 2 + feetHeight / 2;
        int atkX;

        if (facingRight) {
            atkX = (int)(x + feetOffsetX + feetWidth + 2);
        } else {
            atkX = (int)(x + feetOffsetX - atkWidth - 2);
        }

        return new Rectangle(atkX, atkY, atkWidth, atkHeight);
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza jucatorul, umbra si hitbox-urile de debug.
        \details Nu deseneaza nimic daca jucatorul este mort.
                 Flash-ul rosu a fost eliminat.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (isDead) return;

        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;
        Graphics2D g2 = (Graphics2D) g;

        /// Umbra pseudo-3D.
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);

        if (animIdle != null && animRun != null && animTurnAround != null && animAttack != null) {
            int drawX = screenX - 22;
            int drawY = screenY - 50;

            BufferedImage currentFrame;
            if      (isAttacking) currentFrame = animAttack.getCurrentFrame();
            else if (isTurning)   currentFrame = animTurnAround.getCurrentFrame();
            else if (isMoving)    currentFrame = animRun.getCurrentFrame();
            else                  currentFrame = animIdle.getCurrentFrame();

            if (facingRight) {
                g2.drawImage(currentFrame, drawX, drawY, 78, 82, null);
            } else {
                g2.drawImage(currentFrame,
                        drawX + 78, drawY, drawX, drawY + 82,
                        0, 0, currentFrame.getWidth(), currentFrame.getHeight(), null);
            }
        } else {
            /// Fallback daca imaginile nu s-au incarcat.
            g2.setColor(new Color(180, 190, 220));
            g2.fillRect(screenX, screenY, width, height);
        }

        /// DEBUG: hitbox-uri (activat cu tasta H).
        if (Game.showHitboxes) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);

            Rectangle atk = getAttackHitbox();
            if (atk != null) {
                g2.setColor(Color.CYAN);
                int dbgX = atk.x - (offsetX - cameraX);
                int dbgY = atk.y - (offsetY - cameraY);
                g2.drawRect(dbgX, dbgY, atk.width, atk.height);
            }
        }
    }
}

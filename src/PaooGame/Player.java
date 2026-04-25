package PaooGame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/*! \class Player
    \brief Implementeaza personajul principal controlat de jucator, mostenind clasa de baza Entity.
 */
public class Player extends Entity {

    /// NOTA: Variabilele fizice (x, y, width, height, speed) si hitbox-urile
    /// sunt mostenite automat din clasa Entity. Nu mai trebuie declarate aici!

    private Animation animIdle;         /*!< Animatia pentru starea de repaus (idle).*/
    private Animation animRun;          /*!< Animatia pentru starea de miscare (run).*/
    private Animation animTurnAround;   /*!< Animatia de tranzitie cand jucatorul schimba directia.*/

    private boolean isMoving = false;   /*!< Flag care indica daca jucatorul este in miscare.*/
    private boolean isTurning = false;  /*!< Flag care indica daca jucatorul se afla intr-o tranzitie de intoarcere.*/
    private boolean facingRight = true; /*!< Flag care retine directia in care este orientat jucatorul.*/
    private int turnTimer = 0;          /*!< Temporizator pentru a sustine durata animatiei de intoarcere.*/

    /*! \fn public Player(float x, float y)
        \brief Constructorul de initializare al clasei Player.
        \param x Pozitia initiala pe axa X.
        \param y Pozitia initiala pe axa Y.
     */
    public Player(float x, float y) {
        /// Apelam constructorul clasei de baza Entity pentru a seta coordonatele si dimensiunile standard (32x32).
        super(x, y, 32, 32);

        /// Setam viteza specifica a jucatorului.
        this.speed = 4.0f;

        /// Incarcam resursele grafice (sprite sheet-urile) pentru animatii.
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
        \brief Actualizeaza pozitia, starea si animatia jucatorului pe baza input-ului.
        \param keyManager Referinta catre obiectul care gestioneaza intrarile de la tastatura.
        \param map Referinta catre harta curenta pentru a verifica coliziunile.
     */
    public void Update(KeyManager keyManager, Map map) {
        float xMove = 0;
        float yMove = 0;
        boolean wasFacingRight = facingRight;

        /// Stabilim directia de miscare in functie de tastele apasate
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

        /// Daca directia s-a schimbat fata de cadrul anterior, declansam starea de intoarcere
        if (wasFacingRight != facingRight && xMove != 0) {
            isTurning = true;
            turnTimer = 15; /// Durata tranzitiei de intoarcere
        }

        /// Gestionam temporizatorul de intoarcere
        if (turnTimer > 0) {
            turnTimer--;
        } else {
            isTurning = false;
        }

        /// Determinam starea generala de miscare
        isMoving = (xMove != 0 || yMove != 0);

        /// Actualizam frame-ul (cadrul) animatiei curente
        if (isTurning) {
            if (animTurnAround != null) animTurnAround.tick();
        } else if (isMoving) {
            if (animRun != null) animRun.tick();
        } else {
            if (animIdle != null) animIdle.tick();
        }

        /// Aplicam miscarea efectiva doar daca testul de coliziune trece cu succes.
        /// Functia CanMoveTo() este apelata din clasa parinte Entity.
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

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Randeaza jucatorul si umbra sa pe ecran in spatiul relativ al camerei.
        \param g Contextul grafic in care se face desenarea.
        \param cameraX Pozitia curenta a camerei pe axa X.
        \param cameraY Pozitia curenta a camerei pe axa Y.
        \param offsetX Decalajul pe axa X pentru centrarea hartii globale.
        \param offsetY Decalajul pe axa Y pentru centrarea hartii globale.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        /// Calculam coordonatele virtuale pe ecran tinand cont de camera si de offset-ul general
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        /// Desenam o umbra eliptica simpla pentru a spori efectul 2.5D (depth)
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);

        /// Randarea animatiei
        if (animIdle != null && animRun != null && animTurnAround != null) {
            int drawX = screenX - 22;
            int drawY = screenY - 52;
            BufferedImage currentFrame;

            /// Selectam imaginea potrivita din memoria de animatii
            if (isTurning) currentFrame = animTurnAround.getCurrentFrame();
            else if (isMoving) currentFrame = animRun.getCurrentFrame();
            else currentFrame = animIdle.getCurrentFrame();

            /// Desenarea propriu-zisa a imaginii
            if (facingRight) {
                g2.drawImage(currentFrame, drawX, drawY, 78, 82, null);
            } else {
                /// Utilizam tehnica de 'flip' orizontal din Java 2D cand player-ul se misca la stanga
                g2.drawImage(currentFrame,
                        drawX + 78, drawY, drawX, drawY + 82,
                        0, 0, currentFrame.getWidth(), currentFrame.getHeight(),
                        null);
            }
        } else {
            /// Fallback de siguranta: Daca imaginile nu au putut fi incarcate, se deseneaza un dreptunghi
            g2.setColor(new Color(180, 190, 220));
            g2.fillRect(screenX, screenY, width, height);
        }
    }
}
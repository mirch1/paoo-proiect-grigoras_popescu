package PaooGame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/*! \class Enemy
    \brief Implementeaza inamicul de tip Lup (Nivel 1), mostenind clasa abstracta Entity.
    \details Utilizeaza AI simplu pentru urmarirea jucatorului si gestionarea starilor de animatie.
 */
public class Enemy extends Entity {

    /// Animatia pentru starea de repaus a lupului.
    private Animation animIdle;
    /// Animatia pentru starea de alergare/urmarire.
    private Animation animRun;

    /// Flag care indica daca lupul se afla in miscare.
    private boolean isMoving = false;
    /// Flag care retine directia in care este orientat lupul (true = dreapta, false = stanga).
    private boolean facingRight = true;

    /// Referinta catre entitatea Jucatorului (tinta AI-ului).
    private Player targetPlayer;
    /// Distanta maxima in pixeli de la care lupul detecteaza jucatorul.
    private final int aggroRadius = 250;

    /*! \fn public Enemy(float x, float y, Player targetPlayer)
        \brief Constructorul de initializare pentru inamicul Lup.
        \param x Pozitia initiala de spawn pe axa X.
        \param y Pozitia initiala de spawn pe axa Y.
        \param targetPlayer Referinta catre jucatorul pe care il va urmari.
     */
    public Enemy(float x, float y, Player targetPlayer) {
        /// Apelam constructorul parintelui Entity stabilind un spatiu fizic general de 32x32 pixeli.
        super(x, y, 32, 32);

        /// Viteza de miscare a lupului (recomandat mai mica decat a player-ului pentru echilibru).
        this.speed = 2.2f;
        this.targetPlayer = targetPlayer;

        /// --- AJUSTARE HITBOX COLIZIUNI ---
        /// Definim zona in care lupul se loveste de pereti doar in jumatatea de jos (la picioare)
        /// Astfel obtinem efectul 2.5D de profunzime, permitand vizual corpului sa se suprapuna peste pereti.
        this.feetOffsetX = 4;
        this.feetOffsetY = 20;
        this.feetWidth = 24;
        this.feetHeight = 12;

        /// =========================================================
        /// INCARCAREA RESURSELOR GRAFICE (SPRITE SHEET LUP)
        /// =========================================================
        try {
            /// Dimensiunile exacte calculate dintr-o imagine de 384x256 (grila 8x8).
            int frameWidth = 32;
            int frameHeight = 32;

            /// Incarcarea intregului fisier in memorie
            BufferedImage sheet = ImageIO.read(new File("res/textures/wolf_gray_full.png"));

            /// Selectam Randul 6 (al 7-lea de sus in jos) unde lupul este orientat clar spre dreapta.
            int rightFacingRow = 6;

            /// 1. ANIMATIA IDLE (Repaus)
            /// Se preia doar primul cadru (index 0) de pe randul stabilit.
            BufferedImage[] framesIdle = new BufferedImage[1];
            framesIdle[0] = sheet.getSubimage(0, rightFacingRow * frameHeight, frameWidth, frameHeight);
            animIdle = new Animation(500, framesIdle);

            /// 2. ANIMATIA RUN (Alergare)
            /// Secventa de alergare are 5 lupi pe acest rand.
            int numberOfRunFrames = 5;
            BufferedImage[] framesRun = new BufferedImage[numberOfRunFrames];
            for(int i = 0; i < framesRun.length; i++) {
                /// Decupam consecutiv de pe randul 6
                framesRun[i] = sheet.getSubimage(i * frameWidth, rightFacingRow * frameHeight, frameWidth, frameHeight);
            }

            /// Initializam animatia de alergare cu o viteza de 90ms pe cadru
            animRun = new Animation(90, framesRun);

        } catch (Exception e) {
            System.out.println("Eroare critica: Nu s-a putut incarca fisierul 'wolf_gray_full.png'!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(Map map)
        \brief Actualizeaza logica de miscare, coliziunile si selecteaza animatia potrivita.
        \param map Referinta catre harta curenta pentru validarea deplasarii.
     */
    public void Update(Map map) {
        /// 1. CALCULUL DISTANTEI PANA LA TINTA
        /// Se foloseste distanta euclidiana.
        float distance = (float) Math.sqrt(Math.pow(targetPlayer.GetX() - this.x, 2) + Math.pow(targetPlayer.GetY() - this.y, 2));

        float xMove = 0;
        float yMove = 0;

        /// 2. LOGICA DE URMARIRE (AI)
        /// Lupul se apropie daca jucatorul este in zona de aggro.
        if (distance < aggroRadius && distance > 5) {

            /// Zona de toleranta (Deadzone) egala cu viteza pentru a preveni "Jittering"-ul
            /// (miscarea spasmodica in stanga si dreapta cand coordonatele sunt aproape egale).
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

        /// 3. ACTUALIZAREA STARII SI A ANIMATIEI CURENTE
        isMoving = (xMove != 0 || yMove != 0);

        if (isMoving) {
            if (animRun != null) animRun.tick();
        } else {
            if (animIdle != null) animIdle.tick();
        }

        /// 4. EXECUTIA MISCARII (cu validare coliziuni preluata din clasa parinte)
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
        \brief Metoda de desenare a inamicului si umbrei sale. Include o marire virtuala a personajului.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        /// Stabilim coordonatele reale pe ecran aplicand si decalarile de la Camera/Harta
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        /// Desenam umbra la baza hitbox-ului pentru profunzime 2.5D
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);

        if (animIdle != null && animRun != null) {

            /// Il facem de 32x32
            int renderWidth = 32;
            int renderHeight = 32;

            /// Calculam offset-ul de desenare pentru a centra desenul marit (64)
            /// peste cutia sa de coliziune (32). -16px pe ambele axe il asaza perfect la centru.
            int drawX = screenX - 1;
            int drawY = screenY - 0;

            BufferedImage currentFrame = isMoving ? animRun.getCurrentFrame() : animIdle.getCurrentFrame();

            /// Randare conditionata pentru directie
            if (facingRight) {
                g2.drawImage(currentFrame, drawX, drawY, renderWidth, renderHeight, null);
            } else {
                /// Tehnica "Flip" pentru a reflecta imaginea pe orizontala
                g2.drawImage(currentFrame,
                        drawX + renderWidth, drawY, drawX, drawY + renderHeight,
                        0, 0, currentFrame.getWidth(), currentFrame.getHeight(), null);
            }
        } else {
            /// Cutie de avertizare rosie in caz de eroare I/O la imagini
            g2.setColor(Color.RED);
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
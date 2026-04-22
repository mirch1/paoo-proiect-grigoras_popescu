package PaooGame;

import PaooGame.Tiles.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Player {

    // --- POZIȚIE ȘI DIMENSIUNI JUCĂTOR ---
    private float x;
    private float y;
    private final int width;
    private final int height;
    private final float speed;

    // --- SETĂRI PENTRU COLIZIUNI (Hitbox-ul picioarelor) ---
    // Aceste variabile definesc "amprenta" jucătorului pe hartă
    // pentru a verifica coliziunile doar la nivelul picioarelor (efect 2.5D).
    private final int feetOffsetX = 9;
    private final int feetOffsetY = 47;
    private final int feetWidth = 10;
    private final int feetHeight = 6;

    // --- ANIMAȚII ȘI RESURSE GRAFICE ---
    private Animation animIdle;         // Cadre pentru stat pe loc
    private Animation animRun;          // Cadre pentru mișcare/alergare
    private Animation animTurnAround;   // Cadre de tranziție pentru pivotare (schimbare direcție)

    // --- STĂRILE JUCĂTORULUI ---
    private boolean isMoving = false;   // Adevărat dacă jucătorul se deplasează
    private boolean isTurning = false;  // Adevărat dacă jucătorul este în mijlocul unei întoarceri
    private boolean facingRight = true; // Ține minte direcția (ajută la desenarea în oglindă)
    private int turnTimer = 0;          // Cronometru care menține animația de întoarcere pe ecran

    public Player(float x, float y) {
        this.x = x;
        this.y = y;

        this.width = 32;
        this.height = 32;
        this.speed = 4.0f;

        // --- ÎNCĂRCAREA IMAGINILOR PENTRU ANIMAȚII ---
        try {
            int frameWidth = 120;
            int frameHeight = 80;

            // 1. Încărcare IDLE
            BufferedImage sheetIdle = ImageIO.read(new File("res/textures/_Idle.png"));
            BufferedImage[] framesIdle = new BufferedImage[10]; // Ajustează cu numărul real de cadre
            for(int i = 0; i < framesIdle.length; i++) {
                framesIdle[i] = sheetIdle.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            animIdle = new Animation(100, framesIdle);

            // 2. Încărcare RUN
            BufferedImage sheetRun = ImageIO.read(new File("res/textures/_Run.png"));
            BufferedImage[] framesRun = new BufferedImage[10]; // Ajustează cu numărul real de cadre
            for(int i = 0; i < framesRun.length; i++) {
                framesRun[i] = sheetRun.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            animRun = new Animation(80, framesRun);

            // 3. Încărcare TURN AROUND
            BufferedImage sheetTurnAround = ImageIO.read(new File("res/textures/_TurnAround.png"));
            BufferedImage[] framesTurnAround = new BufferedImage[3]; // Avem 3 cadre pentru întoarcere
            for(int i = 0; i < framesTurnAround.length; i++) {
                framesTurnAround[i] = sheetTurnAround.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            animTurnAround = new Animation(80, framesTurnAround);

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea imaginilor pentru Player!");
            e.printStackTrace();
        }
    }

    public void Update(KeyManager keyManager, Map map) {
        float xMove = 0;
        float yMove = 0;

        // Ținem minte direcția în care ne uitam înainte de a verifica tastele
        boolean wasFacingRight = facingRight;

        // --- 1. PRELUAREA COMENZILOR (Input) ȘI DIRECȚIEI ---
        if (keyManager.up)    yMove -= speed;
        if (keyManager.down)  yMove += speed;

        if (keyManager.left) {
            xMove -= speed;
            facingRight = false; // Setăm orientarea spre stânga
        }
        if (keyManager.right) {
            xMove += speed;
            facingRight = true;  // Setăm orientarea spre dreapta
        }

        // --- 2. LOGICA PENTRU ANIMAȚIA DE ÎNTOARCERE ---
        // Dacă direcția s-a schimbat față de cadrul anterior, inițiem întoarcerea
        if (wasFacingRight != facingRight && xMove != 0) {
            isTurning = true;
            turnTimer = 15; // Setăm timer-ul la ~15 cadre (aprox. un sfert de secundă)
        }

        // Dacă suntem în starea de întoarcere, scădem timer-ul
        if (turnTimer > 0) {
            turnTimer--;
        } else {
            isTurning = false; // După expirarea timpului, oprim starea de întoarcere
        }

        // --- 3. ACTUALIZAREA STĂRILOR ȘI ANIMAȚIILOR ---
        isMoving = (xMove != 0 || yMove != 0);

        // Dăm "tick" animației corespunzătoare
        if (isTurning) {
            if (animTurnAround != null) animTurnAround.tick();
        } else if (isMoving) {
            if (animRun != null) animRun.tick();
        } else {
            if (animIdle != null) animIdle.tick();
        }

        // --- 4. APLICAREA MIȘCĂRII ȘI VERIFICAREA COLIZIUNILOR ---
        // Mișcare X
        if (xMove != 0) {
            float newX = x + xMove;
            if (CanMoveTo(newX, y, map)) {
                x = newX;
            }
        }

        // Mișcare Y
        if (yMove != 0) {
            float newY = y + yMove;
            if (CanMoveTo(x, newY, map)) {
                y = newY;
            }
        }
    }

    // Funcție de verificare a coliziunilor
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
        // Calculăm poziția de desenare pe ecran
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        // --- 1. DESENARE UMBRĂ ---
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);

        // --- 2. DESENARE JUCĂTOR ---
        if (animIdle != null && animRun != null && animTurnAround != null) {
            int drawWidth = 78;
            int drawHeight = 82;
            int drawX = screenX - 22;
            int drawY = screenY - 28;

            // Extragem cadrul potrivit ținând cont de priorități
            BufferedImage currentFrame;
            if (isTurning) {
                currentFrame = animTurnAround.getCurrentFrame();
            } else if (isMoving) {
                currentFrame = animRun.getCurrentFrame();
            } else {
                currentFrame = animIdle.getCurrentFrame();
            }

            // --- TRUCUL PENTRU FLIP (ÎNTOARCEREA IMAGINII) ---
            if (facingRight) {
                // Dacă mergem la dreapta, desenăm normal
                g2.drawImage(currentFrame, drawX, drawY, drawWidth, drawHeight, null);
            } else {
                // Dacă mergem la stânga, desenăm în oglindă inversând coordonatele X de destinație
                g2.drawImage(currentFrame,
                        drawX + drawWidth, drawY, drawX, drawY + drawHeight, // Destinație (pe ecran) răsturnată
                        0, 0, currentFrame.getWidth(), currentFrame.getHeight(), // Sursa (din imagine) normală
                        null);
            }

        } else {
            // FALLBACK
            g2.setColor(new Color(180, 190, 220));
            g2.fillRect(screenX, screenY, width, height);
            g2.setColor(new Color(40, 40, 60));
            g2.drawRect(screenX, screenY, width, height);
        }
    }

    // --- GETTERS & SETTERS ---
    public float GetX() { return x; }
    public float GetY() { return y; }
    public int GetWidth() { return width; }
    public int GetHeight() { return height; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
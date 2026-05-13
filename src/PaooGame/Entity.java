package PaooGame;

import java.awt.*;

/*! \class Entity
    \brief Clasa abstracta de baza pentru toate entitatile din joc (Player, Inamici, NPC-uri).
    \details Defineste atributele fizice fundamentale (coordonate, dimensiuni, viteza) si logica
             universala de coliziune cu harta.
             Sistemul HP include: puncte de viata, damage cu invincibilitate post-hit si bara
             de viata desenata deasupra sprite-ului.
             Efectul de flash rosu a fost eliminat intentionat.
 */
public abstract class Entity {

    protected float x;          /*!< Coordonata curenta pe axa X.           */
    protected float y;          /*!< Coordonata curenta pe axa Y.           */
    protected int   width;      /*!< Latimea entitatii (pixeli).            */
    protected int   height;     /*!< Inaltimea entitatii (pixeli).          */
    protected float speed;      /*!< Viteza de deplasare (pixeli/cadru).    */

    /// --- Hitbox de coliziune (baza picioarelor) ---
    protected int feetOffsetX = 9;  /*!< Decalaj X fata de coltul stanga-sus al sprite-ului. */
    protected int feetOffsetY = 47; /*!< Decalaj Y fata de coltul stanga-sus al sprite-ului. */
    protected int feetWidth   = 10; /*!< Latimea hitbox-ului fizic.                          */
    protected int feetHeight  = 6;  /*!< Inaltimea hitbox-ului fizic.                        */

    /// --- Sistem HP ---
    protected int     maxHp     = 100; /*!< Punctele de viata maxime.                            */
    protected int     currentHp = 100; /*!< Punctele de viata curente.                           */
    protected boolean isDead    = false; /*!< true cand HP <= 0; Update/Draw nu mai executa nimic.*/

    /// Durata invincibilitatii post-hit in frame-uri (40 cadre ≈ 0.67 sec la 60 FPS).
    private static final int INVINCIBILITY_FRAMES = 40;

    /// Contor descrescator pentru invincibilitate.
    private int invincibilityTimer = 0;

    /*! \fn public Entity(float x, float y, int width, int height)
        \brief Constructor de initializare.
     */
    public Entity(float x, float y, int width, int height) {
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    // =========================================================================
    //  SISTEM HP
    // =========================================================================

    /*! \fn public void takeDamage(int amount)
        \brief Aplica damage entitatii daca nu este in invincibilitate si nu este moarta.
        \param amount Cantitatea de damage (valoare pozitiva).
     */
    public void takeDamage(int amount) {
        if (isDead || invincibilityTimer > 0) return;

        currentHp -= amount;
        invincibilityTimer = INVINCIBILITY_FRAMES;

        if (currentHp <= 0) {
            currentHp = 0;
            isDead    = true;
        }
    }

    /*! \fn protected void tickTimers()
        \brief Decrementeaza timer-ul de invincibilitate la fiecare cadru.
        \details Trebuie apelata la inceputul metodei Update() din fiecare subclasa.
     */
    protected void tickTimers() {
        if (invincibilityTimer > 0) invincibilityTimer--;
    }

    /*! \fn public boolean isDead()
        \brief Returneaza true daca entitatea a murit.
     */
    public boolean isDead() { return isDead; }

    /*! \fn public int getCurrentHp()
        \brief Returneaza HP-ul curent.
     */
    public int getCurrentHp() { return currentHp; }

    /*! \fn public int getMaxHp()
        \brief Returneaza HP-ul maxim.
     */
    public int getMaxHp() { return maxHp; }

    /*! \fn public Rectangle getFeetRect()
        \brief Returneaza hitbox-ul fizic al entitatii ca Rectangle in coordonate de world.
        \details Utilizat in Game.checkCombat() pentru detectia coliziunilor.
     */
    public Rectangle getFeetRect() {
        return new Rectangle(
            (int)(x + feetOffsetX),
            (int)(y + feetOffsetY),
            feetWidth,
            feetHeight
        );
    }

    /*! \fn protected void drawHealthBar(Graphics2D g2, int screenX, int screenY)
        \brief Deseneaza bara de viata deasupra sprite-ului.
        \details Bara schimba culoarea: verde > 60%, galben > 30%, rosu <= 30%.
                 Nu se deseneaza daca HP-ul este maxim.
        \param g2      Contextul grafic.
        \param screenX Coordonata X pe ecran a entitatii.
        \param screenY Coordonata Y pe ecran a entitatii.
     */
    protected void drawHealthBar(Graphics2D g2, int screenX, int screenY) {
        if (currentHp >= maxHp) return;

        int   barWidth  = width;
        int   barHeight = 4;
        int   barX      = screenX;
        int   barY      = screenY - 8;
        float ratio     = (float) currentHp / maxHp;

        /// Fundal inchis (zona de HP pierdut).
        g2.setColor(new Color(30, 10, 10, 200));
        g2.fillRect(barX, barY, barWidth, barHeight);

        /// Bara colorata (HP ramas).
        Color barColor = ratio > 0.6f ? new Color(50,  200, 50)
                       : ratio > 0.3f ? new Color(220, 190, 20)
                       :                new Color(210,  40, 40);
        g2.setColor(barColor);
        g2.fillRect(barX, barY, (int)(barWidth * ratio), barHeight);

        /// Contur subtil pentru lizibilitate.
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawRect(barX, barY, barWidth, barHeight);
    }

    /*! \fn public void drawHealthBarOnly(Graphics2D g2, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza DOAR bara de viata, separat de sprite.
        \details Apelata din Game.Draw() DUPA DrawForeground() — bara apare deasupra copacilor.
     */
    public void drawHealthBarOnly(Graphics2D g2, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (isDead || currentHp >= maxHp) return;
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;
        drawHealthBar(g2, screenX, screenY);
    }

    // =========================================================================
    //  COLIZIUNI CU HARTA
    // =========================================================================

    /*! \fn protected boolean CanMoveTo(float testX, float testY, Map map)
        \brief Verifica daca entitatea se poate deplasa la coordonatele specificate.
        \return true daca miscarea este valida (fara coliziuni), false altfel.
     */
    protected boolean CanMoveTo(float testX, float testY, Map map) {
        float left   = testX + feetOffsetX;
        float right  = testX + feetOffsetX + feetWidth  - 1;
        float top    = testY + feetOffsetY;
        float bottom = testY + feetOffsetY + feetHeight - 1;

        if (left < 0 || top < 0 || right >= map.getPixelWidth() || bottom >= map.getPixelHeight())
            return false;

        return !map.isSolidAtPixel(left,  top)    &&
               !map.isSolidAtPixel(right, top)    &&
               !map.isSolidAtPixel(left,  bottom) &&
               !map.isSolidAtPixel(right, bottom);
    }

    // =========================================================================
    //  GETTERI GENERALI
    // =========================================================================

    public float GetX()           { return x;      }
    public float GetY()           { return y;      }
    public int   GetWidth()       { return width;  }
    public int   GetHeight()      { return height; }

    public void  setPosition(float x, float y) { this.x = x; this.y = y; }

    public float GetFeetCenterX() { return x + feetOffsetX + feetWidth  / 2.0f; }
    public float GetFeetCenterY() { return y + feetOffsetY + feetHeight / 2.0f; }
    public float GetFeetBottomY() { return y + feetOffsetY + feetHeight - 1;    }
}

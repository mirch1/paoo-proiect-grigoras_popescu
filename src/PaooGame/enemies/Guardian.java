package PaooGame.enemies;

import PaooGame.Entity;
import PaooGame.Game;
import PaooGame.Map;
import PaooGame.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/*! \class Guardian
    \brief Implementeaza un gardian static care pazeste intrarea gresita in Nivelul 2.

    \details
    Gardienii stau pe loc si nu ataca din proprie initiativa.
    Daca jucatorul incearca sa-i loveasca, ii aplica damage instant fatal (999).
    In acest caz se seteaza flagul wrongEntrance = true, care este citit de Game
    pentru a afisa mesajul "WRONG ENTRANCE" pe ecranul de moarte.
    Gardienii nu pot fi ucisi de jucator — daca primesc damage, ei aplica
    damage instant jucatorului, nu invers.
*/
public class Guardian extends Entity {

    /// Referinta catre jucator, necesara pentru a-i aplica damage instant.
    private Player targetPlayer;

    /// Flag setat true cand jucatorul a incercat sa loveasca gardienii.
    private boolean wrongEntranceTriggered = false;

    /// Imaginea statica a gardianului (sprite simplu).
    private BufferedImage sprite;

    /// Directia in care priveste gardienii (implicit spre dreapta).
    private boolean facingRight;

    /// Damage instant aplicat jucatorului cand incearca sa atace gardienii.
    public static final int INSTANT_KILL_DAMAGE = 999;

    /*! \fn public Guardian(float x, float y, Player player, boolean facingRight)
        \brief Constructor — pozitioneaza gardienii si incarca sprite-ul.

        \param x           Coordonata X in world.
        \param y           Coordonata Y in world.
        \param player      Referinta la jucator.
        \param facingRight true daca gardienii privesc spre dreapta.
    */
    public Guardian(float x, float y, Player player, boolean facingRight) {
        super(x, y, 32, 64);
        this.targetPlayer  = player;
        this.facingRight   = facingRight;
        this.maxHp         = 9999;
        this.currentHp     = 9999;

        // Hitbox la picioare, similar cu lupul
        this.feetOffsetX = 4;
        this.feetOffsetY = 52;
        this.feetWidth   = 24;
        this.feetHeight  = 10;

        // Incercam sa incarcam sprite-ul gardianului.
        // Daca nu exista, gardienii vor fi desenati ca un dreptunghi colorat.
        try {
            sprite = ImageIO.read(new File("res/textures/guardian.png"));
        } catch (Exception e) {
            sprite = null;
            System.out.println("[Guardian] Sprite guardian.png nu a fost gasit — se foloseste fallback.");
        }
    }

    /*! \fn public void Update(Map map)
        \brief Gardienii nu se misca — metoda este goala intentionat.

        \param map Harta curenta (neutilizata).
    */
    public void Update(Map map) {
        // Gardienii sunt statici — nu au logica de miscare.
        tickTimers();
    }

    /*! \fn public void reactToPlayerAttack()
        \brief Apelata din Game.checkCombat() cand jucatorul loveste gardienii.

        \details
        In loc sa ia damage, gardienii aplica damage instant fatal jucatorului
        si seteaza flagul wrongEntrance.
    */
    public void reactToPlayerAttack() {
        if (targetPlayer == null || targetPlayer.isDead()) return;

        wrongEntranceTriggered = true;

        // Aplicam damage instant fatal jucatorului
        targetPlayer.takeDamage(INSTANT_KILL_DAMAGE);
    }

    /*! \fn public boolean isWrongEntranceTriggered()
        \brief Returneaza true daca jucatorul a incercat sa loveasca gardienii.
    */
    public boolean isWrongEntranceTriggered() {
        return wrongEntranceTriggered;
    }

    /*! \fn public void resetWrongEntrance()
        \brief Reseteaza flagul dupa ce ecranul de moarte a fost afisat.
    */
    public void resetWrongEntrance() {
        wrongEntranceTriggered = false;
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza gardienii pe ecran.

        \param g        Contextul grafic.
        \param cameraX  Coordonata X a camerei.
        \param cameraY  Coordonata Y a camerei.
        \param offsetX  Decalaj X al scenei.
        \param offsetY  Decalaj Y al scenei.
    */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        // Umbra
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 4, screenY + 56, 24, 8);

        if (sprite != null) {
            // Daca avem sprite, il desenam (cu flip daca e necesar)
            if (facingRight) {
                g2.drawImage(sprite, screenX, screenY, width, height, null);
            } else {
                g2.drawImage(sprite, screenX + width, screenY, screenX, screenY + height,
                        0, 0, sprite.getWidth(), sprite.getHeight(), null);
            }
        } else {
            // Fallback vizual: dreptunghi gri inchis cu contur auriu (aspect de gardian)
            g2.setColor(new Color(60, 65, 80));
            g2.fillRect(screenX + 6, screenY, 20, 56);

            // Cap
            g2.setColor(new Color(180, 150, 100));
            g2.fillOval(screenX + 8, screenY + 2, 16, 16);

            // Contur auriu (armura)
            g2.setColor(new Color(218, 165, 32));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(screenX + 6, screenY, 20, 56);

            // Sulita
            g2.setColor(new Color(150, 150, 160));
            if (facingRight) {
                g2.fillRect(screenX + 26, screenY + 10, 3, 40);
            } else {
                g2.fillRect(screenX + 3, screenY + 10, 3, 40);
            }

            g2.setStroke(new BasicStroke(1f));
        }

        // Hitboxes debug
        if (Game.showHitboxes) {
            g2.setColor(Color.CYAN);
            g2.drawRect(screenX, screenY, width, height);
            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }
}

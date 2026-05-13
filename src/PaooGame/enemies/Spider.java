package PaooGame.enemies;

import PaooGame.Animation;
import PaooGame.Entity;
import PaooGame.Game;
import PaooGame.Map;
import PaooGame.PathFinder;
import PaooGame.Player;
import PaooGame.Tiles.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/*! \class Spider
    \brief Implementeaza un inamic de tip Paianjen (Nivel 2), mostenind clasa de baza Entity.
    \details Urmareste jucatorul prin pathfinding si ataca de la distanta cu un proiectil de panza.
             Damage-ul de contact fizic este aplicat in Game.checkCombat() prin getFeetRect().
             Damage-ul proiectilului este aplicat in Game.checkCombat() prin isWebActive()/getWebX/Y().
             Flash-ul rosu la damage a fost eliminat.
             Bara de viata este randata separat prin drawHealthBarOnly() dupa DrawForeground().
 */
public class Spider extends Entity {

    private Animation animIdle; /*!< Animatia de repaus.                          */
    private Animation animRun;  /*!< Animatia de deplasare.                       */
    private Animation animWeb;  /*!< Animatia proiectilului de panza.             */

    private boolean isMoving    = false; /*!< true daca paianjenul se misca.      */
    private boolean facingRight = true;  /*!< Directia de randare a sprite-ului.  */

    private Player targetPlayer;          /*!< Referinta catre jucatorul urmarit. */
    private final int aggroRadius = 220;  /*!< Distanta maxima de detectare.      */

    /// --- PROIECTIL PANZA ---
    private float   webX, webY;             /*!< Pozitia curenta a proiectilului.   */
    private float   webVelX, webVelY;       /*!< Viteza proiectilului pe axe.       */
    private boolean webActive    = false;   /*!< true daca exista un proiectil activ. */
    private final float webSpeed = 4.0f;    /*!< Viteza de deplasare a panzei.      */
    private int attackCooldown   = 0;       /*!< Cooldown intre doua aruncari (cadre). */

    /// Damage aplicat jucatorului de fiecare proiectil de panza.
    public static final int WEB_DAMAGE = 8;

    /// Damage aplicat la contact fizic direct (corp la corp).
    public static final int ATTACK_DAMAGE = 8;

    /// Configurare decupare pentru panza din sheet (grila 32x32).
    private static final int WEB_ROW       = 7; /*!< Randul din sheet unde este animatia panzei. */
    private static final int WEB_COL_START = 0; /*!< Coloana de start a animatiei panzei.       */
    private static final int WEB_FRAMES    = 6; /*!< Numarul de cadre al animatiei panzei.       */

    /// --- Pathfinding ---
    private List<Point> path;
    private int pathCooldown  = 0;
    private int lastTargetRow = -1;
    private int lastTargetCol = -1;

    /// HP maxim al paianjenului.
    private static final int SPIDER_MAX_HP = 50;

    /*! \fn public Spider(float x, float y, Player targetPlayer)
        \brief Constructor — initializeaza HP, viteza, hitbox si animatiile paianjenului.
     */
    public Spider(float x, float y, Player targetPlayer) {
        super(x, y, 32, 32);

        this.speed        = 1.6f;
        this.targetPlayer = targetPlayer;
        this.maxHp        = SPIDER_MAX_HP;
        this.currentHp    = SPIDER_MAX_HP;

        this.feetOffsetX = 6;
        this.feetOffsetY = 22;
        this.feetWidth   = 20;
        this.feetHeight  = 10;

        try {
            BufferedImage sheet = ImageIO.read(new File("res/textures/Spider Sprite Sheet.png"));

            int frameWidth  = 32;
            int frameHeight = 32;

            int idleRow    = 0, idleFrames = 6;
            int runRow     = 1, runFrames  = 6;

            /// 1. ANIMATIA IDLE
            BufferedImage[] framesIdle = new BufferedImage[idleFrames];
            for (int i = 0; i < idleFrames; i++)
                framesIdle[i] = sheet.getSubimage(i * frameWidth, idleRow * frameHeight, frameWidth, frameHeight);
            animIdle = new Animation(160, framesIdle);

            /// 2. ANIMATIA RUN
            BufferedImage[] framesRun = new BufferedImage[runFrames];
            for (int i = 0; i < runFrames; i++)
                framesRun[i] = sheet.getSubimage(i * frameWidth, runRow * frameHeight, frameWidth, frameHeight);
            animRun = new Animation(90, framesRun);

            /// 3. ANIMATIA PROIECTILULUI DE PANZA
            BufferedImage[] framesWeb = new BufferedImage[WEB_FRAMES];
            for (int i = 0; i < WEB_FRAMES; i++)
                framesWeb[i] = sheet.getSubimage((WEB_COL_START + i) * frameWidth, WEB_ROW * frameHeight, frameWidth, frameHeight);
            animWeb = new Animation(70, framesWeb);

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea sprite-urilor pentru Spider!");
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  UPDATE
    // =========================================================================

    /*! \fn public void Update(Map map)
        \brief Actualizeaza pathfinding, miscare, proiectilul de panza si timer-ele de HP.
        \details Paianjenul se opreste din mers cand este la distanta de atac si lanseaza panza.
                 Damage-ul proiectilului este verificat in Game.checkCombat().
     */
    public void Update(Map map) {
        if (isDead || targetPlayer == null || map == null) return;

        tickTimers();

        float oldX = x, oldY = y;

        float dxPlayer = targetPlayer.GetFeetCenterX() - this.GetFeetCenterX();
        float dyPlayer = targetPlayer.GetFeetBottomY()  - this.GetFeetBottomY();
        float distance = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

        /// Lanseaza panza daca jucatorul este in raza de atac, nu prea aproape si cooldown-ul a expirat.
        if (distance < aggroRadius && distance > 40 && attackCooldown == 0 && !webActive) {
            attackCooldown = 60;

            float len = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);
            if (len != 0) { webVelX = (dxPlayer / len) * webSpeed; webVelY = (dyPlayer / len) * webSpeed; }
            else          { webVelX = webSpeed; webVelY = 0; }

            /// Proiectilul apare din centrul paianjenului.
            webX = this.x + width  / 2.0f;
            webY = this.y + height / 2.0f;
            webActive = true;
        }

        float xMove = 0, yMove = 0;

        /// Deplasare spre jucator prin pathfinding, doar cand nu este in raza de atac directa.
        if (distance < aggroRadius) {
            int sc = (int)(GetFeetCenterX() / Tile.TILE_WIDTH);
            int sr = (int)(GetFeetBottomY()  / Tile.TILE_HEIGHT);
            int tc = (int)(targetPlayer.GetFeetCenterX() / Tile.TILE_WIDTH);
            int tr = (int)(targetPlayer.GetFeetBottomY()  / Tile.TILE_HEIGHT);

            boolean needNew = (path == null || path.isEmpty())
                           || (tr != lastTargetRow || tc != lastTargetCol)
                           || (pathCooldown <= 0);
            if (needNew) {
                path = PathFinder.findPath(map, sr, sc, tr, tc);
                lastTargetRow = tr; lastTargetCol = tc; pathCooldown = 15;
            } else pathCooldown--;

            if (path != null && path.size() >= 2) {
                Point next = path.get(1);
                float tcx = next.x * Tile.TILE_WIDTH  + Tile.TILE_WIDTH  / 2.0f;
                float tcy = next.y * Tile.TILE_HEIGHT + Tile.TILE_HEIGHT / 2.0f;
                float dx  = tcx - GetFeetCenterX();
                float dy  = tcy - GetFeetBottomY();
                if (Math.abs(dx) > speed) { xMove = (dx > 0) ? speed : -speed; facingRight = (dx > 0); } else xMove = dx;
                if (Math.abs(dy) > speed) { yMove = (dy > 0) ? speed : -speed; } else yMove = dy;
            } else {
                if (Math.abs(dxPlayer) > speed) { xMove = (dxPlayer > 0) ? speed : -speed; facingRight = (dxPlayer > 0); }
                if (Math.abs(dyPlayer) > speed) { yMove = (dyPlayer > 0) ? speed : -speed; }
            }
        }

        isMoving = (xMove != 0 || yMove != 0);

        if (isMoving) { if (animRun  != null) animRun.tick();  }
        else          { if (animIdle != null) animIdle.tick(); }

        if (xMove != 0) { float nx = x + xMove; if (CanMoveTo(nx, y,  map)) x = nx; }
        if (yMove != 0) { float ny = y + yMove; if (CanMoveTo(x,  ny, map)) y = ny; }

        /// Daca paianjenul s-a blocat in geometrie, resetam path-ul.
        if (distance < aggroRadius) {
            if (Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f) {
                path = null; pathCooldown = 0;
            }
        }

        /// Actualizam pozitia proiectilului.
        if (webActive) {
            webX += webVelX;
            webY += webVelY;

            /// Dezactivam proiectilul daca iese din harta.
            if (webX < 0 || webY < 0 || webX > map.getPixelWidth() || webY > map.getPixelHeight())
                webActive = false;

            if (animWeb != null) animWeb.tick();
        }

        if (attackCooldown > 0) attackCooldown--;
    }

    // =========================================================================
    //  GETTERI PROIECTIL (folositi in Game.checkCombat())
    // =========================================================================

    /*! \fn public boolean isWebActive()
        \brief Returneaza true daca exista un proiectil de panza activ pe ecran.
     */
    public boolean isWebActive() { return webActive; }

    /*! \fn public float getWebX()
        \brief Returneaza coordonata X curenta a proiectilului.
     */
    public float getWebX() { return webX; }

    /*! \fn public float getWebY()
        \brief Returneaza coordonata Y curenta a proiectilului.
     */
    public float getWebY() { return webY; }

    /*! \fn public void deactivateWeb()
        \brief Dezactiveaza proiectilul curent (apelata din Game.checkCombat() la impact).
     */
    public void deactivateWeb() { webActive = false; }

    // =========================================================================
    //  DRAW
    // =========================================================================

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza paianjenul, umbra, proiectilul activ si hitbox-urile de debug.
        \details Bara de viata NU este desenata aici — randata separat prin drawHealthBarOnly()
                 dupa DrawForeground() in Game.Draw().
                 Flash-ul rosu a fost eliminat.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (isDead) return;

        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;
        Graphics2D g2 = (Graphics2D) g;

        /// Umbra 2.5D sub paianjen.
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 6, screenY + 18, 20, 8);

        int renderWidth  = 48;
        int renderHeight = 48;
        int drawX        = screenX - 8;
        int drawY        = screenY - 24;

        if (animIdle != null && animRun != null) {
            BufferedImage frame = isMoving ? animRun.getCurrentFrame() : animIdle.getCurrentFrame();

            if (facingRight) {
                g2.drawImage(frame, drawX, drawY, renderWidth, renderHeight, null);
            } else {
                g2.drawImage(frame, drawX + renderWidth, drawY, drawX, drawY + renderHeight,
                        0, 0, frame.getWidth(), frame.getHeight(), null);
            }
        } else {
            /// Fallback daca sprite-urile nu s-au incarcat.
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(screenX, screenY, width, height);
        }

        /// Desenam proiectilul de panza daca este activ.
        if (webActive) {
            int webScreenX = offsetX + (int) webX - cameraX;
            int webScreenY = offsetY + (int) webY - cameraY;

            if (animWeb != null) {
                BufferedImage webFrame = animWeb.getCurrentFrame();
                int webW = webFrame.getWidth();
                int webH = webFrame.getHeight();
                g2.drawImage(webFrame, webScreenX - webW / 2, webScreenY - webH / 2, webW, webH, null);

                if (Game.showHitboxes) {
                    g2.setColor(Color.CYAN);
                    g2.drawRect(webScreenX - webW / 2, webScreenY - webH / 2, webW, webH);
                }
            } else {
                /// Fallback proiectil: cerc alb simplu.
                int r = 6;
                g2.setColor(new Color(220, 235, 255));
                g2.fillOval(webScreenX - r, webScreenY - r, r * 2, r * 2);
            }
        }

        /// DEBUG: hitbox-uri.
        if (Game.showHitboxes) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);
            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }
}

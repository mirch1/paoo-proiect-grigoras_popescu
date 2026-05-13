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
    \details Utilizeaza pathfinding pe tile-uri pentru miscare si un proiectil animat de panza.
 */
public class Spider extends Entity {

    private Animation animIdle;      /*!< Animatia pentru starea de repaus.            */
    private Animation animRun;       /*!< Animatia pentru deplasare.                   */
    private Animation animWeb;       /*!< Animatia pentru proiectilul de panza.        */

    private boolean isMoving = false;    /*!< Daca paianjenul se afla in miscare.      */
    private boolean facingRight = true;  /*!< Directia de randare a sprite-ului.       */

    private Player targetPlayer;         /*!< Referinta catre jucatorul urmarit.       */
    private final int aggroRadius = 220; /*!< Distanta maxima de detectare.            */

    /// --- LOGICA PENTRU ATAC CU PANZA ---
    private float webX, webY;        /*!< Pozitia curenta a proiectilului.           */
    private float webVelX, webVelY;  /*!< Viteza proiectilului pe axe.               */
    private boolean webActive = false; /*!< Daca exista un proiectil activ pe ecran. */
    private final float webSpeed = 4.0f; /*!< Viteza de deplasare a panzei.          */
    private int attackCooldown = 0;      /*!< Cooldown intre doua aruncari.          */

    /// Configurare decupare pentru panza din sheet (grila 32x32).
    private static final int WEB_ROW       = 7; /*!< Randul din sheet unde este panza (0 sus). */
    private static final int WEB_COL_START = 0; /*!< Coloana de start pentru panza.           */
    private static final int WEB_FRAMES    = 6; /*!< Cate cadre consecutive are panza.        */

    /// --- Pathfinding ---
    private List<Point> path;         /*!< Path-ul curent in coordonate de tile (col,row). */
    private int pathCooldown = 0;     /*!< Cate frame-uri mai asteptam pana la recalculare. */
    private int lastTargetRow = -1;   /*!< Ultimul rand al jucatorului folosit la path. */
    private int lastTargetCol = -1;   /*!< Ultima coloana a jucatorului folosita la path. */

    /*! \fn public Spider(float x, float y, Player targetPlayer)
        \brief Constructor ce initializeaza noul inamic Spider.
     */
    public Spider(float x, float y, Player targetPlayer) {
        super(x, y, 32, 32);

        this.speed = 1.6f;
        this.targetPlayer = targetPlayer;

        /// --- HITBOX: picioarele paianjenului ---
        this.feetOffsetX = 6;
        this.feetOffsetY = 22;
        this.feetWidth   = 20;
        this.feetHeight  = 10;

        try {
            BufferedImage sheet = ImageIO.read(new File("res/textures/Spider Sprite Sheet.png"));

            int frameWidth  = 32;
            int frameHeight = 32;

            /// Randurile pentru animatiile de baza.
            int idleRow = 0;
            int runRow  = 1;

            int idleFrames = 6;
            int runFrames  = 6;

            /// 1. ANIMATIA IDLE
            BufferedImage[] framesIdle = new BufferedImage[idleFrames];
            for (int i = 0; i < idleFrames; i++) {
                framesIdle[i] = sheet.getSubimage(
                        i * frameWidth,
                        idleRow * frameHeight,
                        frameWidth,
                        frameHeight
                );
            }
            animIdle = new Animation(160, framesIdle);

            /// 2. ANIMATIA RUN
            BufferedImage[] framesRun = new BufferedImage[runFrames];
            for (int i = 0; i < runFrames; i++) {
                framesRun[i] = sheet.getSubimage(
                        i * frameWidth,
                        runRow * frameHeight,
                        frameWidth,
                        frameHeight
                );
            }
            animRun = new Animation(90, framesRun);

            /// 3. ANIMATIA PROIECTILULUI DE PANZA
            BufferedImage[] framesWeb = new BufferedImage[WEB_FRAMES];
            for (int i = 0; i < WEB_FRAMES; i++) {
                framesWeb[i] = sheet.getSubimage(
                        (WEB_COL_START + i) * frameWidth,
                        WEB_ROW * frameHeight,
                        frameWidth,
                        frameHeight
                );
            }
            animWeb = new Animation(70, framesWeb);

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea sprite-urilor pentru Spider!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(Map map)
        \brief Actualizeaza miscarea paianjenului, pathfinding-ul si proiectilul de panza.
        \param map Referinta catre harta curenta.
     */
    public void Update(Map map) {
        if (targetPlayer == null || map == null) {
            return;
        }

        float oldX = x;
        float oldY = y;

        /// 1. Distanta fata de jucator calculata din hitbox-urile reale.
        float dxPlayer = targetPlayer.GetFeetCenterX() - this.GetFeetCenterX();
        float dyPlayer = targetPlayer.GetFeetBottomY() - this.GetFeetBottomY();
        float distance = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

        float xMove = 0;
        float yMove = 0;

        /// 2. Atac cu panza: daca jucatorul este in raza, lansam proiectilul.
        if (distance < aggroRadius && distance > 40 && attackCooldown == 0 && !webActive) {
            attackCooldown = 60;

            float len = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);
            if (len != 0) {
                webVelX = (dxPlayer / len) * webSpeed;
                webVelY = (dyPlayer / len) * webSpeed;
            } else {
                webVelX = webSpeed;
                webVelY = 0;
            }

            /// Proiectilul apare din centrul logic al paianjenului.
            webX = this.x + width / 2.0f;
            webY = this.y + height / 2.0f;
            webActive = true;
        }

        /// 3. Miscarea catre jucator este facuta prin pathfinding pe tile-uri.
        if (distance < aggroRadius) {
            int startCol = (int)(GetFeetCenterX() / Tile.TILE_WIDTH);
            int startRow = (int)(GetFeetBottomY() / Tile.TILE_HEIGHT);

            int targetCol = (int)(targetPlayer.GetFeetCenterX() / Tile.TILE_WIDTH);
            int targetRow = (int)(targetPlayer.GetFeetBottomY() / Tile.TILE_HEIGHT);

            boolean needNewPath = false;

            if (path == null || path.isEmpty()) {
                needNewPath = true;
            }
            if (targetRow != lastTargetRow || targetCol != lastTargetCol) {
                needNewPath = true;
            }
            if (pathCooldown <= 0) {
                needNewPath = true;
            }

            if (needNewPath) {
                path = PathFinder.findPath(map, startRow, startCol, targetRow, targetCol);
                lastTargetRow = targetRow;
                lastTargetCol = targetCol;
                pathCooldown = 15;
            } else {
                pathCooldown--;
            }

            /// Daca avem path, urmarim urmatorul nod.
            if (path != null && path.size() >= 2) {
                Point nextTile = path.get(1);

                float targetTileCenterX = nextTile.x * Tile.TILE_WIDTH + Tile.TILE_WIDTH / 2.0f;
                float targetTileCenterY = nextTile.y * Tile.TILE_HEIGHT + Tile.TILE_HEIGHT / 2.0f;

                float dx = targetTileCenterX - GetFeetCenterX();
                float dy = targetTileCenterY - GetFeetBottomY();

                if (Math.abs(dx) > speed) {
                    xMove = (dx > 0) ? speed : -speed;
                    facingRight = (dx > 0);
                } else {
                    xMove = dx;
                }

                if (Math.abs(dy) > speed) {
                    yMove = (dy > 0) ? speed : -speed;
                } else {
                    yMove = dy;
                }

            } else {
                /// Fallback daca nu exista path disponibil.
                if (Math.abs(dxPlayer) > speed) {
                    xMove = (dxPlayer > 0) ? speed : -speed;
                    facingRight = (dxPlayer > 0);
                }
                if (Math.abs(dyPlayer) > speed) {
                    yMove = (dyPlayer > 0) ? speed : -speed;
                }
            }
        }

        isMoving = (xMove != 0 || yMove != 0);

        /// 4. Actualizam animatia curenta a paianjenului.
        if (isMoving) {
            if (animRun != null) animRun.tick();
        } else {
            if (animIdle != null) animIdle.tick();
        }

        /// 5. Aplicam miscarea efectiva cu coliziuni.
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

        /// 6. Daca paianjenul nu s-a putut misca, resetam path-ul pentru recalculare.
        if (distance < aggroRadius) {
            boolean stuck = (Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f);
            if (stuck) {
                path = null;
                pathCooldown = 0;
            }
        }

        /// 7. Actualizam proiectilul de panza.
        if (webActive) {
            webX += webVelX;
            webY += webVelY;

            /// Daca proiectilul iese din harta, il dezactivam.
            if (webX < 0 || webY < 0 ||
                    webX > map.getPixelWidth() ||
                    webY > map.getPixelHeight()) {
                webActive = false;
            }

            if (animWeb != null) {
                animWeb.tick();
            }
        }

        /// 8. Cooldown intre doua atacuri cu panza.
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza paianjenul, umbra, proiectilul si hitbox-urile de debug.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        /// Umbra 2.5D sub paianjen.
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 6, screenY + 18, 20, 8);

        BufferedImage currentFrame = null;
        if (animIdle != null && animRun != null) {
            if (isMoving) {
                currentFrame = animRun.getCurrentFrame();
            } else {
                currentFrame = animIdle.getCurrentFrame();
            }
        }

        int renderWidth  = 48;
        int renderHeight = 48;

        int drawX = screenX - 8;
        int drawY = screenY - 24;

        if (currentFrame != null) {
            if (facingRight) {
                g2.drawImage(currentFrame, drawX, drawY, renderWidth, renderHeight, null);
            } else {
                g2.drawImage(currentFrame,
                        drawX + renderWidth, drawY, drawX, drawY + renderHeight,
                        0, 0, currentFrame.getWidth(), currentFrame.getHeight(), null);
            }
        } else {
            g2.setColor(Color.MAGENTA);
            g2.fillRect(screenX, screenY, width, height);
        }

        /// Desenam proiectilul de panza daca este activ.
        if (webActive) {
            int webScreenX = offsetX + (int) webX - cameraX;
            int webScreenY = offsetY + (int) webY - cameraY;

            if (animWeb != null) {
                BufferedImage webFrame = animWeb.getCurrentFrame();

                int webRenderW = webFrame.getWidth();
                int webRenderH = webFrame.getHeight();

                float scale = 1.0f;
                webRenderW = (int)(webRenderW * scale);
                webRenderH = (int)(webRenderH * scale);

                int drawWX = webScreenX - webRenderW / 2;
                int drawWY = webScreenY - webRenderH / 2;

                g2.drawImage(webFrame, drawWX, drawWY, webRenderW, webRenderH, null);

                if (Game.showHitboxes) {
                    g2.setColor(Color.CYAN);
                    g2.drawRect(drawWX, drawWY, webRenderW, webRenderH);
                }
            } else {
                int radius = 6;
                g2.setColor(new Color(220, 235, 255));
                g2.fillOval(webScreenX - radius, webScreenY - radius, radius * 2, radius * 2);

                if (Game.showHitboxes) {
                    g2.setColor(Color.CYAN);
                    g2.drawOval(webScreenX - radius, webScreenY - radius, radius * 2, radius * 2);
                }
            }
        }

        /// DEBUG: afisam hitbox-urile.
        if (Game.showHitboxes) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }
}
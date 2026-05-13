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

/*! \class Skeleton
    \brief Implementeaza un inamic de tip Schelet (Nivel 2), mostenind clasa abstracta Entity.
    \details Utilizeaza pathfinding pe tile-uri pentru a urmari jucatorul in jurul obstacolelor.
 */
public class Skeleton extends Entity {

    private Animation animIdle;       /*!< Animatia de repaus.              */
    private Animation animRun;        /*!< Animatia de mers/alergare.       */
    private Animation animAttack;     /*!< Animatia de atac.                */

    private boolean isMoving = false; /*!< Daca scheletul se afla in miscare. */
    private boolean isAttacking = false; /*!< Daca scheletul ataca.           */
    private boolean facingRight = true;  /*!< Directia de randare.            */

    private int attackTimer = 0;      /*!< Durata atacului, in frame-uri.   */

    private Player targetPlayer;      /*!< Referinta la jucatorul urmarit.  */
    private final int aggroRadius = 250; /*!< Raza maxima de detectare.      */

    /// --- Pathfinding ---
    private List<Point> path;         /*!< Path-ul curent in coordonate de tile (col,row). */
    private int pathCooldown = 0;     /*!< Cate frame-uri mai asteptam pana la recalculare. */
    private int lastTargetRow = -1;   /*!< Ultimul rand al jucatorului folosit la path. */
    private int lastTargetCol = -1;   /*!< Ultima coloana a jucatorului folosita la path. */

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

            /// 3. INCARCARE ANIMATIE ATTACK
            BufferedImage attackSheet = ImageIO.read(new File("res/textures/Skeleton_Attack-Sheet.png"));
            int numFramesAttack = 5;
            int attackFrameWidth = attackSheet.getWidth() / numFramesAttack;
            int attackFrameHeight = attackSheet.getHeight();

            BufferedImage[] framesAttack = new BufferedImage[numFramesAttack];
            for (int i = 0; i < numFramesAttack; i++) {
                framesAttack[i] = attackSheet.getSubimage(
                        i * attackFrameWidth,
                        0,
                        attackFrameWidth,
                        attackFrameHeight
                );
            }
            animAttack = new Animation(90, framesAttack);

        } catch (Exception e) {
            System.out.println("Eroare critica: Nu s-au putut incarca fisierele pentru Skeleton!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(Map map)
        \brief Actualizeaza logica de urmarire, atac, pathfinding si animatii.
        \param map Referinta catre harta curenta.
     */
    public void Update(Map map) {
        float oldX = x;
        float oldY = y;

        /// 1. Distanta reala fata de jucator, calculata din zona picioarelor.
        float dxPlayer = targetPlayer.GetFeetCenterX() - this.GetFeetCenterX();
        float dyPlayer = targetPlayer.GetFeetBottomY() - this.GetFeetBottomY();
        float distance = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

        float xMove = 0;
        float yMove = 0;

        /// 2. Daca jucatorul este foarte aproape, scheletul intra in atac.
        if (distance < 36 && attackTimer == 0) {
            isAttacking = true;
            attackTimer = 30;
        }

        /// 3. Daca jucatorul este in raza si nu atacam, folosim pathfinding.
        if (!isAttacking && distance < aggroRadius) {

            /// Tile-ul de start este calculat din hitbox-ul real al scheletului.
            int startCol = (int)(GetFeetCenterX() / Tile.TILE_WIDTH);
            int startRow = (int)(GetFeetBottomY() / Tile.TILE_HEIGHT);

            /// Tile-ul tinta este calculat din hitbox-ul real al jucatorului.
            int targetCol = (int)(targetPlayer.GetFeetCenterX() / Tile.TILE_WIDTH);
            int targetRow = (int)(targetPlayer.GetFeetBottomY() / Tile.TILE_HEIGHT);

            boolean needNewPath = false;

            /// Recalculam path-ul daca:
            /// - nu avem unul valid,
            /// - jucatorul s-a mutat pe alt tile,
            /// - sau a expirat cooldown-ul.
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

            /// 4. Daca exista un traseu valid, mergem spre urmatorul tile din path.
            if (path != null && path.size() >= 2) {
                /// path[0] este tile-ul curent, path[1] este urmatorul nod.
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
                /// Fallback: daca nu exista path, urmarim direct jucatorul.
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

        /// 5. Tick pentru animatia curenta.
        if (isAttacking) {
            if (animAttack != null) animAttack.tick();
        } else if (isMoving) {
            if (animRun != null) animRun.tick();
        } else {
            if (animIdle != null) animIdle.tick();
        }

        /// 6. Aplicam miscarea efectiva, separata pe axe, pentru coliziuni stabile.
        if (!isAttacking) {
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

        /// 7. Daca scheletul nu s-a miscat deloc, fortam recalcularea path-ului.
        if (!isAttacking && distance < aggroRadius) {
            boolean stuck = (Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f);
            if (stuck) {
                path = null;
                pathCooldown = 0;
            }
        }

        /// 8. Timer-ul pentru atac scade in fiecare frame.
        if (attackTimer > 0) {
            attackTimer--;
            if (attackTimer == 0) {
                isAttacking = false;
            }
        }
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza scheletul, umbra si hitbox-urile de debug.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        /// Umbra 2.5D de sub personaj.
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 16, 8);

        if (animIdle != null && animRun != null && animAttack != null) {
            int renderWidth = 24;
            int renderHeight = 24;

            int drawX = screenX + 4;
            int drawY = screenY + 8;

            BufferedImage currentFrame;
            if (isAttacking) {
                currentFrame = animAttack.getCurrentFrame();
            } else if (isMoving) {
                currentFrame = animRun.getCurrentFrame();
            } else {
                currentFrame = animIdle.getCurrentFrame();
            }

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

        /// DEBUG: afisam hitbox-urile.
        if (Game.showHitboxes) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }
}
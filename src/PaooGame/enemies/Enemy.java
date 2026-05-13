package PaooGame.enemies;

import PaooGame.*;
import PaooGame.Tiles.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/*! \class Enemy
    \brief Implementeaza inamicul de tip Lup (Nivel 1), mostenind clasa abstracta Entity.
    \details Utilizeaza pathfinding pe tile-uri pentru a-l urmari pe jucator in jurul obstacolelor.
 */
public class Enemy extends Entity {

    private Animation animIdle;    /*!< Animatia pentru starea de repaus a lupului.    */
    private Animation animRun;     /*!< Animatia pentru starea de alergare/urmarire.   */
    private Animation animAttack;  /*!< Animatia pentru atac (saritura / muscatura).   */

    private boolean isMoving = false;
    private boolean isAttacking = false;
    private int attackTimer = 0;

    private boolean facingRight = true;

    private Player targetPlayer;
    private final int aggroRadius = 250;

    /// --- Pathfinding ---
    private List<Point> path;      /*!< Path-ul curent, in coordonate de tile (col,row). */
    private int pathIndex = 0;     /*!< Indexul in path al urmatorului tile tinta.      */
    private int pathCooldown = 0;  /*!< Cate frame-uri mai asteptam pana recalculam path-ul. */
    private int lastTargetRow = -1;
    private int lastTargetCol = -1;

    public Enemy(float x, float y, Player targetPlayer) {
        super(x, y, 32, 32);

        this.speed = 2.2f;
        this.targetPlayer = targetPlayer;

        this.feetOffsetX = 4;
        this.feetOffsetY = 20;
        this.feetWidth = 24;
        this.feetHeight = 12;

        try {
            int frameWidth = 32;
            int frameHeight = 32;

            BufferedImage sheet = ImageIO.read(new File("res/textures/wolf_gray_full.png"));
            int rightFacingRow = 6;

            int sitCol = 7;
            int howlCol = 10;

            BufferedImage[] framesIdle = new BufferedImage[4];
            framesIdle[0] = sheet.getSubimage(sitCol * frameWidth, rightFacingRow * frameHeight, frameWidth, frameHeight);
            framesIdle[1] = sheet.getSubimage(sitCol * frameWidth, rightFacingRow * frameHeight, frameWidth, frameHeight);
            framesIdle[2] = sheet.getSubimage(howlCol * frameWidth, rightFacingRow * frameHeight, frameWidth, frameHeight);
            framesIdle[3] = sheet.getSubimage(sitCol * frameWidth, rightFacingRow * frameHeight, frameWidth, frameHeight);
            animIdle = new Animation(700, framesIdle);

            int numberOfRunFrames = 5;
            BufferedImage[] framesRun = new BufferedImage[numberOfRunFrames];
            for(int i = 0; i < framesRun.length; i++) {
                framesRun[i] = sheet.getSubimage(i * frameWidth, rightFacingRow * frameHeight, frameWidth, frameHeight);
            }
            animRun = new Animation(90, framesRun);

            int attackRow = 7;
            int attackFramesCount = 5;
            BufferedImage[] framesAttack = new BufferedImage[attackFramesCount];
            for (int i = 0; i < attackFramesCount; i++) {
                framesAttack[i] = sheet.getSubimage(
                        i * frameWidth,
                        attackRow * frameHeight,
                        frameWidth,
                        frameHeight
                );
            }
            animAttack = new Animation(80, framesAttack);

        } catch (Exception e) {
            System.out.println("Eroare critica: Nu s-a putut incarca fisierul pentru Lup!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(Map map)
        \brief Actualizeaza logica de miscare, path-ul si animatia.
     */
    public void Update(Map map) {
        float oldX = x;
        float oldY = y;

        /// 1. Distanta pana la tinta.
        float dxPlayer = targetPlayer.GetFeetCenterX() - this.GetFeetCenterX();
        float dyPlayer = targetPlayer.GetFeetBottomY() - this.GetFeetBottomY();
        float distance = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

        /// 2. Atac de proximitate (fara pathfinding).
        if (distance < 40 && attackTimer == 0) {
            isAttacking = true;
            attackTimer = 30;
        }

        float xMove = 0;
        float yMove = 0;

        if (!isAttacking && distance < aggroRadius) {
            /// 3. Calculam tile-ul de start (sub picioarele lupului).
            int startCol = (int)(GetFeetCenterX() / Tile.TILE_WIDTH);
            int startRow = (int)(GetFeetBottomY() / Tile.TILE_HEIGHT);

            /// 4. Calculam tile-ul tinta (sub picioarele jucatorului).
            int targetCol = (int)(targetPlayer.GetFeetCenterX() / Tile.TILE_WIDTH);
            int targetRow = (int)(targetPlayer.GetFeetBottomY() / Tile.TILE_HEIGHT);

            /// 5. Recalculam path-ul doar cand este nevoie:
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
                pathIndex = 0;
                lastTargetRow = targetRow;
                lastTargetCol = targetCol;
                pathCooldown = 15; /// recalc la ~0.25 secunde la 60 FPS
            } else {
                pathCooldown--;
            }

            /// 6. Daca avem un path valid, mergem spre urmatorul tile.
            if (path != null && path.size() >= 2) {
                /// path[0] este tile-ul in care suntem acum; path[1] este urmatorul nod.
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
                /// Daca path-ul nu poate fi calculat, revenim la urmarirea simpla.
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

        /// 7. Tick animatii.
        if (isAttacking) {
            if (animAttack != null) animAttack.tick();
        } else if (isMoving) {
            if (animRun != null) animRun.tick();
        } else {
            if (animIdle != null) animIdle.tick();
        }

        /// 8. Miscare efectiva cu coliziuni.
        if (!isAttacking) {
            if (xMove != 0) {
                float newX = x + xMove;
                if (CanMoveTo(newX, y, map)) x = newX;
            }
            if (yMove != 0) {
                float newY = y + yMove;
                if (CanMoveTo(x, newY, map)) y = newY;
            }
        }

        /// 9. Daca ne-am impotmolit, resetam path-ul pentru a-l recalcula.
        if (!isAttacking && distance < aggroRadius) {
            boolean stuck = (Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f);
            if (stuck) {
                path = null;
                pathCooldown = 0;
            }
        }

        /// 10. Timer pentru atac.
        if (attackTimer > 0) {
            attackTimer--;
            if (attackTimer == 0) {
                isAttacking = false;
            }
        }
    }

    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);

        if (animIdle != null && animRun != null && animAttack != null) {
            int renderWidth = 32;
            int renderHeight = 32;

            int drawX = screenX - 1;
            int drawY = screenY;

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

        if (Game.showHitboxes) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }
}
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
    \details Utilizeaza pathfinding pe tile-uri pentru a-l urmari pe jucator.
             Atacul este bazat pe coliziunea hitbox-urilor: lupul aplica damage DOAR cand
             hitbox-ul sau se suprapune efectiv cu cel al jucatorului, nu pe un timer arbitrar.
             Flash-ul rosu la damage a fost eliminat.
             Bara de viata este randata separat prin drawHealthBarOnly(), apelata din Game.Draw()
             dupa DrawForeground() — apare deasupra copacilor.
 */
public class Enemy extends Entity {

    private Animation animIdle;    /*!< Animatia de repaus.                  */
    private Animation animRun;     /*!< Animatia de alergare/urmarire.       */
    private Animation animAttack;  /*!< Animatia de atac.                    */

    private boolean isMoving    = false;
    private boolean isAttacking = false;
    private boolean facingRight = true;

    /// Timer pentru animatia de atac — evita spam-ul vizual.
    private int attackAnimTimer = 0;

    private Player targetPlayer;
    private final int aggroRadius = 250; /*!< Distanta maxima de detectare. */

    /// --- Pathfinding ---
    private List<Point> path;
    private int pathCooldown  = 0;
    private int lastTargetRow = -1;
    private int lastTargetCol = -1;

    private static final int ENEMY_MAX_HP = 60;
    /// Damage aplicat jucatorului la fiecare contact fizic confirmat prin hitbox.
    public  static final int ATTACK_DAMAGE = 10;

    /*! \fn public Enemy(float x, float y, Player targetPlayer)
        \brief Constructor — initializeaza HP, viteza si animatiile lupului.
     */
    public Enemy(float x, float y, Player targetPlayer) {
        super(x, y, 32, 32);

        this.speed        = 2.2f;
        this.targetPlayer = targetPlayer;
        this.maxHp        = ENEMY_MAX_HP;
        this.currentHp    = ENEMY_MAX_HP;

        this.feetOffsetX = 4;
        this.feetOffsetY = 20;
        this.feetWidth   = 24;
        this.feetHeight  = 12;

        try {
            int frameWidth  = 32;
            int frameHeight = 32;

            BufferedImage sheet = ImageIO.read(new File("res/textures/wolf_gray_full.png"));
            int rightRow = 6;

            /// 1. ANIMATIA IDLE
            BufferedImage[] framesIdle = new BufferedImage[4];
            framesIdle[0] = sheet.getSubimage(7  * frameWidth, rightRow * frameHeight, frameWidth, frameHeight);
            framesIdle[1] = sheet.getSubimage(7  * frameWidth, rightRow * frameHeight, frameWidth, frameHeight);
            framesIdle[2] = sheet.getSubimage(10 * frameWidth, rightRow * frameHeight, frameWidth, frameHeight);
            framesIdle[3] = sheet.getSubimage(7  * frameWidth, rightRow * frameHeight, frameWidth, frameHeight);
            animIdle = new Animation(700, framesIdle);

            /// 2. ANIMATIA RUN
            BufferedImage[] framesRun = new BufferedImage[5];
            for (int i = 0; i < framesRun.length; i++)
                framesRun[i] = sheet.getSubimage(i * frameWidth, rightRow * frameHeight, frameWidth, frameHeight);
            animRun = new Animation(90, framesRun);

            /// 3. ANIMATIA ATTACK
            BufferedImage[] framesAttack = new BufferedImage[5];
            for (int i = 0; i < framesAttack.length; i++)
                framesAttack[i] = sheet.getSubimage(i * frameWidth, 7 * frameHeight, frameWidth, frameHeight);
            animAttack = new Animation(80, framesAttack);

        } catch (Exception e) {
            System.out.println("Eroare critica: Nu s-a putut incarca fisierul pentru Lup!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(Map map)
        \brief Actualizeaza pathfinding, miscare, animatii si timer-ele de HP.
        \details Atacul vizual (animatia) se declanseaza cand hitbox-urile se suprapun.
                 Damage-ul real este aplicat in Game.checkCombat() prin getFeetRect().
     */
    public void Update(Map map) {
        if (isDead) return;
        tickTimers();

        float oldX = x, oldY = y;

        float dxPlayer = targetPlayer.GetFeetCenterX() - this.GetFeetCenterX();
        float dyPlayer = targetPlayer.GetFeetBottomY()  - this.GetFeetBottomY();
        float distance = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

        /// Declanseaza animatia de atac cand hitbox-urile se suprapun (distanta < suma razelor).
        /// Damage-ul real vine din Game.checkCombat() — nu il aplicam aici.
        boolean touching = getFeetRect().intersects(targetPlayer.getFeetRect());
        if (touching && attackAnimTimer == 0) {
            isAttacking     = true;
            attackAnimTimer = 25;
        }
        if (attackAnimTimer > 0) {
            attackAnimTimer--;
            if (attackAnimTimer == 0) isAttacking = false;
        }

        float xMove = 0, yMove = 0;

        /// Deplasare spre jucator prin pathfinding, oprita la contact.
        if (!touching && distance < aggroRadius) {
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
            } else { pathCooldown--; }

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

        if      (isAttacking) { if (animAttack != null) animAttack.tick(); }
        else if (isMoving)    { if (animRun    != null) animRun.tick();    }
        else                  { if (animIdle   != null) animIdle.tick();   }

        if (xMove != 0) { float nx = x + xMove; if (CanMoveTo(nx, y, map)) x = nx; }
        if (yMove != 0) { float ny = y + yMove; if (CanMoveTo(x, ny, map)) y = ny; }

        /// Daca ne-am blocat in geometrie, resetam path-ul.
        if (!touching && distance < aggroRadius) {
            if (Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f) {
                path = null; pathCooldown = 0;
            }
        }
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza lupul, umbra si hitbox-urile de debug.
        \details Bara de viata NU este desenata aici — este randata separat prin
                 drawHealthBarOnly() dupa DrawForeground() in Game.Draw().
                 Flash-ul rosu a fost eliminat.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (isDead) return;

        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 18, 8);

        if (animIdle != null && animRun != null && animAttack != null) {
            int rw = 32, rh = 32, dx = screenX - 1, dy = screenY;

            BufferedImage frame;
            if      (isAttacking) frame = animAttack.getCurrentFrame();
            else if (isMoving)    frame = animRun.getCurrentFrame();
            else                  frame = animIdle.getCurrentFrame();

            if (facingRight) {
                g2.drawImage(frame, dx, dy, rw, rh, null);
            } else {
                g2.drawImage(frame, dx + rw, dy, dx, dy + rh,
                        0, 0, frame.getWidth(), frame.getHeight(), null);
            }
        } else {
            g2.setColor(Color.DARK_GRAY);
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

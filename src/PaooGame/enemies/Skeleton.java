package PaooGame.enemies;

import PaooGame.*;
import PaooGame.Tiles.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/*! \class Skeleton
    \brief Implementeaza inamicul de tip Schelet (Nivel 2), mostenind clasa abstracta Entity.
    \details Urmareste jucatorul prin pathfinding si ataca la contact fizic confirmat prin hitbox.
             Animatia de atac se declanseaza cand hitbox-urile se suprapun.
             Damage-ul real este aplicat in Game.checkCombat().
             Flash-ul rosu la damage a fost eliminat.
             Bara de viata randata separat prin drawHealthBarOnly() dupa DrawForeground().
 */
public class Skeleton extends Entity {

    private Animation animIdle;
    private Animation animRun;
    private Animation animAttack;

    private boolean isMoving    = false;
    private boolean isAttacking = false;
    private boolean facingRight = true;
    private int     attackAnimTimer = 0;

    private Player targetPlayer;
    private final int aggroRadius = 250;

    private List<Point> path;
    private int pathCooldown  = 0;
    private int lastTargetRow = -1;
    private int lastTargetCol = -1;

    private static final int SKELETON_MAX_HP = 80;
    public  static final int ATTACK_DAMAGE   = 6;

    /*! \fn public Skeleton(float x, float y, Player targetPlayer)
        \brief Constructor — initializeaza HP, viteza si animatiile scheletului.
     */
    public Skeleton(float x, float y, Player targetPlayer) {
        super(x, y, 32, 32);

        this.speed        = 1.5f;
        this.targetPlayer = targetPlayer;
        this.maxHp        = SKELETON_MAX_HP;
        this.currentHp    = SKELETON_MAX_HP;

        this.feetOffsetX = 8;
        this.feetOffsetY = 24;
        this.feetWidth   = 16;
        this.feetHeight  = 8;

        try {
            /// 1. ANIMATIA MERS
            BufferedImage walkSheet = ImageIO.read(new File("res/textures/Skeleton_walk-Sheet.png"));
            int nw = 5, wfw = walkSheet.getWidth() / nw, wfh = walkSheet.getHeight();
            BufferedImage[] fw = new BufferedImage[nw];
            for (int i = 0; i < nw; i++) fw[i] = walkSheet.getSubimage(i * wfw, 0, wfw, wfh);
            animRun = new Animation(120, fw);

            /// 2. ANIMATIA REPAUS
            BufferedImage idleSheet = ImageIO.read(new File("res/textures/Skeleton_idle-Sheet.png"));
            int ni = 5, ifw = idleSheet.getWidth() / ni, ifh = idleSheet.getHeight();
            BufferedImage[] fi = new BufferedImage[ni];
            for (int i = 0; i < ni; i++) fi[i] = idleSheet.getSubimage(i * ifw, 0, ifw, ifh);
            animIdle = new Animation(200, fi);

            /// 3. ANIMATIA ATAC
            BufferedImage attackSheet = ImageIO.read(new File("res/textures/Skeleton_Attack-Sheet.png"));
            int na = 5, afw = attackSheet.getWidth() / na, afh = attackSheet.getHeight();
            BufferedImage[] fa = new BufferedImage[na];
            for (int i = 0; i < na; i++) fa[i] = attackSheet.getSubimage(i * afw, 0, afw, afh);
            animAttack = new Animation(90, fa);

        } catch (Exception e) {
            System.out.println("Eroare critica: Nu s-au putut incarca fisierele pentru Skeleton!");
            e.printStackTrace();
        }
    }

    /*! \fn public void Update(Map map)
        \brief Actualizeaza pathfinding, miscare, animatii si timer-ele de HP.
        \details Animatia de atac porneste la contact fizic (hitbox overlap).
                 Damage-ul real este aplicat in Game.checkCombat().
     */
    public void Update(Map map) {
        if (isDead) return;
        tickTimers();

        float oldX = x, oldY = y;

        float dxPlayer = targetPlayer.GetFeetCenterX() - this.GetFeetCenterX();
        float dyPlayer = targetPlayer.GetFeetBottomY()  - this.GetFeetBottomY();
        float distance = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

        /// Atac vizual la contact fizic — damage aplicat in checkCombat().
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

        if      (isAttacking) { if (animAttack != null) animAttack.tick(); }
        else if (isMoving)    { if (animRun    != null) animRun.tick();    }
        else                  { if (animIdle   != null) animIdle.tick();   }

        if (xMove != 0) { float nx = x + xMove; if (CanMoveTo(nx, y, map)) x = nx; }
        if (yMove != 0) { float ny = y + yMove; if (CanMoveTo(x, ny, map)) y = ny; }

        if (!touching && distance < aggroRadius) {
            if (Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f) {
                path = null; pathCooldown = 0;
            }
        }
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza scheletul, umbra si hitbox-urile de debug.
        \details Bara de viata NU este desenata aici — randata separat prin drawHealthBarOnly().
                 Flash-ul rosu a fost eliminat.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (isDead) return;

        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(screenX + 8, screenY + 24, 16, 8);

        if (animIdle != null && animRun != null && animAttack != null) {
            int rw = 24, rh = 24, dx = screenX + 4, dy = screenY + 8;

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

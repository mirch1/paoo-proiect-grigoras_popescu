package PaooGame.enemies;

import PaooGame.*;
import PaooGame.Tiles.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/*! \class Malakar
    \brief Boss-ul final din Great Hall.

    \details
    Malakar mosteneste Enemy pentru a ramane compatibil cu sistemul existent:
    - getFeetRect()
    - forceDead()
    - drawHealthBarOnly(...)
    - campurile comune: hp, speed, isDead, pozitii, hitbox etc.

    Comportament:
    - urmareste jucatorul prin pathfinding;
    - are doua tipuri de atac: Attack1 si Attack2;
    - intra in faza 2 sub 50% HP;
    - reda animatie de damage si animatie de moarte;
    - afiseaza o bara speciala de boss sus pe ecran.
 */
public class Malakar extends Enemy {

    /// Animatii boss.
    private Animation animRun;
    private Animation animAttack1;
    private Animation animAttack2;
    private Animation animTakeHit;
    private Animation animDeath;

    /// Stari interne.
    private boolean isMoving    = false;
    private boolean isAttacking = false;
    private boolean facingRight = true;
    private boolean isTakingHit = false;
    private boolean isDying     = false;
    private boolean secondPhase = false;
    private boolean useAttack2  = false;

    /// Timer-e interne pentru stari vizuale.
    private int attackAnimTimer   = 0;
    private int hitAnimTimer      = 0;
    private int attackSwitchTimer = 0;
    private int deathAnimTimer    = 0;

    /// Referinta la playerul urmarit.
    private Player targetPlayer;

    /// Raza in care boss-ul incepe sa urmareasca playerul.
    private final int aggroRadius = 320;

    /// Pathfinding.
    private List<Point> path;
    private int pathCooldown  = 0;
    private int lastTargetRow = -1;
    private int lastTargetCol = -1;

    /// Statistici boss.
    private static final int MALAKAR_MAX_HP = 400;

    /// Damage de baza pentru Attack1.
    public static final int ATTACK1_DAMAGE = 12;

    /// Damage mai mare pentru Attack2.
    public static final int ATTACK2_DAMAGE = 18;

    /*!
     * \brief Cooldown scurt intre doua lovituri primite.
     * \details
     * Previne situatia in care Malakar primeste damage in fiecare frame
     * cat timp hitbox-ul de atac al jucatorului ramane activ.
     */
    private int hurtCooldown = 0;
    private static final int HURT_COOLDOWN_MAX = 18;

    /*!
     * \brief Fereastra minima intre doua atacuri pornite de boss.
     * \details
     * Ajuta animatia de attack sa se termine corect si evita restartarea ei
     * instant in fiecare frame cat timp playerul ramane in contact.
     */
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_MAX = 20;

    /*! \fn public Malakar(float x, float y, Player targetPlayer)
        \brief Constructor pentru boss-ul final.
     */
    public Malakar(float x, float y, Player targetPlayer) {
        super(x, y, targetPlayer);

        this.targetPlayer = targetPlayer;

        /*!
         * \brief Marim boss-ul pentru un fight mai spectaculos.
         * \details
         * Boss-ul devine mai impunator vizual, dar pastram hitbox-ul de picioare
         * relativ compact pentru coliziuni corecte.
         */
        this.width     = 128;
        this.height    = 128;
        this.speed     = 1.45f;
        this.maxHp     = MALAKAR_MAX_HP;
        this.currentHp = this.maxHp;

        this.feetOffsetX = 30;
        this.feetOffsetY = 84;
        this.feetWidth   = 36;
        this.feetHeight  = 16;

        try {
            /*
             * Sprite-urile lui Malakar.
             */
            BufferedImage runSheet     = ImageIO.read(new File("res/textures/Malakar_Run.png"));
            BufferedImage attack1Sheet = ImageIO.read(new File("res/textures/Malakar_Attack1.png"));
            BufferedImage attack2Sheet = ImageIO.read(new File("res/textures/Malakar_Attack2.png"));
            BufferedImage hitSheet     = ImageIO.read(new File("res/textures/Malakar_TakeHit.png"));
            BufferedImage deathSheet   = ImageIO.read(new File("res/textures/Malakar_Death.png"));

            /*
             * Daca animatia de attack pare prea rapida sau prea lenta,
             * ajusteaza doar valorile de speed de mai jos.
             */
            animRun     = loadStripAnimation(runSheet, 8,  90);
            animAttack1 = loadStripAnimation(attack1Sheet, 6, 95);
            animAttack2 = loadStripAnimation(attack2Sheet, 7, 85);
            animTakeHit = loadStripAnimation(hitSheet, 4, 110);
            animDeath   = loadStripAnimation(deathSheet, 9, 120);

        } catch (Exception e) {
            System.out.println("Eroare: Nu s-au putut incarca sprite-urile pentru Malakar.");
            e.printStackTrace();
        }
    }

    /*! \fn private Animation loadStripAnimation(BufferedImage sheet, int frames, int speed)
        \brief Incarca un sprite sheet orizontal intr-o animatie.
     */
    private Animation loadStripAnimation(BufferedImage sheet, int frames, int speed) {
        int frameWidth  = sheet.getWidth() / frames;
        int frameHeight = sheet.getHeight();

        BufferedImage[] loadedFrames = new BufferedImage[frames];
        for (int i = 0; i < frames; i++) {
            loadedFrames[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
        }

        return new Animation(speed, loadedFrames);
    }

    /*! \fn public void Update(Map map)
        \brief Actualizeaza logica boss-ului.
     */
    @Override
    public void Update(Map map) {
        if (isDead || targetPlayer == null || map == null) return;

        /*
         * Scadem cooldown-urile globale.
         */
        if (hurtCooldown > 0) {
            hurtCooldown--;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        /// Intrare in faza 2 sub 50% HP.
        if (!secondPhase && currentHp <= maxHp / 2) {
            secondPhase = true;
            this.speed = 1.9f;
        }

        /// Daca este in moarte, rulam doar animatia de death.
        if (isDying) {
            if (animDeath != null) animDeath.tick();

            if (deathAnimTimer > 0) {
                deathAnimTimer--;
            } else {
                isDying = false;
                isDead  = true;
            }
            return;
        }

        float oldX = x;
        float oldY = y;

        float dxPlayer = targetPlayer.GetFeetCenterX() - this.GetFeetCenterX();
        float dyPlayer = targetPlayer.GetFeetBottomY() - this.GetFeetBottomY();
        float distance = (float) Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer);

        boolean touching = getFeetRect().intersects(targetPlayer.getFeetRect());

        /*
         * Daca boss-ul este in animatia de hit, nu pornim un atac nou.
         */
        if (hitAnimTimer > 0) {
            hitAnimTimer--;
            if (hitAnimTimer == 0) {
                isTakingHit = false;
            }
        }

        /*
         * Pornim un atac doar daca:
         * - playerul este in contact;
         * - boss-ul nu este deja in atac;
         * - boss-ul nu este in hit reaction;
         * - cooldown-ul de atac s-a terminat.
         *
         * Astfel evitam restartarea animatiei de attack in fiecare frame.
         */
        if (touching && !isAttacking && !isTakingHit && attackCooldown == 0) {
            isAttacking = true;

            /*
             * Alternam intre Attack1 si Attack2.
             * In faza 2, Attack2 apare mai des.
             */
            useAttack2 = secondPhase && attackSwitchTimer == 0;

            attackAnimTimer   = useAttack2 ? 32 : 24;
            attackCooldown    = ATTACK_COOLDOWN_MAX;
            attackSwitchTimer = secondPhase ? 55 : 90;
        }

        if (attackSwitchTimer > 0) {
            attackSwitchTimer--;
        }

        /*
         * Cat timp animația de atac ruleaza, o lasam sa se consume complet.
         */
        if (isAttacking) {
            if (attackAnimTimer > 0) {
                attackAnimTimer--;
            }

            if (attackAnimTimer == 0) {
                isAttacking = false;
                useAttack2  = false;
            }
        }

        float xMove = 0;
        float yMove = 0;

        /*
         * Boss-ul se misca spre player doar daca:
         * - nu este deja in contact;
         * - nu este in hit animation;
         * - nu este in attack animation;
         * - playerul este in raza de aggro.
         */
        if (!touching && !isTakingHit && !isAttacking && distance < aggroRadius) {
            int sc = (int) (GetFeetCenterX() / Tile.TILE_WIDTH);
            int sr = (int) (GetFeetBottomY()  / Tile.TILE_HEIGHT);
            int tc = (int) (targetPlayer.GetFeetCenterX() / Tile.TILE_WIDTH);
            int tr = (int) (targetPlayer.GetFeetBottomY()  / Tile.TILE_HEIGHT);

            boolean needNewPath =
                    (path == null || path.isEmpty())
                            || (tr != lastTargetRow || tc != lastTargetCol)
                            || (pathCooldown <= 0);

            if (needNewPath) {
                path = PathFinder.findPath(map, sr, sc, tr, tc);
                lastTargetRow = tr;
                lastTargetCol = tc;
                pathCooldown = 12;
            } else {
                pathCooldown--;
            }

            if (path != null && path.size() >= 2) {
                Point next = path.get(1);

                float tcx = next.x * Tile.TILE_WIDTH  + Tile.TILE_WIDTH  / 2.0f;
                float tcy = next.y * Tile.TILE_HEIGHT + Tile.TILE_HEIGHT / 2.0f;

                float dx = tcx - GetFeetCenterX();
                float dy = tcy - GetFeetBottomY();

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
                /*
                 * Fallback simplu spre player daca nu exista path.
                 */
                if (Math.abs(dxPlayer) > speed) {
                    xMove = (dxPlayer > 0) ? speed : -speed;
                    facingRight = (dxPlayer > 0);
                } else {
                    xMove = dxPlayer;
                }

                if (Math.abs(dyPlayer) > speed) {
                    yMove = (dyPlayer > 0) ? speed : -speed;
                } else {
                    yMove = dyPlayer;
                }
            }
        }

        isMoving = (xMove != 0 || yMove != 0) && !isAttacking && !isTakingHit;

        /*
         * Prioritate animatii:
         * 1. Death
         * 2. TakeHit
         * 3. Attack
         * 4. Run
         */
        if (isTakingHit) {
            if (animTakeHit != null) animTakeHit.tick();
        } else if (isAttacking) {
            if (useAttack2) {
                if (animAttack2 != null) animAttack2.tick();
            } else {
                if (animAttack1 != null) animAttack1.tick();
            }
        } else {
            if (animRun != null) animRun.tick();
        }

        /*
         * Blocam miscarea cat timp ataca sau primeste hit,
         * pentru a face boss-ul mai "greu".
         */
        if (!isAttacking && !isTakingHit) {
            if (xMove != 0) {
                float nx = x + xMove;
                if (CanMoveTo(nx, y, map)) x = nx;
            }

            if (yMove != 0) {
                float ny = y + yMove;
                if (CanMoveTo(x, ny, map)) y = ny;
            }
        }

        /// Daca pare blocat, fortam recalcul de path.
        if (!touching && distance < aggroRadius) {
            if (Math.abs(x - oldX) < 0.1f && Math.abs(y - oldY) < 0.1f) {
                path = null;
                pathCooldown = 0;
            }
        }
    }

    /*! \fn public void takeDamage(int amount)
        \brief Aplica damage boss-ului si schimba starea in TakeHit sau Death.
     */
    @Override
    public void takeDamage(int amount) {
        if (isDead || isDying || amount <= 0) return;

        /*
         * Anti multi-hit:
         * daca tocmai a fost lovit, ignoram alte hit-uri pentru cateva frame-uri.
         */
        if (hurtCooldown > 0) return;

        currentHp -= amount;
        if (currentHp < 0) currentHp = 0;

        hurtCooldown = HURT_COOLDOWN_MAX;

        if (currentHp <= 0) {
            currentHp      = 0;
            isDying        = true;
            isAttacking    = false;
            isMoving       = false;
            isTakingHit    = false;
            deathAnimTimer = 70;
        } else {
            /*
             * Cand primeste hit, iesim din atacul curent pentru feedback vizual clar.
             */
            isAttacking  = false;
            useAttack2   = false;
            isTakingHit  = true;
            hitAnimTimer = 18;
        }
    }

    /*! \fn public boolean isUsingAttack2()
        \brief Returneaza true daca boss-ul reda Attack2 in acest moment.
     */
    public boolean isUsingAttack2() {
        return isAttacking && useAttack2;
    }

    /*! \fn public boolean isSecondPhase()
        \brief Returneaza true daca boss-ul a intrat in faza 2.
     */
    public boolean isSecondPhase() {
        return secondPhase;
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Deseneaza boss-ul.
     */
    @Override
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (isDead) return;

        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        /// Umbra sub boss.
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(screenX + 10, screenY + 48, 42, 12);

        /*!
         * \brief Randare mai mare pentru boss.
         * \details
         * Mărim sprite-ul ca să ocupe mai mult spațiu în sala mare și să pară
         * un boss fight real, nu un enemy normal.
         */
        int renderWidth  = 176;
        int renderHeight = 176;
        int drawX        = screenX - 44;
        int drawY        = screenY - 25;

        BufferedImage frame = null;

        if (isDying) {
            if (animDeath != null) frame = animDeath.getCurrentFrame();
        } else if (isTakingHit) {
            if (animTakeHit != null) frame = animTakeHit.getCurrentFrame();
        } else if (isAttacking) {
            if (useAttack2) {
                if (animAttack2 != null) frame = animAttack2.getCurrentFrame();
            } else {
                if (animAttack1 != null) frame = animAttack1.getCurrentFrame();
            }
        } else {
            if (animRun != null) frame = animRun.getCurrentFrame();
        }

        if (frame != null) {
            if (facingRight) {
                g2.drawImage(frame, drawX, drawY, renderWidth, renderHeight, null);
            } else {
                g2.drawImage(frame, drawX + renderWidth, drawY, drawX, drawY + renderHeight,
                        0, 0, frame.getWidth(), frame.getHeight(), null);
            }
        } else {
            /// Fallback vizual daca lipsesc sprite-urile.
            g2.setColor(new Color(90, 20, 20));
            g2.fillRect(screenX, screenY, width, height);
        }

        if (Game.showHitboxes) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX, screenY, width, height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }

    /*! \fn public void drawBossBar(Graphics2D g2d, int logicalWidth)
        \brief Deseneaza bara speciala de boss sus pe ecran.
     */
    public void drawBossBar(Graphics2D g2d, int logicalWidth) {
        if (isDead) return;

        int barW = 420;
        int barH = 18;
        int barX = (logicalWidth - barW) / 2;
        int barY = 18;

        float ratio = (float) currentHp / maxHp;
        if (ratio < 0) ratio = 0;

        g2d.setColor(new Color(20, 20, 20, 210));
        g2d.fillRoundRect(barX - 3, barY - 3, barW + 6, barH + 6, 10, 10);

        g2d.setColor(new Color(70, 10, 10, 230));
        g2d.fillRoundRect(barX, barY, barW, barH, 8, 8);

        g2d.setColor(new Color(180, 25, 25, 240));
        g2d.fillRoundRect(barX, barY, (int) (barW * ratio), barH, 8, 8);

        g2d.setColor(new Color(230, 220, 200));
        g2d.setFont(new Font("Serif", Font.BOLD, 18));
        String bossName = secondPhase ? "MALAKAR — PHASE II" : "MALAKAR";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(bossName, (logicalWidth - fm.stringWidth(bossName)) / 2, barY - 8);
    }

    /*!
     * Suprascriem bara normala de HP deasupra enemy-ului pentru a o face
     * ceva mai lata si mai usor de observat.
     */
    @Override
    public void drawHealthBarOnly(Graphics2D g2, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (isDead) return;

        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        int barWidth  = 72;
        int barHeight = 8;
        int barX      = screenX + (width - barWidth) / 2 - 12;
        int barY      = screenY + 14;

        float hpRatio = (float) currentHp / maxHp;
        if (hpRatio < 0) hpRatio = 0;

        g2.setColor(new Color(20, 20, 20, 200));
        g2.fillRoundRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2, 4, 4);

        g2.setColor(new Color(90, 10, 10, 220));
        g2.fillRoundRect(barX, barY, barWidth, barHeight, 3, 3);

        g2.setColor(new Color(190, 30, 30, 240));
        g2.fillRoundRect(barX, barY, (int) (barWidth * hpRatio), barHeight, 3, 3);
    }

    /*!
     * Fortam boss-ul in starea de mort pentru Load Game.
     */
    @Override
    public void forceDead() {
        currentHp = 0;
        isDead = true;
        isDying = false;
        isAttacking = false;
        isTakingHit = false;
        isMoving = false;
        hurtCooldown = 0;
        attackCooldown = 0;
    }
}
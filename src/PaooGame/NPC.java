package PaooGame;

import PaooGame.Graphics.ImageLoader;
import PaooGame.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NPC extends Enemy {
    public enum NPCType {
        VILLAGER,
        GUARD,
        ROYAL_GUARD
    }

    public enum NPCState {
        IDLE,
        WALK,
        ATTACK,
        DEFEATED
    }

    public static final int GUARD_ATTACK_DAMAGE = 10;

    private BufferedImage sprite;
    private NPCType type;
    private NPCState state = NPCState.IDLE;
    private Player targetPlayer;
    private int animationTick = 0;
    private int attackCooldown = 0;
    private int defeatedTimer = 0;
    private boolean attackHitThisFrame = false;

    public NPC(float x, float y, Player targetPlayer, NPCType type, String imagePath) {
        super(x, y, targetPlayer);

        this.targetPlayer = targetPlayer;
        this.type = type;
        this.sprite = trimTransparent(ImageLoader.LoadImage(imagePath));

        this.width = 32;
        this.height = 48;

        this.feetOffsetX = 12;
        this.feetOffsetY = 41;
        this.feetWidth = 8;
        this.feetHeight = 5;

        if (type == NPCType.ROYAL_GUARD) {
            this.maxHp = 80;
            this.currentHp = 80;
            this.speed = 1.3f;
        } else if (type == NPCType.GUARD) {
            this.maxHp = 80;
            this.currentHp = 80;
            this.speed = 0;
        } else {
            this.maxHp = 1;
            this.currentHp = 1;
            this.speed = 0;
        }
    }


    @Override
    public void Update(Map map) {
        animationTick++;
        attackHitThisFrame = false;

        if (state == NPCState.DEFEATED) {
            defeatedTimer++;
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (type == NPCType.VILLAGER) {
            state = NPCState.IDLE;
            return;
        }

        if (type == NPCType.GUARD) {
            state = NPCState.IDLE;
            return;
        }

        float oldX = x;
        float oldY = y;

        super.Update(map);

        boolean touchingPlayer = targetPlayer != null
                && getFeetRect().intersects(targetPlayer.getFeetRect());

        if (touchingPlayer) {
            state = NPCState.ATTACK;

            if (attackCooldown == 0) {
                attackCooldown = 35;
                attackHitThisFrame = true;
            }
        } else if (Math.abs(x - oldX) > 0.1f || Math.abs(y - oldY) > 0.1f) {
            state = NPCState.WALK;
        } else {
            state = NPCState.IDLE;
        }
    }

    @Override
    public void takeDamage(int amount) {
        if (!canBeAttacked()) {
            return;
        }

        currentHp -= amount;

        if (currentHp <= 0) {
            currentHp = 0;
            state = NPCState.DEFEATED;
            defeatedTimer = 0;
            speed = 0;
            isDead = false;
        }
    }

    public boolean canBeAttacked() {
        return type == NPCType.ROYAL_GUARD && state != NPCState.DEFEATED;
    }

    public boolean isGuardActive() {
        return type == NPCType.ROYAL_GUARD && state != NPCState.DEFEATED;
    }

    public boolean canDamagePlayer(Player player) {
        return type == NPCType.ROYAL_GUARD
                && state == NPCState.ATTACK
                && attackHitThisFrame
                && player != null
                && getFeetRect().intersects(player.getFeetRect());
    }

    @Override
    public void drawHealthBarOnly(Graphics2D g2, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (type == NPCType.ROYAL_GUARD && state != NPCState.DEFEATED) {
            super.drawHealthBarOnly(g2, cameraX, cameraY, offsetX, offsetY);
        }
    }


    @Override
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;
        Graphics2D g2 = (Graphics2D) g;

        int feetX = screenX + feetOffsetX + feetWidth / 2;
        int feetY = screenY + feetOffsetY + feetHeight;

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(feetX - 9, feetY - 3, 18, 6);

        if (sprite != null) {
            if (type == NPCType.VILLAGER) {
                int drawW = 34;
                int drawH = 46;

                int drawX = feetX - drawW / 2;
                int drawY = feetY - drawH + 8;

                g2.drawImage(sprite, drawX, drawY, drawW, drawH, null);
            } else {
                int drawW = 40;
                int drawH = 54;

                if (state == NPCState.DEFEATED) {
                    drawW = 42;
                    drawH = 30;
                }

                int drawX = feetX - drawW / 2;
                int drawY = feetY - drawH;

                g2.drawImage(sprite, drawX, drawY, drawW, drawH, null);
            }
        }

        if (Game.showHitboxes) {
            g2.setColor(Color.MAGENTA);
            g2.drawRect(screenX, screenY, width, height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);
        }
    }

    private BufferedImage trimTransparent(BufferedImage img) {
        if (img == null) {
            return null;
        }

        int minX = img.getWidth();
        int minY = img.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xff;

                if (alpha > 10) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return img;
        }

        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
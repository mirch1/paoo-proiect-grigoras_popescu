package PaooGame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyManager implements KeyListener {
    private final boolean[] keys = new boolean[256];

    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;

    public void Update() {
        up = isPressed(KeyEvent.VK_W) || isPressed(KeyEvent.VK_UP);
        down = isPressed(KeyEvent.VK_S) || isPressed(KeyEvent.VK_DOWN);
        left = isPressed(KeyEvent.VK_A) || isPressed(KeyEvent.VK_LEFT);
        right = isPressed(KeyEvent.VK_D) || isPressed(KeyEvent.VK_RIGHT);
    }

    private boolean isPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
            keys[code] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
            keys[code] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
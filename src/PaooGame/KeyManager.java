package PaooGame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/*! \class KeyManager
    \brief Gestioneaza intrarile de la tastatura, inregistrand apasarile pentru logica jocului.
 */
public class KeyManager implements KeyListener {

    private final boolean[] keys = new boolean[256]; /*!< Vector boolean ce stocheaza starea fiecarei taste (Apasata/Eliberata).*/

    public boolean up;       /*!< Flag pentru tasta W / Sageata Sus.*/
    public boolean down;     /*!< Flag pentru tasta S / Sageata Jos.*/
    public boolean left;     /*!< Flag pentru tasta A / Sageata Stanga.*/
    public boolean right;    /*!< Flag pentru tasta D / Sageata Dreapta.*/

    public boolean escape;   /*!< Flag pentru tasta ESC (Activare/Deactivare Meniu de pauza).*/
    public boolean enter;    /*!< Flag pentru tasta ENTER (Confirmarea selectiei in meniu).*/
    public boolean debug;    /*!< Flag pentru tasta H (Activare/Deactivare Hitbox-uri).*/

    public boolean attack;   /*!< Flag pentru tasta SPACE (Atac Knight).*/

    public boolean space;    /*!< Flag pentru tasta SPACE (Inchidere dialog narativ).*/

    /*! \fn public void Update()
        \brief Actualizeaza flagurile de control pe baza starii vectorului de taste.
     */
    public void Update() {
        up    = isPressed(KeyEvent.VK_W) || isPressed(KeyEvent.VK_UP);
        down  = isPressed(KeyEvent.VK_S) || isPressed(KeyEvent.VK_DOWN);
        left  = isPressed(KeyEvent.VK_A) || isPressed(KeyEvent.VK_LEFT);
        right = isPressed(KeyEvent.VK_D) || isPressed(KeyEvent.VK_RIGHT);

        escape = isPressed(KeyEvent.VK_ESCAPE);
        enter  = isPressed(KeyEvent.VK_ENTER);
        debug  = isPressed(KeyEvent.VK_H);

        /// SPACE este folosit atat pentru atac cat si pentru dialog.
        attack = isPressed(KeyEvent.VK_SPACE);
        space  = isPressed(KeyEvent.VK_SPACE);
    }

    /*! \fn private boolean isPressed(int keyCode)
        \brief Functie utilitara ce verifica starea unei taste in siguranta.
     */
    private boolean isPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    /*! \fn public void Clear()
        \brief Reseteaza toate tastele memorate ca fiind apasate.
     */
    public void Clear() {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = false;
        }

        up    = false;
        down  = false;
        left  = false;
        right = false;

        escape = false;
        enter  = false;
        debug  = false;
        attack = false;
        space  = false; /// FIX: space resetat la Clear()
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode >= 0 && keyCode < keys.length) keys[keyCode] = true;

        if (keyCode == KeyEvent.VK_W)      up     = true;
        if (keyCode == KeyEvent.VK_S)      down   = true;
        if (keyCode == KeyEvent.VK_A)      left   = true;
        if (keyCode == KeyEvent.VK_D)      right  = true;
        if (keyCode == KeyEvent.VK_ESCAPE) escape = true;
        if (keyCode == KeyEvent.VK_ENTER)  enter  = true;
        if (keyCode == KeyEvent.VK_SPACE)  { attack = true; space = true; }
        if (keyCode == KeyEvent.VK_H)      debug  = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode >= 0 && keyCode < keys.length) keys[keyCode] = false;

        if (keyCode == KeyEvent.VK_W)      up     = false;
        if (keyCode == KeyEvent.VK_S)      down   = false;
        if (keyCode == KeyEvent.VK_A)      left   = false;
        if (keyCode == KeyEvent.VK_D)      right  = false;
        if (keyCode == KeyEvent.VK_ESCAPE) escape = false;
        if (keyCode == KeyEvent.VK_ENTER)  enter  = false;
        if (keyCode == KeyEvent.VK_SPACE)  { attack = false; space = false; }
        if (keyCode == KeyEvent.VK_H)      debug  = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

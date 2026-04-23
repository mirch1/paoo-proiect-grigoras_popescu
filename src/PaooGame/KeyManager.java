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

    public boolean escape;   /*!< Flag pentru tasta ESC (Meniu de pauza).*/
    public boolean enter;    /*!< Flag pentru tasta ENTER (Confirmari Meniu).*/

    /*! \fn public void Update()
        \brief Actualizeaza flagurile de control pe baza starii vectorului de taste.
     */
    public void Update() {
        /// Mapam tastele directionale (suportam si WASD si Sageti)
        up = isPressed(KeyEvent.VK_W) || isPressed(KeyEvent.VK_UP);
        down = isPressed(KeyEvent.VK_S) || isPressed(KeyEvent.VK_DOWN);
        left = isPressed(KeyEvent.VK_A) || isPressed(KeyEvent.VK_LEFT);
        right = isPressed(KeyEvent.VK_D) || isPressed(KeyEvent.VK_RIGHT);

        /// Preluam starea pentru meniul UI suprapus
        escape = isPressed(KeyEvent.VK_ESCAPE);
        enter = isPressed(KeyEvent.VK_ENTER);
    }

    /*! \fn private boolean isPressed(int keyCode)
        \brief Functie utilitara ce previne exceptiile de tip IndexOutOfBounds.
     */
    private boolean isPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    /*! \fn public void keyPressed(KeyEvent e)
        \brief Functie apelata automat la apasarea oricarei taste fizice.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
            keys[code] = true;
        }
    }

    /*! \fn public void keyReleased(KeyEvent e)
        \brief Functie apelata automat la eliberarea oricarei taste fizice.
     */
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
            keys[code] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        /// Neimplementat, nu avem nevoie de caracterele propriu-zise tiparite.
    }
}
package PaooGame.GameWindow;

import javax.swing.*;
import java.awt.*;

/*! \class GameWindow
    \brief Implementeaza notiunea de fereastra a jocului.

    Membrul wndFrame este un obiect de tip JFrame care va avea utilitatea unei
    ferestre grafice si totodata si cea a unui container (toate elementele
    grafice vor fi continute de fereastra).
 */
public class GameWindow
{
    private JFrame  wndFrame;       /*!< fereastra principala a jocului*/
    private String  wndTitle;       /*!< titlul ferestrei*/
    private int     wndWidth;       /*!< latimea ferestrei in pixeli*/
    private int     wndHeight;      /*!< inaltimea ferestrei in pixeli*/

    private Canvas  canvas;         /*!< "panza/tablou" in care se poate desena*/

    /*! \fn GameWindow(String title, int width, int height)
            \brief Constructorul cu parametri al clasei GameWindow

            Retine proprietatile ferestrei (titlu, latime, inaltime)
            in variabilele membre deoarece vor fi necesare pe parcursul jocului.
            Crearea obiectului va trebui urmata de crearea ferestrei propriuzise
            prin apelul metodei BuildGameWindow()

            \param title Titlul ferestrei.
            \param width Latimea ferestrei in pixeli.
            \param height Inaltimea ferestrei in pixeli.
         */
    public GameWindow(String title, int width, int height){
        wndTitle    = title;    /*!< Retine titlul ferestrei.*/
        wndWidth    = width;    /*!< Retine latimea ferestrei.*/
        wndHeight   = height;   /*!< Retine inaltimea ferestrei.*/
        wndFrame    = null;     /*!< Fereastra nu este construita.*/
    }

    /*! \fn private void BuildGameWindow()
        \brief Construieste/creaza fereastra si seteaza toate proprietatile
        necesare: dimensiuni, pozitionare, operatia de inchidere si modul fullscreen.

     */
    public void BuildGameWindow()
    {
        /// Daca fereastra a mai fost construita intr-un apel anterior
        /// se renunta la apel
        if(wndFrame != null)
        {
            return;
        }
        /// Aloca memorie pentru obiectul de tip fereastra si seteaza denumirea
        /// ce apare in bara de titlu
        wndFrame = new JFrame(wndTitle);

        /// Ascunde marginile si bara de titlu a ferestrei (obligatoriu pentru Fullscreen).
        /// Aceasta comanda trebuie plasata inainte ca fereastra sa devina vizibila.
        wndFrame.setUndecorated(true);

        /// Seteaza dimensiunile ferestrei in pixeli
        wndFrame.setSize(wndWidth, wndHeight);
        /// Operatia de inchidere (garanteaza ca si programul este inchis cand fereastra dispare)
        wndFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        /// Invalideaza redimensionarea ferestrei
        wndFrame.setResizable(false);
        /// Recomand ca fereastra sa apara in centrul ecranului.
        wndFrame.setLocationRelativeTo(null);

        /// Creaza obiectul de tip canvas (panza) pe care se poate desena.
        canvas = new Canvas();
        /// Seteaza dimensiunile preferate, minime si maxime pentru canvas.
        canvas.setPreferredSize(new Dimension(wndWidth, wndHeight));
        canvas.setMaximumSize(new Dimension(wndWidth, wndHeight));
        canvas.setMinimumSize(new Dimension(wndWidth, wndHeight));

        /// Adauga obiectul canvas in fereastra
        wndFrame.add(canvas);
        /// Redimensioneaza fereastra ca tot ce contine sa poata fi afisat complet
        wndFrame.pack();

        /// Preia controlul placii video si activeaza modul Full-Screen Exclusive (FSEM).
        /// Prin aceasta metoda, fereastra noastra sare peste Window Manager-ul sistemului de operare.
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();
        try {
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(wndFrame); // Aceasta linie face fereastra vizibila automat
            } else {
                /// Daca sistemul nu suporta fullscreen exclusiv, fereastra este maximizata clasic.
                wndFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                wndFrame.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            wndFrame.setVisible(true);
        }
    }

    /*! \fn public int GetWndWidth()
        \brief Returneaza latimea ferestrei.
     */
    public int GetWndWidth()
    {
        return wndWidth;
    }

    /*! \fn public int GetWndHeight()
        \brief Returneaza inaltimea ferestrei.
     */
    public int GetWndHeight()
    {
        return wndHeight;
    }

    /*! \fn public Canvas GetCanvas()
        \brief Returneaza referinta catre canvas-ul din fereastra pe care se poate desena.
     */
    public Canvas GetCanvas() {
        return canvas;
    }
}
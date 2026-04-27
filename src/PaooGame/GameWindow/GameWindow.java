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
    /*! \fn public void BuildGameWindow()
    \brief Construieste fereastra jocului si canvas-ul pe care se deseneaza scena.

    Observatie:
    Nu mai folosim Full-Screen Exclusive Mode, deoarece acesta poate produce
    decalaje vizuale la revenirea din joc in meniul principal.
 */
    public void BuildGameWindow()
    {
        /// Daca fereastra a mai fost construita, nu o reconstruim.
        if(wndFrame != null)
        {
            return;
        }

        /*
         * Obținem monitorul principal.
         * Folosim dimensiunile configuratiei grafice, nu Full-Screen Exclusive Mode.
         */
        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        /// Eliberam orice fullscreen exclusiv ramas activ.
        if (device.getFullScreenWindow() != null) {
            device.setFullScreenWindow(null);
        }

        /// Luam dimensiunea reala a zonei ecranului.
        Rectangle screenBounds = device.getDefaultConfiguration().getBounds();

        /*
         * Actualizam latimea si inaltimea ferestrei.
         * Aceste valori sunt folosite ulterior si in Game.Draw() pentru scalare.
         */
        wndWidth = screenBounds.width;
        wndHeight = screenBounds.height;

        /// Cream fereastra principala a jocului.
        wndFrame = new JFrame(wndTitle);

        /// Eliminam bara de titlu si marginile ferestrei.
        wndFrame.setUndecorated(true);

        /// Inchiderea ferestrei inchide aplicatia.
        wndFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /// Nu permitem redimensionarea manuala.
        wndFrame.setResizable(false);

        /// Dezactivam repaint-ul automat, deoarece desenarea este controlata de game loop.
        wndFrame.setIgnoreRepaint(true);

        /*
         * IMPORTANT:
         * Nu folosim device.setFullScreenWindow(wndFrame).
         * In schimb, facem o fereastra fara margini pe tot ecranul.
         */
        wndFrame.setBounds(0, 0, wndWidth, wndHeight);

        /// Cream canvas-ul pe care va desena jocul.
        canvas = new Canvas();

        /// Dezactivam repaint-ul automat si pentru canvas.
        canvas.setIgnoreRepaint(true);

        /// Setam dimensiunile canvas-ului egale cu dimensiunile ferestrei.
        canvas.setPreferredSize(new Dimension(wndWidth, wndHeight));
        canvas.setMaximumSize(new Dimension(wndWidth, wndHeight));
        canvas.setMinimumSize(new Dimension(wndWidth, wndHeight));

        /// Folosim BorderLayout pentru ca Canvas-ul sa ocupe toata fereastra.
        wndFrame.setLayout(new BorderLayout());

        /// Adaugam canvas-ul in centrul ferestrei.
        wndFrame.add(canvas, BorderLayout.CENTER);

        /// Ajustam fereastra la dimensiunea canvas-ului.
        wndFrame.pack();

        /*
         * Dupa pack(), setam din nou bounds.
         * Uneori pack() poate recalcula dimensiunea, asa ca fortam pozitia corecta.
         */
        wndFrame.setBounds(0, 0, wndWidth, wndHeight);

        /// Afisam fereastra jocului.
        wndFrame.setVisible(true);

        /// Cerem focus pe canvas, ca tastatura sa functioneze imediat.
        canvas.requestFocus();
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



    /*! \fn public void CloseWindow()
    \brief Inchide corect fereastra jocului si elibereaza orice stare grafica ramasa activa.
 */
    public void CloseWindow()
    {
        /*
         * Eliberam orice fullscreen exclusiv ramas activ.
         * Chiar daca nu il mai folosim, pastram protectia pentru siguranta.
         */
        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        if (device.getFullScreenWindow() != null) {
            device.setFullScreenWindow(null);
        }

        /// Inchidem fereastra jocului daca exista.
        if (wndFrame != null) {
            wndFrame.dispose();
            wndFrame = null;
        }
    }
}

package PaooGame;

import java.awt.*;
import java.awt.event.KeyEvent;

/*! \class DialogBox
    \brief Caseta de dialog narativ pentru jocul Aethelgard.

    \details
    Afiseaza un panou semitransparent cu text narativ (titlu + linii de continut)
    in partea de jos a ecranului. Jocul este pauzat cat timp dialogul este activ.
    Jucatorul apasa SPACE pentru a inchide dialogul si a continua jocul.

    Utilizare:
    \code
        dialogBox.show("Titlu", "Linie 1", "Linie 2", "[ SPACE ]  Continue");
        // In Update(): if (dialogBox.isActive()) { ... return; }
        // In Draw():   dialogBox.draw(g2d, LOGICALWIDTH, LOGICALHEIGHT);
    \endcode
*/
public class DialogBox {

    /// Liniile de text afisate in caseta de dialog.
    private String[] lines;

    /// Indica daca dialogul este vizibil si activ in acest moment.
    private boolean active = false;

    // -------------------------------------------------------------------------
    //  Stiluri vizuale
    // -------------------------------------------------------------------------

    /// Culoarea fundalului semitransparent al casetei.
    private static final Color BG_COLOR     = new Color(0, 0, 0, 185);

    /// Culoarea bordurii aurii a casetei.
    private static final Color BORDER_COLOR = new Color(200, 170, 100);

    /// Culoarea titlului (prima linie din array).
    private static final Color TITLE_COLOR  = new Color(218, 165, 32);

    /// Culoarea textului obisnuit (liniile 2+).
    private static final Color TEXT_COLOR   = new Color(230, 220, 195);

    /// Fontul pentru titlu (prima linie).
    private static final Font TITLE_FONT    = new Font("Serif", Font.BOLD, 17);

    /// Fontul pentru liniile de continut.
    private static final Font TEXT_FONT     = new Font("Serif", Font.PLAIN, 15);

    // =========================================================================
    //  API PUBLIC
    // =========================================================================

    /*! \fn public void show(String... lines)
        \brief Afiseaza dialogul cu liniile de text specificate.

        \details
        Prima linie este tratata ca titlu (font bold, culoare aurie).
        Urmatoarele linii sunt afisate ca text narativ obisnuit.
        Ultima linie este de obicei "[ SPACE ]  Continue" pentru hint.

        \param lines Liniile de text afisate in caseta de dialog.
    */
    public void show(String... lines) {
        this.lines  = lines;
        this.active = true;
    }

    /*! \fn public void hide()
        \brief Inchide si dezactiveaza caseta de dialog.
    */
    public void hide() {
        this.active = false;
    }

    /*! \fn public boolean isActive()
        \brief Returneaza true daca dialogul este vizibil si activ.
        \return Starea curenta a dialogului.
    */
    public boolean isActive() {
        return active;
    }

    // =========================================================================
    //  DESENARE
    // =========================================================================

    /*! \fn public void draw(Graphics g, int screenWidth, int screenHeight)
        \brief Deseneaza caseta de dialog pe ecran daca este activa.

        \details
        Caseta este pozitionata in partea de jos a ecranului, centrata orizontal.
        Prima linie este titlul (auriu, bold). Restul sunt linii de continut.
        Daca dialogul nu este activ, metoda nu face nimic.

        \param g            Contextul grafic curent.
        \param screenWidth  Latimea logica a ecranului (ex. 800).
        \param screenHeight Inaltimea logica a ecranului (ex. 600).
    */
    public void draw(Graphics g, int screenWidth, int screenHeight) {
        if (!active || lines == null || lines.length == 0) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        /// Dimensiunile si pozitia casetei de dialog.
        int boxWidth  = (int) (screenWidth * 0.80);
        int lineH     = 22;
        int padding   = 16;
        int boxHeight = padding + lines.length * lineH + padding;
        int boxX      = (screenWidth - boxWidth) / 2;
        int boxY      = screenHeight - boxHeight - 30;

        /// Fundal semitransparent.
        g2d.setColor(BG_COLOR);
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 14, 14);

        /// Bordura aurie.
        g2d.setColor(BORDER_COLOR);
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 14, 14);

        int textX = boxX + padding + 4;
        int textY = boxY + padding + 4;

        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                /// Prima linie = titlu capitol (auriu, bold).
                g2d.setFont(TITLE_FONT);
                g2d.setColor(TITLE_COLOR);
            } else {
                /// Restul liniilor = text narativ obisnuit.
                g2d.setFont(TEXT_FONT);
                g2d.setColor(TEXT_COLOR);
            }
            g2d.drawString(lines[i], textX, textY + i * lineH);
        }
    }
}

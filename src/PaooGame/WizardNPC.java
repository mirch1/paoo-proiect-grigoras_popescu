package PaooGame;

import PaooGame.Graphics.ImageLoader;

import java.awt.*;
import java.awt.image.BufferedImage;

/*! \class WizardNPC
\brief NPC pasiv, animat, folosit in nivelul 2 pentru dialog contextual.

\details
Aceasta clasa reprezinta magicianul din dungeon:
- foloseste un sprite sheet cu 8 frame-uri de idle;
- nu ataca si nu poate fi atacat;
- permite interactiunea cu jucatorul cand acesta este suficient de aproape;
- la interactiune afiseaza un dialog prin DialogBox.

Magicianul este separat de clasa NPC clasica pentru a evita
modificari mari in ierarhia actuala a jocului.
*/
public class WizardNPC {

    /// Pozitia NPC-ului in lume (coordonate world-space).
    private float x;
    private float y;

    /// Referinta la jucator, folosita pentru detectia interactiunii.
    private final Player player;

    /// Frame-urile de idle extrase din sprite sheet.
    private final BufferedImage[] idleFrames;

    /// Tick intern pentru animatie.
    private int animationTick = 0;

    /// Indexul frame-ului curent din animatia idle.
    private int animationIndex = 0;

    /// Numarul de update-uri necesare pana la schimbarea frame-ului.
    private static final int ANIMATION_SPEED = 10;

    /// Dimensiunile logice ale NPC-ului.
    private final int width = 32;
    private final int height = 48;

    /// Hitbox-ul de tip "feet" pentru pozitionare/interactiune usoara.
    private final int feetOffsetX = 12;
    private final int feetOffsetY = 41;
    private final int feetWidth = 8;
    private final int feetHeight = 5;

    /// Distanta suplimentara in jurul NPC-ului in care este permisa interactiunea.
    private static final int INTERACTION_PADDING = 100;

    /// Liniile de dialog afisate cand jucatorul vorbeste cu magicianul.
    private final String[] dialogLines;

    /*! \fn public WizardNPC(float x, float y, Player player, String spriteSheetPath, String... dialogLines)
    \brief Constructorul magicianului animat.

    \param x Coordonata X in lume.
    \param y Coordonata Y in lume.
    \param player Referinta la jucator.
    \param spriteSheetPath Calea catre sprite sheet-ul cu idle.
    \param dialogLines Liniile de dialog care vor fi afisate la interactiune.
    */
    public WizardNPC(float x, float y, Player player, String spriteSheetPath, String... dialogLines) {
        this.x = x;
        this.y = y;
        this.player = player;
        this.dialogLines = dialogLines;
        this.idleFrames = loadIdleFrames(spriteSheetPath, 8);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /*! \fn public void Update()
    \brief Actualizeaza animatia idle a magicianului.

    \details
    Magicianul este un NPC pasiv, deci nu are AI de deplasare sau atac.
    Singura actualizare necesara este animatia de idle.
    */
    public void Update() {
        animationTick++;

        if (animationTick >= ANIMATION_SPEED) {
            animationTick = 0;
            animationIndex = (animationIndex + 1) % idleFrames.length;
        }
    }

    // =========================================================================
    // DESENARE
    // =========================================================================

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
    \brief Deseneaza magicianul pe ecran.

    \param g Contextul grafic curent.
    \param cameraX Offset-ul camerei pe axa X.
    \param cameraY Offset-ul camerei pe axa Y.
    \param offsetX Offset suplimentar pe X pentru randare.
    \param offsetY Offset suplimentar pe Y pentru randare.
    */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        int screenX = offsetX + (int) x - cameraX;
        int screenY = offsetY + (int) y - cameraY;

        Graphics2D g2 = (Graphics2D) g;

        /// Punctul de referinta pentru umbra si desenare.
        int feetX = screenX + feetOffsetX + feetWidth / 2;
        int feetY = screenY + feetOffsetY + feetHeight;

        /// Umbra subtila sub personaj.
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(feetX - 9, feetY - 3, 18, 6);

        BufferedImage frame = idleFrames[animationIndex];
        if (frame != null) {
            int drawW = 42;
            int drawH = 54;
            int drawX = feetX - drawW / 2;
            int drawY = feetY - drawH;

            g2.drawImage(frame, drawX, drawY, drawW, drawH, null);
        }

        /// In debug mode desenam hitbox-urile utile.
        if (Game.showHitboxes) {
            g2.setColor(Color.CYAN);
            g2.drawRect(screenX, screenY, width, height);

            g2.setColor(Color.BLUE);
            g2.drawRect(screenX + feetOffsetX, screenY + feetOffsetY, feetWidth, feetHeight);

            Rectangle interaction = getInteractionRect();
            g2.setColor(Color.YELLOW);
            g2.drawRect(
                    offsetX + interaction.x - cameraX,
                    offsetY + interaction.y - cameraY,
                    interaction.width,
                    interaction.height
            );
        }
    }

    // =========================================================================
    // INTERACTIUNE
    // =========================================================================

    /*! \fn public boolean canInteract()
    \brief Verifica daca jucatorul este suficient de aproape pentru dialog.

    \return true daca zona de interactiune a magicianului intersecteaza hitbox-ul
    "feet" al jucatorului.
    */
    public boolean canInteract() {
        return player != null && getInteractionRect().intersects(player.getFeetRect());
    }

    /*! \fn public void showDialog(DialogBox dialogBox)
    \brief Afiseaza dialogul magicianului in DialogBox.

    \param dialogBox Caseta de dialog folosita de joc.
    */
    public void showDialog(DialogBox dialogBox) {
        if (dialogBox != null && dialogLines != null && dialogLines.length > 0) {
            dialogBox.show(dialogLines);
        }
    }

    /*! \fn public Rectangle getFeetRect()
    \brief Returneaza hitbox-ul de "feet" al magicianului.

    \return Dreptunghiul hitbox folosit pentru aliniere/spatiu in lume.
    */
    public Rectangle getFeetRect() {
        return new Rectangle((int) x + feetOffsetX, (int) y + feetOffsetY, feetWidth, feetHeight);
    }

    /*! \fn private Rectangle getInteractionRect()
    \brief Construieste zona de interactiune din jurul magicianului.

    \details
    Jucatorul nu trebuie sa fie pixel-perfect langa NPC.
    Adaugam un padding in jurul corpului pentru un dialog mai comod.

    \return Dreptunghiul de interactiune in world-space.
    */
    private Rectangle getInteractionRect() {
        return new Rectangle(
                (int) x - INTERACTION_PADDING,
                (int) y - INTERACTION_PADDING,
                width + INTERACTION_PADDING * 2,
                height + INTERACTION_PADDING * 2
        );
    }

    // =========================================================================
    // SPRITE SHEET
    // =========================================================================

    /*! \fn private BufferedImage[] loadIdleFrames(String path, int frameCount)
    \brief Incarca si taie sprite sheet-ul in frame-uri egale pe orizontala.

    \param path Calea resursei imagine.
    \param frameCount Numarul total de frame-uri din sprite sheet.
    \return Un array cu frame-urile idle.
    */
    private BufferedImage[] loadIdleFrames(String path, int frameCount) {
        BufferedImage sheet = ImageLoader.LoadImage(path);
        BufferedImage[] frames = new BufferedImage[frameCount];

        if (sheet == null) {
            return frames;
        }

        int frameWidth = sheet.getWidth() / frameCount;
        int frameHeight = sheet.getHeight();

        for (int i = 0; i < frameCount; i++) {
            BufferedImage rawFrame = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            frames[i] = trimTransparent(rawFrame);
        }

        return frames;
    }

    /*! \fn private BufferedImage trimTransparent(BufferedImage img)
    \brief Decupeaza marginile transparente ale unei imagini.

    \details
    Procedeaza identic cu utilitarul folosit in NPC.java:
    cauta zona minimala care contine pixeli vizibili si intoarce
    subimaginea rezultata.

    \param img Imaginea sursa.
    \return Imaginea decupata sau originala daca nu exista transparenta utila.
    */
    private BufferedImage trimTransparent(BufferedImage img) {
        if (img == null) {
            return null;
        }

        int minX = img.getWidth();
        int minY = img.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int yy = 0; yy < img.getHeight(); yy++) {
            for (int xx = 0; xx < img.getWidth(); xx++) {
                int alpha = (img.getRGB(xx, yy) >> 24) & 0xff;

                if (alpha > 10) {
                    minX = Math.min(minX, xx);
                    minY = Math.min(minY, yy);
                    maxX = Math.max(maxX, xx);
                    maxY = Math.max(maxY, yy);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return img;
        }

        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}

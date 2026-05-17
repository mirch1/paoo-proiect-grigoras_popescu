package PaooGame.Graphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import PaooGame.Exceptions.ResourceLoadException;
import static java.lang.System.exit;

/*! \class public class ImageLoader
    \brief Clasa ce contine o metoda statica pentru incarcarea unei imagini in memorie.
 */
public class ImageLoader
{
    public static BufferedImage LoadImage(String path) {
        try {
            BufferedImage image = ImageIO.read(ImageLoader.class.getResource(path));

            if (image == null) {
                throw new ResourceLoadException("Imaginea nu a putut fi incarcata: " + path);
            }

            return image;

        } catch (ResourceLoadException e) {
            throw e;

        } catch (Exception e) {
            throw new ResourceLoadException("Eroare la incarcarea imaginii: " + path, e);
        }
    }
}



package PaooGame.Exceptions;

/*! \class ResourceLoadException
    \brief Exceptie aruncata cand o resursa grafica nu poate fi incarcata.

    \details
    Se foloseste pentru imagini, spritesheet-uri, texturi sau alte fisiere
    necesare pentru randarea jocului.
 */
public class ResourceLoadException extends GameException {
    public ResourceLoadException(String message) {
        super(message);
    }

    public ResourceLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
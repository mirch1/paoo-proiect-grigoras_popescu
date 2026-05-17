package PaooGame.Exceptions;

/*! \class MapLoadException
    \brief Exceptie aruncata cand o harta nu poate fi incarcata corect.

    \details
    Se foloseste pentru fisiere .tmx, fisiere base/foreground lipsa sau
    layere logice precum Collisions / Transitions.
 */
public class MapLoadException extends GameException {
    public MapLoadException(String message) {
        super(message);
    }

    public MapLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
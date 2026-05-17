package PaooGame.Exceptions;

/*! \class GameException
    \brief Clasa de baza pentru exceptiile specifice jocului.

    \details
    Este folosita ca parinte pentru exceptiile custom din proiect.
    Am ales RuntimeException pentru a nu modifica masiv semnaturile metodelor
    deja existente in proiect.
 */
public class GameException extends RuntimeException {
    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
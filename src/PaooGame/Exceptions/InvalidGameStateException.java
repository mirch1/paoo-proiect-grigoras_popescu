package PaooGame.Exceptions;

/*! \class InvalidGameStateException
    \brief Exceptie folosita cand jocul ajunge intr-o stare invalida.

    \details
    Exemple:
    - player null cand se incearca salvarea;
    - nivel curent invalid;
    - harta curenta neincarcata;
    - incercare de tranzitie catre un nivel inexistent.
 */
public class InvalidGameStateException extends GameException {
    public InvalidGameStateException(String message) {
        super(message);
    }

    public InvalidGameStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
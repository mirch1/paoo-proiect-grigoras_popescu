package PaooGame.Exceptions;

/*! \class AudioLoadException
    \brief Exceptie aruncata cand un fisier audio nu poate fi incarcat sau redat.

    \details
    Este utila pentru fisiere WAV lipsa, format audio incompatibil sau erori
    aparute la initializarea unui Clip audio.
 */
public class AudioLoadException extends GameException {
    public AudioLoadException(String message) {
        super(message);
    }

    public AudioLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
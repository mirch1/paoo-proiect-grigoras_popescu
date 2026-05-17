package PaooGame.Exceptions;

/*! \class DatabaseException
    \brief Exceptie aruncata la erori de acces sau operatii pe baza de date.

    \details
    Mosteneste GameException si este folosita in DatabaseManager
    pentru a semnala probleme de conectare, creare tabele sau
    operatii CRUD esecate.
 */
public class DatabaseException extends GameException {

    /*! \fn public DatabaseException(String message)
        \brief Constructor cu mesaj de eroare.
        \param message Descrierea erorii aparute.
     */
    public DatabaseException(String message) {
        super(message);
    }

    /*! \fn public DatabaseException(String message, Throwable cause)
        \brief Constructor cu mesaj si cauza originala.
        \param message Descrierea erorii.
        \param cause   Exceptia originala care a cauzat eroarea.
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
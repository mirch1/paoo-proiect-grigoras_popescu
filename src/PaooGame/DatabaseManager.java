package PaooGame;

import PaooGame.Exceptions.DatabaseException;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*! \class DatabaseManager
    \brief Gestioneaza baza de date SQLite a jocului (singleton).

    \details
    Baza de date este stocata in res/db/aethelgard.db si contine:

    Tabelul scores:
    - id          — cheie primara auto-increment
    - player_name — numele profilului (UNIC — un singur rand per jucator)
    - score       — cel mai bun scor obtinut de jucator
    - level       — nivelul maxim atins in sesiunea cu scorul maxim
    - date        — data si ora ultimei salvari cu scor mai bun (yyyy-MM-dd HH:mm)

    Comportament la save:
    - Daca jucatorul nu exista in DB → se insereaza un rand nou.
    - Daca jucatorul exista si noul scor este MAI MARE → se suprascrie.
    - Daca jucatorul exista si noul scor este mai mic sau egal → se ignora.
    Astfel, in leaderboard fiecare jucator apare o singura data, cu best score-ul sau.

    Clasa este singleton: exista o singura instanta pe parcursul rularii.
    Conexiunea se deschide la primul apel getInstance() si se inchide
    explicit prin closeConnection() sau la oprirea aplicatiei.
 */
public class DatabaseManager {

    /*! \brief Calea fisierului bazei de date SQLite. */
    private static final String DB_PATH = "res/db/aethelgard.db";

    /*! \brief URL-ul de conectare JDBC pentru SQLite. */
    private static final String DB_URL  = "jdbc:sqlite:" + DB_PATH;

    /*! \brief Instanta unica a clasei (pattern Singleton). */
    private static DatabaseManager instance;

    /*! \brief Conexiunea activa la baza de date. */
    private Connection connection;

    // =========================================================================
    //  SINGLETON
    // =========================================================================

    /*! \fn private DatabaseManager()
        \brief Constructor privat — initializeaza conexiunea si creeaza tabelele.

        \details
        Apelat o singura data, la primul getInstance().
        Daca DB-ul nu exista, SQLite il creeaza automat.

        \throws DatabaseException daca nu se poate conecta sau crea tabelele.
     */
    private DatabaseManager() throws DatabaseException {
        connect();
        createTables();
    }

    /*! \fn public static DatabaseManager getInstance()
        \brief Returneaza instanta unica a DatabaseManager.

        \details
        La primul apel, construieste instanta si deschide conexiunea.
        Apelurile ulterioare returneaza aceeasi instanta fara overhead.

        \return Instanta singleton a DatabaseManager.
        \throws DatabaseException daca initializarea esueaza.
     */
    public static DatabaseManager getInstance() throws DatabaseException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // =========================================================================
    //  CONEXIUNE
    // =========================================================================

    /*! \fn private void connect()
        \brief Deschide conexiunea JDBC la baza de date SQLite.

        \details
        Creeaza folderul res/db/ daca nu exista.
        SQLite creeaza automat fisierul .db la prima conectare.
        Activeaza WAL mode pentru performanta mai buna la scrieri concurente.

        \throws DatabaseException daca driverul lipseste sau conexiunea esueaza.
     */
    private void connect() throws DatabaseException {
        try {
            /// Cream folderul pentru baza de date daca nu exista inca.
            java.io.File dbFolder = new java.io.File("res/db");
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }

            /// Incarcam explicit driverul SQLite pentru compatibilitate cu toate JVM-urile.
            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(DB_URL);

            /// Activam WAL mode pentru performanta mai buna la scrieri concurente.
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
            }

            System.out.println("DatabaseManager: conexiune SQLite deschisa la " + DB_PATH);

        } catch (ClassNotFoundException e) {
            throw new DatabaseException(
                    "Driverul SQLite (sqlite-jdbc) nu a fost gasit in classpath. " +
                    "Adauga sqlite-jdbc-*.jar in lib/.", e
            );
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Nu s-a putut deschide baza de date: " + DB_PATH, e
            );
        }
    }

    /*! \fn public void closeConnection()
        \brief Inchide conexiunea la baza de date.

        \details
        Apeleaza aceasta metoda explicit la oprirea aplicatiei (System.exit)
        pentru a evita coruptia fisierului .db pe Windows.
        Dupa apel, instanta singleton devine inutilizabila — nu mai apela getInstance().
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("DatabaseManager: conexiune SQLite inchisa.");
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager: eroare la inchiderea conexiunii: " + e.getMessage());
        }
    }

    // =========================================================================
    //  CREARE TABELE
    // =========================================================================

    /*! \fn private void createTables()
        \brief Creeaza tabelele bazei de date daca nu exista deja.

        \details
        Constrangerea UNIQUE pe player_name garanteaza ca fiecare jucator
        apare o singura data in leaderboard.
        Foloseste IF NOT EXISTS, deci este sigur sa fie apelata la fiecare pornire.

        IMPORTANT: Daca ai deja un fisier aethelgard.db fara constrangerea UNIQUE,
        sterge-l manual din res/db/ — va fi recreat automat la urmatoarea pornire.

        \throws DatabaseException daca executia SQL esueaza.
     */
    private void createTables() throws DatabaseException {
        String sql = """
            CREATE TABLE IF NOT EXISTS scores (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                player_name TEXT    NOT NULL UNIQUE,
                score       INTEGER NOT NULL DEFAULT 0,
                level       INTEGER NOT NULL DEFAULT 1,
                date        TEXT    NOT NULL
            );
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("DatabaseManager: tabelul scores verificat/creat.");
        } catch (SQLException e) {
            throw new DatabaseException("Eroare la crearea tabelelor: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    //  SCOR EXISTENT
    // =========================================================================

    /*! \fn private int getExistingScore(String playerName)
        \brief Returneaza scorul existent din DB pentru un jucator dat.

        \details
        Folosit intern de insertScore() pentru a decide daca noul scor
        il depaseste pe cel salvat anterior.
        Daca jucatorul nu are niciun rand in DB, returneaza 0.

        \param playerName Numele jucatorului cautat.
        \return Scorul salvat anterior, sau 0 daca nu exista niciun rand.
     */
    private int getExistingScore(String playerName) {
        String sql = "SELECT score FROM scores WHERE player_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("score");
                }
            }
        } catch (SQLException e) {
            /// Non-fatal: returnam 0 si lasam insertScore sa continue normal.
            System.out.println("DatabaseManager: nu s-a putut citi scorul existent: " + e.getMessage());
        }
        return 0;
    }

    // =========================================================================
    //  INSERARE / ACTUALIZARE SCOR
    // =========================================================================

    /*! \fn public void insertScore(String playerName, int score, int level)
        \brief Insereaza sau actualizeaza scorul unui jucator in leaderboard.

        \details
        Comportament:
        - Daca jucatorul nu exista in DB → INSERT (rand nou).
        - Daca jucatorul exista si noul scor este STRICT MAI MARE → UPDATE
          (suprascrie scorul, nivelul si data cu valorile noi).
        - Daca noul scor este mai mic sau egal cu cel existent → nu se face nimic,
          pentru a nu degrada best score-ul deja salvat (ex. dupa un retry).

        Foloseste sintaxa SQLite ON CONFLICT ... DO UPDATE pentru upsert atomic.
        Constrangerea UNIQUE pe player_name asigura ca exista un singur rand per jucator.

        \param playerName Numele profilului activ.
        \param score      Scorul total obtinut in sesiunea curenta.
        \param level      Nivelul maxim atins in sesiunea curenta.
        \throws DatabaseException daca operatia SQL esueaza.
     */
    public void insertScore(String playerName, int score, int level) throws DatabaseException {
        /// Normalizam numele: null devine "Anonim".
        String name = (playerName != null) ? playerName : "Anonim";

        /// Verificam scorul existent — nu suprascriem cu un scor mai mic.
        int existingScore = getExistingScore(name);
        if (score <= existingScore) {
            System.out.println("DatabaseManager: scor ignorat (" + score +
                    " <= best " + existingScore + ") pentru " + name);
            return;
        }

        /// Formatam data curenta ca string lizibil: "2026-05-17 19:30".
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        /*
         * ON CONFLICT(player_name) DO UPDATE:
         * Daca player_name exista deja (conflict UNIQUE), in loc sa arunce eroare,
         * SQLite executa UPDATE pe randul existent cu valorile noi (excluded.*).
         * Astfel, fiecare jucator are mereu un singur rand in leaderboard.
         */
        String sql = """
            INSERT INTO scores (player_name, score, level, date)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_name) DO UPDATE SET
                score = excluded.score,
                level = excluded.level,
                date  = excluded.date
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt   (2, score);
            pstmt.setInt   (3, level);
            pstmt.setString(4, date);
            pstmt.executeUpdate();

            System.out.println("DatabaseManager: scor salvat — "
                    + name + " | " + score + " pts | nivel " + level);

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Eroare la salvarea scorului pentru " + name + ": " + e.getMessage(), e
            );
        }
    }

    // =========================================================================
    //  LEADERBOARD
    // =========================================================================

    /*! \fn public List<ScoreEntry> getTopScores(int limit)
        \brief Returneaza primele <limit> scoruri din baza de date, ordonate descendent.

        \details
        Deoarece player_name este UNIQUE, fiecare jucator apare cel mult o data
        in rezultat, cu best score-ul sau.
        Rezultatele sunt sortate dupa scor descendent (cel mai mare primul).

        \param limit Numarul maxim de intrari returnate (de obicei 10).
        \return Lista de ScoreEntry ordonata dupa scor descendent.
        \throws DatabaseException daca query-ul SQL esueaza.
     */
    public List<ScoreEntry> getTopScores(int limit) throws DatabaseException {
        String sql = """
            SELECT player_name, score, level, date
            FROM scores
            ORDER BY score DESC
            LIMIT ?
            """;

        List<ScoreEntry> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new ScoreEntry(
                            rs.getString("player_name"),
                            rs.getInt   ("score"),
                            rs.getInt   ("level"),
                            rs.getString("date")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Eroare la citirea leaderboard-ului: " + e.getMessage(), e
            );
        }

        return results;
    }

    // =========================================================================
    //  CLASA INTERNA ScoreEntry
    // =========================================================================

    /*! \class ScoreEntry
        \brief Reprezinta un rand din tabelul scores al leaderboard-ului.

        \details
        Obiect imutabil (toate campurile sunt final).
        Folosit ca DTO (Data Transfer Object) intre DatabaseManager si UI.
     */
    public static class ScoreEntry {

        /*! \brief Numele jucatorului care a obtinut scorul. */
        public final String playerName;

        /*! \brief Cel mai bun scor obtinut de jucator. */
        public final int    score;

        /*! \brief Nivelul maxim atins in sesiunea cu scorul maxim. */
        public final int    level;

        /*! \brief Data si ora ultimei salvari cu scor mai bun (yyyy-MM-dd HH:mm). */
        public final String date;

        /*! \fn public ScoreEntry(String playerName, int score, int level, String date)
            \brief Constructor complet pentru un rand de leaderboard.

            \param playerName Numele jucatorului.
            \param score      Scorul obtinut.
            \param level      Nivelul atins.
            \param date       Data sesiunii.
         */
        public ScoreEntry(String playerName, int score, int level, String date) {
            this.playerName = playerName;
            this.score      = score;
            this.level      = level;
            this.date       = date;
        }
    }
}

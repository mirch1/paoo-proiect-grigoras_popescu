package PaooGame;

/*! \class PlayerProfile
    \brief Reprezinta profilul unui jucator — date persistente intre sesiuni.

    Stocheaza: numele jucatorului, nivelul curent, ultima pozitie salvata
    si preferintele de volum. Profilul este serializat/deserializat in format
    CSV pentru persistenta in fisierul "profiles.dat".

    Format CSV:  nume,nivel,playerX,playerY,musicVolume,sfxVolume
*/
public class PlayerProfile {

    private String name;         /*!< Numele jucatorului (unic, case-insensitive). */
    private int    level;        /*!< Nivelul curent al jucatorului (1, 2 sau 3). */
    private float  playerX;      /*!< Coordonata X a ultimei pozitii salvate. */
    private float  playerY;      /*!< Coordonata Y a ultimei pozitii salvate. */
    private int    musicVolume;  /*!< Volumul muzicii de fundal (0-100). */
    private int    sfxVolume;    /*!< Volumul efectelor sonore (0-100). */

    // =========================================================================
    //  CONSTRUCTORI
    // =========================================================================

    /*! \fn public PlayerProfile(String name)
        \brief Constructor minimal — creeaza un profil nou cu valori implicite.
        \param name Numele jucatorului.
    */
    public PlayerProfile(String name) {
        this.name        = name;
        this.level       = 1;
        this.playerX     = 0f;
        this.playerY     = 0f;
        this.musicVolume = 100;
        this.sfxVolume   = 100;
    }

    /*! \fn public PlayerProfile(String name, int level, float playerX, float playerY, int musicVolume, int sfxVolume)
        \brief Constructor complet — folosit la deserializare din CSV.
        \param name        Numele jucatorului.
        \param level       Nivelul curent.
        \param playerX     Coordonata X salvata.
        \param playerY     Coordonata Y salvata.
        \param musicVolume Volumul muzicii (0-100).
        \param sfxVolume   Volumul SFX (0-100).
    */
    public PlayerProfile(String name, int level, float playerX, float playerY,
                         int musicVolume, int sfxVolume) {
        this.name        = name;
        this.level       = level;
        this.playerX     = playerX;
        this.playerY     = playerY;
        this.musicVolume = musicVolume;
        this.sfxVolume   = sfxVolume;
    }

    // =========================================================================
    //  SERIALIZARE / DESERIALIZARE CSV
    // =========================================================================

    /*! \fn public String toCsv()
        \brief Serialzeaza profilul intr-o linie CSV.
        \details Format: nume,nivel,playerX,playerY,musicVolume,sfxVolume
        \return String-ul CSV reprezentand profilul.
    */
    public String toCsv() {
        return name + "," + level + "," + playerX + "," + playerY
             + "," + musicVolume + "," + sfxVolume;
    }

    /*! \fn public static PlayerProfile fromCsv(String csv)
        \brief Deserialzeaza un profil dintr-o linie CSV.
        \details Asteapta exact 6 campuri separate prin virgula.
                 Returneaza null daca linia este corupta sau are format gresit.
        \param csv Linia CSV de parsat.
        \return Profilul deserializat sau null la eroare.
    */
    public static PlayerProfile fromCsv(String csv) {
        try {
            String[] parts = csv.split(",");
            /// Validare: trebuie sa existe exact 6 campuri.
            if (parts.length < 6) return null;

            String name   = parts[0].trim();
            int    level  = Integer.parseInt(parts[1].trim());
            float  x      = Float.parseFloat(parts[2].trim());
            float  y      = Float.parseFloat(parts[3].trim());
            int    music  = Integer.parseInt(parts[4].trim());
            int    sfx    = Integer.parseInt(parts[5].trim());

            return new PlayerProfile(name, level, x, y, music, sfx);
        } catch (NumberFormatException e) {
            /// Linia este corupta — o ignoram silentios.
            return null;
        }
    }

    // =========================================================================
    //  INTEGRARE CU GAMESETTINGS
    // =========================================================================

    /*! \fn public void applyToSettings()
        \brief Aplica valorile acestui profil in GameSettings.
        \details Apelata de ProfileManager.setActive() imediat dupa selectia profilului,
                 astfel incat jocul sa porneasca cu setarile corecte ale jucatorului.
    */
    public void applyToSettings() {
        GameSettings.musicVolume = this.musicVolume;
        GameSettings.sfxVolume   = this.sfxVolume;
    }

    // =========================================================================
    //  GETTERI SI SETTERI
    // =========================================================================

    /*! \fn public String getName()
        \brief Returneaza numele jucatorului.
        \return Numele jucatorului.
    */
    public String getName() { return name; }

    /*! \fn public int getLevel()
        \brief Returneaza nivelul curent al jucatorului.
        \return Nivelul curent (1-3).
    */
    public int getLevel() { return level; }

    /*! \fn public void setLevel(int level)
        \brief Seteaza nivelul curent al jucatorului.
        \param level Noul nivel (1-3).
    */
    public void setLevel(int level) { this.level = level; }

    /*! \fn public float getPlayerX()
        \brief Returneaza coordonata X a ultimei pozitii salvate.
        \return Coordonata X in pixeli.
    */
    public float getPlayerX() { return playerX; }

    /*! \fn public float getPlayerY()
        \brief Returneaza coordonata Y a ultimei pozitii salvate.
        \return Coordonata Y in pixeli.
    */
    public float getPlayerY() { return playerY; }

    /*! \fn public void setPosition(float x, float y)
        \brief Salveaza ultima pozitie a jucatorului in profil.
        \param x Coordonata X in pixeli.
        \param y Coordonata Y in pixeli.
    */
    public void setPosition(float x, float y) {
        this.playerX = x;
        this.playerY = y;
    }

    /*! \fn public int getMusicVolume()
        \brief Returneaza volumul muzicii (0-100).
        \return Volumul muzicii.
    */
    public int getMusicVolume() { return musicVolume; }

    /*! \fn public void setMusicVolume(int v)
        \brief Seteaza volumul muzicii.
        \param v Volumul (0-100).
    */
    public void setMusicVolume(int v) { this.musicVolume = v; }

    /*! \fn public int getSfxVolume()
        \brief Returneaza volumul efectelor sonore (0-100).
        \return Volumul SFX.
    */
    public int getSfxVolume() { return sfxVolume; }

    /*! \fn public void setSfxVolume(int v)
        \brief Seteaza volumul efectelor sonore.
        \param v Volumul (0-100).
    */
    public void setSfxVolume(int v) { this.sfxVolume = v; }
}

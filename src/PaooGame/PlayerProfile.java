package PaooGame;

/*! \class PlayerProfile
    \brief Reprezinta profilul unui jucator — date persistente intre sesiuni.

    \details
    Stocheaza: numele jucatorului, nivelul curent, ultima pozitie salvata,
    preferintele de volum si cel mai bun scor obtinut vreodata.

    Format CSV: nume,nivel,playerX,playerY,musicVolume,sfxVolume,bestScore
*/
public class PlayerProfile {

    private String name;         /*!< Numele jucatorului (unic, case-insensitive). */
    private int    level;        /*!< Nivelul curent al jucatorului (1-4).         */
    private float  playerX;      /*!< Coordonata X a ultimei pozitii salvate.      */
    private float  playerY;      /*!< Coordonata Y a ultimei pozitii salvate.      */
    private int    musicVolume;  /*!< Volumul muzicii de fundal (0-100).           */
    private int    sfxVolume;    /*!< Volumul efectelor sonore (0-100).            */

    /*! \brief Cel mai bun scor obtinut vreodata de acest jucator. */
    private int bestScore;

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
        this.bestScore   = 0;
    }

    /*! \fn public PlayerProfile(String name, int level, float playerX, float playerY, int musicVolume, int sfxVolume)
        \brief Constructor de compatibilitate — folosit la deserializare din CSV vechi (6 campuri).
    */
    public PlayerProfile(String name, int level, float playerX, float playerY,
                         int musicVolume, int sfxVolume) {
        this(name, level, playerX, playerY, musicVolume, sfxVolume, 0);
    }

    /*! \fn public PlayerProfile(String name, int level, float playerX, float playerY, int musicVolume, int sfxVolume, int bestScore)
        \brief Constructor complet — folosit la deserializare din CSV cu 7 campuri.

        \param name        Numele jucatorului.
        \param level       Nivelul curent.
        \param playerX     Coordonata X salvata.
        \param playerY     Coordonata Y salvata.
        \param musicVolume Volumul muzicii (0-100).
        \param sfxVolume   Volumul SFX (0-100).
        \param bestScore   Cel mai bun scor al jucatorului.
    */
    public PlayerProfile(String name, int level, float playerX, float playerY,
                         int musicVolume, int sfxVolume, int bestScore) {
        this.name        = name;
        this.level       = level;
        this.playerX     = playerX;
        this.playerY     = playerY;
        this.musicVolume = musicVolume;
        this.sfxVolume   = sfxVolume;
        this.bestScore   = bestScore;
    }

    // =========================================================================
    //  SERIALIZARE / DESERIALIZARE CSV
    // =========================================================================

    /*! \fn public String toCsv()
        \brief Serialzeaza profilul intr-o linie CSV.
        \details Format: nume,nivel,playerX,playerY,musicVolume,sfxVolume,bestScore
        \return String-ul CSV reprezentand profilul.
    */
    public String toCsv() {
        return name + "," + level + "," + playerX + "," + playerY
                + "," + musicVolume + "," + sfxVolume + "," + bestScore;
    }

    /*! \fn public static PlayerProfile fromCsv(String csv)
        \brief Deserialzeaza un profil dintr-o linie CSV.

        \details
        Accepta atat formatul vechi (6 campuri) cat si cel nou (7 campuri cu bestScore).
        Returneaza null daca linia este corupta sau are format gresit.

        \param csv Linia CSV de parsat.
        \return Profilul deserializat sau null la eroare.
    */
    public static PlayerProfile fromCsv(String csv) {
        try {
            String[] parts = csv.split(",");
            if (parts.length < 6) return null;

            String name  = parts[0].trim();
            int    level = Integer.parseInt(parts[1].trim());
            float  x     = Float.parseFloat(parts[2].trim());
            float  y     = Float.parseFloat(parts[3].trim());
            int    music = Integer.parseInt(parts[4].trim());
            int    sfx   = Integer.parseInt(parts[5].trim());

            /// Compatibilitate cu profiluri vechi care nu aveau campul bestScore.
            int best = (parts.length >= 7) ? Integer.parseInt(parts[6].trim()) : 0;

            return new PlayerProfile(name, level, x, y, music, sfx, best);
        } catch (NumberFormatException e) {
            /// Linia este corupta — o ignoram silentios.
            return null;
        }
    }

    // =========================================================================
    //  SCOR
    // =========================================================================

    /*! \fn public int getBestScore()
        \brief Returneaza cel mai bun scor al acestui jucator.
        \return Scorul maxim obtinut vreodata.
    */
    public int getBestScore() { return bestScore; }

    /*! \fn public void setBestScore(int score)
        \brief Actualizeaza best score-ul doar daca noul scor este mai mare.
        \param score Scorul sesiunii curente de joc.
    */
    public void setBestScore(int score) {
        if (score > this.bestScore) {
            this.bestScore = score;
        }
    }

    // =========================================================================
    //  INTEGRARE CU GAMESETTINGS
    // =========================================================================

    /*! \fn public void applyToSettings()
        \brief Aplica valorile acestui profil in GameSettings.
        \details Apelata de ProfileManager.setActive() imediat dupa selectia profilului.
    */
    public void applyToSettings() {
        GameSettings.musicVolume = this.musicVolume;
        GameSettings.sfxVolume   = this.sfxVolume;
    }

    // =========================================================================
    //  GETTERI SI SETTERI
    // =========================================================================

    /*! \fn public String getName()       \brief Returneaza numele jucatorului.      */
    public String getName()   { return name; }

    /*! \fn public int getLevel()         \brief Returneaza nivelul curent (1-4).    */
    public int getLevel()     { return level; }

    /*! \fn public void setLevel(int level) \brief Seteaza nivelul curent.           */
    public void setLevel(int level) { this.level = level; }

    /*! \fn public float getPlayerX()    \brief Returneaza coordonata X salvata.     */
    public float getPlayerX() { return playerX; }

    /*! \fn public float getPlayerY()    \brief Returneaza coordonata Y salvata.     */
    public float getPlayerY() { return playerY; }

    /*! \fn public void setPosition(float x, float y) \brief Salveaza ultima pozitie. */
    public void setPosition(float x, float y) { this.playerX = x; this.playerY = y; }

    /*! \fn public int getMusicVolume()  \brief Returneaza volumul muzicii (0-100).  */
    public int getMusicVolume()   { return musicVolume; }

    /*! \fn public void setMusicVolume(int v) \brief Seteaza volumul muzicii.        */
    public void setMusicVolume(int v) { this.musicVolume = v; }

    /*! \fn public int getSfxVolume()    \brief Returneaza volumul SFX (0-100).      */
    public int getSfxVolume()     { return sfxVolume; }

    /*! \fn public void setSfxVolume(int v) \brief Seteaza volumul SFX.              */
    public void setSfxVolume(int v) { this.sfxVolume = v; }
}
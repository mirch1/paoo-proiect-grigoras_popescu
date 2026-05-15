package PaooGame;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/*! \class ProfileManager
    \brief Gestionar static al profilurilor de jucator.

    Incarca, salveaza, sterge si gestioneaza lista de profile din fisierul
    de persistenta "profiles.dat". Mentine si profilul activ curent in memorie.

    Aceasta versiune include metode de compatibilitate pentru toate apelurile
    existente deja in proiect:
    - getProfiles()
    - load()
    - addProfile(PlayerProfile)
    - save(PlayerProfile)
    - deleteProfile(String)
    - setActive(PlayerProfile)
    - getActive()
    - saveActiveProfile()
    - saveProgress(int, float, float)
    - nameExists(String)
    - createProfile(String)
*/
public class ProfileManager {

    /*! \brief Calea catre fisierul de persistenta al profilurilor. */
    private static final String PROFILES_FILE = "profiles.dat";

    /*! \brief Profilul activ curent, setat la selectia din ProfileSelectScreen. */
    public static PlayerProfile activeProfile = null;

    // =========================================================================
    //  CITIRE / SCRIERE
    // =========================================================================

    /*! \fn public static List<PlayerProfile> getProfiles()
        \brief Returneaza lista tuturor profilurilor salvate pe disc.
        \details Citeste fisierul "profiles.dat" linie cu linie si deserializeaza
                 fiecare profil. Liniile goale sau corupte sunt ignorate silentios.
        \return Lista de obiecte PlayerProfile. Lista poate fi goala, dar nu null.
    */
    public static List<PlayerProfile> getProfiles() {
        List<PlayerProfile> list = new ArrayList<>();
        File f = new File(PROFILES_FILE);

        /// Daca fisierul nu exista inca, returnam lista goala.
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                PlayerProfile p = PlayerProfile.fromCsv(line);
                /// Ignoram liniile corupte sau incomplete.
                if (p != null) list.add(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    /*! \fn public static List<PlayerProfile> load()
        \brief Alias pentru getProfiles().
        \details Compatibilitate cu Main.java si alte clase vechi care folosesc load().
        \return Lista profilurilor existente.
    */
    public static List<PlayerProfile> load() {
        return getProfiles();
    }

    /*! \fn private static void saveAllProfiles(List<PlayerProfile> profiles)
        \brief Suprascrie fisierul de persistenta cu lista curenta de profile.
        \param profiles Lista completa care trebuie scrisa pe disc.
    */
    private static void saveAllProfiles(List<PlayerProfile> profiles) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(PROFILES_FILE, false))) {
            for (PlayerProfile p : profiles) {
                pw.println(p.toCsv());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  OPERATII PROFILE
    // =========================================================================

    /*! \fn public static void addProfile(PlayerProfile profile)
        \brief Adauga un profil nou sau actualizeaza unul existent.
        \details Daca exista deja un profil cu acelasi nume (case-insensitive),
                 acesta este inlocuit. Altfel, este adaugat la finalul listei.
        \param profile Profilul de adaugat sau actualizat.
    */
    public static void addProfile(PlayerProfile profile) {
        List<PlayerProfile> list = getProfiles();
        boolean found = false;

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equalsIgnoreCase(profile.getName())) {
                list.set(i, profile);
                found = true;
                break;
            }
        }

        if (!found) list.add(profile);
        saveAllProfiles(list);
    }

    /*! \fn public static void save(PlayerProfile profile)
        \brief Alias pentru addProfile().
        \details Compatibilitate cu codul vechi care apeleaza save(profile).
        \param profile Profilul de salvat.
    */
    public static void save(PlayerProfile profile) {
        addProfile(profile);
    }

    /*! \fn public static boolean nameExists(String name)
        \brief Verifica daca exista deja un profil cu numele dat.
        \details Comparatia este case-insensitive si ignora spatiile marginale.
        \param name Numele care trebuie verificat.
        \return true daca numele exista deja, altfel false.
    */
    public static boolean nameExists(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return false;

        for (PlayerProfile p : getProfiles()) {
            if (p.getName().equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /*! \fn public static PlayerProfile createProfile(String name)
        \brief Creeaza un profil nou, il salveaza, il seteaza activ si il returneaza.
        \details Este metoda folosita de NameEntryScreen dupa validarea numelui.
                 Profilul nou porneste la nivelul 1, cu pozitie implicita si volum 100.
        \param name Numele noului jucator.
        \return Profilul creat.
    */
    public static PlayerProfile createProfile(String name) {
        PlayerProfile profile = new PlayerProfile(name.trim());
        addProfile(profile);
        setActive(profile);
        return profile;
    }

    /*! \fn public static void deleteProfile(String name)
        \brief Sterge profilul cu numele dat din fisierul de persistenta.
        \details Daca profilul sters era activ, activeProfile este resetat la null.
        \param name Numele profilului de sters.
    */
    public static void deleteProfile(String name) {
        List<PlayerProfile> list = getProfiles();
        list.removeIf(p -> p.getName().equalsIgnoreCase(name));
        saveAllProfiles(list);

        if (activeProfile != null && activeProfile.getName().equalsIgnoreCase(name)) {
            activeProfile = null;
        }
    }

    // =========================================================================
    //  PROFIL ACTIV
    // =========================================================================

    /*! \fn public static void setActive(PlayerProfile profile)
        \brief Seteaza profilul activ si aplica setarile lui in GameSettings.
        \param profile Profilul de activat. Poate fi null.
    */
    public static void setActive(PlayerProfile profile) {
        activeProfile = profile;
        if (profile != null) {
            profile.applyToSettings();
        }
    }

    /*! \fn public static PlayerProfile getActive()
        \brief Returneaza profilul activ curent.
        \return Profilul activ sau null daca nu exista unul selectat.
    */
    public static PlayerProfile getActive() {
        return activeProfile;
    }

    /*! \fn public static void saveActiveProfile()
        \brief Persista pe disc profilul activ curent.
        \details Nu face nimic daca nu exista profil activ.
    */
    public static void saveActiveProfile() {
        if (activeProfile == null) return;
        addProfile(activeProfile);
    }

    /*! \fn public static void saveProgress(int level, float playerX, float playerY)
        \brief Salveaza progresul curent in profilul activ.
        \details Actualizeaza nivelul si pozitia in activeProfile, apoi scrie
                 modificarile pe disc. Daca nu exista profil activ, metoda nu face nimic.
        \param level   Nivelul curent al jucatorului.
        \param playerX Pozitia X curenta a jucatorului.
        \param playerY Pozitia Y curenta a jucatorului.
    */
    public static void saveProgress(int level, float playerX, float playerY) {
        if (activeProfile == null) return;

        activeProfile.setLevel(level);
        activeProfile.setPosition(playerX, playerY);
        addProfile(activeProfile);
    }
}
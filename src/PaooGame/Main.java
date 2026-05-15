package PaooGame;

/*! \class Main
    \brief Punctul de intrare al aplicatiei Aethelgard.

    La pornire, incarca toate profilurile salvate din fisierul profiles.json
    prin ProfileManager, apoi decide ce ecran sa afiseze:
    - Daca nu exista niciun profil (prima rulare) → NameEntryScreen.
    - Daca exista cel putin un profil → ProfileSelectScreen.

    Toate operatiile UI sunt executate pe EDT (Event Dispatch Thread)
    prin SwingUtilities.invokeLater(), conform bunelor practici Swing.
 */
public class Main {

    /*! \fn public static void main(String[] args)
        \brief Metoda principala a aplicatiei.
        \details Incarca profilele si lanseaza ecranul corespunzator pe EDT.
        \param args Argumentele din linia de comanda (neutilizate).
     */
    public static void main(String[] args) {
        /// Incarcam profilurile salvate inainte de orice operatie UI.
        ProfileManager.load();

        /// Lansam interfata grafica pe Event Dispatch Thread.
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (ProfileManager.getProfiles().isEmpty()) {
                /// Prima rulare sau toate profilurile au fost sterse — cerem un nume nou.
                new NameEntryScreen().setVisible(true);
            } else {
                /// Exista profile salvate — afisam ecranul de selectie.
                new ProfileSelectScreen().setVisible(true);
            }
        });
    }
}

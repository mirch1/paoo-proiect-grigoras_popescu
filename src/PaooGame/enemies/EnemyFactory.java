package PaooGame.enemies;

import PaooGame.Player;

/*! \class EnemyFactory
    \brief Factory simplu pentru crearea inamicilor din joc.

    \details
    Centralizeaza instantierile pentru:
    - lup (Enemy)
    - schelet (Skeleton)
    - paianjen (Spider)
    - boss final (Malakar)

    Avantaj:
    Game.java nu mai trebuie sa cunoasca direct constructorii fiecarei clase.
 */
public class EnemyFactory {

    /*! \fn public static Enemy createWolf(float x, float y, Player player)
        \brief Creeaza un inamic de tip lup.

        \param x Pozitia X in lume.
        \param y Pozitia Y in lume.
        \param player Referinta la jucatorul urmarit.
        \return Obiect Enemy configurat ca lup.
     */
    public static Enemy createWolf(float x, float y, Player player) {
        return new Enemy(x, y, player);
    }

    /*! \fn public static Skeleton createSkeleton(float x, float y, Player player)
        \brief Creeaza un inamic de tip schelet.

        \param x Pozitia X in lume.
        \param y Pozitia Y in lume.
        \param player Referinta la jucatorul urmarit.
        \return Obiect Skeleton.
     */
    public static Skeleton createSkeleton(float x, float y, Player player) {
        return new Skeleton(x, y, player);
    }

    /*! \fn public static Spider createSpider(float x, float y, Player player)
        \brief Creeaza un inamic de tip paianjen.

        \param x Pozitia X in lume.
        \param y Pozitia Y in lume.
        \param player Referinta la jucatorul urmarit.
        \return Obiect Spider.
     */
    public static Spider createSpider(float x, float y, Player player) {
        return new Spider(x, y, player);
    }

    /*! \fn public static Malakar createMalakar(float x, float y, Player player)
        \brief Creeaza boss-ul final Malakar.

        \param x Pozitia X in lume.
        \param y Pozitia Y in lume.
        \param player Referinta la jucatorul urmarit.
        \return Obiect Malakar.
     */
    public static Malakar createMalakar(float x, float y, Player player) {
        return new Malakar(x, y, player);
    }
}
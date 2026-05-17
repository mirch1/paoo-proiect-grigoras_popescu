package PaooGame.enemies;

import PaooGame.Entity;
import PaooGame.Player;

/*! \class EnemyFactory
    \brief Creeaza inamici in functie de tipul cerut.

    \details
    Aceasta clasa implementeaza sablonul de proiectare Factory.
    In loc sa cream inamicii direct in Game.java folosind new Enemy(),
    new Skeleton(), new Spider(), centralizam constructia lor aici.

    Avantaj:
    - Game.java ramane mai curat;
    - adaugarea unui nou inamic se face mai usor;
    - avem un design pattern explicit pentru cerinta proiectului.
 */
public class EnemyFactory {

    /*! \fn public static Entity createEnemy(EnemyType type, float x, float y, Player player)
        \brief Creeaza un inamic pe baza tipului primit.

        \param type Tipul inamicului.
        \param x Coordonata X in world.
        \param y Coordonata Y in world.
        \param player Referinta catre player.
        \return Entitatea creata.
     */
    public static Entity createEnemy(EnemyType type, float x, float y, Player player) {
        switch (type) {
            case WOLF:
                return new Enemy(x, y, player);

            case SKELETON:
                return new Skeleton(x, y, player);

            case SPIDER:
                return new Spider(x, y, player);

            default:
                throw new IllegalArgumentException("Tip de inamic necunoscut: " + type);
        }
    }

    /*! \fn public static Enemy createWolf(float x, float y, Player player)
        \brief Creeaza inamicul de tip lup.
     */
    public static Enemy createWolf(float x, float y, Player player) {
        return (Enemy) createEnemy(EnemyType.WOLF, x, y, player);
    }

    /*! \fn public static Skeleton createSkeleton(float x, float y, Player player)
        \brief Creeaza inamicul de tip schelet.
     */
    public static Skeleton createSkeleton(float x, float y, Player player) {
        return (Skeleton) createEnemy(EnemyType.SKELETON, x, y, player);
    }

    /*! \fn public static Spider createSpider(float x, float y, Player player)
        \brief Creeaza inamicul de tip paianjen.
     */
    public static Spider createSpider(float x, float y, Player player) {
        return (Spider) createEnemy(EnemyType.SPIDER, x, y, player);
    }
}
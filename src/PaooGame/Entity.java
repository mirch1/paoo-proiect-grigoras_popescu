package PaooGame;

/*! \class Entity
    \brief Clasa abstracta de baza pentru toate entitatile din joc (Player, Inamici, NPC-uri).
    \details Defineste atributele fizice fundamentale (coordonate, dimensiuni, viteza) si logica universala de coliziune cu harta.
 */
public abstract class Entity {

    /// Coordonata curenta pe axa X a entitatii.
    protected float x;
    /// Coordonata curenta pe axa Y a entitatii.
    protected float y;
    /// Latimea entitatii (in pixeli).
    protected int width;
    /// Inaltimea entitatii (in pixeli).
    protected int height;
    /// Viteza de deplasare a entitatii (pixeli per cadru).
    protected float speed;

    /// --- Setari implicite pentru Hitbox-ul de coliziune (Baza picioarelor) ---

    /// Decalajul pe axa X fata de coltul din stanga-sus al sprite-ului.
    protected int feetOffsetX = 9;
    /// Decalajul pe axa Y fata de coltul din stanga-sus al sprite-ului.
    protected int feetOffsetY = 47;
    /// Latimea hitbox-ului fizic utilizat pentru coliziuni.
    protected int feetWidth = 10;
    /// Inaltimea hitbox-ului fizic utilizat pentru coliziuni.
    protected int feetHeight = 6;

    /*! \fn public Entity(float x, float y, int width, int height)
        \brief Constructorul de initializare al clasei Entity.
        \param x Pozitia initiala pe axa X.
        \param y Pozitia initiala pe axa Y.
        \param width Latimea entitatii.
        \param height Inaltimea entitatii.
     */
    public Entity(float x, float y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /*! \fn protected boolean CanMoveTo(float testX, float testY, Map map)
        \brief Verifica daca entitatea se poate deplasa la coordonatele specificate fara a declansa o coliziune.
        \details Calculeaza pozitia viitoare a hitbox-ului si verifica suprapunerea cu elementele solide (ziduri, apa, etc.) ale hartii.
        \param testX Coordonata X viitoare pe care dorim sa o testam.
        \param testY Coordonata Y viitoare pe care dorim sa o testam.
        \param map Referinta catre harta curenta pentru validarea tile-urilor solide.
        \return true daca miscarea este valida (nu exista coliziuni), false in caz contrar.
     */
    protected boolean CanMoveTo(float testX, float testY, Map map) {
        float left = testX + feetOffsetX;
        float right = testX + feetOffsetX + feetWidth - 1;
        float top = testY + feetOffsetY;
        float bottom = testY + feetOffsetY + feetHeight - 1;

        /// Previne iesirea entitatii in afara marginilor exterioare ale hartii
        if (left < 0 || top < 0 || right >= map.getPixelWidth() || bottom >= map.getPixelHeight()) {
            return false;
        }

        /// Verifica cele 4 colturi ale hitbox-ului pentru a se asigura ca niciunul nu atinge un tile solid
        return !map.isSolidAtPixel(left, top) &&
                !map.isSolidAtPixel(right, top) &&
                !map.isSolidAtPixel(left, bottom) &&
                !map.isSolidAtPixel(right, bottom);
    }

    /// Returneaza coordonata X curenta a entitatii.
    public float GetX() { return x; }

    /// Returneaza coordonata Y curenta a entitatii.
    public float GetY() { return y; }

    /// Returneaza latimea entitatii.
    public int GetWidth() { return width; }

    /// Returneaza inaltimea entitatii.
    public int GetHeight() { return height; }

    /*! \fn public void setPosition(float x, float y)
        \brief Seteaza fortat coordonatele entitatii.
        \details Utilizata in special la incarcarea nivelului pentru a repozitiona personajele fara a recrea obiectele.
        \param x Noua coordonata X.
        \param y Noua coordonata Y.
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
package PaooGame;

import PaooGame.Tiles.Tile;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/*! \class Map
    \brief Gestioneaza incarcarea, logica de coliziune si randarea hartii.
 */
public class Map {
    private int[][] mapData;
    private int rows;
    private int cols;

    /// Imaginea exportata din Tiled pentru nivelul 1.
    private BufferedImage visualMap;

    /// Matrice logica pentru coliziuni.
    /// true = solid, player-ul nu poate trece.
    /// false = liber.
    private boolean[][] collisionData;

    /// Matrice logica pentru tranzitia catre urmatorul nivel.
    /// true = zona de trecere.
    /// false = zona normala.
    private boolean[][] transitionData;

    /// Daca este true, harta se deseneaza dintr-o imagine PNG,
    /// iar logica se citeste din fisierul .tmx.
    private boolean useVisualMap;

    private BufferedImage foregroundMap;

    /*! Constructor vechi, folosit pentru hartile bazate pe .txt. */
    public Map(String filePath) {
        useVisualMap = false;
        loadMap(filePath);
    }

    /*! Constructor nou, folosit pentru harta exportata din Tiled. */
    public Map(String baseMapPath, String foregroundMapPath, String tmxPath) {
        useVisualMap = true;
        loadVisualMap(baseMapPath);
        loadForegroundMap(foregroundMapPath);
        loadLogicFromTmx(tmxPath);
    }

    public int getPixelWidth() {
        if (useVisualMap && visualMap != null) {
            return visualMap.getWidth();
        }

        return cols * Tile.TILE_WIDTH;
    }

    public int getPixelHeight() {
        if (useVisualMap && visualMap != null) {
            return visualMap.getHeight();
        }

        return rows * Tile.TILE_HEIGHT;
    }

    public Tile getTile(int row, int col) {
        if(row < 0 || row >= rows || col < 0 || col >= cols) {
            return Tile.mountainTile;
        }

        int tileId = mapData[row][col];
        Tile tile = Tile.tiles[tileId];

        if(tile == null) {
            return Tile.grassTile;
        }

        return tile;
    }

   
    /*! \fn public boolean isSolidAtPixel(float x, float y)
    \brief Verifica daca punctul dat se afla pe o zona solida.

    \details
    Pentru hartile exportate din Tiled, logica vine din layer-ul Collisions.
    Pentru hartile vechi, logica vine din proprietatea IsSolid() a tile-ului.
 */
    public boolean isSolidAtPixel(float x, float y) {
        int col = (int)(x / Tile.TILE_WIDTH);
        int row = (int)(y / Tile.TILE_HEIGHT);

        return isSolidAtTile(row, col);
    }

    /*! \fn public boolean isSolidAtTile(int row, int col)
    \brief Verifica daca tile-ul de la pozitia (row, col) este solid.

    \details
    Aceasta metoda centralizeaza logica de coliziune:
    - daca harta este exportata din Tiled, folosim matricea collisionData;
    - daca harta este veche, folosim Tile.IsSolid().
 */
    public boolean isSolidAtTile(int row, int col) {
        /// Daca iesim in afara hartii, tratam zona ca fiind solida.
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return true;
        }

        /*
         * Pentru hartile noi exportate din Tiled:
         * orice celula nenula din layer-ul Collisions este considerata solida.
         */
        if (useVisualMap) {
            return collisionData != null && collisionData[row][col];
        }

        /*
         * Pentru hartile vechi bazate pe matrice text:
         * folosim proprietatea IsSolid() a tile-ului.
         */
        return getTile(row, col).IsSolid();
    }


    public boolean isSolidRectAtPixel(float left, float top, float width, float height) {
        float right  = left + width  - 1;
        float bottom = top  + height - 1;

        return isSolidAtPixel(left,  top)
                || isSolidAtPixel(right, top)
                || isSolidAtPixel(left,  bottom)
                || isSolidAtPixel(right, bottom);
    }  

    /*! \fn public boolean isTransitionAtPixel(float x, float y)
        \brief Verifica daca punctul dat se afla pe o zona de tranzitie.
     */
    public boolean isTransitionAtPixel(float x, float y) {
        if (!useVisualMap || transitionData == null) {
            return false;
        }

        int col = (int)(x / Tile.TILE_WIDTH);
        int row = (int)(y / Tile.TILE_HEIGHT);

        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return false;
        }

        return transitionData[row][col];
    }

    /*! \fn private void loadVisualMap(String visualMapPath)
    \brief Incarca imaginea principala a hartii exportate din Tiled.

    \details
    Daca imaginea nu poate fi incarcata, tratam eroarea prin MapLoadException,
    dar o prindem local ca sa putem afisa clar problema in consola.
 */
    private void loadVisualMap(String visualMapPath) {
        try {
            File file = new File(visualMapPath);

            if (!file.exists()) {
                throw new MapLoadException("Imaginea hartii lipseste: " + visualMapPath);
            }

            visualMap = ImageIO.read(file);

            if (visualMap == null) {
                throw new MapLoadException("Fisierul hartii nu este o imagine valida: " + visualMapPath);
            }

        } catch (MapLoadException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();

            /*
             * Foarte important:
             * Nu lasam jocul sa crape, dar visualMap ramane null.
             * In consola vom vedea exact ce fisier lipseste.
             */
            visualMap = null;

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea imaginii hartii: " + visualMapPath);
            e.printStackTrace();
            visualMap = null;
        }
    }
    

    /*! \fn private void loadForegroundMap(String foregroundMapPath)
    \brief Incarca imaginea de foreground a hartii.

    \details
    Foreground-ul este optional. Daca lipseste, jocul continua fara el.
 */
    private void loadForegroundMap(String foregroundMapPath) {
        try {
            File file = new File(foregroundMapPath);

            if (!file.exists()) {
                System.out.println("Foreground lipsa, jocul continua fara el: " + foregroundMapPath);
                foregroundMap = null;
                return;
            }

            foregroundMap = ImageIO.read(file);

            if (foregroundMap == null) {
                throw new MapLoadException("Foreground-ul nu este o imagine valida: " + foregroundMapPath);
            }

        } catch (MapLoadException e) {
            System.out.println(e.getMessage());
            foregroundMap = null;

        } catch (Exception e) {
            System.out.println("Eroare la incarcarea foreground-ului: " + foregroundMapPath);
            e.printStackTrace();
            foregroundMap = null;
        }
    }
    
    public void DrawForeground(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        if (useVisualMap && foregroundMap != null) {
            g.drawImage(foregroundMap, offsetX - cameraX, offsetY - cameraY, null);
        }
    }

    /*! \fn private void loadLogicFromTmx(String tmxPath)
        \brief Citeste din fisierul .tmx layer-ele Collisions si tranzitiile.
     */
    /*! \fn private void loadLogicFromTmx(String tmxPath)
    \brief Citeste din fisierul .tmx layer-ele Collisions si tranzitiile.

    \details
    Daca TMX-ul are probleme, afisam eroarea clar, dar nu blocam complet jocul.
 */
    private void loadLogicFromTmx(String tmxPath) {
        try {
            File file = new File(tmxPath);

            if (!file.exists()) {
                throw new MapLoadException("Fisierul TMX lipseste: " + tmxPath);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            Element mapElement = doc.getDocumentElement();

            cols = Integer.parseInt(mapElement.getAttribute("width"));
            rows = Integer.parseInt(mapElement.getAttribute("height"));

            collisionData = new boolean[rows][cols];
            transitionData = new boolean[rows][cols];

            NodeList layers = doc.getElementsByTagName("layer");

            for (int i = 0; i < layers.getLength(); i++) {
                Element layer = (Element) layers.item(i);
                String layerName = layer.getAttribute("name");
                String normalizedName = layerName.trim().toLowerCase();

                if (normalizedName.equals("collisions")
                        || normalizedName.equals("collision")) {
                    collisionData = readBooleanLayer(layer);
                }

                if (normalizedName.equals("transitiontodungeon")
                        || normalizedName.equals("transitiontogreathall")
                        || normalizedName.equals("transitiontovillage")
                        || normalizedName.equals("transitions")
                        || normalizedName.equals("transition")) {
                    transitionData = readBooleanLayer(layer);
                }
            }

        } catch (MapLoadException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();

            /*
             * Daca TMX-ul nu merge, cream matrici goale ca sa nu dea NullPointerException.
             * Harta vizuala se poate desena in continuare.
             */
            if (rows > 0 && cols > 0) {
                collisionData = new boolean[rows][cols];
                transitionData = new boolean[rows][cols];
            }

        } catch (Exception e) {
            System.out.println("Eroare la citirea logicii din TMX: " + tmxPath);
            e.printStackTrace();

            if (rows > 0 && cols > 0) {
                collisionData = new boolean[rows][cols];
                transitionData = new boolean[rows][cols];
            }
        }
    }
    

    /*! \fn private boolean[][] readBooleanLayer(Element layer)
        \brief Transforma un layer din Tiled intr-o matrice boolean.
        \details Orice tile diferit de 0 devine true.
     */
    private boolean[][] readBooleanLayer(Element layer) {
        boolean[][] result = new boolean[rows][cols];

        NodeList dataNodes = layer.getElementsByTagName("data");

        if (dataNodes.getLength() == 0) {
            return result;
        }

        Element data = (Element) dataNodes.item(0);
        String encoding = data.getAttribute("encoding");

        /*
         * Varianta recomandata: layer salvat ca CSV.
         */
        if ("csv".equals(encoding)) {
            String text = data.getTextContent().trim();
            String[] values = text.split(",");

            for (int i = 0; i < values.length && i < rows * cols; i++) {
                int gid = Integer.parseInt(values[i].trim());

                int row = i / cols;
                int col = i % cols;

                result[row][col] = gid != 0;
            }

            return result;
        }

        /*
         * Varianta alternativa: layer XML cu tile-uri individuale.
         */
        NodeList tiles = data.getElementsByTagName("tile");

        for (int i = 0; i < tiles.getLength() && i < rows * cols; i++) {
            Element tile = (Element) tiles.item(i);
            int gid = Integer.parseInt(tile.getAttribute("gid"));

            int row = i / cols;
            int col = i % cols;

            result[row][col] = gid != 0;
        }

        return result;
    }

    /*! \fn private void loadMap(String filePath)
        \brief Citeste harta veche din fisier text.
     */
    private void loadMap(String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            List<int[]> tempRows = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] tokens = line.split("\\s+");
                int[] row = new int[tokens.length];

                for (int i = 0; i < tokens.length; i++) {
                    row[i] = Integer.parseInt(tokens[i]);
                }

                tempRows.add(row);
            }

            br.close();

            rows = tempRows.size();
            cols = tempRows.get(0).length;
            mapData = new int[rows][cols];

            for (int i = 0; i < rows; i++) {
                mapData[i] = tempRows.get(i);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*! \fn public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY)
        \brief Randeaza harta curenta.
     */
    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        /*
         * MOD NOU:
         * Level 1 este desenat ca imagine exportata din Tiled.
         */
        if (useVisualMap) {
            g.drawImage(visualMap, offsetX - cameraX, offsetY - cameraY, null);

            /*
             * DEBUG:
             * Daca apasam H, afisam coliziunile citite din layer-ul Collisions.
             */
            if (Game.showHitboxes && collisionData != null) {
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        if (collisionData[row][col]) {
                            int drawX = offsetX + col * Tile.TILE_WIDTH - cameraX;
                            int drawY = offsetY + row * Tile.TILE_HEIGHT - cameraY;

                            g.setColor(new Color(0, 0, 255, 100));
                            g.fillRect(drawX, drawY, Tile.TILE_WIDTH, Tile.TILE_HEIGHT);

                            g.setColor(Color.BLUE);
                            g.drawRect(drawX, drawY, Tile.TILE_WIDTH, Tile.TILE_HEIGHT);
                        }
                    }
                }
            }

            /*
             * DEBUG:
             * Zona de trecere spre urmatorul nivel este mov.
             */
            if (Game.showHitboxes && transitionData != null) {
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        if (transitionData[row][col]) {
                            int drawX = offsetX + col * Tile.TILE_WIDTH - cameraX;
                            int drawY = offsetY + row * Tile.TILE_HEIGHT - cameraY;

                            g.setColor(new Color(255, 0, 255, 120));
                            g.fillRect(drawX, drawY, Tile.TILE_WIDTH, Tile.TILE_HEIGHT);

                            g.setColor(Color.MAGENTA);
                            g.drawRect(drawX, drawY, Tile.TILE_WIDTH, Tile.TILE_HEIGHT);
                        }
                    }
                }
            }

            return;
        }

        /*
         * MOD VECHI:
         * Level 2 si Level 3 raman desenate din matricea .txt.
         */
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int tileId = mapData[row][col];

                Tile tile = Tile.tiles[tileId];
                if (tile == null) {
                    tile = Tile.grassTile;
                }

                int drawX = offsetX + col * Tile.TILE_WIDTH - cameraX;
                int drawY = offsetY + row * Tile.TILE_HEIGHT - cameraY;

                tile.Draw(g, drawX, drawY);

                if (Game.showHitboxes && tile.IsSolid()) {
                    g.setColor(new Color(0, 0, 255, 100));
                    g.fillRect(drawX, drawY, Tile.TILE_WIDTH, Tile.TILE_HEIGHT);

                    g.setColor(Color.BLUE);
                    g.drawRect(drawX, drawY, Tile.TILE_WIDTH, Tile.TILE_HEIGHT);
                }
            }
        }
    }
}


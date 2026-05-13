package PaooGame;

import PaooGame.Tiles.Tile;

import java.awt.Point;
import java.util.*;

/*! \class PathFinder
    \brief Implementeaza un algoritm simplu de pathfinding pe grid de tile-uri .
    \details Lucreaza peste logica de coliziune existenta din Map, folosind tile-urile solide
             definite in layer-ul Collisions sau in matricea text.
 */
public class PathFinder {

    /*! \fn public static java.util.List<Point> findPath(Map map, int startRow, int startCol, int goalRow, int goalCol)
        \brief Calculeaza un traseu de la un tile de start la un tile tinta folosind BFS.
        \param map Referinta catre harta curenta (pentru a sti ce tile-uri sunt solide).
        \param startRow Randul tile-ului de start (pozitia inamicului).
        \param startCol Coloana tile-ului de start.
        \param goalRow Randul tile-ului tinta (pozitia jucatorului).
        \param goalCol Coloana tile-ului tinta.
        \return Lista de puncte (row,col) reprezentand path-ul. Include si start, si goal.
                Intoarce lista vida daca nu exista traseu valid.
     */
    public static java.util.List<Point> findPath(Map map, int startRow, int startCol, int goalRow, int goalCol) {
        java.util.List<Point> path = new ArrayList<>();

        /// Daca start-ul si tinta sunt identice, nu avem nimic de facut.
        if (startRow == goalRow && startCol == goalCol) {
            path.add(new Point(startCol, startRow));
            return path;
        }

        int cols = map.getPixelWidth() / Tile.TILE_WIDTH;
        int rows = map.getPixelHeight() / Tile.TILE_HEIGHT;

        /// Validam limitele.
        if (!isInside(startRow, startCol, rows, cols) || !isInside(goalRow, goalCol, rows, cols)) {
            return path;
        }

        boolean[][] visited = new boolean[rows][cols];
        Point[][] parent = new Point[rows][cols];

        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startCol, startRow));
        visited[startRow][startCol] = true;

        /// Vectorii de deplasare: sus, jos, stanga, dreapta.
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int cCol = current.x;
            int cRow = current.y;

            if (cRow == goalRow && cCol == goalCol) {
                /// Am gasit tinta – reconstruim path-ul mergand inapoi prin parent[][].
                reconstructPath(path, parent, startRow, startCol, goalRow, goalCol);
                return path;
            }

            for (int dir = 0; dir < 4; dir++) {
                int nRow = cRow + dRow[dir];
                int nCol = cCol + dCol[dir];

                if (!isInside(nRow, nCol, rows, cols)) {
                    continue;
                }
                if (visited[nRow][nCol]) {
                    continue;
                }

                /// Verificam daca tile-ul vecin este walkable (nu este solid).
                if (isSolidTile(map, nRow, nCol)) {
                    continue;
                }

                visited[nRow][nCol] = true;
                parent[nRow][nCol] = current;
                queue.add(new Point(nCol, nRow));
            }
        }

        /// Daca am iesit din BFS fara sa gasim tinta, path-ul ramane gol.
        return path;
    }

    /*! \fn private static boolean isInside(int row, int col, int rows, int cols)
        \brief Verifica daca un index (row,col) este in interiorul hartii.
     */
    private static boolean isInside(int row, int col, int rows, int cols) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /*! \fn private static boolean isSolidTile(Map map, int row, int col)
        \brief Verifica daca tile-ul de la (row,col) este solid folosind map.isSolidAtPixel().
        \details Se testeaza centrul tile-ului.
     */
    private static boolean isSolidTile(Map map, int row, int col) {
        float px = col * Tile.TILE_WIDTH + Tile.TILE_WIDTH / 2.0f;
        float py = row * Tile.TILE_HEIGHT + Tile.TILE_HEIGHT / 2.0f;
        return map.isSolidAtPixel(px, py);
    }

    /*! \fn private static void reconstructPath(java.util.List<Point> path, Point[][] parent, int startRow, int startCol, int goalRow, int goalCol)
        \brief Reconstruieste traseul din parinti, de la tinta la start, apoi il inverseaza.
     */
    private static void reconstructPath(java.util.List<Point> path, Point[][] parent,
                                        int startRow, int startCol, int goalRow, int goalCol) {
        ArrayDeque<Point> stack = new ArrayDeque<>();
        int row = goalRow;
        int col = goalCol;

        while (!(row == startRow && col == startCol)) {
            stack.push(new Point(col, row));
            Point p = parent[row][col];
            if (p == null) {
                break;
            }
            col = p.x;
            row = p.y;
        }

        /// Adaugam si tile-ul de start.
        stack.push(new Point(startCol, startRow));

        while (!stack.isEmpty()) {
            path.add(stack.pop());
        }
    }
}
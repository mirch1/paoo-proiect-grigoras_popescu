package PaooGame;

import PaooGame.Tiles.Tile;

import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Map {
    private int[][] mapData;
    private int rows;
    private int cols;

    public Map(String filePath) {
        loadMap(filePath);
    }

    public int getPixelWidth() {
        return cols * Tile.TILE_WIDTH;
    }

    public int getPixelHeight() {
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

    public boolean isSolidAtPixel(float x, float y) {
        int col = (int)(x / Tile.TILE_WIDTH);
        int row = (int)(y / Tile.TILE_HEIGHT);

        return getTile(row, col).IsSolid();
    }

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

    public void Draw(Graphics g, int cameraX, int cameraY, int offsetX, int offsetY) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int tileId = mapData[row][col];

                Tile tile = Tile.tiles[tileId];
                if (tile == null) {
                    tile = Tile.grassTile;
                }

                tile.Draw(g,
                        offsetX + col * Tile.TILE_WIDTH - cameraX,
                        offsetY + row * Tile.TILE_HEIGHT - cameraY);
            }
        }
    }
}
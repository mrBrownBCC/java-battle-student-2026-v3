package app.background;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.awt.Graphics;

public final class Map {
    private static int width;
    private static int height;
    private static int[][] tiles = new int[0][0];

    private Map() {
    }

    static void load(String name) {
        width = 0;
        height = 0;
        tiles = new int[0][0];

        ArrayList<int[]> rows = new ArrayList<>();
        String path = "src/main/resources/maps/" + name + ".txt";
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    switch (tokens[i]) {
                        case "W":
                            row[i] = Utilities.WALL;
                            break;
                        case "G":
                            row[i] = Utilities.GRASS;
                            break;
                        case "M":
                            row[i] = Utilities.MUD;
                            break;
                        default:
                            row[i] = -1;
                    }
                }
                rows.add(row);
            }
            tiles = rows.toArray(new int[rows.size()][]);
            if (tiles.length > 0) {
                width = tiles[0].length;
                height = tiles.length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int[][] getTilesInternal() {
        return tiles;
    }

    public static int[][] getTiles() {
        return deepCopy(tiles);
    }

    public static int getHeight() {
        return height;
    }

    public static int getWidth() {
        return width;
    }

    private static int[][] deepCopy(int[][] source) {
        if (source == null) {
            return null;
        }

        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] != null ? source[i].clone() : null;
        }
        return copy;
    }

    static void display(Graphics g, int panelWidth, int panelHeight, int cameraX, int cameraY, double zoomFactor) {
        if (tiles == null || tiles.length == 0) {
            return;
        }
        int rows = tiles.length;
        int cols = tiles[0].length;

        double currentTileSize = Utilities.TILE_SIZE * zoomFactor;

        int startCol = (int) Math.floor(cameraX / currentTileSize);
        int endCol = (int) Math.ceil((cameraX + panelWidth) / currentTileSize);
        int startRow = (int) Math.floor(cameraY / currentTileSize);
        int endRow = (int) Math.ceil((cameraY + panelHeight) / currentTileSize);

        startCol = Math.max(0, startCol);
        endCol = Math.min(cols, endCol);
        startRow = Math.max(0, startRow);
        endRow = Math.min(rows, endRow);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                int tileType = tiles[r][c];
                if (tileType != -1) {
                    double x = (c * currentTileSize) - cameraX;
                    double y = (r * currentTileSize) - cameraY;

                    java.awt.Image img = null;
                    switch (tileType) {
                        case Utilities.WALL:
                            img = Utilities.WALL_IMAGE;
                            break;
                        case Utilities.GRASS:
                            img = Utilities.GRASS_IMAGE;
                            break;
                        case Utilities.MUD:
                            img = Utilities.MUD_IMAGE;
                            break;
                    }
                    if (img != null) {
                        g.drawImage(img, (int) x, (int) y, (int) currentTileSize, (int) currentTileSize, null);
                    }
                }
            }
        }
    }
}

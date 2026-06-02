package app.background;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.awt.Point;

class Game extends JPanel {
    private ArrayList<Robot> robots;
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private ArrayList<PowerUp> powerUps;
    private List<Robot> readOnlyRobots;
    private List<Projectile> readOnlyProjectiles;
    private List<PowerUp> readOnlyPowerUps;
    private int duration = 0;
    private Random randomGenerator;
    private int currentWidth, currentHeight, currentCameraX, currentCameraY;
    private double currentZoomFactor;
    private int maxDuration;
    private final HashMap<Integer, ArrayList<Robot>> robotGrid = new HashMap<>();

    private void logConcurrentModification(String context, ConcurrentModificationException e) {
        System.err.println(context + ": " + e.getMessage());
        e.printStackTrace();
    }

    Game(ArrayList<String> robotFileNames, String mapName, int maxDuration) {
        robots = new ArrayList<>();
        powerUps = new ArrayList<>();
        Map.load(mapName);
        readOnlyRobots = Collections.unmodifiableList(robots);
        readOnlyProjectiles = Collections.unmodifiableList(projectiles);
        readOnlyPowerUps = Collections.unmodifiableList(powerUps);
        this.maxDuration = maxDuration;
        randomGenerator = new Random();

        try {
            for (String className : robotFileNames) {
                int encodedSpawnLocation = smartSpawn();
                if (encodedSpawnLocation == -1) {
                    System.err.println("Could not find a valid spawn location for " + className + ". Skipping robot.");
                    continue;
                }

                int[][] tiles = Map.getTilesInternal();
                int numCols = (tiles != null && tiles.length > 0)
                    ? tiles[0].length
                        : 0;
                if (numCols == 0) {
                    System.err.println("Map has no columns. Cannot place robot " + className);
                    continue;
                }
                int spawnRow = encodedSpawnLocation / numCols;
                int spawnCol = encodedSpawnLocation % numCols;

                int robotSpawnX = spawnCol * Utilities.TILE_SIZE;
                int robotSpawnY = spawnRow * Utilities.TILE_SIZE;
                Robot robot = Utilities.createRobot(robotSpawnX, robotSpawnY, className);
                if (robot != null) {
                    robots.add(robot);
                    System.out.println("Added robot: " + className + " at (" + spawnCol + "," + spawnRow + ")");
                } else {
                    System.err.println("Failed to create robot from class: " + className);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize URLClassLoader for robots directory: " + e.getMessage());
            e.printStackTrace();
        }

        setPreferredSize(new Dimension(Utilities.SCREEN_WIDTH, Utilities.SCREEN_HEIGHT));
        setFocusable(true);
    }

    void setDisplayParameters(int width, int height, int cameraX, int cameraY, double zoomFactor) {
        currentWidth = width;
        currentHeight = height;
        currentCameraX = cameraX;
        currentCameraY = cameraY;
        currentZoomFactor = zoomFactor;
    }

    private int smartSpawn() {
        int[][] tiles = Map.getTilesInternal();
        if (tiles == null || tiles.length == 0 || tiles[0].length == 0) {
            System.err.println("SmartSpawn: Map is not properly initialized.");
            return -1;
        }
        int numRows = tiles.length;
        int numCols = tiles[0].length;

        List<Point> grassLocations = new ArrayList<>();
        Set<Point> visitedGrassLocations = new HashSet<>();
        int samplingAttempts = 0;
        int maxSamplingAttempts = 100;

        while (grassLocations.size() < 10 && samplingAttempts < maxSamplingAttempts) {
            int r = randomGenerator.nextInt(numRows);
            int c = randomGenerator.nextInt(numCols);
            Point candidatePoint = new Point(c, r);

            if (tiles[r][c] == Utilities.GRASS && !visitedGrassLocations.contains(candidatePoint)) {
                grassLocations.add(candidatePoint);
                visitedGrassLocations.add(candidatePoint);
            }
            samplingAttempts++;
        }

        if (grassLocations.isEmpty()) {
            System.err.println("SmartSpawn: Could not find any grass tiles after " + maxSamplingAttempts + " random attempts. Scanning map...");
            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    if (Map.getTilesInternal()[r][c] == Utilities.GRASS) {
                        System.out.println("SmartSpawn: Found fallback grass tile at (" + c + "," + r + ")");
                        return r * numCols + c;
                    }
                }
            }
            System.err.println("SmartSpawn: No grass tiles found on the entire map.");
            return -1;
        }

        if (robots.isEmpty() && powerUps.isEmpty()) {
            Point firstGrass = grassLocations.get(0);
            return firstGrass.y * numCols + firstGrass.x;
        }

        Point bestSpawnTile = null;
        double largestMinDistanceToNearestEntity = -1.0;

        for (Point currentTile : grassLocations) {
            double tileCenterX = currentTile.x * Utilities.TILE_SIZE + Utilities.TILE_SIZE / 2.0;
            double tileCenterY = currentTile.y * Utilities.TILE_SIZE + Utilities.TILE_SIZE / 2.0;
            double currentPointMinDistanceToAnEntity = Double.MAX_VALUE;

            try {
                if (!robots.isEmpty()) {
                    for (Robot robot : robots) {
                        if (!robot.isAlive()) {
                            continue;
                        }
                        double robotCenterX = robot.getX() + Utilities.ROBOT_SIZE / 2.0;
                        double robotCenterY = robot.getY() + Utilities.ROBOT_SIZE / 2.0;
                        double dist = Math.hypot(tileCenterX - robotCenterX, tileCenterY - robotCenterY);
                        currentPointMinDistanceToAnEntity = Math.min(currentPointMinDistanceToAnEntity, dist);
                    }
                } else if (powerUps.isEmpty()) {
                    currentPointMinDistanceToAnEntity = Double.MAX_VALUE;
                }

                if (!powerUps.isEmpty()) {
                    for (PowerUp powerUp : powerUps) {
                        double powerUpCenterX = powerUp.getX() * Utilities.TILE_SIZE + Utilities.POWER_UP_SIZE / 2.0;
                        double powerUpCenterY = powerUp.getY() * Utilities.TILE_SIZE + Utilities.POWER_UP_SIZE / 2.0;
                        double dist = Math.hypot(tileCenterX - powerUpCenterX, tileCenterY - powerUpCenterY);
                        currentPointMinDistanceToAnEntity = Math.min(currentPointMinDistanceToAnEntity, dist);
                    }
                } else if (robots.isEmpty()) {
                    currentPointMinDistanceToAnEntity = Double.MAX_VALUE;
                }
            } catch (ConcurrentModificationException e) {
                logConcurrentModification("Concurrent modification while evaluating spawn location", e);
                continue;
            }

            if (currentPointMinDistanceToAnEntity > largestMinDistanceToNearestEntity) {
                largestMinDistanceToNearestEntity = currentPointMinDistanceToAnEntity;
                bestSpawnTile = currentTile;
            }
        }

        if (bestSpawnTile != null) {
            return bestSpawnTile.y * numCols + bestSpawnTile.x;
        }

        Point fallbackGrass = grassLocations.get(0);
        return fallbackGrass.y * numCols + fallbackGrass.x;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Map.display(g, currentWidth, currentHeight, currentCameraX, currentCameraY, currentZoomFactor);

        try {
            for (Robot robot : robots) {
                if (robot.getImage() != null && robot.isAlive()) {
                    int screenX = (int) (robot.getX() * currentZoomFactor - currentCameraX);
                    int screenY = (int) (robot.getY() * currentZoomFactor - currentCameraY);
                    int scaledSize = (int) (Utilities.ROBOT_SIZE * currentZoomFactor);
                    g.drawImage(robot.getImage(), screenX, screenY, scaledSize, scaledSize, null);
                }
            }
        } catch (ConcurrentModificationException e) {
            logConcurrentModification("Concurrent modification while rendering robots", e);
        }

        try {
            for (Projectile projectile : projectiles) {
                if (projectile.getProjectileImage() != null) {
                    int screenX = (int) (projectile.getX() * currentZoomFactor - currentCameraX);
                    int screenY = (int) (projectile.getY() * currentZoomFactor - currentCameraY);
                    int scaledSize = (int) (Utilities.PROJECTILE_SIZE * currentZoomFactor);
                    g.drawImage(projectile.getProjectileImage(), screenX, screenY, scaledSize, scaledSize, null);
                }
            }
        } catch (ConcurrentModificationException e) {
            logConcurrentModification("Concurrent modification while rendering projectiles", e);
        }

        try {
            for (PowerUp powerUp : powerUps) {
                if (powerUp.getImage() != null) {
                    int screenX = (int) (powerUp.getX() * currentZoomFactor - currentCameraX);
                    int screenY = (int) (powerUp.getY() * currentZoomFactor - currentCameraY);
                    int scaledSize = (int) (Utilities.POWER_UP_SIZE * currentZoomFactor);
                    g.drawImage(powerUp.getImage(), screenX, screenY, scaledSize, scaledSize, null);
                }
            }
        } catch (ConcurrentModificationException e) {
            logConcurrentModification("Concurrent modification while rendering power-ups", e);
        }
    }

    private void rebuildRobotGrid() {
        robotGrid.clear();
        try {
            for (Robot r : robots) {
                if (!r.isAlive()) continue;
                int minCX = r.getX() / Utilities.TILE_SIZE;
                int maxCX = (r.getX() + Utilities.ROBOT_SIZE - 1) / Utilities.TILE_SIZE;
                int minCY = r.getY() / Utilities.TILE_SIZE;
                int maxCY = (r.getY() + Utilities.ROBOT_SIZE - 1) / Utilities.TILE_SIZE;
                for (int cx = minCX; cx <= maxCX; cx++) {
                    for (int cy = minCY; cy <= maxCY; cy++) {
                        robotGrid.computeIfAbsent((cx << 16) | cy, k -> new ArrayList<>()).add(r);
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            logConcurrentModification("Concurrent modification while rebuilding robot grid", e);
        }
    }

    List<Robot> getRobotsInCell(int cellX, int cellY) {
        List<Robot> list = robotGrid.get((cellX << 16) | cellY);
        return list != null ? list : Collections.emptyList();
    }

    void step() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        HashSet<Integer> excludeRobotIds = new HashSet<>();
        String thinkThreadPrefix = "RobotThinkThread-";
        for(Thread thread : threads) {
            String name = thread.getName();
            if(name.startsWith(thinkThreadPrefix)) {
                String idString = name.substring(thinkThreadPrefix.length());
                try {
                    excludeRobotIds.add(Integer.parseInt(idString));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (robots != null) {
            try {
                for (Robot robot : robots) {
                    if (!robot.isAlive()) {
                        continue;
                    }

                    if(excludeRobotIds.contains(robot.getId())) {
                        robot.setSuccessfulThink(false);
                        continue;
                    }

                    Thread thinkThread = new Thread(() -> {
                        try {
                            robot.think(readOnlyRobots, readOnlyProjectiles, readOnlyPowerUps);
                            robot.step(this);
                        } catch (Exception e) {
                            System.err.println("Exception in Robot " + robot.getName() + " think method: " + e.getMessage());
                            e.printStackTrace();
                            robot.setSuccessfulThink(false);
                        }
                    });

                    thinkThread.setName(thinkThreadPrefix + robot.getId());
                    thinkThread.start();
                    try {
                        thinkThread.join(Utilities.GAME_DELAY);
                    } catch (InterruptedException e) {
                        System.err.println("Thread was interrupted: " + e.getMessage());
                    }
                }
            } catch (ConcurrentModificationException e) {
                logConcurrentModification("Concurrent modification while starting robot turns", e);
            }
            rebuildRobotGrid();
        }

        if (robots != null && powerUps != null && !powerUps.isEmpty()) {
            HashSet<PowerUp> collectedPowerUps = new HashSet<>();
            try {
                for (Robot robot : robots) {
                    if (!robot.isAlive()) {
                        continue;
                    }

                    double robotMinX = robot.getX();
                    double robotMaxX = robot.getX() + Utilities.ROBOT_SIZE;
                    double robotMinY = robot.getY();
                    double robotMaxY = robot.getY() + Utilities.ROBOT_SIZE;

                    for (PowerUp powerUp : powerUps) {
                        if (collectedPowerUps.contains(powerUp)) {
                            continue;
                        }

                        double p_topLeftX = powerUp.getX();
                        double p_topLeftY = powerUp.getY();
                        double p_bottomRightX = p_topLeftX + Utilities.POWER_UP_SIZE;
                        double p_bottomRightY = p_topLeftY + Utilities.POWER_UP_SIZE;

                        double powerUpMinX = p_topLeftX;
                        double powerUpMaxX = p_bottomRightX;
                        double powerUpMinY = p_topLeftY;
                        double powerUpMaxY = p_bottomRightY;

                        if (robotMaxX > powerUpMinX &&
                            robotMinX < powerUpMaxX &&
                            robotMaxY > powerUpMinY &&
                            robotMinY < powerUpMaxY) {
                            robot.applyPowerUpEffect(powerUp.getType());
                            collectedPowerUps.add(powerUp);
                            break;
                        }
                    }
                }
            } catch (ConcurrentModificationException e) {
                logConcurrentModification("Concurrent modification while checking power-up collection", e);
            }
            powerUps.removeAll(collectedPowerUps);
        }

        if (projectiles != null) {
            try {
                for (Projectile projectile : projectiles) {
                    projectile.update(this);
                }

                projectiles.removeIf(projectile -> !projectile.isAlive());
            } catch (ConcurrentModificationException e) {
                System.err.println("Concurrent modification while updating projectiles: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (Math.random() < Utilities.POWER_UP_SPAWN_CHANCE) {
            int encodedSpawnLocation = smartSpawn();
            if (encodedSpawnLocation != -1) {
                int[][] tiles = Map.getTilesInternal();
                int numCols = (tiles != null && tiles.length > 0)
                    ? tiles[0].length
                        : 0;
                if (numCols > 0) {
                    int spawnRow = encodedSpawnLocation / numCols;
                    int spawnCol = encodedSpawnLocation % numCols;
                    PowerUp newPowerUp = new PowerUp(
                            (spawnCol + 0.5) * Utilities.TILE_SIZE - Utilities.POWER_UP_SIZE / 2,
                            (spawnRow + 0.5) * Utilities.TILE_SIZE - Utilities.POWER_UP_SIZE / 2);
                    powerUps.add(newPowerUp);
                    System.out.println("Spawned PowerUp of type " + newPowerUp.getType() + " at tile (" + spawnCol + "," + spawnRow + ")");
                } else {
                    System.err.println("Cannot spawn power-up: Map has no columns.");
                }
            } else {
                System.err.println("Cannot spawn power-up: smartSpawn failed to find a location.");
            }
        }

        duration++;
    }

    int getDuration() {
        return duration;
    }

    int getMaxDuration() {
        return maxDuration;
    }

    List<Robot> getRobots() {
        return robots;
    }

    void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    List<Projectile> getProjectiles() {
        return projectiles;
    }

    List<PowerUp> getPowerUps() {
        return powerUps;
    }

    boolean isGameOver() {
        if (duration >= maxDuration) {
            return true;
        }

        int aliveCount = 0;
        try {
            for (Robot robot : robots) {
                if (robot.isAlive()) {
                    aliveCount++;
                }
            }
        } catch (ConcurrentModificationException e) {
            logConcurrentModification("Concurrent modification while checking game over state", e);
            return false;
        }
        return aliveCount == 1;
    }

    Robot getWinner() {
        if (!isGameOver()) {
            return null;
        }
        if (duration >= maxDuration) {
            int highestHealthPercentage = -1;
            Robot winner = null;
            try {
                for (Robot robot : robots) {
                    if (robot.isAlive()) {
                        int healthPercentage = (int) ((robot.getHealth() / (double) robot.getStartHealth()) * 100);
                        if (healthPercentage > highestHealthPercentage) {
                            highestHealthPercentage = healthPercentage;
                            winner = robot;
                        }
                    }
                }
            } catch (ConcurrentModificationException e) {
                logConcurrentModification("Concurrent modification while determining winner", e);
                return null;
            }
            return winner;
        }
        try {
            for (Robot robot : robots) {
                if (robot.isAlive()) {
                    return robot;
                }
            }
        } catch (ConcurrentModificationException e) {
            logConcurrentModification("Concurrent modification while determining winner", e);
            return null;
        }
        return null;
    }
}

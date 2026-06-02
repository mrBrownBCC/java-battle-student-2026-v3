package app.background;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Projectile {
    private double x;
    private double y;
    private double angle;
    private Robot owner;
    private boolean alive = true;
    private int projectileSpeed;
    private int projectileDamage;
    private BufferedImage projectileImage;

    Projectile(double x, double y, int xTarget, int yTarget, int projectileSpeed, int projectileDamage,
            BufferedImage projectileImage, Robot owner) {
        this.x = x;
        this.y = y;
        this.projectileSpeed = projectileSpeed;
        this.projectileDamage = projectileDamage;
        this.projectileImage = projectileImage;
        this.owner = owner;

        this.angle = Math.atan2(yTarget - this.y, xTarget - this.x);
    }

    void update(Game game) {
        if (!alive) {
            return;
        }

        int subSteps = 5;
        double totalDx = this.projectileSpeed * Math.cos(this.angle);
        double totalDy = this.projectileSpeed * Math.sin(this.angle);

        double subStepDx = totalDx / subSteps;
        double subStepDy = totalDy / subSteps;

        for (int i = 0; i < subSteps; i++) {
            if (!alive) break;

            double currentProjectileX = x + subStepDx;
            double currentProjectileY = y + subStepDy;

            int[][] tiles = Map.getTilesInternal();
            if (tiles != null && tiles.length > 0 && tiles[0].length > 0) {
                double[] pX = {
                    currentProjectileX,
                    currentProjectileX + Utilities.PROJECTILE_SIZE - 1,
                    currentProjectileX,
                    currentProjectileX + Utilities.PROJECTILE_SIZE - 1
                };
                double[] pY = {
                    currentProjectileY,
                    currentProjectileY,
                    currentProjectileY + Utilities.PROJECTILE_SIZE - 1,
                    currentProjectileY + Utilities.PROJECTILE_SIZE - 1
                };

                for (int corner = 0; corner < 4; corner++) {
                    int tileCol = (int) (pX[corner] / Utilities.TILE_SIZE);
                    int tileRow = (int) (pY[corner] / Utilities.TILE_SIZE);

                        if (tileRow < 0 || tileRow >= tiles.length ||
                            tileCol < 0 || tileCol >= tiles[0].length ||
                            tiles[tileRow][tileCol] == Utilities.WALL) {
                        destroy();
                        return;
                    }
                }
            }

            int minCX = (int) currentProjectileX / Utilities.TILE_SIZE;
            int maxCX = (int) (currentProjectileX + Utilities.PROJECTILE_SIZE - 1) / Utilities.TILE_SIZE;
            int minCY = (int) currentProjectileY / Utilities.TILE_SIZE;
            int maxCY = (int) (currentProjectileY + Utilities.PROJECTILE_SIZE - 1) / Utilities.TILE_SIZE;
            ArrayList<Robot> checkedRobots = new ArrayList<>();
            for (int cx = minCX; cx <= maxCX; cx++) {
                for (int cy = minCY; cy <= maxCY; cy++) {
                    for (Robot robot : game.getRobotsInCell(cx, cy)) {
                        if (checkedRobots.contains(robot)) continue;
                        checkedRobots.add(robot);
                        if (!robot.isAlive() || robot == this.owner) continue;
                        if (currentProjectileX < robot.getX() + Utilities.ROBOT_SIZE &&
                            currentProjectileX + Utilities.PROJECTILE_SIZE > robot.getX() &&
                            currentProjectileY < robot.getY() + Utilities.ROBOT_SIZE &&
                            currentProjectileY + Utilities.PROJECTILE_SIZE > robot.getY()) {
                            robot.takeDamage(this.projectileDamage);
                            destroy();
                            return;
                        }
                    }
                }
            }

            x = currentProjectileX;
            y = currentProjectileY;
        }
    }

    void destroy() {
        this.alive = false;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public Robot getOwner() {
        return owner;
    }

    public int getProjectileSpeed() {
        return projectileSpeed;
    }

    public int getProjectileDamage() {
        return projectileDamage;
    }

    BufferedImage getProjectileImage() {
        return projectileImage;
    }

    public boolean isAlive() {
        return alive;
    }
}

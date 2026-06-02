package app.robots;

import app.background.*;
import java.util.List;

public class Rando extends Robot {
    private int curXMovement = 0;
    private int curYMovement = 0;
    public Rando(int x, int y) {
        super(x, y, 3, 1, 2, 4, "Rando", "randomBot.png", "defaultProjectile.png");
        // Health: 3, Speed: 1, Attack Speed: 2, Projectile Strength: 4
    }

    @Override
    public void think(List<Robot> robots, List<Projectile> projectiles, List<PowerUp> powerups) {
        if (Math.random() < 0.1) {
            double r = Math.random();
            if (r < 0.25) {
                curXMovement = -1;
                curYMovement = 0;
            } else if (r < 0.5) {
                curXMovement = 1;
                curYMovement = 0;
            } else if (r < 0.75) {
                curYMovement = -1;
                curXMovement = 0;
            } else {
                curYMovement = 1;
                curXMovement = 0;
            }
        }
        xMovement = curXMovement;
        yMovement = curYMovement;
        if (canAttack()) {
            Robot closest = null;
            double bestDist = Double.POSITIVE_INFINITY;
            double rx = getX();
            double ry = getY();
            for (Robot robot : robots) {
                if (robot == this || !robot.isAlive()) continue;
                double dx = robot.getX() - rx;
                double dy = robot.getY() - ry;
                double d2 = Math.hypot(dx, dy);
                if (d2 < bestDist) {
                    bestDist = d2;
                    closest = robot;
                }
            }
            if (closest != null) {
                shootAtLocation(closest.getX() + Utilities.ROBOT_SIZE / 2, closest.getY() + Utilities.ROBOT_SIZE / 2);
            }
        }
    }
}
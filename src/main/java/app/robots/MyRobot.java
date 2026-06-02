package app.robots;

import app.background.*;
import java.util.List;

public class MyRobot extends Robot {
    public MyRobot(int x, int y){
        super(x, y, 3, 2, 2, 3,"MyRobot", "myRobot.png", "defaultProjectile.png");
        // Health: 3, Speed: 2, Attack Speed: 2, Projectile Strength: 3
        // Total = 10
    }

    @Override
    public void think(List<Robot> robots, List<Projectile> projectiles, List<PowerUp> powerups) {
        // Put your think logic here
        /*
            to move left, use xMovement = -1
            to move right, use xMovement = 1
            to move up, use yMovement = -1
            to move down, use yMovement = 1

            to shoot, use shootAtLocation(x, y), where x and y are the coordinates of the target
        */

        /*
            look at the Robot class to see accessible helper methods
        */
    }
}
package app.robots;

import app.background.*;
import java.util.List;

public class Rock extends Robot {
    public Rock(int x, int y){
        super(x, y, 5, 1, 3, 1,"Rock", "rock.png", "rock.png");
        
        // Health: 5, Speed: 1, Attack Speed: 3, Projectile Strength: 1
        // Total = 10
    }

    @Override
    public void think(List<Robot> robots, List<Projectile> projectiles, List<PowerUp> powerups) {
        //rock robot is not smart and doesn't think very well.
        
    }
}

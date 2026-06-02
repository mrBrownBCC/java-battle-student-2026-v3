package app.background;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import java.awt.image.BufferedImage;

public abstract class Robot {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private int healthPoints;
    private int speedPoints;
    private int attackSpeedPoints;
    private int projectileStrengthPoints;

    private int health;
    private int startHealth;
    private int maxHealth;
    private int speed;
    private int attackMaxCooldown;
    private int attackCurCooldown;
    private int projectileSpeed;
    private int projectileDamage;
    private String name;
    private final int id;
    private BufferedImage image;
    private BufferedImage projectileImage;
    private boolean successfulThink = true;

    private int x;
    private int y;

    protected int xMovement;
    protected int yMovement;
    protected int xTarget;
    protected int yTarget;
    protected boolean shoot;

    private static final int BOOST_DURATION_TICKS = 150;
    private int speedBoostDuration = 0;
    private int attackBoostDuration = 0;
    private boolean immortalityBoost = false;

    private int originalSpeed;
    private int originalProjectileSpeed;
    private int originalProjectileDamage;

    public Robot(int x, int y, int healthPoints, int speedPoints, int attackSpeedPoints, int projectileStrengthPoints, String robotName, String imageName, String projectileImageName) {
        int sum = healthPoints + speedPoints + attackSpeedPoints + projectileStrengthPoints;

        if (healthPoints < 1) {
            throw new IllegalArgumentException(robotName + "'s health points must be at least 1");
        } else if (speedPoints < 1) {
            throw new IllegalArgumentException(robotName + "'s speed points must be at least 1");
        } else if (attackSpeedPoints < 1) {
            throw new IllegalArgumentException(robotName + "'s attack speed points must be at least 1");
        } else if (projectileStrengthPoints < 1) {
            throw new IllegalArgumentException(robotName + "'s projectile strength points must be at least 1");
        } else if (sum != 10) {
            throw new IllegalArgumentException(robotName + "'s sum of all points must equal 10");
        } else if (healthPoints > 5) {
            throw new IllegalArgumentException(robotName + "'s health points must not exceed 5");
        } else if (speedPoints > 5) {
            throw new IllegalArgumentException(robotName + "'s speed points must not exceed 5");
        } else if (attackSpeedPoints > 5) {
            throw new IllegalArgumentException(robotName + "'s attack speed points must not exceed 5");
        } else if (projectileStrengthPoints > 5) {
            throw new IllegalArgumentException(robotName + "'s projectile strength points must not exceed 5");
        }

        this.image = Utilities.loadImage(imageName);
        if (this.image == null) {
            this.image = Utilities.ROBOT_ERROR;
        }
        this.projectileImage = Utilities.loadImage(projectileImageName);
        if (this.projectileImage == null) {
            this.projectileImage = Utilities.DEFAULT_PROJECTILE_IMAGE;
        }

        this.x = x;
        this.y = y;
        this.id = NEXT_ID.getAndIncrement();
        this.name = robotName;

        this.healthPoints = healthPoints;
        this.speedPoints = speedPoints;
        this.attackSpeedPoints = attackSpeedPoints;
        this.projectileStrengthPoints = projectileStrengthPoints;

        this.health = 30 + healthPoints * 20;
        this.startHealth = this.health;
        this.maxHealth = this.health;
        this.speed = 3 + speedPoints;
        this.attackMaxCooldown = 22 - attackSpeedPoints * 2;
        this.attackCurCooldown = attackMaxCooldown;
        this.projectileSpeed = 5 + projectileStrengthPoints;
        this.projectileDamage = 10 + projectileStrengthPoints * 3;

        this.originalSpeed = this.speed;
        this.originalProjectileSpeed = this.projectileSpeed;
        this.originalProjectileDamage = this.projectileDamage;
    }

    public final boolean canAttack() {
        return attackCurCooldown <= 0;
    }

    protected void shootAtLocation(int x, int y) {
        xTarget = x;
        yTarget = y;
        shoot = true;
    }

    protected void print(String message) {
        System.out.print(message);
    }

    protected void println(String message) {
        System.out.println(message);
    }

    public abstract void think(final List<Robot> robots, final List<Projectile> projectiles, final List<PowerUp> powerups);

    private final boolean isPointOkay(int pX, int pY, ArrayList<Robot> allRobots) {
        int[][] mapTiles = Map.getTilesInternal();
        if (mapTiles == null)
            return false;
        int mapRows = mapTiles.length;
        if (mapRows == 0)
            return false;
        int mapCols = mapTiles[0].length;
        if (mapCols == 0)
            return false;

        if (pX < 0 || pY < 0 || pX >= mapCols * Utilities.TILE_SIZE || pY >= mapRows * Utilities.TILE_SIZE) {
            return false;
        }

        int tileCol = (int) (pX / Utilities.TILE_SIZE);
        int tileRow = (int) (pY / Utilities.TILE_SIZE);
        if (tileRow < 0 || tileRow >= mapRows || tileCol < 0 || tileCol >= mapCols) {
            return false;
        }
        if (mapTiles[tileRow][tileCol] == Utilities.WALL) {
            return false;
        }

        if (allRobots != null) {
            for (Robot otherRobot : allRobots) {
                if (otherRobot == this || !otherRobot.isAlive()) {
                    continue;
                }
                if (pX >= otherRobot.getX() && pX < (otherRobot.getX() + Utilities.ROBOT_SIZE) &&
                        pY >= otherRobot.getY() && pY < (otherRobot.getY() + Utilities.ROBOT_SIZE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private final boolean canMoveTo(int targetX, int targetY, Game game) {
        int c1x = targetX;
        int c1y = targetY;
        int c2x = targetX + Utilities.ROBOT_SIZE - 1;
        int c2y = targetY;
        int c3x = targetX;
        int c3y = targetY + Utilities.ROBOT_SIZE - 1;
        int c4x = targetX + Utilities.ROBOT_SIZE - 1;
        int c4y = targetY + Utilities.ROBOT_SIZE - 1;

        int minCX = targetX / Utilities.TILE_SIZE;
        int maxCX = (targetX + Utilities.ROBOT_SIZE - 1) / Utilities.TILE_SIZE;
        int minCY = targetY / Utilities.TILE_SIZE;
        int maxCY = (targetY + Utilities.ROBOT_SIZE - 1) / Utilities.TILE_SIZE;
        ArrayList<Robot> nearbyRobots = new ArrayList<>();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cy = minCY; cy <= maxCY; cy++) {
                for (Robot r : game.getRobotsInCell(cx, cy)) {
                    if (!nearbyRobots.contains(r)) nearbyRobots.add(r);
                }
            }
        }

        return isPointOkay(c1x, c1y, nearbyRobots) &&
                isPointOkay(c2x, c2y, nearbyRobots) &&
                isPointOkay(c3x, c3y, nearbyRobots) &&
                isPointOkay(c4x, c4y, nearbyRobots);
    }

    public final boolean isTileMud(int currentX, int currentY) {
        int[][] mapTiles = Map.getTilesInternal();
        if (mapTiles == null)
            return false;
        int mapRows = mapTiles.length;
        if (mapRows == 0)
            return false;
        int mapCols = mapTiles[0].length;
        if (mapCols == 0)
            return false;

        int[] cornersX = { currentX, currentX + Utilities.ROBOT_SIZE - 1, currentX,
                currentX + Utilities.ROBOT_SIZE - 1 };
        int[] cornersY = { currentY, currentY, currentY + Utilities.ROBOT_SIZE - 1,
                currentY + Utilities.ROBOT_SIZE - 1 };

        for (int i = 0; i < 4; i++) {
            int tileCol = (int) (cornersX[i] / Utilities.TILE_SIZE);
            int tileRow = (int) (cornersY[i] / Utilities.TILE_SIZE);

            if (tileRow >= 0 && tileRow < mapRows && tileCol >= 0 && tileCol < mapCols) {
                if (mapTiles[tileRow][tileCol] == Utilities.MUD) {
                    return true;
                }
            }
        }
        return false;
    }

    final void applyPowerUpEffect(String type) {
        System.out.println(this.name + " picked up " + type + " power-up!");
        switch (type) {
            case "health":
                this.health += this.startHealth / 2;
                if(this.health > this.maxHealth) {
                    this.maxHealth = this.health;
                }
                System.out.println(this.name + " new health: " + this.health);
                break;
            case "speed":
                if (speedBoostDuration == 0) {
                    this.originalSpeed = this.speed;
                }
                this.speed = this.originalSpeed * 2;
                this.speedBoostDuration = BOOST_DURATION_TICKS;
                System.out.println(this.name + " new speed: " + this.speed + " for " + BOOST_DURATION_TICKS + " ticks.");
                break;
            case "attack":
                if (attackBoostDuration == 0) {
                    this.originalProjectileSpeed = this.projectileSpeed;
                    this.originalProjectileDamage = this.projectileDamage;
                }
                this.projectileSpeed = this.originalProjectileSpeed * 2;
                this.projectileDamage = this.originalProjectileDamage * 2;
                this.attackBoostDuration = BOOST_DURATION_TICKS;
                System.out.println(this.name + " new projectile speed: " + this.projectileSpeed + ", new damage: " + this.projectileDamage + " for " + BOOST_DURATION_TICKS + " ticks.");
                break;
            case "immortality":
                this.immortalityBoost = true;
                break;
            default:
                System.err.println("Unknown power-up type: " + type);
                break;
        }
    }

    private final void updatePowerUpEffects() {
        if (speedBoostDuration > 0) {
            speedBoostDuration--;
            if (speedBoostDuration == 0) {
                this.speed = this.originalSpeed;
                System.out.println(this.name + " speed boost wore off. Speed reverted to " + this.speed);
            }
        }
        if (attackBoostDuration > 0) {
            attackBoostDuration--;
            if (attackBoostDuration == 0) {
                this.projectileSpeed = this.originalProjectileSpeed;
                this.projectileDamage = this.originalProjectileDamage;
                System.out.println(this.name + " attack boost wore off. Projectile stats reverted.");
            }
        }
    }

    final void step(Game game) {
        if(!isAlive()) {
            return;
        }
        updatePowerUpEffects();

        if(Math.abs(xMovement) + Math.abs(yMovement) > 1) {
            throw new IllegalArgumentException("You can only move in one direction at a time, use xMovement and yMovement to set the direction");
        }
        if (shoot && canAttack()) {
            Projectile p = new Projectile(x + Utilities.ROBOT_SIZE / 2 - Utilities.PROJECTILE_SIZE / 2, y + Utilities.ROBOT_SIZE / 2 - Utilities.PROJECTILE_SIZE / 2, xTarget, yTarget, projectileSpeed, projectileDamage, projectileImage,
                    this);
            game.addProjectile(p);
            attackCurCooldown = attackMaxCooldown;
        }

        int effectiveSpeed = this.speed;
        if (isTileMud(this.x, this.y)) {
            effectiveSpeed = Math.max(1, (this.speed + 1) / 2);
        }

        for (int i = 0; i < effectiveSpeed; i++) {
            int potentialNextX = x + xMovement;
            int potentialNextY = y + yMovement;
            if (canMoveTo(potentialNextX, potentialNextY, game)) {
                x = potentialNextX;
                y = potentialNextY;
            } else {
                break;
            }
        }

        if (attackCurCooldown > 0) {
            attackCurCooldown--;
        }

        xMovement = 0;
        yMovement = 0;
        xTarget = 0;
        yTarget = 0;
        shoot = false;
    }

    public final int getHealth() {
        return health;
    }

    public final int getStartHealth() {
        return startHealth;
    }

    public final int getMaxHealth() {
        return startHealth;
    }

    public final int getSpeed() {
        return speed;
    }

    public final String getName() {
        return name;
    }

    public final int getId() {
        return id;
    }

    public final BufferedImage getImage() {
        return image;
    }

    public final BufferedImage getProjectileImage() {
        return projectileImage;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getXMovement() {
        return xMovement;
    }

    public final int getYMovement() {
        return yMovement;
    }

    public final int getHealthPoints() {
        return healthPoints;
    }

    public final int getSpeedPoints() {
        return speedPoints;
    }

    public final int getAttackSpeedPoints() {
        return attackSpeedPoints;
    }

    public final int getProjectileStrengthPoints() {
        return projectileStrengthPoints;
    }

    public final int getAttackMaxCooldown() {
        return attackMaxCooldown;
    }

    public final int getProjectileDamage() {
        return projectileDamage;
    }

    public final int getProjectileSpeed() {
        return projectileSpeed;
    }

    public final int getCenterX() {
        return x + Utilities.ROBOT_SIZE / 2;
    }

    public final int getCenterY() {
        return y + Utilities.ROBOT_SIZE / 2;
    }

    public final boolean isInMud() {
        return isTileMud(this.x, this.y);
    }

    public final int getLeftX() {
        return x;
    }

    public final int getTopY() {
        return y;
    }

    public final int getRightX() {
        return x + Utilities.ROBOT_SIZE - 1;
    }

    public final int getBottomY() {
        return y + Utilities.ROBOT_SIZE - 1;
    }

    public final int distanceTo(Robot other) {
        return distanceTo(other.getX(), other.getY());
    }

    public final int distanceTo(int targetX, int targetY) {
        return Math.abs(getCenterX() - targetX) + Math.abs(getCenterY() - targetY);
    }

    public final boolean containsPoint(int pointX, int pointY) {
        return pointX >= getLeftX() && pointX <= getRightX()
                && pointY >= getTopY() && pointY <= getBottomY();
    }

    public final boolean overlapsRobot(Robot other) {
        return getLeftX() <= other.getRightX()
                && getRightX() >= other.getLeftX()
                && getTopY() <= other.getBottomY()
                && getBottomY() >= other.getTopY();
    }

    final void takeDamage(int amount) {
        System.out.println(name + " took " + amount + " damage");
        this.health -= amount;
        if (this.health < 0) {
            this.health = 0;
            if(immortalityBoost) {
                this.health = 1;
                immortalityBoost = false;
            }
        }
    }

    public final boolean isAlive() {
        return this.health > 0;
    }

    final void setSuccessfulThink(boolean successful) {
        this.successfulThink = successful;
    }

    public final boolean isSuccessfulThink() {
        return successfulThink;
    }

    public final boolean hasSpeedBoost() {
        return speedBoostDuration > 0;
    }

    public final boolean hasAttackBoost() {
        return attackBoostDuration > 0;
    }

    public final boolean hasImmortalityBoost() {
        return immortalityBoost;
    }
}

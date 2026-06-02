package app.background;

import java.awt.image.BufferedImage;

public class PowerUp {
    private BufferedImage image;
    private double x;
    private double y;
    private String type;

    PowerUp(double x, double y) {
        double r = Math.random();
        if (r < 0.4) {
            this.type = "health";
            this.image = Utilities.loadImage("healthPack.png");
        } else if (r < 0.8) {
            this.type = "speed";
            this.image = Utilities.loadImage("speedPack.png");
        } else if (r < 0.95) {
            this.type = "attack";
            this.image = Utilities.loadImage("attackPack.png");
        } else {
            this.type = "immortality";
            this.image = Utilities.loadImage("immortalityPack.png");
        }
        this.x = x;
        this.y = y;
    }

    PowerUp(double x, double y, String type, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.image = image;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    BufferedImage getImage() {
        return image;
    }

    public String getType() {
        return type;
    }
}

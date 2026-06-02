package app.background;

import javax.swing.SwingUtilities;

class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameManager());
    }
}
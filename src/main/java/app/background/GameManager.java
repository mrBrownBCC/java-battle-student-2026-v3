package app.background;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.text.DecimalFormat;
import java.awt.image.BufferedImage;
import java.io.File;

class GameManager {
    private ArrayList<String> robotOptions = new ArrayList<String>();
    private ArrayList<String> mapOptions = new ArrayList<String>();
    private ArrayList<String> mapFileNames = new ArrayList<String>();
    private ArrayList<String> mapDisplayNames = new ArrayList<String>();
    private ArrayList<HealthBox> healthBoxes = new ArrayList<HealthBox>();

    private JFrame frame;
    private JComboBox<String> mapComboBox;
    private JButton startButton;

    private JFrame gameFrame;
    private Game gamePanel;
    private Timer gameTimer;
    private JLabel timerLabel;
    private JLabel speedLabel;
    private JPanel robotStatusPanel;
    private JFrame gameOverFrame;

    private double cameraX = 0;
    private double cameraY = 0;
    private double zoomFactor = 1.0;
    private double gameSpeedFactor = 1.0;
    private int gameLoopCounter = 0;

    private final int SCROLL_STEP = 32;
    private int GAME_TIMER_DELAY_MS;
    private final double[] speedFactors = {0.25, 0.5, 1.0, 2.0, 4.0};
    private int currentSpeedIndex = 2;
    private static final DecimalFormat df = new DecimalFormat("0.##");

    private static final int MAX_TOTAL_ROBOT_SLOTS = 16;
    private ArrayList<JComboBox<String>> robotSlotComboBoxesList = new ArrayList<JComboBox<String>>();
    private ArrayList<String> nonTestRobotOptions = new ArrayList<>();
    private ArrayList<String> testRobotOptions = new ArrayList<>();
    private ArrayList<JToggleButton> slotTestToggleList = new ArrayList<>();
    private ArrayList<JPanel> robotStatDisplayPanelsList = new ArrayList<JPanel>();
    private JPanel robotSelectionPanel;
    private int currentRobotSlots = 0;


    GameManager() {
        loadRobotOptions();
        loadMapOptions();
        Utilities.loadImages();
        initMenu();
    }

    private void loadRobotOptions() {
        robotOptions.clear();
        
        Utilities.preloadRobotClasses();

        robotOptions.addAll(Utilities.getLoadedRobotNames());

        nonTestRobotOptions.clear();
        testRobotOptions.clear();
        for (String name : robotOptions) {
            if (name != null && name.startsWith("Level_")) testRobotOptions.add(name);
            else nonTestRobotOptions.add(name);
        }
        Collections.sort(nonTestRobotOptions, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(testRobotOptions, (a, b) -> {
            int na = extractTestLevelNumber(a);
            int nb = extractTestLevelNumber(b);
            if (na != nb) {
                return Integer.compare(na, nb);
            }
            return a.compareToIgnoreCase(b);
        });
    }

    private int extractTestLevelNumber(String robotName) {
        if (robotName == null) {
            return Integer.MAX_VALUE;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^Level_(\\d+)")
                .matcher(robotName);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        return Integer.MAX_VALUE;
    }

    private void loadMapOptions() {
        mapOptions.clear();
        mapFileNames.clear();
        mapDisplayNames.clear();
        File mapsDir = new File("src/main/resources/maps");
        if (mapsDir.exists() && mapsDir.isDirectory()) {
            File[] files = mapsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        String name = file.getName().substring(0, file.getName().length() - ".txt".length());
                        mapOptions.add(name);
                    }
                }
                Collections.sort(mapOptions);
            }
        }
        if (mapOptions.isEmpty()) {
            mapOptions.add("Standard");
            System.err.println("No map files found. Added Standard map.");
        }

        for (String fname : mapOptions) {
            mapFileNames.add(fname);
            String disp = fname;
            if (disp.length() > 1 && Character.isLetter(disp.charAt(0))) {
                disp = disp.substring(1).replaceFirst("^[_\\s-]+", "");
            }
            mapDisplayNames.add(disp);
        }
    }

    private void initMenu() {
        frame = new JFrame("Java Jostle - Setup Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(Color.DARK_GRAY);

        robotSelectionPanel = new JPanel();
        robotSelectionPanel.setLayout(new BoxLayout(robotSelectionPanel, BoxLayout.Y_AXIS));
        robotSelectionPanel.setBackground(Color.BLACK);
        robotSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        currentRobotSlots = 0;
        robotSlotComboBoxesList.clear();
        robotStatDisplayPanelsList.clear();
        addRobotSlotRow();
        if (currentRobotSlots < 2) addRobotSlotToPanel();
        if (currentRobotSlots < 2) addRobotSlotToPanel();


        JScrollPane scrollPane = new JScrollPane(robotSelectionPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel bottomControlsPanel = new JPanel(new GridBagLayout());
        bottomControlsPanel.setBackground(Color.DARK_GRAY.darker());
        bottomControlsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel mapLabelText = new JLabel("Choose Map:");
        mapLabelText.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        bottomControlsPanel.add(mapLabelText, gbc);

        if (this.mapOptions.isEmpty()) {
            this.mapOptions.add("Standard");
            this.mapFileNames.add("Standard");
            this.mapDisplayNames.add("Standard");
        }
        mapComboBox = new JComboBox<>(mapDisplayNames.toArray(new String[0]));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5;
        bottomControlsPanel.add(mapComboBox, gbc);

        JButton addRobotButton = new JButton("+ Add Robot Slot");
        addRobotButton.setFont(new Font("Arial", Font.PLAIN, 14));
        addRobotButton.addActionListener(e -> {
            if (currentRobotSlots < MAX_TOTAL_ROBOT_SLOTS) {
                addRobotSlotToPanel();
            }
            if (currentRobotSlots >= MAX_TOTAL_ROBOT_SLOTS) {
                addRobotButton.setEnabled(false);
            }
        });
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        bottomControlsPanel.add(addRobotButton, gbc);

        startButton = new JButton("Begin Jostle!");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.5;
        bottomControlsPanel.add(startButton, gbc);
        startButton.addActionListener(e -> startGame());


        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomControlsPanel, BorderLayout.SOUTH);

        frame.setMinimumSize(new Dimension(800, 600));
        frame.setSize(new Dimension(900, 750));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel currentRowPanel = null;

    private void addRobotSlotRow() {
        currentRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        currentRowPanel.setOpaque(false);
        robotSelectionPanel.add(currentRowPanel);
        robotSelectionPanel.revalidate();
        robotSelectionPanel.repaint();
    }
    
    private void addRobotSlotToPanel() {
        if (currentRobotSlots >= MAX_TOTAL_ROBOT_SLOTS) return;

        if (currentRowPanel == null || currentRowPanel.getComponentCount() >= 2) {
            addRobotSlotRow();
        }

        final int slotIndex = currentRobotSlots;

        ArrayList<String> comboBoxOptions = new ArrayList<>();
        comboBoxOptions.add("None");
        for (String name : nonTestRobotOptions) {
            if (name != null) comboBoxOptions.add(name);
        }

        JPanel slotPanel = new JPanel();
        slotPanel.setLayout(new BoxLayout(slotPanel, BoxLayout.Y_AXIS));
        slotPanel.setBorder(BorderFactory.createEtchedBorder());
        slotPanel.setOpaque(false);
        slotPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        header.setOpaque(false);
        JLabel slotLabel = new JLabel("Robot " + (slotIndex + 1));
        slotLabel.setForeground(Color.WHITE);
        header.add(slotLabel);
        JToggleButton slotTestToggle = new JToggleButton("Test Robots");
        slotTestToggle.setFont(new Font("Arial", Font.PLAIN, 12));
        slotTestToggle.setPreferredSize(new Dimension(110, 26));
        slotTestToggle.setToolTipText("Toggle between Player Robots and Level_* test robots for this slot");
        Color btnBg = new Color(60, 63, 65);
        applyRobotModeToggleStyle(slotTestToggle, btnBg);
        slotTestToggle.addActionListener(e -> {
            boolean selected = slotTestToggle.isSelected();
            slotTestToggle.setText(selected ? "Player Robots" : "Test Robots");
            updateComboModelForSlot(slotIndex, selected);
            applyRobotModeToggleStyle(slotTestToggle, btnBg);
        });
        slotTestToggleList.add(slotTestToggle);
        header.add(slotTestToggle);
        slotPanel.add(header);

        JComboBox<String> comboBox = new JComboBox<>(comboBoxOptions.toArray(new String[0]));
        comboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        comboBox.setMaximumSize(new Dimension(250, comboBox.getPreferredSize().height));
        robotSlotComboBoxesList.add(comboBox);

        JPanel statDisplay = new JPanel(new BorderLayout());
        statDisplay.setPreferredSize(new Dimension(400, 260));
        statDisplay.setOpaque(false);
        statDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        robotStatDisplayPanelsList.add(statDisplay);

        comboBox.addActionListener(e -> {
            String selectedName = (String) robotSlotComboBoxesList.get(slotIndex).getSelectedItem();
            updateStatDisplayForSlot(slotIndex, selectedName);
            slotPanel.revalidate();
            slotPanel.repaint();
        });

        slotPanel.add(comboBox);
        slotPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        slotPanel.add(statDisplay);

        currentRowPanel.add(slotPanel); 
        currentRobotSlots++;

        updateStatDisplayForSlot(slotIndex, "None"); 

        robotSelectionPanel.revalidate();
        robotSelectionPanel.repaint();
        
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, robotSelectionPanel);
        if (scrollPane != null) {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            SwingUtilities.invokeLater(() -> vertical.setValue(vertical.getMaximum()));
        }
    }

    private void applyRobotModeToggleStyle(JToggleButton toggleButton, Color backgroundColor) {
        toggleButton.setFocusPainted(false);
        toggleButton.setContentAreaFilled(true);
        toggleButton.setBorderPainted(false);
        toggleButton.setOpaque(true);
        toggleButton.setBackground(backgroundColor);
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        toggleButton.setRolloverEnabled(false);
            toggleButton.setContentAreaFilled(false);
            toggleButton.setOpaque(true);
            toggleButton.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
                @Override
                public void paint(Graphics g, JComponent c) {
                    AbstractButton b = (AbstractButton) c;
                    g.setColor(backgroundColor);
                    g.fillRect(0, 0, c.getWidth(), c.getHeight());
                    b.setForeground(Color.WHITE);
                    super.paint(g, c);
                }
            });
    }


    private void updateStatDisplayForSlot(int slotIndex, String robotName) {
        if (slotIndex < 0 || slotIndex >= robotStatDisplayPanelsList.size()) {
            System.err.println("updateStatDisplayForSlot: slotIndex " + slotIndex + " out of bounds for " + robotStatDisplayPanelsList.size());
            return;
        }
        JPanel displayArea = robotStatDisplayPanelsList.get(slotIndex);
        displayArea.removeAll();
            displayArea.setBackground(Color.BLACK);

        if (robotName != null && !robotName.equals("None")) {
            Robot robotInstance = Utilities.createRobot(0, 0, robotName); 
            if (robotInstance != null) {
                JPanel statsPanel = createRobotDisplayPanel(robotInstance);
                statsPanel.setOpaque(false);
                displayArea.add(statsPanel, BorderLayout.CENTER);
            } else {
                JLabel errorLabel = new JLabel(robotName + " (preview unavailable)");
                errorLabel.setForeground(Color.ORANGE);
                errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                displayArea.add(errorLabel, BorderLayout.CENTER);
            }
        } else {
            JLabel noneLabel = new JLabel("Select a Robot");
            noneLabel.setForeground(Color.GRAY);
            noneLabel.setHorizontalAlignment(SwingConstants.CENTER);
            displayArea.add(noneLabel, BorderLayout.CENTER);
        }
        displayArea.revalidate();
        displayArea.repaint();
    }

    private void updateComboModelForSlot(int slotIndex, boolean useTest) {
        if (slotIndex < 0 || slotIndex >= robotSlotComboBoxesList.size()) return;
        ArrayList<String> baseList = useTest ? testRobotOptions : nonTestRobotOptions;
        String[] modelItems = new String[baseList.size() + 1];
        modelItems[0] = "None";
        for (int i = 0; i < baseList.size(); i++) modelItems[i + 1] = baseList.get(i);

        JComboBox<String> combo = robotSlotComboBoxesList.get(slotIndex);
        Object previous = combo.getSelectedItem();
        combo.setModel(new DefaultComboBoxModel<>(modelItems));
        if (previous != null) {
            boolean found = false;
            for (String it : modelItems) {
                if (previous.equals(it)) { found = true; break; }
            }
            if (found) combo.setSelectedItem(previous);
            else combo.setSelectedIndex(0);
        } else {
            combo.setSelectedIndex(0);
        }
        updateStatDisplayForSlot(slotIndex, (String) combo.getSelectedItem());
    }


    private void startGame() {
        ArrayList<String> selectedRobotList = new ArrayList<>();

        for (int i = 0; i < robotSlotComboBoxesList.size(); i++) { 
            String selectedName = (String) robotSlotComboBoxesList.get(i).getSelectedItem();
            if (selectedName != null && !selectedName.equals("None")) {
                selectedRobotList.add(selectedName);
            }
        }

        if (selectedRobotList.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select at least one robot to start the game.", "No Robots Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selIndex = mapComboBox.getSelectedIndex();
        String selectedMapName = (selIndex >= 0 && selIndex < mapFileNames.size()) ? mapFileNames.get(selIndex) : (String) mapComboBox.getSelectedItem();

        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
        if (gameOverFrame != null) {
            gameOverFrame.dispose();
            gameOverFrame = null;
        }
        
        int maxGameDurationSeconds = 300;
        GAME_TIMER_DELAY_MS = Utilities.GAME_DELAY * selectedRobotList.size() + 10;
        gamePanel = new Game(selectedRobotList, selectedMapName, maxGameDurationSeconds * 1000 / (GAME_TIMER_DELAY_MS * 4));
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setFocusable(true);

        gameFrame = new JFrame("Java Jostle - Game");
        gameFrame.setBackground(Color.BLACK);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setLayout(new BorderLayout());

        JPanel bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
        bottomContainer.setBackground(Color.DARK_GRAY);

        JPanel controlPanelUI = new JPanel(new GridBagLayout());
        controlPanelUI.setBackground(Color.GRAY);
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.insets = new Insets(2, 5, 2, 5);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftControls.setOpaque(false);
        JButton zoomInButton = new JButton("+");
        zoomInButton.setFont(new Font("Arial", Font.BOLD, 18));
        zoomInButton.addActionListener(e -> {
            double oldZoomFactor = this.zoomFactor;
            this.zoomFactor *= 1.5;
            adjustCameraForCenteredZoom(oldZoomFactor, this.zoomFactor);
            updateGameDisplayAndRepaint();
            if(gamePanel != null) gamePanel.requestFocusInWindow();
        });
        leftControls.add(zoomInButton);

        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setFont(new Font("Arial", Font.BOLD, 18));
        zoomOutButton.addActionListener(e -> {
            double oldZoomFactor = this.zoomFactor;
            this.zoomFactor = Math.max(0.1, this.zoomFactor / 1.5);
            adjustCameraForCenteredZoom(oldZoomFactor, this.zoomFactor);
            updateGameDisplayAndRepaint();
            if(gamePanel != null) gamePanel.requestFocusInWindow();
        });
        leftControls.add(zoomOutButton);

        cgbc.gridx = 0; cgbc.gridy = 0; cgbc.weightx = 0.33; cgbc.anchor = GridBagConstraints.LINE_START;
        controlPanelUI.add(leftControls, cgbc);

        JPanel centerControls = new JPanel();
        centerControls.setLayout(new BoxLayout(centerControls, BoxLayout.Y_AXIS));
        centerControls.setOpaque(false);
        timerLabel = new JLabel("Time: 00:00:000");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timerLabel.setForeground(Color.BLUE);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerControls.add(timerLabel);
        speedLabel = new JLabel("Speed: " + df.format(gameSpeedFactor) + "x");
        speedLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        speedLabel.setForeground(Color.WHITE);
        speedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerControls.add(speedLabel);
        cgbc.gridx = 1; cgbc.gridy = 0; cgbc.weightx = 0.34; cgbc.anchor = GridBagConstraints.CENTER;
        controlPanelUI.add(centerControls, cgbc);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);
        JButton speedToggleButton = new JButton("Speed");
        speedToggleButton.setFont(new Font("Arial", Font.PLAIN, 14));
        speedToggleButton.addActionListener(e -> {
            currentSpeedIndex = (currentSpeedIndex + 1) % speedFactors.length;
            gameSpeedFactor = speedFactors[currentSpeedIndex];
            speedLabel.setText("Speed: " + df.format(gameSpeedFactor) + "x");
            if(gamePanel != null) gamePanel.requestFocusInWindow();
        });
        rightControls.add(speedToggleButton);
        cgbc.gridx = 2; cgbc.gridy = 0; cgbc.weightx = 0.33; cgbc.anchor = GridBagConstraints.LINE_END;
        controlPanelUI.add(rightControls, cgbc);

        controlPanelUI.setPreferredSize(new Dimension(Utilities.SCREEN_WIDTH, 60));
        bottomContainer.add(controlPanelUI);

        robotStatusPanel = new JPanel();
        robotStatusPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        robotStatusPanel.setBackground(Color.DARK_GRAY.darker());
        robotStatusPanel.setPreferredSize(new Dimension(Utilities.SCREEN_WIDTH, 100));
        for(Robot robot : gamePanel.getRobots()) {
            HealthBox healthBox = new HealthBox(robot);
            healthBoxes.add(healthBox);
            robotStatusPanel.add(healthBox);
        }
        bottomContainer.add(robotStatusPanel);

        gameFrame.add(gamePanel, BorderLayout.CENTER);
        gameFrame.add(bottomContainer, BorderLayout.SOUTH);
        gameFrame.pack();
        gameFrame.setMinimumSize(new Dimension(Utilities.SCREEN_WIDTH, Utilities.SCREEN_HEIGHT));
        gameFrame.setLocationRelativeTo(null);

        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                boolean moved = false;
                double scrollAmount = SCROLL_STEP / zoomFactor;
                if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) { cameraX -= scrollAmount; moved = true; }
                else if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) { cameraX += scrollAmount; moved = true; }
                else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) { cameraY -= scrollAmount; moved = true; }
                else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) { cameraY += scrollAmount; moved = true; }
                if (moved) updateGameDisplayAndRepaint();
            }
        });
         gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gamePanel == null || !gamePanel.isFocusOwner()) {
                    gamePanel.requestFocusInWindow();
                }
                double mousePanelX = e.getX(); double mousePanelY = e.getY();
                double oldZoomFactor = zoomFactor; double worldMouseX, worldMouseY, newZoomFactor;
                if (e.getButton() == MouseEvent.BUTTON1) { newZoomFactor = zoomFactor * 1.5; }
                else if (e.getButton() == MouseEvent.BUTTON3) { newZoomFactor = Math.max(0.1, zoomFactor / 1.5); }
                else { return; }
                worldMouseX = (mousePanelX + cameraX) / oldZoomFactor;
                worldMouseY = (mousePanelY + cameraY) / oldZoomFactor;
                zoomFactor = newZoomFactor;
                cameraX = worldMouseX * newZoomFactor - mousePanelX;
                cameraY = worldMouseY * newZoomFactor - mousePanelY;
                updateGameDisplayAndRepaint();
            }
        });


        gameFrame.setVisible(true);
        gamePanel.requestFocusInWindow();

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(GAME_TIMER_DELAY_MS, ae -> gameLoop());
        gameTimer.start();
        gameLoopCounter = 0;
        updateGameDisplayAndRepaint();
    }

    private void adjustCameraForCenteredZoom(double oldZoomFactor, double newZoomFactor) {
        if (gamePanel == null || gamePanel.getWidth() == 0 || gamePanel.getHeight() == 0) return;
        double panelCenterX = gamePanel.getWidth() / 2.0;
        double panelCenterY = gamePanel.getHeight() / 2.0;
        double worldCenterX = (panelCenterX + cameraX) / oldZoomFactor;
        double worldCenterY = (panelCenterY + cameraY) / oldZoomFactor;
        cameraX = worldCenterX * newZoomFactor - panelCenterX;
        cameraY = worldCenterY * newZoomFactor - panelCenterY;
    }

    private void updateRobotStatusDisplay() {
        if (robotStatusPanel == null || gamePanel == null || gamePanel.getRobots() == null) return;
        for (HealthBox healthBox : healthBoxes) {
            healthBox.updateHealth();
        }
        robotStatusPanel.revalidate();
        robotStatusPanel.repaint();
    }

    private void updateGameDisplayAndRepaint() {
        if (gamePanel != null && gameFrame != null && gameFrame.isVisible()) {
            gamePanel.setDisplayParameters(gamePanel.getWidth(), gamePanel.getHeight(), (int) Math.round(cameraX), (int) Math.round(cameraY), zoomFactor);
            gamePanel.repaint();
            if (timerLabel != null) {
                int stepsTaken = gamePanel.getDuration();
                int maxSteps = gamePanel.getMaxDuration();
                int stepsRemaining = maxSteps - stepsTaken;
                if (stepsRemaining < 0) {
                    stepsRemaining = 0;
                }

                long remainingMilliseconds = (long) stepsRemaining * GAME_TIMER_DELAY_MS * 4;

                long minutes = (remainingMilliseconds / 1000) / 60;
                long seconds = (remainingMilliseconds / 1000) % 60;
                long millis = remainingMilliseconds % 1000;

                timerLabel.setText(String.format("Time: %02d:%02d:%03d", minutes, seconds, millis));
            }
            if (speedLabel != null) speedLabel.setText("Speed: " + df.format(gameSpeedFactor) + "x");
            updateRobotStatusDisplay();
        }
    }

    private void gameLoop() {
        if (gamePanel == null) {
            if (gameTimer != null) gameTimer.stop();
            return;
        }

        if (gamePanel.isGameOver()) {
            if (gameTimer != null) gameTimer.stop();
            if (gameOverFrame == null || !gameOverFrame.isVisible()) {
                Robot winner = gamePanel.getWinner();
                showGameOverScreen(winner);
            }
            return;
        }
        
        if (gameLoopCounter % (int) Math.round(4.0 / gameSpeedFactor) == 0) {
            gamePanel.step();
        }
        
        if (gamePanel.isGameOver()) {
            if (gameTimer != null) gameTimer.stop();
            if (gameOverFrame == null || !gameOverFrame.isVisible()) {
                Robot winner = gamePanel.getWinner();
                showGameOverScreen(winner);
            }
        } else {
            updateGameDisplayAndRepaint();
        }
            
        gameLoopCounter++;
        if (gameLoopCounter >= 10000) gameLoopCounter = 0;
    }

    JPanel createRobotDisplayPanel(Robot robot) {
        JPanel robotDisplayPanel = new JPanel();
        robotDisplayPanel.setLayout(new BoxLayout(robotDisplayPanel, BoxLayout.Y_AXIS));
        robotDisplayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        robotDisplayPanel.setOpaque(false); 

        JLabel nameLabel = new JLabel(robot.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        robotDisplayPanel.add(nameLabel);
        robotDisplayPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        BufferedImage img = robot.getImage();
        JLabel imageLabel;
        if (img != null) {
            Image scaledImg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            imageLabel = new JLabel(new ImageIcon(scaledImg));
        } else {
            imageLabel = new JLabel("No Image");
            imageLabel.setPreferredSize(new Dimension(100, 100));
            imageLabel.setForeground(Color.WHITE);
        }
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        robotDisplayPanel.add(imageLabel);
        robotDisplayPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel statsContainerPanel = new JPanel();
        statsContainerPanel.setLayout(new BoxLayout(statsContainerPanel, BoxLayout.Y_AXIS));
        statsContainerPanel.setOpaque(false);
        statsContainerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statsContainerPanel.add(new StatDisplayPanel("Health", robot.getHealthPoints()));
        statsContainerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        statsContainerPanel.add(new StatDisplayPanel("Speed", robot.getSpeedPoints()));
        statsContainerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        statsContainerPanel.add(new StatDisplayPanel("Attack Speed", robot.getAttackSpeedPoints()));
        statsContainerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        statsContainerPanel.add(new StatDisplayPanel("Projectile Str.", robot.getProjectileStrengthPoints()));
        
        robotDisplayPanel.add(statsContainerPanel);
        return robotDisplayPanel;
    }

    private void showGameOverScreen(Robot winner) {
        if (gameTimer != null) gameTimer.stop();
        if (gameFrame != null) {
            gameFrame.setVisible(false);
            gameFrame.dispose(); 
            gameFrame = null;
        }

        gameOverFrame = new JFrame("Game Over!");
        gameOverFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameOverFrame.setSize(500, 650);
        gameOverFrame.setLayout(new BorderLayout(10, 10));
        gameOverFrame.getContentPane().setBackground(Color.BLACK);
        gameOverFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                gameOverFrame = null;
            }
        });


        JLabel gameOverLabel = new JLabel("We have a winner!", SwingConstants.CENTER);
        gameOverLabel.setFont(new Font("Arial", Font.BOLD, 36));
        gameOverLabel.setForeground(Color.CYAN);
        gameOverFrame.add(gameOverLabel, BorderLayout.NORTH);

        if (winner != null) {
            JPanel winnerPanel = createRobotDisplayPanel(winner);
            gameOverFrame.add(winnerPanel, BorderLayout.CENTER);
        } else {
            JLabel noWinnerLabel = new JLabel("It's a draw or error!", SwingConstants.CENTER);
            noWinnerLabel.setFont(new Font("Arial", Font.BOLD, 28));
            noWinnerLabel.setForeground(Color.ORANGE);
            gameOverFrame.add(noWinnerLabel, BorderLayout.CENTER);
        }

        JButton continueButton = new JButton("Continue to Menu");
        continueButton.setFont(new Font("Arial", Font.BOLD, 20));
        continueButton.setFocusPainted(false);
        continueButton.addActionListener(e -> {
            if (gameOverFrame != null) {
                 gameOverFrame.dispose();
                 gameOverFrame = null;
            }
            gamePanel = null;
            if (gameTimer != null) {
                gameTimer.stop();
                gameTimer = null;
            }
            
            cameraX = 0; cameraY = 0; zoomFactor = 1.0;
            gameSpeedFactor = 1.0; currentSpeedIndex = 2;
            gameLoopCounter = 0;
            
            loadRobotOptions();
            loadMapOptions();
            initMenu();
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(continueButton);
        gameOverFrame.add(buttonPanel, BorderLayout.SOUTH);

        gameOverFrame.setLocationRelativeTo(null);
        gameOverFrame.setVisible(true);
    }

    class StatDisplayPanel extends JPanel {
        private String statName;
        private int statValue;
        private static final int MAX_POINTS = 5;
        private static final int RECT_WIDTH = 45;
        private static final int RECT_HEIGHT = 20;
        private static final int RECT_SPACING = 5;
        private static final int LABEL_WIDTH = 150;

        StatDisplayPanel(String statName, int statValue) {
            this.statName = statName;
            this.statValue = statValue;
            int panelWidth = LABEL_WIDTH + (RECT_WIDTH + RECT_SPACING) * MAX_POINTS - RECT_SPACING + 20;
            setPreferredSize(new Dimension(panelWidth, RECT_HEIGHT + 10));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            int stringY = (RECT_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(statName + ":", 5, stringY);

            int startX = LABEL_WIDTH;
            for (int i = 0; i < MAX_POINTS; i++) {
                if (i < statValue) {
                    g2d.setColor(new Color(0, 220, 255));
                    g2d.fillRect(startX + i * (RECT_WIDTH + RECT_SPACING), 0, RECT_WIDTH, RECT_HEIGHT);
                } else {
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.fillRect(startX + i * (RECT_WIDTH + RECT_SPACING), 0, RECT_WIDTH, RECT_HEIGHT);
                }
                g2d.setColor(Color.BLACK);
                g2d.drawRect(startX + i * (RECT_WIDTH + RECT_SPACING), 0, RECT_WIDTH, RECT_HEIGHT);
            }
        }
    }

    class HealthBox extends JPanel {
        private Robot robot;
        private HealthBar healthBar;

        HealthBox(Robot robot) {
            this.robot = robot;

            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.CENTER_ALIGNMENT);

            BufferedImage img = robot.getImage();
            JLabel imageLabel = (img != null) ? new JLabel(new ImageIcon(img.getScaledInstance(32, 32, Image.SCALE_SMOOTH))) : new JLabel("No Img");
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(imageLabel);
            JLabel nameLabel = new JLabel(robot.getName());
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(nameLabel);
            healthBar = new HealthBar(robot.getHealth(), robot.getMaxHealth());
            healthBar.setPreferredSize(new Dimension(50, 10));
            healthBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(healthBar);
        }

        void updateHealth() {
            healthBar.currentHealth = robot.getHealth();
            healthBar.maxHealth = robot.getMaxHealth();
            if (!robot.isSuccessfulThink()) {
                setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else if (robot.hasSpeedBoost()) {
                setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
            } else if (robot.hasAttackBoost()) {
                setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            } else {
                setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }
        }
    }

    class HealthBar extends JPanel {
        private int currentHealth;
        private int maxHealth;

        HealthBar(int currentHealth, int maxHealth) {
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (maxHealth <= 0) return;
            int width = getWidth(); int height = getHeight();
            double healthPercentage = (double) currentHealth / maxHealth;
            g.setColor(Color.DARK_GRAY); g.fillRect(0, 0, width, height);
            if (healthPercentage > 0.7) g.setColor(Color.GREEN);
            else if (healthPercentage > 0.3) g.setColor(Color.YELLOW);
            else g.setColor(Color.RED);
            g.fillRect(0, 0, (int) (width * healthPercentage), height);
            g.setColor(Color.BLACK); g.drawRect(0, 0, width - 1, height - 1);
        }
    }
}
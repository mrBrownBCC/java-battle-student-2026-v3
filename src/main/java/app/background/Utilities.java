package app.background;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

class RestrictedRobotClassLoader extends URLClassLoader {
    private static final Set<String> ALLOWED_PACKAGE_PREFIXES = Set.of(
        "app.background.",
        "app.robots.",
        "java.lang.",
        "java.util."
    );

    private static final Set<String> ALLOWED_EXACT_CLASSES = Set.of(
        "java.lang.invoke.StringConcatFactory",
        "java.lang.invoke.MethodHandles",
        "java.lang.invoke.MethodHandles$Lookup",
        "java.lang.invoke.MethodType",
        "java.lang.invoke.CallSite",
        "java.lang.invoke.LambdaMetafactory"
    );

    private static final Set<String> BLOCKED_EXACT_CLASSES = Set.of(
        "java.lang.System",
        "java.lang.Runtime",
        "java.lang.Process",
        "java.lang.ProcessBuilder",
        "java.lang.Thread",
        "java.lang.Runnable",
        "java.lang.ThreadGroup",
        "java.lang.ThreadLocal",
        "java.lang.ClassLoader",
        "java.util.Timer",
        "java.util.TimerTask",
        "java.util.ServiceLoader"
    );

    private static final Set<String> BLOCKED_PACKAGE_MATCHERS = Set.of(
        "java.lang.reflect.",
        "java.lang.invoke.",
        "java.lang.management.",
        "java.util.concurrent.",
        "java.util.logging.",
        "java.util.prefs."
    );

    private final Path classRoot;

    public RestrictedRobotClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.classRoot = resolveClassRoot(urls);
    }

    private static Path resolveClassRoot(URL[] urls) {
        if (urls == null || urls.length == 0) {
            return null;
        }

        try {
            return Paths.get(urls[0].toURI());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (classRoot != null) {
            Class<?> standardClass = loadClassFromPath(name, classRoot.resolve(toStandardClassPath(name)));
            if (standardClass != null) {
                return standardClass;
            }

            if (name.startsWith("app.robots.Level_")) {
                Class<?> testRobotClass = loadClassFromPath(name, classRoot.resolve(toTestRobotClassPath(name)));
                if (testRobotClass != null) {
                    return testRobotClass;
                }
            }
        }

        throw new ClassNotFoundException(name);
    }

    private Class<?> loadClassFromPath(String name, Path classPath) throws ClassNotFoundException {
        if (classPath == null || !Files.exists(classPath)) {
            return null;
        }

        try {
            byte[] bytes = Files.readAllBytes(classPath);
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to read class bytes for " + name + " from " + classPath, e);
        }
    }

    private String toStandardClassPath(String name) {
        return name.replace('.', File.separatorChar) + ".class";
    }

    private String toTestRobotClassPath(String name) {
        String suffix = name.substring("app.robots.".length());
        return "app" + File.separator + "robots" + File.separator + "Test Robots" + File.separator + suffix.replace('.', File.separatorChar) + ".class";
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isAllowedByPackage(name)) {
            throw new ClassNotFoundException("Access denied (not in allowlist): " + name);
        }

        if (isBlocked(name)) {
            throw new ClassNotFoundException("Access denied by policy: " + name);
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);

            if (loaded == null) {
                if (name.startsWith("app.robots.")) {
                    loaded = findClass(name);
                } else {
                    loaded = super.loadClass(name, false);
                }
            }

            if (resolve) {
                resolveClass(loaded);
            }

            return loaded;
        }
    }

    private static boolean isBlocked(String name) {
        if (ALLOWED_EXACT_CLASSES.contains(name)) {
            return false;
        }

        for (String blockedClass : BLOCKED_EXACT_CLASSES) {
            if (name.equals(blockedClass)) {
                return true;
            }
        }

        for (String blockedPackage : BLOCKED_PACKAGE_MATCHERS) {
            if (name.contains(blockedPackage)) {
                if (ALLOWED_EXACT_CLASSES.contains(name)) {
                    return false;
                }
                return true;
            }
        }

        return false;
    }

    private static boolean isAllowedByPackage(String name) {
        if (ALLOWED_EXACT_CLASSES.contains(name)) {
            return true;
        }

        for (String prefix : ALLOWED_PACKAGE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}

public class Utilities {
    static BufferedImage WALL_IMAGE;
    static BufferedImage GRASS_IMAGE;
    static BufferedImage MUD_IMAGE;
    static BufferedImage ROBOT_ERROR;
    static BufferedImage DEFAULT_PROJECTILE_IMAGE;
    static BufferedImage HEALTH_PACK_IMAGE;
    static BufferedImage SPEED_PACK_IMAGE;
    static BufferedImage ATTACK_PACK_IMAGE;

    public static final int WALL = 0;
    public static final int GRASS = 1;
    public static final int MUD = 2;

    static final double POWER_UP_SPAWN_CHANCE = 0.006;

    public static final int GAME_DELAY = 5;

    public static final int SCREEN_WIDTH = 768;
    public static final int SCREEN_HEIGHT = 576;
    public static final int TILE_SIZE = 32;
    public static final int PROJECTILE_SIZE = 10;
    public static final int POWER_UP_SIZE = 20;
    public static final int ROBOT_SIZE = 32;

    static ArrayList<Integer> keysPressed = new ArrayList<>();
    
    private static java.util.Map<String, Class<?>> loadedRobotClasses = new HashMap<>();
    private static java.util.Map<String, RestrictedRobotClassLoader> robotLoaders = new HashMap<>();
    private static final java.util.Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();
    private static final String ROBOT_PACKAGE = "app.robots";

    static BufferedImage loadImage(String imgName) {
        if (imgName == null) {
            return null;
        }

        BufferedImage cached = imageCache.get(imgName);
        if (cached != null) {
            return cached;
        }

        try {
            BufferedImage img = ImageIO.read(new File("src/main/resources/images/" + imgName));
            BufferedImage cropped = cropToContent(img);
            imageCache.put(imgName, cropped);
            return cropped;
        } catch (IOException e) {
            System.err.println("Failed to load image: " + imgName + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    static void loadImages() {
        WALL_IMAGE = loadImage("wall.png");
        GRASS_IMAGE = loadImage("grass.png");
        MUD_IMAGE = loadImage("mud.png");
        ROBOT_ERROR = loadImage("robotError.png");
        DEFAULT_PROJECTILE_IMAGE = loadImage("defaultProjectile.png");

        if (WALL_IMAGE == null) {
            System.err.println("WALL_IMAGE could not be loaded.");
        }
        if (GRASS_IMAGE == null) {
            System.err.println("GRASS_IMAGE could not be loaded.");
        }
        if (MUD_IMAGE == null) {
            System.err.println("MUD_IMAGE could not be loaded.");
        }
    }

    static BufferedImage cropToContent(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        int minX = width;
        int minY = height;
        int maxX = 0;
        int maxY = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = src.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;

                if (alpha > 0) {
                    if (x < minX)
                        minX = x;
                    if (y < minY)
                        minY = y;
                    if (x > maxX)
                        maxX = x;
                    if (y > maxY)
                        maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        return src.getSubimage(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }

    static void handleKeyPressed(int keyCode) {
        if (!keysPressed.contains(keyCode)) {
            keysPressed.add(keyCode);
        }
    }

    static void handleKeyReleased(int keyCode) {
        for (int i = 0; i < keysPressed.size(); i++) {
            if (keysPressed.get(i) == keyCode) {
                keysPressed.remove(i);
                break;
            }
        }
    }

    private static boolean[] testKeyStates = new boolean[256];

    static void resetKeyStates() {
        Arrays.fill(testKeyStates, false);
    }

    static void setKeyPressed(int keyCode, boolean isPressed) {
        if (keyCode < testKeyStates.length) {
            testKeyStates[keyCode] = isPressed;
        }
    }

    private static boolean inTestMode = false;

    static void setTestMode(boolean enabled) {
        inTestMode = enabled;
    }

    static boolean isKeyPressed(int keyCode) {
        if (inTestMode) {
            return keyCode < testKeyStates.length && testKeyStates[keyCode];
        } else {
            return keysPressed.contains(keyCode);
        }
    }

    static void preloadRobotClasses() {
        File robotsResourceDir = new File("src/main/resources/robots");
        loadedRobotClasses.clear();
        robotLoaders.clear();
        
        if (!robotsResourceDir.exists() || !robotsResourceDir.isDirectory()) {
            System.err.println("Robots resource directory not found: " + robotsResourceDir.getAbsolutePath());
            return;
        }
        
        try {
            loadClassesFromDirectory(robotsResourceDir, robotsResourceDir);
            
            System.out.println("Preloaded " + loadedRobotClasses.size() + " robot classes in isolated loaders");
        } catch (Exception e) {
            System.err.println("Failed to preload robot classes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void loadClassesFromDirectory(File dir, File rootDir) {
        if (!dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                loadClassesFromDirectory(file, rootDir);
            } else if (file.isFile() && file.getName().endsWith(".class")) {
                String relativeClassPath = rootDir.toPath().relativize(file.toPath()).toString();
                String normalizedClassPath = relativeClassPath.replace("Test Robots" + File.separator, "");
                String fullClassName = normalizedClassPath
                        .replace(File.separatorChar, '.')
                    .replaceAll("\\.class$", "");

                if (!fullClassName.startsWith(ROBOT_PACKAGE + ".") || fullClassName.contains("$")) {
                    continue;
                }

                String className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                
                try {
                    URL robotsUrl = rootDir.toURI().toURL();
                    RestrictedRobotClassLoader isolatedLoader = new RestrictedRobotClassLoader(
                            new URL[] { robotsUrl }, 
                            Utilities.class.getClassLoader()
                    );
                    
                    robotLoaders.put(fullClassName, isolatedLoader);
                    
                    Class<?> loadedClass = isolatedLoader.loadClass(fullClassName);

                    if (loadedClass.getClassLoader() != isolatedLoader) {
                        throw new SecurityException(
                            "Robot class was not defined by its restricted loader: " + fullClassName +
                            " (actual loader=" + loadedClass.getClassLoader() + ")"
                        );
                    }

                    if (!Robot.class.isAssignableFrom(loadedClass)) {
                        continue;
                    }

                    loadedClass.getConstructor(int.class, int.class);
                    loadedRobotClasses.put(className, loadedClass);
                    System.out.println("Preloaded robot in isolated loader: " + fullClassName + " (cached as " + className + ")");
                } catch (Throwable t) {
                    System.err.println("Failed to preload class " + fullClassName + ": " + t.getMessage());
                }
            }
        }
    }

    static ArrayList<String> getLoadedRobotNames() {
        ArrayList<String> names = new ArrayList<>(loadedRobotClasses.keySet());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }
    
    static Class<?> getRobotClass(String className) {
        return loadedRobotClasses.get(className);
    }

    static Robot createRobot(int x, int y, String className) {
        Class<?> loadedClass = getRobotClass(className);
        
        if (loadedClass == null) {
            System.err.println("Robot class not preloaded: " + className);
            return null;
        }

        if (!(loadedClass.getClassLoader() instanceof RestrictedRobotClassLoader)) {
            System.err.println("Refusing to instantiate robot not loaded by restricted loader: " + className +
                    " (actual loader=" + loadedClass.getClassLoader() + ")");
            return null;
        }
        
        try {
            Constructor<?> constructor = loadedClass.getConstructor(int.class, int.class);
            Robot robot = (Robot) constructor.newInstance(x, y);

            System.out.println("Created robot instance: " + className);
            return robot;
        } catch (NoSuchMethodException e) {
            System.err.println("Constructor (int, int) not found for " + className + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error instantiating robot class " + className + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
}
package org.coffeepop.latchac.core.check;

import org.coffeepop.latchac.core.LatchAC;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Scans the {@code latchac-core} JAR for {@link CheckInfo}-annotated
 * classes under the {@code impl} package and registers them.
 * <p>
 * Locates the JAR via {@link LatchAC#class}'s {@code URLClassLoader} URLs
 * — works reliably under Paper's plugin classloader.
 */
public final class CheckScanner {

    private static final String PKG = "org/coffeepop/latchac/core/check/impl";

    private CheckScanner() {}

    public static void scanAndRegister(Logger logger) {
        if (!(LatchAC.class.getClassLoader() instanceof URLClassLoader ucl)) {
            logger.warning("CheckScanner: classloader is not URLClassLoader");
            return;
        }
        for (URL url : ucl.getURLs()) {
            try {
                String path = new File(url.toURI()).getPath();
                if (path.endsWith(".jar")) scanJar(path, logger);
            } catch (Exception e) {
                logger.warning("CheckScanner: bad URL " + url);
            }
        }
    }

    private static void scanJar(String path, Logger logger) {
        try (JarFile jar = new JarFile(path)) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(PKG) && name.endsWith(".class") && !name.contains("$")) {
                    register(name, logger);
                }
            }
        } catch (Exception e) {
            logger.warning("CheckScanner: failed to scan " + path + " — " + e.getMessage());
        }
    }

    private static void register(String classPath, Logger logger) {
        try {
            String className = classPath.replace('/', '.').replace(".class", "");
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(CheckInfo.class) && Check.class.isAssignableFrom(clazz)) {
                Check check = (Check) clazz.getDeclaredConstructor().newInstance();
                LatchAC.get().registerCheck(check);
            }
        } catch (Exception e) {
            logger.warning("CheckScanner: failed " + classPath + " — " + e.getMessage());
        }
    }
}

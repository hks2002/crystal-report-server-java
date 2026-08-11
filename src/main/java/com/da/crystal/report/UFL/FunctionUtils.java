package com.da.crystal.report.UFL;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.crystaldecisions.reports.formulas.FormulaFunction;

import lombok.extern.log4j.Log4j2;

/**
 * FunctionUtils
 *
 * <p>
 * Scans a package for all classes implementing {@link FormulaFunction} and
 * instantiates them. Used by {@link JavaUFL} to auto-discover UFL functions.
 * </p>
 */
@Log4j2
public final class FunctionUtils {

  private FunctionUtils() {
  }

  /**
   * Scans the given package for all non-abstract classes that implement
   * {@link FormulaFunction} and returns a list of new instances.
   *
   * @param packageName  the fully-qualified package name (e.g.
   *                     {@code com.da.crystal.report.UF})
   * @param resourcePath the resource path form of the package (e.g.
   *                     {@code com/da/crystal/report/UF})
   * @return a list of instantiated {@link FormulaFunction}s (never null)
   */
  public static List<FormulaFunction> scanFunctions(String packageName, String resourcePath) {
    List<FormulaFunction> result = new ArrayList<>();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try {
      Enumeration<URL> resources = classLoader.getResources(resourcePath);
      List<Class<?>> classes = new ArrayList<>();

      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        String protocol = resource.getProtocol();
        if ("file".equals(protocol)) {
          scanDirectory(new File(resource.getFile()), packageName, classes);
        } else if ("jar".equals(protocol)) {
          String jarPath = resource.getPath();
          if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring(5);
          }
          int bangIndex = jarPath.indexOf("!");
          if (bangIndex > 0) {
            jarPath = jarPath.substring(0, bangIndex);
          }
          scanJar(jarPath, packageName, resourcePath, classes);
        }
      }

      log.debug("Scanned {} candidate classes in package {}", classes.size(), packageName);

      for (Class<?> clazz : classes) {
        if (FormulaFunction.class.isAssignableFrom(clazz)
            && !clazz.isInterface()
            && !clazz.isEnum()
            && !clazz.isAnonymousClass()) {
          try {
            FormulaFunction instance = (FormulaFunction) clazz.getDeclaredConstructor().newInstance();
            result.add(instance);
            log.debug("Discovered UFL function: {} (from {})", instance.getIdentifier(), clazz.getSimpleName());
          } catch (Exception e) {
            log.error("Failed to instantiate UFL class {}: {}", clazz.getName(), e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to scan for UFL functions: {}", e.getMessage(), e);
    }

    return result;
  }

  /**
   * Scans a directory for .class files and loads them.
   */
  private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
    if (!directory.exists()) {
      return;
    }
    File[] files = directory.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        scanDirectory(file, packageName, classes);
      } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
        String className = file.getName().substring(0, file.getName().length() - 6);
        String fullClassName = packageName + "." + className;
        try {
          Class<?> clazz = Class.forName(fullClassName);
          classes.add(clazz);
        } catch (ClassNotFoundException e) {
          log.debug("Could not load class: {}", fullClassName);
        }
      }
    }
  }

  /**
   * Scans a JAR file for .class files in the given package.
   */
  private static void scanJar(String jarPath, String packageName, String resourcePath, List<Class<?>> classes) {
    try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath)) {
      Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        java.util.jar.JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (name.startsWith(resourcePath + "/")
            && name.endsWith(".class")
            && !name.contains("$")) {
          String className = name.substring(name.lastIndexOf('/') + 1, name.length() - 6);
          String fullClassName = packageName + "." + className;
          try {
            Class<?> clazz = Class.forName(fullClassName);
            classes.add(clazz);
          } catch (ClassNotFoundException e) {
            log.debug("Could not load class from JAR: {}", fullClassName);
          }
        }
      }
    } catch (Exception e) {
      log.debug("Could not scan JAR {}: {}", jarPath, e.getMessage());
    }
  }
}

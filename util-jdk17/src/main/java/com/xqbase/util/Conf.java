package com.xqbase.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Easy Locating Path<p>
 * Easy Loading/Storing Configurations<p>
 * Easy Opening/Closing Logs<p>
 * Easy Traversing Classes
 */
public class Conf {
	public static boolean DEBUG = false;

	private static String rootDir = null, confDir = null, logDir = null;

	static {
		for (String argument : ManagementFactory.
				getRuntimeMXBean().getInputArguments()) {
			if (argument.startsWith("-agentlib:jdwp")) {
				DEBUG = true;
				break;
			}
		}

		Properties p = load("Conf");
		confDir = p.getProperty("conf_dir");
		logDir = p.getProperty("log_dir");
	}

	private static void load(Properties p, String path) {
		try (FileInputStream in = new FileInputStream(path)) {
			p.load(in);
		} catch (FileNotFoundException e) {
			// Ignored
		} catch (IOException e) {
			Log.w(e.getMessage());
		}
	}

	private static String getConfPath(String name, String confDir_) {
		return confDir_ == null ? getAbsolutePath("conf/" + name + ".properties") :
				(confDir_.endsWith("/") ? confDir_ : confDir_ + "/") +
				name + ".properties";
	}

	private static Class<?> getParentClass() {
		for (StackTraceElement ste : new Throwable().getStackTrace()) {
			String className = ste.getClassName();
			if (!className.equals(Conf.class.getName())) {
				try {
					return Class.forName(className);
				} catch (ReflectiveOperationException e) {
					Log.e(e);
					return Conf.class;
				}
			}
		}
		return Conf.class;
	}

	/**
	 * Change the current folder
	 */
	public static synchronized void chdir(String path) {
		if (path != null) {
			rootDir = new File(getAbsolutePath(path)).getAbsolutePath();
		}
	}

	/**
	 * Get the absolute path relative to the current folder,
	 * which is the parent folder of <b>classpath</b> (<b>classes/</b> or <b>lib/</b>) by default
	 */
	public static synchronized String getAbsolutePath(String path) {
		try {
			if (rootDir == null) {
				Class<?> parentClass = getParentClass();
				String classFile = parentClass.getName().replace('.', '/') + ".class";
				URL url = parentClass.getResource("/" + classFile);
				if (url == null) {
					return null;
				}
				if (url.getProtocol().equals("jar")) {
					rootDir = url.getPath();
					int i = rootDir.lastIndexOf('!');
					if (i >= 0) {
						rootDir = rootDir.substring(0, i);
					}
					rootDir = new File(new URL(rootDir).toURI()).getParent();
				} else {
					rootDir = new File(url.toURI()).getPath();
					rootDir = rootDir.substring(0, rootDir.length() - classFile.length());
					if (rootDir.endsWith(File.separator)) {
						rootDir = rootDir.substring(0, rootDir.length() - 1);
					}
				}
				rootDir = new File(rootDir).getParent();
			}
			return new File(rootDir + File.separator + path).getCanonicalPath();
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Open a {@link Logger} with output file under folder <b>log_dir</b>
	 * (if defined in Conf.properties) or <b>logs/</b> relative to the current folder
	 *
	 * @param name logging output file with the pattern "${name}%g.log"
	 * @param limit the maximum number of bytes to write to any one file count the number of files to use
	 * @param count the number of files to use
	 */
	public static Logger openLogger(String name, int limit, int count) {
		Logger logger = Logger.getAnonymousLogger();
		logger.setLevel(Level.ALL);
		logger.setUseParentHandlers(false);
		if (DEBUG) {
			ConsoleHandler handler = new ConsoleHandler();
			handler.setLevel(Level.ALL);
			logger.addHandler(handler);
		}
		FileHandler handler;
		try {
			String logDir_ = logDir == null ? getAbsolutePath("logs") : logDir;
			new File(logDir_).mkdirs();
			String pattern = (logDir_.endsWith("/") ? logDir_ : logDir_ + "/") +
					name + "%g.log";
			handler = new FileHandler(pattern, limit, count, true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		return logger;
	}

	/**
	 * Close a logger
	 */
	public static void closeLogger(Logger logger) {
		for (Handler handler : logger.getHandlers()) {
			logger.removeHandler(handler);
			handler.close();
		}
	}

	/**
	 * Load the following properties (if exists) successively:
	 * <ul>
	 * <li>A. <b>/${name}.properties</b> as resource (in <b>classes/</b> folder or jar file)</li>
	 * <li>B. <b>conf/${name}.properties</b> relative to the current folder
	 *		(the parent folder of <b>classpath</b> by default)</li>
	 * <li>C. <b>${name}.properties</b> under folder <b>conf_dir</b> defined in Conf.properties</li>
	 * </ul>
	 */
	public static Properties load(String name) {
		Properties p = new Properties();
		InputStream in = getParentClass().getResourceAsStream("/" + name + ".properties");
		if (in != null) {
			try {
				p.load(in);
			} catch (IOException e) {
				Log.w(e.getMessage());
			}
		}
		load(p, getConfPath(name, null));
		if (confDir != null) {
			load(p, getConfPath(name, confDir));
		}
		return p;
	}

	/**
	 * Store the properties file into <b>conf_dir</b> (if defined in Conf.properties) or
	 * <b>conf/</b> relative to the current folder
	 */
	public static void store(String name, Properties p) {
		new File(confDir == null ? getAbsolutePath("conf") : confDir).mkdirs();
		try (FileOutputStream out = new FileOutputStream(getConfPath(name, confDir))) {
			p.store(out, null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final Set<String> TRUE_VALUES =
			new HashSet<>(Arrays.asList("true", "yes", "on", "enable", "enabled"));
	private static final Set<String> FALSE_VALUES =
			new HashSet<>(Arrays.asList("false", "no", "off", "disable", "disabled"));

	/**
	 * @return <b>true</b> for "true", "yes", "on", "enable" or "enabled";
	 *		   <b>false</b> for "false", "no", "off", "disable" or "disabled"
	 */
	public static boolean getBoolean(String value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		String value_ = value.trim().toLowerCase();
		return defaultValue ? !FALSE_VALUES.contains(value_) : TRUE_VALUES.contains(value_);
	}

	private static void search(List<String> classes, String root, String path) {
		File folder = new File(root + path);
		String[] list = folder.list();
		if (list == null) {
			return;
		}
		for (String file : folder.list()) {
			if (new File(root + path + "/" + file).isDirectory()) {
				search(classes, root, path + "/" + file);
			} else if (file.endsWith(".class")) {
				classes.add(path.replace('/', '.') + "." +
						file.substring(0, file.length() - 6));
			}
		}
	}

	/**
	 * Traverse all classes under given packages
	 */
	public static List<String> getClasses(String... packageNames) {
		List<String> classes = new ArrayList<>();
		for (String packageName : packageNames) {
			String packagePath = packageName.replace('.', '/');
			URL url = Conf.class.getResource("/" + packagePath);
			if (url == null) {
				return classes;
			}
			if (url.getProtocol().equals("jar")) {
				String path = url.getPath();
				int i = path.lastIndexOf('!');
				if (i >= 0) {
					path = path.substring(0, i);
				}
				try (JarFile jar = new JarFile(new File(new URL(path).toURI()))) {
					Enumeration<JarEntry> e = jar.entries();
					while (e.hasMoreElements()) {
						path = e.nextElement().getName();
						if (path.startsWith(packagePath) && path.endsWith(".class")) {
							classes.add(path.substring(0, path.length() - 6).replace('/', '.'));
						}
					}
				} catch (IOException | URISyntaxException e) {
					throw new RuntimeException(e);
				}
			} else {
				try {
					String root = new File(url.toURI()).getPath();
					root = root.substring(0, root.length() - packagePath.length());
					search(classes, root, packagePath);
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return classes;
	}
}
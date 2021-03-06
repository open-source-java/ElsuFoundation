/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package elsu.support;

import elsu.common.*;
import java.util.*;
import java.io.*;
import java.nio.file.Paths;
import java.text.*;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.*;

/**
 * ConfigLoader is the base class for factory. The core purpose is to load the
 * app.config provided through the application command line arguments or the
 * default app.config stored as the resource in the jar file.
 * <p>
 * app.config once extracted from the jar file is not over-written every time
 * but reused allowing the user to change the extracted app.config file.
 * <p>
 * The configuration load is done using the direct XPath references to the node
 * properties and recursive nodes are processed by first collecting the node
 * names into a list and then iterating over the list.
 * <p>
 * log4j.properties is also extracted upon initial run of the program. Logging
 * is configured during the initial load.
 *
 * 6/12/16 - moved xml show config to separate class XMLViewer 10/20/19 -
 * removed endsWith for path to add / at the end if not provided
 * 
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @version .53
 */
public class ConfigLoader {

	// <editor-fold desc="class private storage">
	// runtime sync object
	private Object _runtimeSync = new Object();

	// static property for app.config store and extraction from jar file
	// 20170110 - changed from _APPCONFIG to _RESOURCEPATH
	private static String _RESOURCEPATH = "config/app.config";
	private static String _LOGCONFIGFILE = "log4j.properties";
	private static String _LOGCLASS = "logDefault";
	private static String _LOGDATETIME = "yyyyMMdd_HHmmss";

	// .52 added to allow specification of alternate extract directory when
	// security permissions deny write into deployed location
	private static String _LOGPATH = "logs/";
	// 20170110 - changed from _TEMPPATH to _LOCALPATH
	private static String _LOCALPATH = "output/";

	// static property for data format across the application for display
	// purposes
	private static String _DTGFORMAT = "YYYMMDD HH24:mm:ss";

	// store all properties from app.config
	private Map<String, Object> _properties = new HashMap<>();

	// array of path strings which need to be filtered from hashmap
	private String[] _filterPath = new String[] {};

	// system logger if configured
	public static String _LOGFILENAMEPROPERTY = "application.framework.attributes.key.log.filename";
	public static String _LOGCONFIGPROPERTY = "application.framework.attributes.key.log.config";
	public static String _LOGCLASSPROPERTY = "application.framework.attributes.key.log.class";
	public static String _LOGPATHPROPERTY = "application.framework.attributes.key.log.path";
	private Log4JManager _log4JManager = null;
	// </editor-fold>

	// <editor-fold desc="class constructor destructor">
	/**
	 * ConfigLoader() no-argument constructor is used to load the default
	 * app.config which is set through the static APPCONFIG variable prior to
	 * instantiation of the class.
	 * <p>
	 * Constructor will try to extract the stored app.config in the application
	 * jar file if available.
	 *
	 * @throws Exception
	 */
	public ConfigLoader() throws Exception {
		this(ConfigLoader._RESOURCEPATH, null);
	}

	public ConfigLoader(String[] filterPath) throws Exception {
		this(ConfigLoader._RESOURCEPATH, filterPath);
	}

	// .52 updated to allow specification of alternate extract directory when
	// security permissions deny write into deployed location
	public ConfigLoader(String config, String[] filterPath) throws Exception {
		String file = "";

		try {
			// update filter path before processing
			if (filterPath != null) {
				this._filterPath = filterPath;
			}

			// if config is null, then use the default
			if ((config == null) || (config.isEmpty())) {
				config = ConfigLoader._RESOURCEPATH;
			}

			// check the format, if raw xml?
			if (config.startsWith("<?xml ")) {
				// try to create the XML reader instance for XML document
				// parsing
				// using the app.config file location
				file = config;
			} else {
				// load the resource or file path
				ConfigLoader._RESOURCEPATH = config;
				String configFile;

				// check is app.config and log4j.properties file is stored in
				// the
				// application; note, if variable already contains a path then
				// external config is used view package extraction
				configFile = ConfigLoader._RESOURCEPATH;

				// extract file to local file system
				extractConfigFile(configFile);

				// try to create the XML reader instance for XML document
				// parsing
				// using the app.config file location
				file = configFile;
			}

			// display the config to the user
			showConfig(file);

			// load the config into application or service properties hashMaps
			getProperties().clear();
			this._properties = initializeConfig(file);

			// open log if provided
			for (String key : getProperties().keySet()) {
				if (key.equals(_LOGCONFIGPROPERTY)) {
					try {
						initializeLogger(getProperty(key).toString());
					} catch (Exception ex) {
						System.out.println(getClass().toString() + ", " + "ConfigLoader(), " + ex.getMessage());
					}

					break;
				}
			}

			logInfo("configuration loaded.");
		} catch (Exception ex) {
			// display exception to the user and exit
			System.out.println(getClass().toString() + ", " + "ConfigLoader(), " + ex.getMessage());
		}
	}

	/**
	 * ConfigLoader(...) constructor is used to load custom configuration passed
	 * through the string variable. Normally used by control service to pass
	 * custom XML sent from the client.
	 *
	 * @param config
	 *            is the Map<String, Object> data passed from the calling
	 *            function.
	 * @throws Exception
	 */
	public ConfigLoader(Map<String, Object> config) throws Exception {
		try {
			// if config is null, then throw exception
			if ((config == null) || (config.size() == 0)) {
				throw new Exception("config passed is empty.");
			}

			// display the config to the user
			showConfig(config);

			// load the config into application or service properties hashMaps
			initializeConfig(config);

			// open log if provided
			for (String key : getProperties().keySet()) {
				if (key.equals(_LOGCONFIGPROPERTY)) {
					try {
						initializeLogger(getProperty(key).toString());
					} catch (Exception ex) {
						System.out.println(getClass().toString() + ", " + "ConfigLoader()/2, " + ex.getMessage());
					}

					break;
				}
			}

			logInfo("configuration loaded.");
		} catch (Exception ex) {
			// display exception to the user and exit
			System.out.println(getClass().toString() + ", " + "ConfigLoader()/2, " + ex.getMessage());
		}
	}

	/**
	 * initializeConfig() clears the storage application and service hashMaps
	 * and then loads the app.config using XPath to reference each property.
	 *
	 * @throws Exception
	 * 
	 *             20170105 - updated from private to public static
	 */
	public static Map<String, Object> initializeConfig(String file) throws Exception {
		Map<String, Object> result = null;

		XML2Map xml = new XML2Map(file);
		result = xml.getProperties();
		xml = null;

		return result;
	}

	private void initializeConfig(Map<String, Object> config) throws Exception {
		// clear the storage hashMaps
		getProperties().clear();
		_properties = config;
	}
	// </editor-fold>

	// <editor-fold desc="class getter/setters">
	public static String getResourcePath() {
		return _RESOURCEPATH;
	}

	public static void setResourcePath(String path) {
		_RESOURCEPATH = path;
	}

	public static String getDTGFormat() {
		return _DTGFORMAT;
	}

	public static void setDTGFormat(String format) {
		_DTGFORMAT = format;
	}

	public static String getLOGDATETIME() {
		return _LOGDATETIME;
	}

	public static void setLOGDATETIME(String logDatetime) {
		_LOGDATETIME = logDatetime;
	}

	public static String getLocalPath() {
		return _LOCALPATH;
	}

	public static void setLocalPath(String path) {
		_LOCALPATH = path;
	}

	public static String getLogPath() {
		return _LOGPATH;
	}

	public static void setLogPath(String path) {
		_LOGPATH = path;
	}

	/**
	 * getApplicationProperties() method returns the hashMap containing the
	 * application properties key/value pair extracted from the app.config
	 * application.attributes section
	 *
	 * @return <code>hashMap</code> key/value set of all application properties
	 */
	public Map<String, Object> getProperties() {
		return this._properties;
	}

	public Object getProperty(String key) {
		return getProperties().get(key);
	}

	public Set<String> getKeySet() {
		return getProperties().keySet();
	}

	public List<String> getClassSet() {
		List<String> result = new ArrayList<>();

		for (String key : getProperties().keySet()) {
			if (key.endsWith(".class") && !key.endsWith("key.log.class")) {
				result.add(key);
			}
		}

		return result;
	}

	public List<String> getClassSet(String partialKey) {
		List<String> result = new ArrayList<>();

		for (String key : getProperties().keySet()) {
			if ((key.startsWith(partialKey)) && key.endsWith(".class")) {
				result.add(key);
			}
		}

		return result;
	}

	public String getKeyByValue(String value) {
		String result = "";

		for (String key : getProperties().keySet()) {
			if (getProperty(key).equals(value)) {
				result = key;
				break;
			}
		}
		return result;
	}

	public static String getTempLogName() {
		return String.format("TMPLOG%s.LOG",
				"_" + new SimpleDateFormat(_LOGDATETIME).format(Calendar.getInstance().getTime()));
	}

	public static String getLogName(String name) {
		return String.format(name + "%s.LOG",
				"_" + new SimpleDateFormat(_LOGDATETIME).format(Calendar.getInstance().getTime()));
	}
	// </editor-fold>

	// <editor-fold desc="class methods">
	/**
	 * extractConfigFile(...) method verifies if the external config exists, if
	 * not, it tries to extract the config file from jar file. If either are
	 * unsuccessful, exception is thrown to notify user of missing config.
	 *
	 * @param filename
	 *            location of the config file
	 * @throws Exception
	 */
	public static String extractConfigFile(String filename) throws Exception {
		return extractConfigFile(filename, null);
	}

	public static String extractConfigFile(String filename, String tempPath) throws Exception {
		// storage for result
		String result = null;

		// create a reference to the location of the configuration file
		File cf = null;
		if (elsu.common.StringUtils.IsNull(tempPath)) {
			result = filename;
		} else {
			result = tempPath + Paths.get(filename).getFileName();

			System.out.println("elsu.ConfigLoader, " + "extractConfigFile(), " + result);
		}
		cf = new File(result);

		// if the file does not exist, try to extract it from the jar resource
		if (!cf.exists()) {
			// notify the user we are extracting the store app.config
			System.out.println("elsu.ConfigLoader, " + "extractConfigFile(), " + filename);

			// create directories
			cf.getParentFile().mkdirs();

			// open the input stream from the jar resource
			BufferedReader configIFile = null;
			configIFile = new BufferedReader(new InputStreamReader(
					ConfigLoader.class.getClassLoader().getResourceAsStream(filename.replace("\\", "/"))));

			// declare storage for the output file
			BufferedWriter configOFile = null;

			// if input file if valid, then extract the data
			if (configIFile != null) {
				try {
					// open the output file
					configOFile = new BufferedWriter(new FileWriter(cf));

					// declare storage for the data from the input stream
					String line;

					// loop the config file, read each line until no more data
					while ((line = configIFile.readLine()) != null) {
						// write the data to the output file and insert the new
						// line terminator after each line
						configOFile.write(line + GlobalStack.LINESEPARATOR);

						// yield processing to other threads
						Thread.yield();
					}

					// notify user the status of the config file
					System.out.println("elsu.ConfigLoader, " + "extractConfigFile(), config file extracted successfully");
				} catch (Exception ex) {
					// if exception during processing, return it to the user
					throw new Exception("elsu.ConfigLoader:extractConfigFile()//" + ex.getMessage());
				} finally {
					// close the input file to prevent resource leaks
					try {
						configIFile.close();
					} catch (Exception exi) {
					}

					// close the output file to prevent resource leaks
					if (configOFile != null) {
						try {
							configOFile.flush();
						} catch (Exception exi) {
						}
						try {
							configOFile.close();
						} catch (Exception exi) {
						}
					}
				}
			}
		} else {
			// config file already existed, notify user we are using it
			System.out.println("elsu.ConfigLoader, " + "extractConfigFile(), using config file: " + result);
		}

		return result;
	}

	/**
	 * extractWebConfig(...) method verifies if the external config exists, if
	 * not, it tries to extract the config file from jar file. If either are
	 * unsuccessful, exception is thrown to notify user of missing config.
	 *
	 * @param filename
	 *            location of the config file
	 * @throws Exception
	 */
	/**
	 * showConfig() method displays the configuration to the console output.
	 *
	 */
	private void showConfig(String config) throws Exception {
		new XMLViewer(config, 1);

		// System.out.println("------------");
		// org.w3c.dom.NodeList nl = _xmlr.getNodesByElement("connections");
		// for (int i = 0; i < nl.getLength(); i++) {
		// showConfigNodes(nl.item(i), 1);
		// }
		// System.out.println("------------");
		// nl = _xmlr.getNodesByElement("service");
		// for (int i = 0; i < nl.getLength(); i++) {
		// System.out.println("---" + nl.item(i).getNodeName());
		// showConfigNodes(nl.item(i), 1);
		// }
	}

	private void showConfig(Map<String, Object> config) {
		// assign new config to the variable
		for (String key : config.keySet()) {
			System.out.println(key + " (TEXT=" + config.get(key) + ")");
		}
	}

	public void addProperties(Map<String, Object> properties) {
		for (String key : properties.keySet()) {
			addProperty(key, properties.get(key));
		}
	}

	public void addProperty(String key, Object value) {
		// check the filterPath to ensure the key does not start with the filter
		if (_filterPath.length > 0) {
			Boolean match = false;
			for (String filter : _filterPath) {
				if (key.startsWith(filter)) {
					match = true;
					break;
				}
			}

			if (!match) {
				return;
			}
		}

		// add the key to the keymap for utilization
		getProperties().put(key, value);
	}
	// </editor-fold>

	// <editor-fold desc="class logging">
	/**
	 *
	 */
	// .52 updated to allow specification of alternate extract directory when
	// security permissions deny write into deployed location
	private void initializeLogger(String logConfig) throws Exception {
		// log attribute value is defined, set the static variable to the
		// log property file location; also, check if path is provided as
		// part of the file name - if yes, then ignore class path
		String logFileName = getProperty(ConfigLoader._LOGFILENAMEPROPERTY).toString();
		String logPath = getLogPath();

		// check if logpath is overridden
		if (getProperty(ConfigLoader._LOGPATHPROPERTY) != null) {
			logPath = getProperty(ConfigLoader._LOGPATHPROPERTY).toString();
		}

		if (!logConfig.contains("\\") && !logConfig.contains("/")) {
			ConfigLoader._LOGCONFIGFILE = (new File(getClass().getName().replace(".", "\\"))).getParent() + "\\"
					+ logConfig;
		} else {
			ConfigLoader._LOGCONFIGFILE = logConfig;
		}

		// extract the config file
		extractConfigFile(ConfigLoader._LOGCONFIGFILE);

		// if log.filename is empty, then assign a temporary one
		if ((logFileName == null) || (logFileName.isEmpty())) {
			logFileName = "$TMP_LOG$.LOG";
		} else {
			logFileName = Paths.get(logFileName).getFileName().toString();
		}

		// fix if logPath is provided
		logFileName = logPath + logFileName;
		ConfigLoader._LOGCLASS = getProperty(ConfigLoader._LOGCLASSPROPERTY).toString();

		// if temp file created, then we need to use it
		_log4JManager = new Log4JManager(ConfigLoader._LOGCONFIGFILE, ConfigLoader._LOGCLASS, logFileName);

		System.out.println(getClass().toString() + ", " + "initializeLogger(), log file location: " + logFileName);
	}

	public static Log4JManager initializeLogger(String logConfig, String logClass, String fileName) throws Exception {
		Log4JManager log4JManager = null;

		if (logConfig.isEmpty() || (logConfig.length() == 0)) {
			return null;
		} else {
			log4JManager = new Log4JManager(logConfig, logClass, getLogName(fileName));
		}

		return log4JManager;
	}

	/*
	 * public static Log4JManager initializeLogger(String logPropertyFile,
	 * String fileName) throws Exception { // log attribute value is defined,
	 * set the static variable to the // log property file location; also, check
	 * if path is provided as Log4JManager log4JManager = null;
	 * 
	 * // check if the log property file exists, if not extract it
	 * extractConfigFile(logPropertyFile);
	 * 
	 * log4JManager = new Log4JManager(logPropertyFile, fileName); return
	 * log4JManager; }
	 */
	/**
	 *
	 */
	public org.apache.logging.log4j.Logger getLogger() {
		org.apache.logging.log4j.Logger result = null;

		synchronized (this._runtimeSync) {
			result = this._log4JManager.getLogger();
		}

		return result;
	}

	public void setLogger(Log4JManager pLog4JManager) {
		synchronized (this._runtimeSync) {
			this._log4JManager = pLog4JManager;
		}
	}

	/**
	 * logDebug(...) method is an interface method to Log4JManager logging
	 * capability. This method is provided to allow multiple threads to log to
	 * one file.
	 * <p>
	 * Debug messages are will only be processed if log4j.properties are set to
	 * log debug or info or warn or error or fatal messages
	 *
	 * @param info
	 *            is the object whose string representation will be stored in
	 *            the log file
	 * 
	 *            20170114 - added exception handler to close out the
	 *            synchronized block - cleanly
	 */
	public void logDebug(Object info) {
		synchronized (this._runtimeSync) {
			try {
				getLogger().debug(info.toString());
			} catch (Exception ex) {
				System.out.println(getClass().toString() + ", " + "logDebug()" + ex.getMessage());
			}
		}
	}

	/**
	 * logError(...) method is an interface method to Log4JManager logging
	 * capability. This method is provided to allow multiple threads to log to
	 * one file.
	 * <p>
	 * Error messages are will only be processed if log4j.properties are set to
	 * log error or fatal messages
	 *
	 * @param info
	 *            is the object whose string representation will be stored in
	 *            the log file
	 * 
	 *            20170114 - added exception handler to close out the
	 *            synchronized block - cleanly
	 */
	public void logError(Object info) {
		synchronized (this._runtimeSync) {
			try {
				getLogger().error(info.toString());
			} catch (Exception ex) {
				System.out.println(getClass().toString() + ", " + "logError()" + ex.getMessage());
			}
		}
	}

	/**
	 * logInfo(...) method is an interface method to Log4JManager logging
	 * capability. This method is provided to allow multiple threads to log to
	 * one file.
	 * <p>
	 * Debug messages are will only be processed if log4j.properties are set to
	 * log info or warn or error or fatal messages
	 *
	 * @param info
	 *            is the object whose string representation will be stored in
	 *            the log file
	 * 
	 *            20170114 - added exception handler to close out the
	 *            synchronized block - cleanly
	 */
	public void logInfo(Object info) {
		synchronized (this._runtimeSync) {
			try {
				getLogger().info(info.toString());
			} catch (Exception ex) {
				System.out.println(getClass().toString() + ", " + "logInfo()" + ex.getMessage());
			}
		}
	}
	// </editor-fold>

	@Override
	public String toString() {
		String result = "";

		try {
			result = JsonXMLUtils.Object2JSon(_properties);
		} catch (Exception ex) {
			result = this.getClass().toString() + ".toString(), \n" + ex.getMessage() + "\n" + ex.getStackTrace();
		}

		return result;
	}
}

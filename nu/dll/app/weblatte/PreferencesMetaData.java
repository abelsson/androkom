package nu.dll.app.weblatte;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import java.nio.charset.Charset;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import nu.dll.lyskom.Debug;

/**
 * Singleton class handling all meta-data regarding User-Area user configuration.
 *
 * Reads meta data information from ~/.weblatte.preferences.ini or using the
 * class loader resource nu/dll/app/weblatte/preferences.ini.
 *
 * The data is accessible directly through the instance variable collections
 * "list", "blocks" and "blockKeys", which are immutable, so it is always
 * safe to iterate over them without synchronization.
 * 
 * Convenience methods getDefault() and containsKey() are also provided.
 */
public class PreferencesMetaData {
    public List list = new LinkedList();
    public Map blocks = new HashMap();
    public Map blockKeys = new HashMap();

    private static PreferencesMetaData instance = new PreferencesMetaData();

    private PreferencesMetaData() {

	Map charsets = Charset.availableCharsets();
	String[] charsetNames = new String[charsets.size()];
	Iterator charsetIterator = charsets.keySet().iterator();
	for (int i=0; i < charsets.size(); i++) {
	    charsetNames[i] = (String) charsetIterator.next();
	}

	try {
	    // first check if there's a file ~/.weblatte.preferences.ini
	    // and if it doesn't exist, instead use the default, loaded
	    // as a resource stream.
	    File prefsIniFile = new File(new File(System.getProperty("user.home")),
						   ".weblatte.preferences.ini");
	    InputStream is = null;
	    if (prefsIniFile.exists()) {
		Debug.println("Reading preferences meta-data from " +
			      prefsIniFile.getAbsolutePath());
		is = new FileInputStream(prefsIniFile);
	    } else {
		Debug.println("Reading preferences meta-data as a class " +
			      "loader resource");
		is = getClass().getClassLoader().
		    getResourceAsStream("nu/dll/app/weblatte/preferences.ini");
		if (is == null) {
		    throw new RuntimeException("No preferences meta-data available.");
		}
	    }
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));

	    String block = null;
	    String row = null;
	    Properties object = null;
	    boolean newBlock = false;
	    boolean lastRow = false;
	    do {
		lastRow = row == null && object != null;
		if (row == null) row = "";
		if (!lastRow && row.trim().equals("")) {
		    row = reader.readLine();
		    continue;
		}
		if (row.startsWith("#")) {
		    row = reader.readLine();
		    continue;
		}

		String newBlockName = null;
		if (row.startsWith("[")) {
		    // new block name, but don't assign it until we've
		    // seen if we have just finished reading an object
		    newBlockName = row.replaceAll("[\\[\\]]", "");
		    newBlock = true;
		} else {
		    newBlock = false;
		}
		if (!newBlock && block == null) {
		    throw new IOException("Meta-data file must start with a block identifier (ie \"[common]\")");
		}

		String key = null, value = null;
		if (!newBlock && !lastRow) {
		    StringTokenizer st = new StringTokenizer(row, ":");
		    key = st.nextToken().trim().toLowerCase();
		    value = st.nextToken().trim();
		}

		if (lastRow || newBlock || key.equals("name")) { // new object
		    // construct a PreferenceMetaData instance out of the information in the
		    // object data we've recorded in the Preferences object
		    if (object != null) {
			String name = object.getProperty("name");
			String type = object.getProperty("type").trim().toLowerCase();
			
			PreferenceMetaData pmd = null;
			if (type.equals("boolean") || type.equals("integer")) {
			    pmd = new PreferenceMetaData(name,
							 object.getProperty("description"),
							 block,
							 type,
							 object.getProperty("default"));
			} else if (type.equals("single-select")) {
			    // create-text-charset is a special case where
			    // the list values are read from the charset
			    // available on the system, but a "values" key
			    // may override that.
			    if (name.equals("create-text-charset") && !object.containsKey("values")) {
				pmd = new PreferenceMetaData(name, 
							     object.getProperty("description"),
							     block,
							     type,
							     object.getProperty("default"),
							     charsetNames);
			    } else {
				// parse the "values" key, which is a semicolon separated list
				// of possible values of the preference
				StringTokenizer valuesSt =
				    new StringTokenizer(object.getProperty("values"),
							";");
				List foo = new LinkedList();
				while (valuesSt.hasMoreTokens()) {
				    foo.add(valuesSt.nextToken().trim());
				}
				String[] valuesArr = new String[foo.size()];
				Iterator fooIter = foo.iterator();
				for (int i=0; i < valuesArr.length; i++) {
				    valuesArr[i] = (String) fooIter.next();
				}
				pmd = new PreferenceMetaData(name,
							     object.getProperty("description"),
							     block,
							     type,
							     object.getProperty("default"),
							     valuesArr);
			    }
			} else {
			    throw new IOException("Unknown data type: " + type);
			}

			Debug.println("Added \"" + name + "\" in block \"" + block + "\".");
			list.add(pmd);

		    }
		    object = new Properties();
		}

		if (newBlock) {
		    Debug.println("New block: " + newBlockName);
		    block = newBlockName;
		    object = null;
		}

		if (object != null && key != null) {
		    object.setProperty(key, value);
		} else if (!newBlock && !lastRow) {
		    throw new IOException("A \"name\" property must appear first in every object.");
		}
		row = reader.readLine();
	    } while (!lastRow);

	} catch (Exception ex1) {
	    throw new RuntimeException("Unable to read meta-data properties.", ex1);
	}

	// iterate through all the PMD's and create appropriate maps
	// in order to speed up later retreival of the objects based
	// on block and key name.
	for (Iterator i = list.iterator(); i.hasNext();) {
	    PreferenceMetaData pmd = (PreferenceMetaData) i.next();
	    List blockList = (List) blocks.get(pmd.block);
	    if (blockList == null) blockList = new LinkedList();
	    Map blockMap = (Map) blockKeys.get(pmd.block);
	    if (blockMap == null) blockMap = new HashMap();
	    blockList.add(pmd);
	    blockMap.put(pmd.key, pmd);
	    blocks.put(pmd.block, blockList);
	    blockKeys.put(pmd.block, blockMap);
	}
	list = Collections.unmodifiableList(list);
	blocks = Collections.unmodifiableMap(blocks);
    }

    public static PreferencesMetaData getInstance() {
	return instance;
    }

    public static void reload() {
	instance = new PreferencesMetaData();
    }

    public String getDefault(String blockName, String key) {
	Map block = (Map) blockKeys.get(blockName);
	if (block == null)
	    throw new IllegalArgumentException("Bad block \"" + blockName + "\"");
	if (!block.containsKey(key))
	    throw new IllegalArgumentException("Block \"" + 
					       blockName + "\" does not have a key \"" +
					       key + "\"");
	PreferenceMetaData pmd = (PreferenceMetaData) block.get(key);
	return pmd.defaultValue;
    }

    public boolean containsKey(String blockName, String key) {
	Map block = (Map) blockKeys.get(blockName);
	if (block == null)
	    throw new IllegalArgumentException("Bad block \"" + blockName + "\"");
	return block.containsKey(key);
    }
}


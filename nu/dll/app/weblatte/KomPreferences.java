package nu.dll.app.weblatte;

import nu.dll.lyskom.*;


/**
 * Provides an interface to a HollerithMap keeping configuration data.
 *
 * Uses PreferencesMetaData to provide default values where no data
 * is available in the wrapped HollerithMap.
 */
public class KomPreferences {
    HollerithMap map;
    String blockName;
    public KomPreferences(HollerithMap map, String blockName) {
	this.map = map;
	this.blockName = blockName;
    }

    public boolean getBoolean(String key) {
	if (!map.containsKey(key)) {
	    String defaultValue = PreferencesMetaData.getInstance().getDefault(blockName, key);
	    return defaultValue.equals("1");
	}
	return map.get(key).equals("1");
    }

    public int getInt(String key) {
	try {
	    if (!map.containsKey(key))
		return Integer.parseInt(PreferencesMetaData.getInstance().getDefault(blockName, key));

	    return Integer.parseInt(map.get(key).getContentString());
	} catch (NumberFormatException ex1) {
	    throw new RuntimeException("Unable to parse key \"" + key +
				       "\" to integer (" +
				       ex1.getMessage() + ")");
	}
    }

    public String getString(String key) {
	if (!map.containsKey(key)) {
	    return PreferencesMetaData.getInstance().getDefault(blockName, key);
	}
	return map.get(key).getContentString();
    }

    public void set(String key, String value) {
	map.put(key, value);
    }

    public Hollerith getData() {
	return map;
    }
}

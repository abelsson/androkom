package nu.dll.app.weblatte;

import nu.dll.lyskom.*;

public class KomPreferences {
    HollerithMap map;
    String blockName;
    public KomPreferences(HollerithMap map, String blockName) {
	this.map = map;
	this.blockName = blockName;
    }

    public boolean getBoolean(String key) {
	if (!map.containsKey(key)) {
	    String defaultValue = PreferencesMetaData.getDefault(blockName, key);
	    return defaultValue.equals("1");
	}
	return map.get(key).equals("1");
    }

    public String getString(String key) {
	if (!map.containsKey(key)) {
	    return PreferencesMetaData.getDefault(blockName, key);
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

package nu.dll.app.weblatte;

public class PreferenceMetaData {
    public String key, description, block, type, defaultValue;
    public String[] alternatives = null;
    public PreferenceMetaData(String key, String description,
			      String block, String type, String defaultValue) {
	this(key, description, block, type, defaultValue, new String[] { });
    }
    public PreferenceMetaData(String key, String description,
			      String block, String type, String defaultValue,
			      String[] alternatives) {
	this.key = key;
	this.description = description;
	this.block = block;
	this.type = type;
	this.defaultValue = defaultValue;
	this.alternatives = alternatives;
    }
}


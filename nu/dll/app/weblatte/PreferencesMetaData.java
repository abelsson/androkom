package nu.dll.app.weblatte;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;

public class PreferencesMetaData {
    public static List list = new LinkedList();
    public static Map blocks = new HashMap();
    public static Map blockKeys = new HashMap();
    static {
	list.add(new PreferenceMetaData("created-texts-are-read",
					"Markera skapade texter som lästa",
					"common", "boolean", "1"));
	list.add(new PreferenceMetaData("dashed-lines",
					"Visa streck kring inläggskroppen",
					"common", "boolean", "1"));


	list.add(new PreferenceMetaData("list-news-on-login",
					"Lista nyheter vid inloggning",
					"weblatte", "boolean", "1"));
	list.add(new PreferenceMetaData("hide-standard-boxes",
					"Dölj standardboxarna för endast, läsa inlägg och sända meddelande",
					"weblatte", "boolean", "0"));
	list.add(new PreferenceMetaData("show-plain-old-menu",
					"Visa textmenyer",
					"weblatte", "boolean", "0"));
	list.add(new PreferenceMetaData("always-show-welcome",
					"Visa alltid välkomsttext",
					"weblatte", "boolean", "1"));
	list.add(new PreferenceMetaData("auto-refresh-news",
					"Uppdatera nyhetslista automatiskt",
					"weblatte", "boolean", "1"));
	list.add(new PreferenceMetaData("start-in-frames-mode",
					"Starta med ramvy",
					"weblatte", "boolean", "0"));
	list.add(new PreferenceMetaData("my-name-in-bold",
					"Visa mitt eget namn i fetstil",
					"weblatte", "boolean", "0"));
	list.add(new PreferenceMetaData("create-text-charset",
					"Teckenkodning att använda vid skapande av texter",
					"weblatte", "list", "iso-8859-1",
					new String[] { "iso-8859-1", "utf-8" }));

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

    public static String getDefault(String blockName, String key) {
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

    public static boolean containsKey(String blockName, String key) {
	Map block = (Map) blockKeys.get(blockName);
	if (block == null)
	    throw new IllegalArgumentException("Bad block \"" + blockName + "\"");
	return block.containsKey(key);
    }
}


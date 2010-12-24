package nu.dll.lyskom;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

//import android.util.Log;

public class ParameterList {

    private static final String TAG = "parameterlist";
	private final Map<String, String> list;

    public ParameterList() {
    	list = new HashMap<String, String>();
    }
    
	public void set(String key, String value) {
		// TODO Auto-generated method stub
//		Log.d(TAG, "set: "+key+" : "+value);
		list.put(key, value);
	}

	public String get(String key) {
		// TODO Auto-generated method stub
//		Log.d(TAG, "get: "+key);
		return list.get(key);
	}

	public Enumeration<String> getNames() {
		Enumeration<String> retVals = null;
		Set<String> keys = list.keySet();
		// TODO Auto-generated method stub
//		Log.d(TAG, "getNames: ");
		Vector<String> v = new Vector();
		v.add("charset");
		retVals = v.elements();
		
		return retVals;
	}

}

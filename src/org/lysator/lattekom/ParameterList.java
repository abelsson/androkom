package org.lysator.lattekom;

import java.util.Enumeration;
import java.util.Map;

public class ParameterList {

    private final Map<String, String> list;

    public ParameterList() {
    	list = null;
    }
    
	public void set(String key, String value) {
		// TODO Auto-generated method stub
		list.put(key, value);
	}

	public String get(String key) {
		// TODO Auto-generated method stub
		return list.get(key);
	}

	public Enumeration<String> getNames() {
		// TODO Auto-generated method stub
		return null;
	}

}

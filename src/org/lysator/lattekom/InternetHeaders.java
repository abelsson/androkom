package org.lysator.lattekom;

import java.util.Enumeration;

import android.util.Log;

public class InternetHeaders {

	private static final String TAG = "InternetHeaders";

	public void setHeader(String name, String value) {
		// TODO Auto-generated method stub
		Log.d(TAG, "setHeader:"+name+" : "+value);
	}

	public String getHeader(String string, Object object) {
		// TODO Auto-generated method stub
		Log.d(TAG, "getHeader: "+string+" : "+object);
		return null;
	}

	public Enumeration<Header> getAllHeaders() {
		// TODO Auto-generated method stub
		Log.d(TAG, "getAllHeaders: ");
		return null;
	}

}

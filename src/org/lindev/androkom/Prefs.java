package org.lindev.androkom;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {
	private static final String OPT_SERVER = "server";
	private static final String OPT_SERVER_DEF = "";

	private static final String OPT_SAVEPSW = "savepsw";
	private static final Boolean OPT_SAVEPSW_DEF = true;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	public static String getServer(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_SERVER, OPT_SERVER_DEF);
	}

	public static Boolean getSavePsw(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_SAVEPSW, OPT_SAVEPSW_DEF);
	}
}

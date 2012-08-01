package org.lindev.androkom;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {
	private static final String OPT_SERVER = "server";
	private static final String OPT_SERVER_DEF = "";

	private static final String OPT_OTHER_SERVER = "otherserver";
	private static final String OPT_OTHER_SERVER_DEF = "";

	private static final String OPT_SAVEPSW = "savepsw";
	private static final Boolean OPT_SAVEPSW_DEF = true;

	private static final String OPT_AUTOLOGIN = "autologin";
	private static final Boolean OPT_AUTOLOGIN_DEF = false;

	private static final String OPT_USE_OISAFE = "useoisafe";
	private static final Boolean OPT_USE_OISAFE_DEF = false;

	private static final String OPT_KEEPSCREENON = "keepscreenon";
	private static final Boolean OPT_KEEPSCREENON_DEF = false;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	public static String getServer(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_SERVER, OPT_SERVER_DEF).trim();
	}

	public static String getOtherServer(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_OTHER_SERVER, OPT_OTHER_SERVER_DEF).trim();
	}

	public static Boolean getSavePsw(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_SAVEPSW, OPT_SAVEPSW_DEF);
	}

	public static Boolean getAutologin(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_AUTOLOGIN, OPT_AUTOLOGIN_DEF);
	}

	public static Boolean getUseOISafe(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_USE_OISAFE, OPT_USE_OISAFE_DEF);
	}
}

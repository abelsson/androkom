package org.lindev.androkom;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Prefs extends PreferenceActivity {
	private static final String OPT_SERVER = "server";
	private static final String OPT_SERVER_DEF = "";

	private static final String OPT_OTHER_SERVER = "otherserver";
	private static final String OPT_OTHER_SERVER_DEF = "";

    private static final String OPT_PORTNO = "portnum";
    private static final String OPT_PORTNO_DEF = "4894";

	private static final String OPT_SAVEPSW = "savepsw";
	private static final Boolean OPT_SAVEPSW_DEF = true;

	private static final String OPT_AUTOLOGIN = "autologin";
	private static final Boolean OPT_AUTOLOGIN_DEF = false;

	private static final String OPT_USE_OISAFE = "useoisafe";
	private static final Boolean OPT_USE_OISAFE_DEF = false;

    private static final String OPT_USESSL = "usessl";
    private static final Boolean OPT_USESSL_DEF = true;

    private static final String OPT_CERTLEVEL = "certlevel";
    private static final String OPT_CERTLEVEL_DEF = "0";

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
	
    public static int getPortno(Context context) {
        Log.d(TAG, "getPortno");
        String portval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_PORTNO, OPT_PORTNO_DEF).trim();
        Log.d(TAG, "portval = "+portval);
        int intval = Integer.parseInt(portval);
        return intval;
    }

	public static Boolean getUseSSL(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(OPT_USESSL, OPT_USESSL_DEF);
    }

    public static int getCertLevel(Context context) {
        Log.d(TAG, "getCertLevel");
        String val = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_CERTLEVEL, OPT_CERTLEVEL_DEF).trim();
        Log.d(TAG, "val = "+val);
        int intval = Integer.parseInt(val);
        return intval;
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
	
    private static final String TAG = "Androkom Prefs";
}

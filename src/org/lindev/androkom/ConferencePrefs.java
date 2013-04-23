package org.lindev.androkom;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class ConferencePrefs extends PreferenceActivity {
	private static final String OPT_SERVER = "server";
	private static final String OPT_SERVER_DEF = "";

	private static final String OPT_SAVEPSW = "savepsw";
	private static final Boolean OPT_SAVEPSW_DEF = true;

	private static final String OPT_AUTOLOGIN = "autologin";
	private static final Boolean OPT_AUTOLOGIN_DEF = false;

	private static final String OPT_SHOWHEADERSLEVEL = "showheaderslevel";
	private static final String OPT_SHOWHEADERSLEVEL_DEF = "1";

    private static final String OPT_MARKTEXTREAD = "marktextread";
    private static final Boolean OPT_MARKTEXTREAD_DEF = true;
    private static final String OPT_AUTOMARKOWNTEXTREAD = "autoreadmarkowntext";
    private static final Boolean OPT_AUTOMARKOWNTEXTREAD_DEF = false;
	
    private static final String OPT_SPEAKASYNCH = "speakasynch";
    private static final Boolean OPT_SPEAKASYNCH_DEF = false;
    private static final String OPT_TOASTFORASYNCH = "toastforasynch";
    private static final Boolean OPT_TOASTFORASYNCH_DEF = false;
    private static final String OPT_VIBRATEFORTAP = "vibratefortap";
    private static final Boolean OPT_VIBRATEFORTAP_DEF = true;
    private static final String OPT_VIBRATETIMETAP = "vibratetimetap";
    private static final String OPT_VIBRATETIMETAP_DEF = "15";
    private static final String OPT_VIBRATEFORASYNCH = "vibrateforasynch";
    private static final Boolean OPT_VIBRATEFORASYNCH_DEF = false;
    private static final String OPT_VIBRATETIME = "vibratetime";
    private static final String OPT_VIBRATETIME_DEF = "500";

    private static final String OPT_ENABLETAPTONEXT = "enabletaptonext";
    private static final Boolean OPT_ENABLETAPTONEXT_DEF = true;

    private static final String OPT_PREFERREDLANGUAGE = "preferredlanguage";
    private static final String OPT_PREFERREDLANGUAGE_DEF = "";

    private static final String OPT_USERBUTTONS = "userbuttons";
    private static final Boolean OPT_USERBUTTONS_DEF = false;
    private static final String OPT_USERBUTTON1 = "userbutton1";
    private static final String OPT_USERBUTTON1_DEF = "1";
    private static final String OPT_USERBUTTON2 = "userbutton2";
    private static final String OPT_USERBUTTON2_DEF = "2";
    private static final String OPT_USERBUTTON3 = "userbutton3";
    private static final String OPT_USERBUTTON3_DEF = "3";
    private static final String OPT_USERBUTTON4 = "userbutton4";
    private static final String OPT_USERBUTTON4_DEF = "4";

    private static final String OPT_INCLUDELOCATION = "includelocation";
    private static final Boolean OPT_INCLUDELOCATION_DEF = false;

    private static final String OPT_DIRECTTOUNREADS = "directtounreads";
    private static final String OPT_DIRECTTOUNREADS_DEF = "0";
    
    private static final String OPT_FORCE646DECODE = "force646decode";
    private static final Boolean OPT_FORCE646DECODE_DEF = false;

    private static final String OPT_MAXIMAGESIZEPIX = "maximagesizepix";
    private static final String OPT_MAXIMAGESIZEPIX_DEF = "100000";

    private static final String OPT_FONTSIZE = "fontsize";
    private static final String OPT_FONTSIZE_DEF = "14";
    private static final String OPT_BGCOLOUR = "bgcolour";
    private static final String OPT_BGCOLOUR_DEF = "black";
    private static final String OPT_FGCOLOUR = "fgcolour";
    private static final String OPT_FGCOLOUR_DEF = "white";
    private static final String OPT_LINKCOLOUR = "linkcolour";
    private static final String OPT_LINKCOLOUR_DEF = "blue";

    private static final String OPT_DUMPLOG = "dumplog";
    private static final Boolean OPT_DUMPLOG_DEF = true;


    private static final String TAG = "Androkom ConferencePrefs";
    
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.conference_settings);
	}

	public static String getServer(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_SERVER, OPT_SERVER_DEF);
	}

	public static Boolean getSavePsw(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_SAVEPSW, OPT_SAVEPSW_DEF);
	}

	public static Boolean getAutologin(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_AUTOLOGIN, OPT_AUTOLOGIN_DEF);
	}

    public static String getShowHeadersLevel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_SHOWHEADERSLEVEL, OPT_SHOWHEADERSLEVEL_DEF);
    }

    public static Boolean getMarkTextRead(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_MARKTEXTREAD, OPT_MARKTEXTREAD_DEF);
    }

    public static Boolean getAutoMarkOwnTextRead(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_AUTOMARKOWNTEXTREAD, OPT_AUTOMARKOWNTEXTREAD_DEF);
    }

    public static boolean getSpeakAsynch(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_SPEAKASYNCH, OPT_SPEAKASYNCH_DEF);
    }

    public static Boolean getToastForAsynch(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_TOASTFORASYNCH, OPT_TOASTFORASYNCH_DEF);
    }

    public static String getPreferredLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_PREFERREDLANGUAGE, OPT_PREFERREDLANGUAGE_DEF);
    }

    public static Boolean getUserButtons(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_USERBUTTONS, OPT_USERBUTTONS_DEF);
    }
    public static int getUserButton1val(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_USERBUTTON1, OPT_USERBUTTON1_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }
    public static int getUserButton2val(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_USERBUTTON2, OPT_USERBUTTON2_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }
    public static int getUserButton3val(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_USERBUTTON3, OPT_USERBUTTON3_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }
    public static int getUserButton4val(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_USERBUTTON4, OPT_USERBUTTON4_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }

    public static Boolean getEnableTapToNext(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_ENABLETAPTONEXT, OPT_ENABLETAPTONEXT_DEF);
    }

    public static Boolean getVibrateForTap(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_VIBRATEFORTAP, OPT_VIBRATEFORTAP_DEF);
    }
    public static int getVibrateTimeTap(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_VIBRATETIMETAP, OPT_VIBRATETIMETAP_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }

    public static Boolean getVibrateForAsynch(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_VIBRATEFORASYNCH, OPT_VIBRATEFORASYNCH_DEF);
    }
    public static int getVibrateTime(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_VIBRATETIME, OPT_VIBRATETIME_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }
    
    public static Boolean getIncludeLocation(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_INCLUDELOCATION, OPT_INCLUDELOCATION_DEF);
    }

    public static int getDirectToUnreads(Context context) {
        String dtu = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_DIRECTTOUNREADS, OPT_DIRECTTOUNREADS_DEF);
        int intval = Integer.parseInt(dtu);
        return intval;
    }

    public static boolean getforce646decode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(OPT_FORCE646DECODE, OPT_FORCE646DECODE_DEF);
    }

    public static int getMaxImageSizePix(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_MAXIMAGESIZEPIX, OPT_MAXIMAGESIZEPIX_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }

    public static int getFontSize(Context context) {
        String butval = PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_FONTSIZE, OPT_FONTSIZE_DEF);
        int intval = Integer.parseInt(butval);
        return intval;
    }

    public static String getBGColour(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_BGCOLOUR, OPT_BGCOLOUR_DEF);
    }

    public static String getFGColour(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_FGCOLOUR, OPT_FGCOLOUR_DEF);
    }

    public static String getLinkColour(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(OPT_LINKCOLOUR, OPT_LINKCOLOUR_DEF);
    }

    public static boolean getDumpLog(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_DUMPLOG, OPT_DUMPLOG_DEF);
    }
}

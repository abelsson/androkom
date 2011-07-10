package org.lindev.androkom;

import java.util.Locale;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Common application class shared among all the activities. It's 
 * main purpose in life is to keep a common instance of KomServer 
 * for all activities to share.
 * 
 * @author henrik
 *
 */
public class App extends Application implements ServiceConnection
{
	private static final String OPT_KEEPSCREENON = "keepscreenon";
	private static final Boolean OPT_KEEPSCREENON_DEF = false;
	private static final String TAG = "Androkom Application";

    private Locale locale = null;

	@Override
	public void onCreate()
	{
		super.onCreate();

		Log.d(TAG, "onCreate");
		
		Configuration config = getBaseContext().getResources()
        .getConfiguration();
		
        String lang = ConferencePrefs.getPreferredLanguage(getBaseContext());
        if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
            locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }

        // keep screen on, depending on preferences
        boolean keepScreenOn = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(OPT_KEEPSCREENON, OPT_KEEPSCREENON_DEF);
		Log.d(TAG, "keepscreenon="+keepScreenOn);

		if (keepScreenOn) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			mWakeLock.acquire();
		}
	}
	
    public void doBindService(ServiceConnection connection) 
    {
    	bindService(new Intent(App.this, KomServer.class), connection, Context.BIND_AUTO_CREATE);
    }
    
    public void doUnbindService(ServiceConnection connection)
    {
        new DelayedUnbindTask().execute(connection);
   }

    
    private class DelayedUnbindTask extends
            AsyncTask<ServiceConnection, Void, ServiceConnection> {
        @SuppressWarnings("unused")
        protected void onPreExecute(ServiceConnection connection) {
        }

        // worker thread (separate from UI thread)
        protected ServiceConnection doInBackground(final ServiceConnection... args) {
            Log.d(TAG, "waiting to doUnbind");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return args[0];
        }

        protected void onPostExecute(ServiceConnection connection) {
            Log.d(TAG, "doing delayed doUnbind");
            unbindService(connection);
        }

    }
    
    private PowerManager.WakeLock mWakeLock = null;

	public void onServiceConnected(ComponentName name, IBinder service)
	{
        Log.d(TAG, "onServiceConnected");
        		
	}

	public void onServiceDisconnected(ComponentName name) 
	{
        Log.d(TAG, "onServiceDisconnected");
	}

    /**
     * Called by KomServer on destruction of the service.
     */
    public void shutdown() {
        Log.d(TAG, "shutdown");

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (locale != null) {
            newConfig.locale = locale;
            Locale.setDefault(locale);
            getBaseContext().getResources().updateConfiguration(newConfig,
                    getBaseContext().getResources().getDisplayMetrics());
        }
    }
}

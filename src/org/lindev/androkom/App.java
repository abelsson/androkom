package org.lindev.androkom;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
	private static final String TAG = "Androkom";
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		doBindService(this);
		
        // keep screen on, depending on preferences
        boolean keepScreenOn = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(OPT_KEEPSCREENON, OPT_KEEPSCREENON_DEF);
		Log.d(TAG, "keepscreenon="+keepScreenOn);

		if (keepScreenOn) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			mWakeLock.acquire();
		}
	}
	
	@Override
	public void onTerminate()
	{
	 	if(mWakeLock != null) {
    		mWakeLock.release();
    		mWakeLock = null;
    	}
	 	doUnbindService(this);
	 	super.onTerminate();
	}

    public void doBindService(ServiceConnection connection) 
    {
    	bindService(new Intent(App.this, KomServer.class), connection, Context.BIND_AUTO_CREATE);
    }
    
    public void doUnbindService(ServiceConnection connection)
    {
    	unbindService(connection);
    }
      
    private PowerManager.WakeLock mWakeLock = null;

	public void onServiceConnected(ComponentName name, IBinder service)
	{
		
	}

	public void onServiceDisconnected(ComponentName name) 
	{
		
	}
}

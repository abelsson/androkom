package org.lindev.androkom;

import java.util.Locale;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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

    private int nrServiceUsers = 0;
    
	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.d(TAG, "onCreate");
		
        mHandler = new CustomHandler(this);
        
/*        Configuration config = getResources().getConfiguration();
		
        String lang = ConferencePrefs.getPreferredLanguage(this);
        if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
            Log.d(TAG, "onCreate setting locale");
            locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getResources().updateConfiguration(config,
                    getResources().getDisplayMetrics());
            Log.d(TAG, "onCreate setting locale done");
        }
*/        
        // keep screen on, depending on preferences
        boolean keepScreenOn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(OPT_KEEPSCREENON, OPT_KEEPSCREENON_DEF);
		Log.d(TAG, "keepscreenon="+keepScreenOn);

		if (keepScreenOn) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			mWakeLock.acquire();
		}
        Log.d(TAG, "onCreate done");
	}

    protected void consumeAppMessage(Message msg) {
        Log.d(TAG, "consumeAppMessage");
        ServiceConnection connection;
        switch (msg.what) {
        case Consts.MESSAGE_UNBIND_SERVICE:
            connection = (ServiceConnection) msg.obj;
            Log.d(TAG, "doing delayed doUnbind: " + connection.toString());
            try {
                unbindService(connection);
            } catch (Exception e) {
                Log.d(TAG, "Couldn't unbind service:" + e);
            }
            nrServiceUsers--;
            Log.d(TAG, "SERVICE fewer users:" + nrServiceUsers);
            Log.d(TAG, "done delayed doUnbind: " + connection.toString());
            break;
        default:
            Log.d(TAG, "unknown message: " + msg.what);
        }
        Log.d(TAG, "consumeAppMessage done");
    }

    public void doBindService(ServiceConnection connection) 
    {
        Log.d(TAG, "SERVICE trying to bind new connection: "+connection);
    	if(bindService(new Intent(App.this, KomServer.class), connection, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "SERVICE Succedeed to bind");    	    
    	} else {
            Log.d(TAG, "SERVICE FAILED TO BIND");
    	}
    	nrServiceUsers++;
    	Log.d(TAG, "SERVICE more users:"+nrServiceUsers);
    }
    
    public void doUnbindService(ServiceConnection connection)
    {
        Log.d(TAG, "doUnbindService");
        if (mHandler != null) {
            Message msg = new Message();
            msg.obj = connection;
            msg.what = Consts.MESSAGE_UNBIND_SERVICE;
            mHandler.sendMessageDelayed(msg, 500);
            Log.d(TAG, "doUnbindService message sent for: "+connection);
        } else {
            Log.d(TAG, "doUnbindService null handler");
        }
        Log.d(TAG, "doUnbindService done");
    }
    
    private PowerManager.WakeLock mWakeLock = null;

	public void onServiceConnected(ComponentName name, IBinder service)
	{
        Log.d(TAG, "onServiceConnected:"+name);
	}

	public void onServiceDisconnected(ComponentName name) 
	{
        Log.d(TAG, "onServiceDisconnected"+name);
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
        
        mHandler = null;
    }
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged (done)");
    }
    
/*    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
        if (locale != null) {
            Log.d(TAG, "onConfigurationChanged setting locale: "+locale.toString());
            newConfig.locale = locale;
            Locale.setDefault(locale);
            // getResources().updateConfiguration(newConfig,
            // getResources().getDisplayMetrics());
            getResources().updateConfiguration(newConfig,
                    getResources().getDisplayMetrics());
            Log.d(TAG, "onConfigurationChanged setting locale done");
        }
        Log.d(TAG, "onConfigurationChanged done");
    }
*/    
    public void onLowMemory() {
        //super.onLowMemory();
        Log.d(TAG, "onLowMemeory");
    }

    private static class CustomHandler extends Handler {
        private App activity;

        public CustomHandler(App app) {
            super();
            this.activity = app;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                activity.consumeAppMessage(msg);
            } catch (Exception e) {
                Log.d(TAG, "handleMessage failed to consume:"+msg+" " + e);
            }
        }
    }
    
    private static Handler mHandler=null;
}

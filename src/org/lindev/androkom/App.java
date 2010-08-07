package org.lindev.androkom;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Common application class shared among all the activities. It's 
 * main purpose in life is to keep a common instance of KomServer 
 * for all activities to share.
 * 
 * @author henrik
 *
 */
public class App extends Application 
{
    /**
     * Return a reference to the KomServer instance.
     */
    public KomServer getKom() { return mBoundService; }


    public void doBindService() 
    {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(App.this, KomServer.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    public void doUnbindService()
    {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private KomServer mBoundService;
    private boolean mIsBound;


    private ServiceConnection mConnection = new ServiceConnection() 
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((KomServer.LocalBinder)service).getService();

            // Tell the user about it.
            Toast.makeText(App.this, "KomServer connected",
                    Toast.LENGTH_SHORT).show();

        }

        public void onServiceDisconnected(ComponentName className) 
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(App.this, "KomServer disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };
}

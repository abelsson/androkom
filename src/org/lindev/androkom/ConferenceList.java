package org.lindev.androkom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.lindev.androkom.KomServer.ConferenceInfo;
import org.lysator.lattekom.ConfInfo;
import org.lysator.lattekom.Membership;
import org.lysator.lattekom.Session;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;

public class ConferenceList extends ListActivity {
	public static final String AUTHORITY = "com.lindev.provider.Androkom";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/kom");
   

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        mTimer = new Timer();
        
        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(ConferenceList.CONTENT_URI);
        }

        doBindService();
        	
		mAdapter = new ArrayAdapter<ConferenceInfo>(this, R.layout.main);
	    setListAdapter(mAdapter);
	    
	  
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        
                
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	       
    	mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {						
						populateConferences();		
					}
					
				});						
			}
        	
        }, 500, 1000);
        
    }
    

    @Override
    public void onPause()
    {
    	super.onPause();
    	mTimer.cancel();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	TextView view = (TextView)v;
    	
    	mBoundService.setConference(mAdapter.getItem(position).id);
    	Toast.makeText(getApplicationContext(), view.getText(), Toast.LENGTH_SHORT).show();	
    	
    	Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

    	Log.i("androkom", uri.toString());
    	Intent intent = new Intent(this, Conference.class);
    	intent.putExtra("conference-id", mConferences.get((int)id).id);
    	startActivity(intent);
    }
    
    List<ConferenceInfo> mConferences;
	private void populateConferences() {
		
		mAdapter.clear();
        try {
			mConferences = mBoundService.fetchConferences();
			for(ConferenceInfo elem : mConferences) {
				mAdapter.add(elem);
			}
	      
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("androkom", "uh oh", e);
		}
		
		mAdapter.notifyDataSetChanged();
	}
    
    private KomServer mBoundService;
    private boolean mIsBound;
    private ArrayAdapter<ConferenceInfo> mAdapter;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((KomServer.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(ConferenceList.this, "KomServer connected",
                    Toast.LENGTH_SHORT).show();
           
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(ConferenceList.this, "KomServer disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };
	private Timer mTimer;

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(ConferenceList.this, KomServer.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
 }
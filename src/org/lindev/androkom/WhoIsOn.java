package org.lindev.androkom;

import java.util.List;
import org.lindev.androkom.KomServer.ConferenceInfo;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Show a list of persons logged on.
 * 
 * @author jonas
 *
 */
public class WhoIsOn extends ListActivity implements ServiceConnection
{
	public static final String TAG = "Androkom";
	private KomServer mKom;

	/**
     * Instantiate activity.  
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
                
        getApp().doBindService(this);
        mAdapter = new ArrayAdapter<ConferenceInfo>(this, R.layout.whoison);
        setListAdapter(mAdapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
    }
    
    /**
     * Update on visible?
     */
    @Override
	public void onResume() {
		super.onResume();

		new populatePersonsTask().execute();
	}
    
    /**
     * Pause
     */
    @Override
    public void onPause()
    {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() 
    {
        getApp().doUnbindService(this);
        super.onDestroy();
    }
    
    /**
     * Called when a person has been clicked. Switch to send message or mail?
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        Toast.makeText(getApplicationContext(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();    
        
        //Intent intent = new Intent(this, Conference.class);
        //intent.putExtra("conference-id", mConferences.get((int)id).id);
        //startActivity(intent);
    }
    

    /**
     * Attempt to get all persons online.
     */
    public class populatePersonsTask extends AsyncTask<Void, Integer, Integer> {
        private final ProgressDialog dialog = new ProgressDialog(WhoIsOn.this);

        protected void onPreExecute() {
			this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(false);
            this.dialog.setMessage(getString(R.string.loading));
            this.dialog.setMax(100);
            this.dialog.show();
        }

		protected Integer doInBackground(Void... param) {
	        tPersons = fetchPersons(this);
			return 1;
		}

		void updateProgress(int percent) {
			publishProgress(percent);
		}
		
		protected void onProgressUpdate(Integer... i) {
			this.dialog.setProgress(i[0]);
		}

		protected void onPostExecute(final Integer result) 
        { 
            this.dialog.dismiss();
            mPersons = tPersons;
            populatePersons();
        }

    }

    /**
     * Refresh list of persons.
     */
    private void populatePersons() 
    {
    	Log.d(TAG, "populatePersons");
        mAdapter.clear();

        if (mPersons != null && (!mPersons.isEmpty())) {
        	for(ConferenceInfo elem : mPersons) {
        		mAdapter.add(elem);
        	}

        	mAdapter.notifyDataSetChanged();
        } else {
        	// TODO: Do something here?
        	Log.d(TAG, "populatePersons failed, no Persons");
        	Log.d(TAG, "mPersons is null:"+(mPersons==null));
        	if(mPersons!=null) {
            	Log.d(TAG, "mPersons is empty:"+mPersons.isEmpty());
        	}
        }
    }
 
    private List<ConferenceInfo> fetchPersons(populatePersonsTask populatePersonsT) {
    	List<ConferenceInfo> retlist = null;
    	
    	try {
            App app = getApp();
            if (app != null) {
            	if (mKom != null) {
            		if (mKom.isConnected()) {
            			retlist = mKom.fetchPersons(populatePersonsT);
            		} else {
            			Log.d(TAG, "Can't fetch persons when no connection");
            	        Toast.makeText(getApplicationContext(), "Lost connection", Toast.LENGTH_SHORT).show();
            			mKom.reconnect();
            		}
            	}
            }
    	} catch (Exception e) {
    		Log.d(TAG, "fetchPersons failed:"+e);
    		e.printStackTrace();
	        Toast.makeText(getApplicationContext(), "fetchPersons failed, probably lost connection", Toast.LENGTH_SHORT).show();
    	}
		return retlist;
    }
    
    App getApp() 
    {
        return (App)getApplication();
    }
    
	public void onServiceConnected(ComponentName name, IBinder service) {
		mKom = ((KomServer.LocalBinder)service).getService();		
	}

	public void onServiceDisconnected(ComponentName name) {
		mKom = null;		
	}
    private List<ConferenceInfo> tPersons;
    private List<ConferenceInfo> mPersons;
    private ArrayAdapter<ConferenceInfo> mAdapter;
 }
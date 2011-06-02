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
	public static final String TAG = "Androkom WhoIsOn";
	private KomServer mKom;

	/**
     * Instantiate activity.  
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
                
		Log.d(TAG, "onCreate 1 ");
        getApp().doBindService(this);
        int i=0;
		Log.d(TAG, "onCreate 2 ");
        mAdapter = new ArrayAdapter<ConferenceInfo>(this, R.layout.whoison);
        setListAdapter(mAdapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
		Log.d(TAG, "onCreate 3 ");
    }
    
    /**
     * Update on visible?
     */
    @Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume 1 ");

		new populatePersonsTask().execute();
		Log.d(TAG, "onResume 2 ");
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
    		Log.d(TAG, "populatePersonsTask PreExecute 1 ");
			this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(false);
            this.dialog.setMessage(getString(R.string.loading));
            this.dialog.setMax(100);
            this.dialog.show();
    		Log.d(TAG, "populatePersonsTask PreExecute 2 ");
        }

		protected Integer doInBackground(Void... param) {
    		Log.d(TAG, "populatePersonsTask Background 1 ");
    		int i=0;
    		while ((mKom == null)&&(i<100)) {
    			i++;
    			try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		Log.d(TAG, "slept "+i);
	        tPersons = fetchPersons(this);
    		Log.d(TAG, "populatePersonsTask Background 2 ");
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
    		Log.d(TAG, "populatePersonsTask PostExecute 1 ");
            this.dialog.dismiss();
            mPersons = tPersons;
    		Log.d(TAG, "populatePersonsTask PostExecute 2 ");
            populatePersons();
    		Log.d(TAG, "populatePersonsTask PostExecute 3 ");
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
        	Log.d(TAG, "populatePersons 2");
        	for(ConferenceInfo elem : mPersons) {
        		mAdapter.add(elem);
        	}

        	mAdapter.notifyDataSetChanged();
        	Log.d(TAG, "populatePersons 3");
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
    	
		Log.d(TAG, "fetchPersons 1");
		try {
			if (mKom != null) {
				if (mKom.isConnected()) {
					Log.d(TAG, "fetchPersons 2");
					retlist = mKom.fetchPersons(populatePersonsT);
					Log.d(TAG, "fetchPersons 3");
				} else {
					Log.d(TAG, "Can't fetch persons when no connection");
					mKom.reconnect();
				}
			} else {
				Log.d(TAG, "mKom is null");
			}
		} catch (Exception e) {
			Log.d(TAG, "fetchPersons failed:" + e);
			e.printStackTrace();
		}
		Log.d(TAG, "fetchPersons end");
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
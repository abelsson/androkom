package org.lindev.androkom;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.lindev.androkom.KomServer.ConferenceInfo;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Show a list of all conferences with unread texts.
 * 
 * @author henrik
 *
 */
public class ConferenceList extends ListActivity 
{
	public static final String TAG = "Androkom ConferenceList";

	/**
     * Instantiate activity.  
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
                
        mTimer = new Timer();
    
        mAdapter = new ArrayAdapter<ConferenceInfo>(this, R.layout.main);
        setListAdapter(mAdapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);   
    }
    
    /**
     * While activity is active, keep a timer running to periodically refresh
     * the list of conferences with unread messages.
     */
    @Override
    public void onResume()
    {
        super.onResume();
               
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                // Must populate list in UI thread.
                runOnUiThread(new Runnable() {
                    public void run() {                     
                        populateConferences();      
                    }
                    
                });                     
            }
            
        }, 500, 10000);
        
    }
    
    /**
     * If activity is no longer active, cancel periodic updates.
     */
    @Override
    public void onPause()
    {
        super.onPause();
        mTimer.cancel();
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        if (isFinishing())
            getApp().doUnbindService();
    }
    
    /**
     * Called when a conference has been clicked. Switch to Conference activity, 
     * passing the ID along.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        Toast.makeText(getApplicationContext(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();    
        
        Intent intent = new Intent(this, Conference.class);
        intent.putExtra("conference-id", mConferences.get((int)id).id);
        startActivity(intent);

    }
    
    /**
     * Show options menu. Currently does nothing useful.
     */
    @Override 
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.conferencelist, menu);
        return true;
    }
 
    /**
     * Called when user has selected a menu item from the 
     * menu button popup. 
     */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
		Log.d(TAG, "onOptionsItemSelected");
		// Handle item selection
		switch (item.getItemId()) {

		case R.id.menu_settings_id:
			Log.d(TAG, "Starting menu");
			startActivity(new Intent(this, ConferencePrefs.class));
			return true;

		case R.id.menu_createnewtext_id:
            intent = new Intent(this, CreateNewText.class);    
            intent.putExtra("recipient_type", 1);
            startActivity(intent);
			return true;

		case R.id.menu_createnewmail_id:
            intent = new Intent(this, CreateNewText.class);    
            intent.putExtra("recipient_type", 2);
            startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

    /**
     * Attempt to reconnect to server.
     */
    private class LoginTask extends AsyncTask<Void, Integer, String> {
        private final ProgressDialog dialog = new ProgressDialog(ConferenceList.this);

        protected void onPreExecute() {
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(true);
            this.dialog.setMessage("Logging in...");
            this.dialog.show();
        }

        protected String doInBackground(final Void... args) 
        {
			getApp().getKom().reconnect();
			return "a string";
        }

        protected void onPostExecute(final String result) 
        { 
            this.dialog.dismiss();
                       
            Log.d(TAG, result);
        }
    }

    private void doLogin()
    {
        new LoginTask().execute();
    }

    /**
     * Refresh list of unread conferences.
     */
    private void populateConferences() 
    {
        mAdapter.clear();

        mConferences = fetchConferences();
        if (mConferences != null && (!mConferences.isEmpty())) {
        	for(ConferenceInfo elem : mConferences) {
        		mAdapter.add(elem);
        	}

        	mAdapter.notifyDataSetChanged();
        } else {
        	// TODO: Do something here?
        	Log.d(TAG, "populateConferences failed, no Conferences");
        	Log.d(TAG, "mConferences is null:"+(mConferences==null));
        	if(mConferences!=null) {
            	Log.d(TAG, "mConferences is empty:"+mConferences.isEmpty());
        	}
        }
    }
 
    private List<ConferenceInfo> fetchConferences() {
    	List<ConferenceInfo> retlist = null;
    	
    	try {
            App app = getApp();
            if (app != null) {
            	KomServer kom = app.getKom();
            	if (kom != null) {
            		if (kom.isConnected()) {
            			retlist = kom.fetchConferences();
            		} else {
            			Log.d(TAG, "Can't fetch conferences when no connection");
            	        Toast.makeText(getApplicationContext(), "Lost connection", Toast.LENGTH_SHORT).show();    
            			getApp().getKom().reconnect();
            		}
            	}
            }
    	} catch (Exception e) {
    		Log.d(TAG, "fetchConferences failed:"+e);
    		e.printStackTrace();
	        Toast.makeText(getApplicationContext(), "fetchConferences failed, probably lost connection", Toast.LENGTH_SHORT).show();    
    	}
		return retlist;
    }
    
    App getApp() 
    {
        return (App)getApplication();
    }
    
 
    private List<ConferenceInfo> mConferences;
    private ArrayAdapter<ConferenceInfo> mAdapter;
    private Timer mTimer;
 }
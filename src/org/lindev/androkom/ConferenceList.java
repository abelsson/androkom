package org.lindev.androkom;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.lindev.androkom.KomServer.ConferenceInfo;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
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

        inflater.inflate(R.menu.conference, menu);
        return true;
    }
    
    /**
     * Refresh list of unread conferences.
     */
    private void populateConferences() 
    {
        mAdapter.clear();

        mConferences = getApp().getKom().fetchConferences();
        for(ConferenceInfo elem : mConferences) {
            mAdapter.add(elem);
        }

        mAdapter.notifyDataSetChanged();
    }
    
    App getApp() 
    {
        return (App)getApplication();
    }
    
 
    private List<ConferenceInfo> mConferences;
    private ArrayAdapter<ConferenceInfo> mAdapter;
    private Timer mTimer;
 }
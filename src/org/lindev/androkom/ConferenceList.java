package org.lindev.androkom;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.lindev.androkom.KomServer.ConferenceInfo;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ConferenceList extends ListActivity 
{
	/** Called when the activity is first created. */
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
    protected void onDestroy() 
    {
        super.onDestroy();
        if (isFinishing())
        	getApp().doUnbindService();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
    	Toast.makeText(getApplicationContext(), ((TextView)v).getText(), Toast.LENGTH_SHORT).show();	
    	
    	Intent intent = new Intent(this, Conference.class);
    	intent.putExtra("conference-id", mConferences.get((int)id).id);
    	startActivity(intent);

    }
    
    @Override 
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();

    	inflater.inflate(R.menu.conference, menu);
    	return true;
    }
    
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
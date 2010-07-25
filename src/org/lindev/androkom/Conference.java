package org.lindev.androkom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class Conference extends Activity 
{
	  /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        int confNo = (Integer) getIntent().getExtras().get("conference-id");
        
        Log.i("androkom", "Got passed conference id: " + confNo);
        
        getApp().getKom().setConference(confNo);

		final TextView tv = new TextView(this);
		currentTextId = getApp().getKom().displayText(tv);
		setContentView(tv);
		
		tv.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) 
			{				
				currentTextId = getApp().getKom().displayText(tv);
			}
		});
		
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.reply:
        	Intent intent = new Intent(this, CreateText.class);    
        	intent.putExtra("in-reply-to", currentTextId);
        	startActivity(intent);
        	return true;
       
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    @Override 
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();

    	inflater.inflate(R.menu.conference, menu);
    	return true;
    }

	App getApp() 
	{
		return (App)getApplication();
	}
	
	private int currentTextId;
}

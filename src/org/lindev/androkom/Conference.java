package org.lindev.androkom;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
		getApp().getKom().displayText(tv);
		setContentView(tv);
		
		tv.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) 
			{				
				getApp().getKom().displayText(tv);
			}
		});
		
    }

	App getApp() 
	{
		return (App)getApplication();
	}
	
}

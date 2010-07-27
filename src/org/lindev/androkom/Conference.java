package org.lindev.androkom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Show texts in a LysKOM conference.
 * 
 * @author henrik
 *
 */
public class Conference extends Activity 
{
    /**
     * Set up activity. Will show individual LysKOM texts
     * with a click anywhere on the display moving to the 
     * next unread text. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        final Object data = getLastNonConfigurationInstance();
        int confNo = (Integer) getIntent().getExtras().get("conference-id");
               
        Log.i("androkom", "Got passed conference id: " + confNo);
        
        

        final TextView tv = new TextView(this);
        
        if (data != null) {
        	currentText = (String)data;
        	tv.setText(Html.fromHtml(currentText), TextView.BufferType.SPANNABLE);
        } else {        	
        	getApp().getKom().setConference(confNo);
        	currentText = getApp().getKom().getNextUnreadText();
        	tv.setText(Html.fromHtml(currentText), TextView.BufferType.SPANNABLE);
        }
        
        setContentView(tv);
        
        tv.setOnClickListener(new OnClickListener() {       
            public void onClick(View v) 
            {               
                currentText = getApp().getKom().getNextUnreadText();
                tv.setText(Html.fromHtml(currentText), TextView.BufferType.SPANNABLE);
            }
        });
        
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {    	
    	return currentText;
    }

    /**
     * Called when user has selected a menu item from the 
     * menu button popup. 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        
        /*
         * A reply to the current text was requested, so show a 
         * CreateText activity. 
         */
        case R.id.reply:
            Intent intent = new Intent(this, CreateText.class);    
            intent.putExtra("in-reply-to", getApp().getKom().getLastTextNo());
            startActivity(intent);
            return true;
       
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * The menu key has been pressed, instantiate the requested
     * menu.
     */
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
    
    private String currentText;
}

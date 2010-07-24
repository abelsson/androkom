package org.lindev.androkom;

import java.io.IOException;

import org.lysator.lattekom.RpcFailure;
import org.lysator.lattekom.Session;
import org.lysator.lattekom.Text;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class Conference extends Activity {

	  /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int confNo = (Integer) getIntent().getExtras().get("conference-id");
        
        Log.i("androkom", "Got passed conference id: " + confNo);
        final Session s = KomServer.getSession();
        
        try {
			//s.nextUnreadConference(true);
        	s.changeConference(confNo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		final TextView tv = new TextView(this);
		displayText(s, tv);
		setContentView(tv);
		
		tv.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				displayText(s, tv);
			}
			
		});
		
    }
    
    private void displayText(final Session s, final TextView tv) {
		int textNo;
		try {
			textNo = s.nextUnreadText(false);	 			
			if (textNo < 0) {
				tv.setText("All read");
				s.nextUnreadConference(true);
			} else {
				Text text = s.getText(textNo);
				String username = s.getConfStat(text.getAuthor()).getNameString();
				
				String str = "<b>Author: "+username+"<br/>Subject: " + text.getSubjectString() + "</b><br/>" + text.getBodyString();
				
				
				//tv.setText(str);
				tv.setText(Html.fromHtml(str), TextView.BufferType.SPANNABLE);

				s.markAsRead(textNo);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

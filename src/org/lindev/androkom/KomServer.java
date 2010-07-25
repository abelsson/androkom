package org.lindev.androkom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lysator.lattekom.ConfInfo;
import org.lysator.lattekom.Membership;
import org.lysator.lattekom.RpcFailure;
import org.lysator.lattekom.Session;
import org.lysator.lattekom.Text;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class KomServer extends Service {

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder 
    {
        KomServer getService() 
        {
            return KomServer.this;
        }
    }

    public class ConferenceInfo 
    {
    	public int id;
    	public String name;
    	
    	@Override
    	public String toString() 
    	{
    		return name + " <" + id + ">";
    	}
    }
    
	@Override
    public void onCreate() 
	{
        super.onCreate();
        
        username = "..";
        password = "...";
        server = "kom.lysator.liu.se";
        if (s == null)
        	s = new Session();
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return mBinder;
	}
	
    // This is the object that receives interactions from clients. 
    private final IBinder mBinder = new LocalBinder();


    @Override
    public void onDestroy() 
    {
 
        // Tell the user we stopped.
        Toast.makeText(this,"KomServer stopped", Toast.LENGTH_SHORT).show();
        
        
        try {
        	if (s.getState() == Session.STATE_LOGIN)
        		s.logout(true);
			Log.i("androkom","logged out");
			
			if (s.getState() == Session.STATE_CONNECTED)
				s.disconnect(false);
			
	    	Log.i("androkom","disconnected");	    	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s = null;
    	
    }

    public void connect() 
    {
    	try {
			s.connect(server);
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public List<ConferenceInfo> fetchConferences() 
    {
    	ArrayList<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();
    	try { 
    		if (!s.getConnected())
    			connect();
    		if (!s.getLoggedIn()) {
    			login();
    			Log.i("androkom", "logged in to " + server + " just fine");
    		}

    		s.updateUnreads();

    		Membership[] m = s.getUnreadMembership();
    		for (int i = 0; i < m.length; i++) {
    			int conf = m[i].getConference();
    			String name = s.toString(s.getConfName(conf));
    			Log.i("androkom", name + " <" + conf + ">");

    			ConferenceInfo info = new ConferenceInfo();
    			info.id = conf;
    			info.name = name;

    			arr.add(info);
    		}
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}

    	return arr;
    }
    
    public void setConference(int confNo) 
    {
    	try {
			s.changeConference(confNo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public String fetchMeeting() 
    {
    	try {		
			int textNo = s.nextUnreadText(s.getCurrentConference(), false);
			Text text = s.getText(textNo);
			return text.getBodyString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Invalid text";
    }
    
    void login() 
    {
        ConfInfo usernames[] = new ConfInfo[0];
        try {
            usernames = s.lookupName(username, true, false);
            if (usernames.length != 1) {
                Log.e("androkom", "Invalid/ambigious username");
                System.exit(-1);
            } else {
                // login as hidden
                if (!s.login(usernames[0].confNo, password, true)) {
                	Log.e("androkom","Login failed");
                    System.exit(-1);
                }
            }
        } catch (Exception e) {
        	Log.e("androkom", "Caught " + e.getClass().getName());
        }
    }

    public void displayText(final TextView tv) 
    {
		final int textNo;
		try {
			textNo = s.nextUnreadText(false);	 			
			if (textNo < 0) {
				tv.setText("All read");
				s.nextUnreadConference(true);
			} else {
				final Text text = s.getText(textNo);
				final String username = s.getConfStat(text.getAuthor()).getNameString();
				
				final String str = "<b>Author: "+username+"<br/>Subject: " + text.getSubjectString() + "</b><br/>" + text.getBodyString();
				
				tv.setText(Html.fromHtml(str), TextView.BufferType.SPANNABLE);

				s.markAsRead(textNo);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    private Session s;
    private String username;
    private String password;
    private String server;

}

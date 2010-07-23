package org.lindev.androkom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lysator.lattekom.ConfInfo;
import org.lysator.lattekom.Membership;
import org.lysator.lattekom.Session;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;

public class Androkom extends ListActivity {
	
    private Session s;
    private String username;
    private String password;
    private String server;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        username = "..";
        password = "...";
        server = "kom.lysator.liu.se";
        
        List<String> arr = null;
        try {
			arr = fetchConferences();
	      
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("androkom", "uh oh", e);
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.main, arr);
	    setListAdapter(adapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Toast.makeText(getApplicationContext(), ((TextView)view).getText(), Toast.LENGTH_SHORT).show();				
			}
        });
        
    }
    
    public List<String> fetchConferences() throws IOException {
    	s = new Session();

    	ArrayList<String> arr = new ArrayList<String>();
    	
    	s.connect(server);
    	login(s, username, password);
    	Log.i("androkom", "logged in to " + server + " just fine");
    	s.updateUnreads();
    	Membership[] m = s.getUnreadMembership();
    	for (int i = 0; i < m.length; i++) {
    		int conf = m[i].getConference();
    		String name = new String(s.getConfName(conf));
    		Log.i("androkom", name + " <" + conf + ">");
    		arr.add(name);
    	}
    	s.logout(true);
    	Log.i("androkom","logged out");
    	s.disconnect(false);
    	Log.i("androkom","disconnected");
    	return arr;
    }
    
    void login(Session s, String username, String password) {
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
}
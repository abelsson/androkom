package org.lindev.androkom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.lysator.lattekom.AsynchMessage;
import org.lysator.lattekom.AsynchMessageReceiver;
import org.lysator.lattekom.AuxItem;
import org.lysator.lattekom.ConfInfo;
import org.lysator.lattekom.Membership;
import org.lysator.lattekom.RpcEvent;
import org.lysator.lattekom.RpcEventListener;
import org.lysator.lattekom.RpcFailure;
import org.lysator.lattekom.Session;
import org.lysator.lattekom.Text;
import org.lysator.lattekom.UserArea;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


/**
 * A service which keeps the Lattekom session object and all
 * LysKOM stuff for the various activities in the app.
 * 
 * @author henrik
 *
 */
public class KomServer extends Service implements RpcEventListener, AsynchMessageReceiver
{
	public static final String TAG = "Androkom KomServer";


    /**
     * Class for clients to access.  Because we assume this service always
     * runs in the same process as its clients, we don't deal with IPC.
     */
    public class LocalBinder extends Binder 
    {
        KomServer getService() 
        {
            return KomServer.this;
        }
    }

    /**
     * Small helper class which maps conference names and LysKOM id's.
     */
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

    /**
     * Small helper class to manage texts.
     */
    public class TextInfo 
    {
    	public TextInfo() { }

    	public TextInfo(int textNo, String author, String date, String subject, String body)
    	{
    		this.textNo = textNo;
    		this.author = author;
    		this.date = date;
    		this.subject = subject;
    		this.body = body;
    	}
    	
		public int textNo;
		public String date;
    	public String author;
    	public String subject;
    	public String body;
    }
    
    public KomServer() {
        System.setProperty("lattekom.enable-prefetch", "true"); 
        mLastTextNo = -1;
        mPendingSentTexts = new HashSet<Integer>();
    }


    @Override
    public void onCreate() 
    {
        super.onCreate();

        if (s == null) {
            s = new Session();

            s.addRpcEventListener(this);
            //s.addAsynchMessageReceiver(this);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) 
    {
        return mBinder;
    }


    /**
     * Called upon destruction of the service. If we're logged in,
     * we want to log out and close the connection now.
     */
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

    /**
     * Connect to LysKOM server.
     * 
     * @return 0 on success, non-zero on failure.
     */
    public int connect(String server) 
    {
        try {
            s.connect(server);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    /**
     * Fetch a list of conferences with unread texts.
     */
    public List<ConferenceInfo> fetchConferences() 
    {
        ArrayList<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();
        try { 
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

    /**
     * Return name for given conference.
     */
    public String getConferenceName(int conf)
    {
    	try {
			return s.toString(s.getConfName(conf));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
    }
    

    /**
     * Return name for given conference.
     */
    public String getConferenceName()
    {
    	return getConferenceName(s.getCurrentConference());
    }
    /**
     * Set currently active conference.
     */
    public void setConference(int confNo) 
    {
        try {
            s.changeConference(confNo);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Log in to server. 
     * 
     * @return Empty string on success, string describing failure otherwise
     */
    public String login(String username, String password, String server) 
    {
    	Log.d(TAG, "Trying to login username:"+username);
        if (!s.getConnected()) {
            if (connect(server) != 0)
                return "Couldn't connect to server";
        }

        usernames = new ConfInfo[0];
        try {
            usernames = s.lookupName(username, true, false);
            if (usernames.length != 1) {            
                return "Invalid/ambigious username";
            } else {
                // login as hidden
                if (!s.login(usernames[0].confNo, password, true)) {
                    return "Invalid password";
                }
                s.setClientVersion("Androkom", getVersionName());
            }
        } catch (Exception e) {
            Log.e("androkom", "Caught " + e.getClass().getName());
            return "Unknown error";
        }
        return "";
    }

    /**
     * Log in to server. 
     * 
     * @return Empty string on success, string describing failure otherwise
     */
    public String login(int userid, String password, String server) 
    {
    	Log.d(TAG, "Trying to login userid:"+userid);
        usernames = new ConfInfo[0];
        if (!s.getConnected()) {
            if (connect(server) != 0)
                return "Couldn't connect to server";
        }

        try {
        	// login as hidden
        	if (!s.login(userid, password, true)) {
        		return "Invalid password";
        	}
        	s.setClientVersion("Androkom", getVersionName());
        } catch (Exception e) {
            Log.e("androkom", "Caught " + e.getClass().getName());
            return "Unknown error";
        }
        return "";
    }

    public String getVersionName() {
    	try {
    		PackageInfo pinfo = getBaseContext().getPackageManager().getPackageInfo("org.lindev.androkom", 0);
    		return pinfo.versionName;
    	} catch (android.content.pm.PackageManager.NameNotFoundException e) {
        return "unknown";
      }
    }

    public TextInfo getParentToText(int textNo)
    {
    	try {
			Text t = s.getText(textNo);
			int arr[] = t.getCommented();
			if (arr.length > 0) {
				return getTextAsHTML(arr[0]);
			}
		} catch (RpcFailure e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new TextInfo(-1, "", "", "", "[error fetching text]");
    }

    /**
     * Display the next unread text in a TextView. 
     * 
     * TODO: This should not interact directly with GUI components.
     * 
     * @return text number displayed
     */
    public TextInfo getNextUnreadText() 
    {

        try {
            mLastTextNo = s.nextUnreadText(false);
            if (mLastTextNo < 0) {                
                s.nextUnreadConference(true);
                               
                mLastTextNo = s.nextUnreadText(false);
                if (mLastTextNo < 0)
                	return new TextInfo(-1, "", "", "", "All read");
            } 
            
            return getTextAsHTML(mLastTextNo);                                

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new TextInfo(-1, "", "", "", "[error fetching text]");
    }


    /**
     * Fetch next unread text, as a HTML formatted string. 
     */
    public TextInfo getTextAsHTML(int textNo)
    {
        try {
            Text text;
            text = s.getText(textNo);


            final String username = s.getConfStat(text.getAuthor()).getNameString();

            // TODO: This will only mark text as read in the current conference.
            // TODO: Should batch these up and send in a group, instead of many separate requests.
            int confNo = s.getCurrentConference();
            int[] localTextNo = { text.getLocal(confNo) };
            s.doMarkAsRead(confNo, localTextNo); 

          
            return new TextInfo(textNo, username, text.getCreationTimeString(), text.getSubjectString(), text.getBodyString());


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new TextInfo(-1, "", "", "", "[Error fetching text]");

    }

    /**
     * Get text number of last read text in current meeting, 
     * or -1 if there is no suitable text.
     */
    public int getLastTextNo()
    {
        return mLastTextNo;
    }

    /**
     * Create a text, which is not a reply to another text.
     */
    public void createText(String subject, String body)
    {
        createText(subject, body, -1);
    }

    /**
     * Create a text, in reply to another text.
     */
    public void createText(String subject, String body, int inReplyTo) 
    {
        Text text = new Text();

        if (inReplyTo != -1)
            text.addCommented(inReplyTo);

        text.addRecipient(s.getCurrentConference());

        final byte[] subjectBytes = subject.getBytes();
        final byte[] bodyBytes = body.getBytes();

        byte[] contents = new byte[subjectBytes.length + bodyBytes.length + 1];
        System.arraycopy(subjectBytes, 0, contents, 0, subjectBytes.length);
        System.arraycopy(bodyBytes, 0, contents, subjectBytes.length+1, bodyBytes.length);
        contents[subjectBytes.length] = (byte) '\n';

        text.setContents(contents);
        text.getStat().setAuxItem(new AuxItem(AuxItem.tagContentType, "text/x-kom-basic;charset=utf-8")); 

        try {
            mPendingSentTexts.add(s.doCreateText(text).getId());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    String[] getNextHollerith(String s) {
        int prefixLen = s.indexOf("H");

        int len = Integer.parseInt(s.substring(0, prefixLen));

        prefixLen++;
        String first = s.substring(prefixLen, prefixLen + len);


        String second;
        if (s.length() > first.length() + prefixLen + 1)
            second = s.substring(first.length() + prefixLen + 1);
        else
            second = "";
        
        return new String[]{first, second};
    }

    /**
     * Parse properties the elisp client has set, if any.
     */
    private void parseElispUserArea()
    {
        try {

            UserArea ua = s.getUserArea();
            String[] blocks = ua.getBlockNames();


            mUserAreaProps = new HashMap<String, String>();


            for(String block : blocks) {
               
                if (block.equals("elisp")) {
                    String token = ua.getBlock(block).getContentString();
                    while(token.length() > 0) {
                        String[] first = getNextHollerith(token);
                        String[] second = getNextHollerith(first[1]);

                        mUserAreaProps.put(first[0], second[0]);
                        token = second[1];
                    }
                }

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    /**
     * Get a list of the IDs of all friends which are set in the elisp client 
     * user area.
     */
    public void getFriends()
    {
        parseElispUserArea();
        String friends = mUserAreaProps.get("kom-friends");
        if (friends != null) {
            friends = friends.substring(1,friends.length()-2);
            String[] friendList = friends.split(" ");
            for(String friend : friendList) {
                Log.i("androkom", "friend " + friend);
            }
        }
    }
    private Session s;

    private int mLastTextNo;
    HashMap<String, String> mUserAreaProps;

    // This is the object that receives interactions from clients. 
    private final IBinder mBinder = new LocalBinder();

    public void rpcEvent(RpcEvent e) {
        if (mPendingSentTexts.contains(e.getId())) {
            Log.i("androkom", "Got reply for created text " + e.getId());

            if (!e.getSuccess())
                /* TODO: handle error here */;
        }

    }

    public void asynchMessage(AsynchMessage m) {
        // TODO Auto-generated method stub
    	Log.d(TAG, "asynchMessage:"+m);
    }

    public ConfInfo[] getUserNames() {
    	return usernames;
    }
    
    private HashSet<Integer> mPendingSentTexts;
    ConfInfo usernames[];
}

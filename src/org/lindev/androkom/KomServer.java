package org.lindev.androkom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nu.dll.lyskom.ConfInfo;
import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.DynamicSessionInfo;
import nu.dll.lyskom.KomTime;
import nu.dll.lyskom.KomToken;
import nu.dll.lyskom.Mark;
import nu.dll.lyskom.Membership;
import nu.dll.lyskom.Person;
import nu.dll.lyskom.RpcCall;
import nu.dll.lyskom.RpcEvent;
import nu.dll.lyskom.RpcEventListener;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Session;
import nu.dll.lyskom.Text;
import nu.dll.lyskom.TextMapping;
import nu.dll.lyskom.TextStat;
import nu.dll.lyskom.UConference;
import nu.dll.lyskom.UserArea;

import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;
import org.lindev.androkom.WhoIsOn.populatePersonsTask;
import org.lindev.androkom.im.IMLogger;
import org.lindev.androkom.im.IMNotification;
import org.lindev.androkom.text.TextFetcher;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.Spannable;
import android.util.Log;
import android.widget.Toast;


/**
 * A service which keeps the Lattekom session object and all
 * LysKOM stuff for the various activities in the app.
 * 
 * @author henrik
 *
 */
public class KomServer extends Service implements RpcEventListener,
		nu.dll.lyskom.Log {
	public static final String TAG = "Androkom KomServer";
	public static boolean RELEASE_BUILD = false;

	private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
	    @Override
        public void onReceive(Context context, Intent intent) {
	        Log.d(TAG, "onReceive1");
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            //Log.d(TAG, "onReceive2");
            NetworkInfo activeNetInfo = connectivityManager
                    .getActiveNetworkInfo();
            //Log.d(TAG, "onReceive3");
            @SuppressWarnings("unused")
            NetworkInfo mobNetInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            //Log.d(TAG, "onReceive4");
            if (activeNetInfo != null) {
                //Log.d(TAG, "onReceive5");
                Log.d(TAG, "mConnReceiver isConnected:"+activeNetInfo.isConnected());
                setConnected(activeNetInfo.isConnected());
                //Log.d(TAG, "onReceive6");
            } else {
                Log.d(TAG, "mConnReceiver is not Connected.");
                setConnected(false);
                //Log.d(TAG, "onReceive7");
            }
            Log.d(TAG, "onReceive done");
	    }
	};
	
	/**
     * Class for clients to access.  Because we assume this service always
     * runs in the same process as its clients, we don't deal with IPC.
     */
/*    public class LocalBinder extends Binder
    {
        public KomServer getService()
        {
            return KomServer.this;
        }
    }
*/
	
    /**
     * Small helper class to manage texts.
     */
    public static class TextInfo
    {
        public static final int ALL_READ=1;
        public static final int ERROR_FETCHING_TEXT=2;
        public static final int NO_PARENT=3;

        public TextInfo(Context context, int textNo, String author, int authorno,
                String date, String all_headers, String visible_headers,
                String subject, String body, byte[] rawBody,
                int ShowHeadersLevel) {
            this.textNo = textNo;
            this.author = author;
            this.authorno = authorno;
            this.date = date;
            this.visible_headers = visible_headers;
            this.all_headers = all_headers;
            this.subject = subject;
            this.body = body;
            this.rawBody = rawBody;
            this.spannable = Conference.formatText(context, this);
            this.spannableHeaders = Conference.formatHeaders(context, this,
                    ShowHeadersLevel);
        }

        public static TextInfo createText(Context context, int id) {
            switch (id) {
            case ALL_READ:
                Log.d(TAG, "createText ALL_READ");
                return new TextInfo(context, -1, "", 0, "", "", "", "", context
                        .getString(R.string.all_read), null, 1);
            case ERROR_FETCHING_TEXT:
                Log.d(TAG, "createText ERROR_FETCHING_TEXT");
                return new TextInfo(context, -2, "", 0, "", "", "", "", context
                        .getString(R.string.error_fetching_text), null, 1);
            case NO_PARENT:
                Log.d(TAG, "createText NO_PARENT");
                return new TextInfo(context, -1, "", 0, "", "", "", "", context
                        .getString(R.string.error_no_parent), null, 1);
            default:
                Log.d(TAG, "createText default");
                return new TextInfo(context, -2, "", 0, "", "", "", "", context
                        .getString(R.string.error_fetching_text), null, 1);
            }
        }

        public String getAuthor() {
            return author;
        }

        public String getBody() {
            return body;
        }

        public byte[] getRawBody() {
            return rawBody;
        }

        public String getVisibleHeaders() {
            return visible_headers;
        }

        public String getAllHeaders() {
            return all_headers;
        }

        public String getSubject() {
            return subject;
        }

        public String getDate() {
            return date;
        }

        public int getTextNo() {
            return textNo;
        }

        public Spannable getSpannableHeaders() {
            return spannableHeaders;
        }

        public Spannable getSpannableBody() {
            return spannable;
        }

        public int getAuthorNo() {
            return authorno;
        }

        private int textNo;
        private String date;
        private String subject;
        private String visible_headers;
        private String all_headers;
        private String body;
        private byte[] rawBody;
        private String author;
        private int authorno;
        private Spannable spannable;
        private Spannable spannableHeaders;

    }
    
    public KomServer() {
    	if (!RELEASE_BUILD)
    		System.setProperty("lattekom.debug", "true");
        System.setProperty("lattekom.enable-prefetch", "true"); 
        Session.setLog(this);
        mLastTextNo = -1;
        mPendingSentTexts = new HashSet<Integer>();
    }


    @Override
    public void onCreate() 
    {
        super.onCreate();
        
        Log.d(TAG, "onCreate");
        
        asyncMessagesHandler = new AsyncMessages(getApp(), this);
        asyncMessagesHandler
                .subscribe(asyncMessagesHandler.new MessageToaster());
        
        imLogger = new IMLogger(this);
        new IMNotification(this);
        asyncMessagesHandler.subscribe(imLogger);

        if (s == null) {
            s = new Session();
            s.addRpcEventListener(this);
        }
        
        registerReceiver(mConnReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
   }

    @Override
    public IBinder onBind(Intent arg0) 
    {
        Log.d(TAG, "onBind");
        return new LocalBinder<KomServer>(this);
    }

    public boolean onUnbind (Intent intent) {
        Log.d(TAG, "onUnbind");
        return false;
    }
    
    App getApp() 
    {
        return (App) getApplication();
    }

    /**
     * Called upon destruction of the service. If we're logged in,
     * we want to log out and close the connection now.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        imLogger.close();
        // Tell the user we stopped.
        Toast.makeText(this, getString(R.string.komserver_stopped),
                Toast.LENGTH_SHORT).show();

        unregisterReceiver(mConnReceiver);

        logout();
        getApp().shutdown();

        super.onDestroy();
    }

    /**
     * When no need to wait for logout
     * 
     */
    private class LogoutTask extends AsyncTask<KomToken, Void, Void> {
        protected void onPreExecute() {
            Log.d(TAG, "LogoutTask.onPreExecute");
        }

        // worker thread (separate from UI thread)
        protected Void doInBackground(final KomToken... args) {
            try {
                logout();
            } catch (Exception e1) {
                Log.i(TAG, "Failed to logout exception:"+e1);
                //e1.printStackTrace();
            }
            return null;
        }
    }

    public void logout() {
        Log.d(TAG, "KomServer logout");
        if (s != null) {
            try {
                if (s.getState() == Session.STATE_LOGIN)
                    s.logout(true);
                Log.i("androkom", "logged out");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "logout1 " + e);
                // e.printStackTrace();
            }

            try {
                s.disconnect(true);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "logout2 " + e);
                // e.printStackTrace();
            }

            if(s!=null) {
                try {
                    s.removeRpcEventListener(this);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    Log.d(TAG, "logout3 " + e);
                    // e.printStackTrace();
                }
            }
            Log.i("androkom", "disconnected");
        }

        s = null;
    }

    /**
     * When no need to wait for reconnect
     * 
     */
/*    private class ReconnectTask extends AsyncTask<KomToken, Void, Void> {
        protected void onPreExecute() {
            Log.d(TAG, "ReconnectTask.onPreExecute");
        }

        // worker thread (separate from UI thread)
        protected Void doInBackground(final KomToken... args) {
            try {
                reconnect();
            } catch (Exception e1) {
                Log.i(TAG, "Failed to reconnect exception:"+e1);
                //e1.printStackTrace();
            }
            return null;
        }
    }
*/

    public void reconnect() {
        Log.d(TAG, "KomServer trying to reconnect 1");
        new LogoutTask().execute();
    	
        Log.d(TAG, "KomServer trying to reconnect 2");

        s = new Session();
        s.addRpcEventListener(this);

        if (re_userid > 0) {
            Log.d(TAG, "KomServer trying to login using id " + re_userid
                    + " on server " + re_server);
            login(re_userid, re_password, re_server, re_port, re_useSSL, re_cert_level);
        } else {
            Log.d(TAG, "Can't reconnect because no userid");
        }
    }

    /**
     * Connect to LysKOM server.
     * @param port 
     * 
     * @return 0 on success, non-zero on failure.
     */
    public int connect(String server, int port, boolean useSSL, int cert_level) 
    {
        String  ANDROID         =   android.os.Build.VERSION.RELEASE;       //The current development codename, or the string "REL" if this is a release build.
        String  BOARD           =   android.os.Build.BOARD;                 //The name of the underlying board, like "goldfish".    
        //String  BOOTLOADER      =   android.os.Build.BOOTLOADER;            //  The system bootloader version number.
        String  BRAND           =   android.os.Build.BRAND;                 //The brand (e.g., carrier) the software is customized for, if any.
        String  CPU_ABI         =   android.os.Build.CPU_ABI;               //The name of the instruction set (CPU type + ABI convention) of native code.
        //String  CPU_ABI2        =   android.os.Build.CPU_ABI2;              //  The name of the second instruction set (CPU type + ABI convention) of native code.
        String  DEVICE          =   android.os.Build.DEVICE;                //  The name of the industrial design.
        String  DISPLAY         =   android.os.Build.DISPLAY;               //A build ID string meant for displaying to the user
        String  FINGERPRINT     =   android.os.Build.FINGERPRINT;           //A string that uniquely identifies this build.
        //String  HARDWARE        =   android.os.Build.HARDWARE;              //The name of the hardware (from the kernel command line or /proc).
        String  HOST            =   android.os.Build.HOST;  
        String  ID              =   android.os.Build.ID;                    //Either a changelist number, or a label like "M4-rc20".
        String  MANUFACTURER    =   android.os.Build.MANUFACTURER;          //The manufacturer of the product/hardware.
        String  MODEL           =   android.os.Build.MODEL;                 //The end-user-visible name for the end product.
        String  PRODUCT         =   android.os.Build.PRODUCT;               //The name of the overall product.
        String  RADIO           =   android.os.Build.PRODUCT;               //The radio firmware version number.
        String  TAGS            =   android.os.Build.TAGS;                  //Comma-separated tags describing the build, like "unsigned,debug".
        String  TYPE            =   android.os.Build.TYPE;                  //The type of build, like "user" or "eng".
        String  USER            =   android.os.Build.USER;                  //

        try {
            
            try {
                String hostName = MANUFACTURER.replace(' ', '_') + "_" + MODEL.replace(' ', '_');
                s.setClientHost(hostName);
            } catch (Exception e) {
            }

            try {
                AccountManager accountManager = AccountManager.get(this);
                Account[] accounts = accountManager.getAccountsByType("com.google");
                String userName = accounts[0].name;
                int splitIndex = userName.indexOf("@");
                s.setClientUser(userName.substring(0, splitIndex));
            } catch (Exception e) {
            }
            
            s.connect(server, port, useSSL, cert_level,
                       getBaseContext().getResources().openRawResource(R.raw.root_keystore));
            s.addAsynchMessageReceiver(asyncMessagesHandler);
        } catch (IOException e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "connect1 "+e);

            //e.printStackTrace();
            return -1;
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "connect2 "+e);
        	e.printStackTrace();
            return -1;
        }

        return 0;
    }

    public void disconnect() {
    	try {
    		s.disconnect(true);
		} catch (RpcFailure e) {
		    Log.d(TAG, "disconnect RpcFailure"+e);
			//e.printStackTrace();
		} catch (Exception e) {
		    Log.d(TAG, "disconnect "+e);
			//e.printStackTrace();
		}
    }

    /**
     * Fetch a username from userid
     * 
     * @param userid
     */
    public String fetchUsername(int persNo) {
        String username;
        if (persNo > 0) {
            try {
                nu.dll.lyskom.Conference confStat = s.getConfStat(persNo);
                username = confStat.getNameString();
            } catch (Exception e) {
                Log.d(TAG, "fetchUsername caught exception from getConfStat"+e);
                //e.printStackTrace();
                username = getString(R.string.person) + persNo
                        + getString(R.string.does_not_exist);
            }
        } else {
            Log.d(TAG, "fetchPersons persNo=" + persNo);
            username = getString(R.string.anonymous);
        }
        return username;
    }
    
    /**
     * Fetch a list of persons online
     * 
     * @param populatePersonsTask
     * @throws IOException 
     */
    public List<ConferenceInfo> fetchPersons(
            populatePersonsTask populatePersonsT, int who_type) throws IOException {
        if (s == null) {
            return null;
        }

        Set<Integer> friendsList = new HashSet<Integer>();

        ArrayList<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();
        if (who_type == 2) {
            friendsList = getFriends();
        }

        try {
            DynamicSessionInfo[] persons = s.whoIsOnDynamic(true, false,
                    30 * 60);
            if (populatePersonsT != null) {
                populatePersonsT.changeMax(persons.length);
            }

            for (int i = 0; i < persons.length; i++) {
                int persNo = persons[i].getPerson();
                if ((who_type == 1) || (friendsList.contains(persNo))) {
                    String username;
                    if (persNo > 0) {
                        try {
                            nu.dll.lyskom.Conference confStat = s
                                    .getConfStat(persNo);
                            username = confStat.getNameString();
                        } catch (Exception e) {
                            username = getString(R.string.person) + persNo
                                    + getString(R.string.does_not_exist);
                        }
                    } else {
                        Log.d(TAG, "fetchPersons persNo=" + persNo);
                        username = getString(R.string.anonymous);
                    }
                    Log.i("androkom", username + " <" + persNo + ">");

                    ConferenceInfo info = new ConferenceInfo();
                    info.id = persNo;
                    info.name = username;
                    info.sessionNo = persons[i].session;

                    arr.add(info);
                }
                if (populatePersonsT != null) {
                    populatePersonsT
                            .updateProgress((int) ((i / (float) persons.length) * 100));
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "fetchPersons caught an exception:" + e);
        }
        return arr;
    }
    
    /**
     * Add a new subscriber who's interested in asynchronous messages.
     */
    public void addAsyncSubscriber(AsyncMessageSubscriber sub) {
    	asyncMessagesHandler.subscribe(sub);
    }
    
    /**
     * Add a new subscriber who's interested in asynchronous messages.
     */
    public void removeAsyncSubscriber(AsyncMessageSubscriber sub) {
    	asyncMessagesHandler.unsubscribe(sub);
    }

    /**
     * Fetch a list of conferences with unread texts.
     * @throws IOException 
     * @throws RpcFailure 
     * @throws UnsupportedEncodingException 
     */
    public List<ConferenceInfo> fetchConferences() throws UnsupportedEncodingException, RpcFailure, IOException {
        List<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();
        for (int conf : s.getMyUnreadConfsList(true)) {
            final String name = s.toString(s.getConfName(conf));
            Log.i(TAG, name + " <" + conf + ">");
            final ConferenceInfo info = new ConferenceInfo();
            info.id = conf;
            info.name = name;
            info.numUnread = s.getUnreadCount(conf);
            arr.add(info);
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
        	Log.d(TAG, "getConferenceName "+e);

			//e.printStackTrace();
		}
		return "";
    }
    

    private String mConfName = "";
    public void setConferenceName(final String name) {
        mConfName = name;
    }

    /**
     * Return name for given conference.
     */
    public String getConferenceName() {
        return mConfName;
    }

    /**
     * Return presentation text number for current conference.
     */
	public int getConferencePres() {
		int confNo = s.getCurrentConference();
		if (confNo > 0) {
			try {
				return s.getConfStat(confNo).getPresentation();
			} catch (RpcFailure e) {
			    Log.d(TAG, "getConferencePres RpcFailure"+e);
				//e.printStackTrace();
			} catch (IOException e) {
                Log.d(TAG, "getConferencePres IOException"+e);
				//e.printStackTrace();
			}
		}
		return 0;
	}

    /**
     * Return presentation text number for userid.
     */
    public int getUserPres(int userid) {
        int confNo = userid;
        if (confNo > 0) {
            try {
                return s.getConfStat(confNo).getPresentation();
            } catch (RpcFailure e) {
                Log.d(TAG, "getConferencePres RpcFailure"+e);
                //e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "getConferencePres IOException"+e);
                //e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Return number of unreads for current conference.
     */
    public int getConferenceUnreadsNo() {
        int confNo = s.getCurrentConference();
        if (confNo > 0) {
            try {
                return s.getUnreadCount(confNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "getConferenceUnreadsNo RpcFailure:"+e);
                //e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "getConferenceUnreadsNo IOException:"+e);
                //e.printStackTrace();
            }
        }
        Log.d(TAG, "getConferenceUnreadsNo no current conference (or exception)");
        return 0;
    }

    /**
     * Set currently active conference.
     * @throws IOException 
     * @throws RpcFailure 
     */
    public void setConference(final int confNo) throws RpcFailure, IOException {
        if (s != null) {
            Log.d(TAG, "setConference Byt till conf:" + confNo);
            s.changeConference(confNo);
            readMarker.clear();
            textFetcher.restartPrefetcher();
        } else {
            Log.d(TAG, "setConference Ingen session");
        }
    }

    /* Send message about user active to server.
     * Will not send more than once every 30 sek to keep from 
     * flooding server.
     */
    public void activateUser() throws Exception {
        long long_now = System.currentTimeMillis();
        if (s==null) {
            throw new Exception("Not logged in");
        }
        if ((long_now - lastActivate) > (30 * 1000)) {
            s.doUserActive();
            lastActivate = long_now;
        }
    }
    
    /**
     * Log in to server. 
     * 
     * @return Empty string on success, string describing failure otherwise
     */
    public String login(String username, String password, String server, int port, boolean useSSL, int cert_level) 
    {
    	Log.d(TAG, "Trying to login username:"+username);
        Log.d(TAG, "Trying to login server:"+server);
        Log.d(TAG, "Trying to login port:"+port);
        Log.d(TAG, "Trying to login usessl:"+useSSL);
        Log.d(TAG, "Trying to login cert_level:"+cert_level);

    	if (s == null) {
            s = new Session();
            s.addRpcEventListener(this);
        }

    	try {
    		if (!s.getConnected()) {
    			if (connect(server, port, useSSL, cert_level) != 0) {
                    if (s != null) {
                        s.disconnect(true);
                    }
    				s = null;
                    return getString(R.string.error_could_not_connect);
    			}
    		}
        } catch (Exception e) {
            Log.e("androkom", "Login.name connect Caught " + e.getClass().getName()+":"+e+":"+e.getCause());
            try {
                s.disconnect(true);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                //e1.printStackTrace();
            }
            s = null;
            //e.printStackTrace();
            return getString(R.string.error_unknown)+"(1)";
        }

        usernames = new ConfInfo[0];
        try {
            usernames = s.lookupName(username, true, false);
            if (usernames.length != 1) {            
                return getString(R.string.error_ambigious_name);
            } else {
                // login as hidden
                if (!s.login(usernames[0].confNo, password, hidden_session, false)) {
                    return getString(R.string.error_invalid_password);
                }
            }
        } catch (Exception e) {
            Log.e("androkom", "Login.name Caught " + e.getClass().getName()+":"+e+":"+e.getCause());
            //e.printStackTrace();
            return getString(R.string.error_unknown)+"(2)";
        }
        try {
            s.setClientVersion("Androkom", getVersionName());
            s.setLatteName("AndroKOM " + getVersionName());
        } catch (Exception e) {
        	Log.e(TAG, "Login.name2 Caught " + e.getClass().getName()+":"+e+":"+e.getCause());
        	//e.printStackTrace();
        }
        try {
            re_userid = usernames[0].confNo;
            re_password = password;
            re_server = server;
            re_port = port;
            re_useSSL = useSSL;
            re_cert_level = cert_level;
        } catch (Exception e) {
            Log.e(TAG, "Login.name3 Caught " + e.getClass().getName() + ":" + e
                    + ":" + e.getCause());
            return getString(R.string.error_unknown)+"(3)";
        }

        try {
            parseCommonUserArea();
            parseElispUserArea();
        } catch (Exception e) {
            Log.e(TAG, "Login.name4 Caught " + e.getClass().getName() + ":" + e
                    + ":" + e.getCause());
            return getString(R.string.error_unknown)+"(4)";
        }
        return "";
    }

    /**
     * Log in to server. 
     * @param port 
     * 
     * @return Empty string on success, string describing failure otherwise
     */
    public String login(int userid, String password, String server, int port, boolean useSSL, int cert_level) 
    {
    	Log.d(TAG, "Trying to login userid:"+userid);
        if (s == null) {
            s = new Session();
            s.addRpcEventListener(this);
        }
        usernames = new ConfInfo[0];
        try {
            if (!s.getConnected()) {
                if (connect(server, port, useSSL, cert_level) != 0) {
                    try {
                        if (s != null) {
                            s.disconnect(true);
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        // e.printStackTrace();
                    }
                    s = null;
                    return getString(R.string.error_could_not_connect);
                }
            }
        } catch (Exception e) {
            s = null;
            return getString(R.string.error_could_not_connect);
        }
        
        try {
        	// login as hidden
        	if (!s.login(userid, password, hidden_session, false)) {
        		return getString(R.string.error_invalid_password);
        	}
        	s.setClientVersion("Androkom", getVersionName());
        	s.setLatteName("AndroKOM " + getVersionName());
        } catch (Exception e) {
            Log.e("androkom", "Login.id Caught " + e.getClass().getName());
            //Log.e("androkom", "Login.id Caught " + e.getClass().getName()+e.getStackTrace());
            return "Unknown error";
        }
        re_userid = userid;
        re_password = password;
        re_server = server;
        re_port = port;
        re_useSSL = useSSL;
        re_cert_level = cert_level;

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

    public void endast(int confNo, int no) {
    	try {
			s.endast(confNo, no);
		} catch (RpcFailure e) {
		    Log.d(TAG, "endast RpcFailure:"+e);
		    //e.printStackTrace();
		} catch (IOException e) {
            Log.d(TAG, "endast IOException:"+e);
			//e.printStackTrace();
		}
    }

    public void joinconference(int confNo) {
        try {
            s.joinConference(confNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "joinconference RpcFailure:"+e);
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "joinconference IOException:"+e);
            //e.printStackTrace();
        }
    }

    public void leaveConference(int confNo) {
        try {
            s.leaveConference(confNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "leaveConference RpcFailure:"+e);
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "leaveConference IOException:"+e);
            //e.printStackTrace();
        }
    }

    public String decodeKomErrorCode(int code) {
        switch (code) {
        case 0:
            return "no-error";
        case 2:
            return "not-implemented";
        case 3:
            return "obsolete-call";
        case 4:
            return "invalid-password";
        case 5:
            return "string-too-long";
        case 6:
            return "login-first";
        case 7:
            return "login-disallowed";
        case 8:
            return "conference-zero";
        case 9:
            return "undefined-conference";
        case 10:
            return "undefined-person";
        case 11:
            return "access-denied";
        case 12:
            return "permission-denied";
        case 13:
            return "not-member";
        case 14:
            return "no-such-text";
        case 15:
            return "text-zero";
        case 16:
            return "no-such-local-text";
        case 17:
            return "local-text-zero";
        case 18:
            return "bad-name";
        case 19:
            return "index-out-of-range";
        case 20:
            return "conference-exists";
        case 21:
            return "person-exists";
        case 22:
            return "secret-public";
        case 23:
            return "letterbox";
        case 24:
            return "ldb-error";
        case 25:
            return "illegal-misc";
        case 26:
            return "illegal-info-type";
        case 27:
            return "already-recipient";
        case 28:
            return "already-comment";
        case 29:
            return "already-footnote";
        case 30:
            return "not-recipient";
        case 31:
            return "not-comment";
        case 32:
            return "not-footnote";
        case 33:
            return "recipient-limit";
        case 34:
            return "comment-limit";
        case 35:
            return "footnote-limit";
        case 36:
            return "mark-limit";
        case 37:
            return "not-author";
        case 38:
            return "no-connect";
        case 39:
            return "out-of-memory";
        case 40:
            return "server-is-crazy";
        case 41:
            return "client-is-crazy";
        case 42:
            return "undefined-session";
        case 43:
            return "regexp-error";
        case 44:
            return "not-marked";
        case 45:
            return "temporary-failure";
        case 46:
            return "long-array";
        case 47:
            return "anonymous-rejected";
        case 48:
            return "illegal-aux-item";
        case 49:
            return "aux-item-permission";
        case 50:
            return "unknown-async";
        case 51:
            return "internal-error";
        case 52:
            return "feature-disabled";
        case 53:
            return "message-not-sent";
        case 54:
            return "invalid-membership-type";
        case 55:
            return "invalid-range";
        case 56:
            return "invalid-range-list";
        case 57:
            return "undefined-measurement";
        case 58:
            return "priority-denied";
        case 59:
            return "weight-denied";
        case 60:
            return "weight-zero";
        case 61:
            return "bad-bool";
        default:
            return "Unknown error: " + code;
        }
    }

    public String subRecipient(int textNo, int confNo) {
        String result="";
        
        try {
            Log.d(TAG, "Remove confNo:"+confNo+" from textNo:"+textNo);
            s.subRecipient(textNo, confNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "subRecipient RpcFailure:"+e);
            //e.printStackTrace();
            Log.d(TAG, "Error: "+e.getError());
            Log.d(TAG, "ErrorStatus: "+e.getErrorStatus());
            Log.d(TAG, "Message: "+e.getMessage());

            result = decodeKomErrorCode(e.getError());
        } catch (IOException e) {
            Log.d(TAG, "subRecipient IOException:"+e);
            //e.printStackTrace();
        }
        return result;
    }

    /*
     * Add an aux item of type auxtype to text number textno with content content.
     * Note: textno must be pointing on an text not a conference/person.
     */
    public void addAuxItem(int textno, int auxType, String content) {
        Log.d(TAG, "addAuxItem textno" + textno);
        Log.d(TAG, "addAuxItem axutype" + auxType);
        Log.d(TAG, "addAuxItem content" + content);

        try {
            AuxItem auxItem = new AuxItem(auxType, content);
            AuxItem[] add_items = { auxItem };
            int[] del_items = {};

            s.modifyAuxInfo(false, textno, del_items, add_items);
        } catch (Exception e) {
            Log.d(TAG, "Failed to add aux item. Exception:" + e);
        }
    }
            
    public String subComment(int textNo, int commentNo) {
        String result="";
        
        try {
            Log.d(TAG, "Remove textNo:"+textNo+" from commentNo:"+commentNo);
            s.subComment(textNo, commentNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "subRecipient RpcFailure:"+e);
            //e.printStackTrace();
            Log.d(TAG, "Error: "+e.getError());
            Log.d(TAG, "ErrorStatus: "+e.getErrorStatus());
            Log.d(TAG, "Message: "+e.getMessage());

            result = decodeKomErrorCode(e.getError());
        } catch (IOException e) {
            Log.d(TAG, "subRecipient IOException:"+e);
            //e.printStackTrace();
        }
        return result;
    }

    /**
     * Fetch next unread text, as a HTML formatted string. 
     */
    private final TextFetcher textFetcher = new TextFetcher(this);
    public TextInfo getKomText(final int textNo) {
        return textFetcher.getKomText(textNo);
    }

    public int getNextUnreadTextNo() {
        return textFetcher.getNextUnreadTextNo();
    }

    public TextInfo getNextUnreadText() {
        return textFetcher.getNextUnreadText();
    }

    public TextInfo getParentToText(final int textNo) {
        return textFetcher.getParentToText(textNo);
    }

    private final ReadMarker readMarker = new ReadMarker(this);
    public void markTextAsRead(final int textNo) {
        readMarker.mark(textNo);
    }

    public boolean isLocalRead(final int textNo) {
        return readMarker.isLocalRead(textNo);
    }

    public void markText(int textNo)
    {
    	try {
			s.markText(textNo, 100);
		} catch (RpcFailure e) {
            Log.d(TAG, "markText RpcFailure:"+e);
			//e.printStackTrace();
		} catch (IOException e) {
            Log.d(TAG, "markText IOException:"+e);
            //e.printStackTrace();
		}
    }

    public void unmarkText(int textNo)
    {
    	try {
			s.unmarkText(textNo);
		} catch (RpcFailure e) {
            Log.d(TAG, "unmarkText RpcFailure:"+e);
			//e.printStackTrace();
		} catch (IOException e) {
            Log.d(TAG, "unmarkText IOException:"+e);
            //e.printStackTrace();
		}
    }

    /**
     * Get text number of last read text in current meeting, 
     * or -1 if there is no suitable text.
     */
    public int getLastTextNo()
    {
        return mLastTextNo;
    }

	String[] getNextHollerith(String s) {
		s = s.trim();
		int prefixLen = s.indexOf("H");

        int len = Integer.parseInt(s.substring(0, prefixLen));

        prefixLen++;
        String first = s.substring(prefixLen, prefixLen + len);

		String second;
		if (s.length() > first.length() + prefixLen + 1)
			second = s.substring(first.length() + prefixLen + 1);
		else
			second = "";

		return new String[] { first, second };
	}

	/**
	 * Parse properties from the common area, if any.
	 */
	void parseCommonUserArea() {
		try {
			UserArea ua = s.getUserArea();
			String[] blocks = ua.getBlockNames();

			mCommonUserAreaProps = new HashMap<String, String>();

			for (String block : blocks) {

				if (block.equals("common")) {
					String token = ua.getBlock(block).getContentString();
					while (token.length() > 0) {
						String[] first = getNextHollerith(token);
						String[] second = getNextHollerith(first[1]);

						mCommonUserAreaProps.put(first[0], second[0]);
						token = second[1];
					}
				}

			}
		} catch (Exception e) {
			Log.d(TAG, "parseCommonUserArea " + e);
			//e.printStackTrace();
		}
	}

	/**
	 * Get a property of presence-messages
	 */
	public boolean getPresenceMessages() {
	    boolean presence_messages = true;

        if (mCommonUserAreaProps != null) {
            String messages = mCommonUserAreaProps.get("presence-messages");
            if (messages != null) {
                presence_messages = (messages.compareTo("1") == 0);
            }
        }
		return presence_messages;
	}

	/**
	 * Parse properties the elisp client has set, if any.
	 */
	void parseElispUserArea() {
		try {

			UserArea ua = s.getUserArea();
			String[] blocks = ua.getBlockNames();

			mElispUserAreaProps = new HashMap<String, String>();

			for (String block : blocks) {

				if (block.equals("elisp")) {
					String token = ua.getBlock(block).getContentString();
					while (token.length() > 0) {
						String[] first = getNextHollerith(token);
						String[] second = getNextHollerith(first[1]);

						mElispUserAreaProps.put(first[0], second[0]);
						token = second[1];
					}
				}

			}
		} catch (Exception e) {
			Log.d(TAG, "parseElispUserArea " + e);
			//e.printStackTrace();
		}
	}

	/**
	 * Get a list of the IDs of all friends which are set in the elisp client
	 * user area.
	 */
	public Set<Integer> getFriends() {
		Set<Integer> friendsList=new HashSet<Integer>();
		
		String friends = mElispUserAreaProps.get("kom-friends");
		if (friends != null) {
			friends = friends.substring(1, friends.length() - 2);
			String[] friendList = friends.split(" ");
			for (String friend : friendList) {
				Log.i("androkom", "friend " + friend);
				friendsList.add(Integer.parseInt(friend));
			}
		}
		return friendsList;
	}

    public String addNewRecipientToText(int textNo, int confNo, int texttype) {
        String result="";
        
        Log.d(TAG, "Add new recipient (null method)");
        Log.d(TAG, "-- textNo:" + textNo);
        Log.d(TAG, "-- confNo:" + confNo);
        Log.d(TAG, "-- texttype:" + texttype);
        try {
            s.addRecipient(textNo, confNo, texttype);
        } catch (RpcFailure e) {
            Log.d(TAG, "addNewRecipientToText " + e);
            //e.printStackTrace();
            result = decodeKomErrorCode(e.getError());
        } catch (IOException e) {
            Log.d(TAG, "addNewRecipientToText " + e);
            //e.printStackTrace();
        }
        return result;
    }
    
    public String addNewCommentToText(int textNo, int commentNo) {
        String result="";
        
        Log.d(TAG, "Add new comment (null method)");
        Log.d(TAG, "-- textNo:" + textNo);
        Log.d(TAG, "-- commentNo:" + commentNo);
        try {
            s.addComment(textNo, commentNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "addNewCommentToText " + e);
            //e.printStackTrace();
            result = decodeKomErrorCode(e.getError());
        } catch (IOException e) {
            Log.d(TAG, "addNewCommentToText " + e);
            //e.printStackTrace();
        }
        return result;
    }
    
	public void rpcEvent(RpcEvent e) {
		if (mPendingSentTexts.contains(e.getId())) {
			Log.i("androkom", "Got reply for created text " + e.getId());

			if (!e.getSuccess()) {
				/* TODO: handle error here */;
				Log.d(TAG, "rpcEvent failed " + e);
			}
		}

	}

	public void setLatestIMSender(String name) {
		latestIMSender = name;
	}
	
	public String getLatestIMSender() {
		return latestIMSender;
	}
	
	public boolean sendMessage(int recipient, String message, boolean block)
			throws IOException, RpcFailure {
		final boolean res = s.sendMessage(recipient, message, block);
		imLogger.sendMessage(recipient, message);
		return res;
	}

	public void setShowHeadersLevel(final int h) {
		textFetcher.setShowHeadersLevel(h);
	}

    public ConferenceInfo[] getUserNames() {
        if (s != null) {
            try {
                if (usernames != null && usernames.length > 1) {
                    final ConferenceInfo[] items = new ConferenceInfo[usernames.length];
                    Log.d(TAG, "Ambigous name");
                    for (int i = 0; i < usernames.length; i++) {
                        items[i] = new ConferenceInfo();
                        items[i].name = s.toString(s
                                .getConfName(usernames[i].confNo));
                        items[i].id = usernames[i].confNo;
                        Log.d(TAG, "Name " + i + ":" + items[i]);
                    }
                    return items;
                }

            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, "getUserNames " + e);
                // e.printStackTrace();
            } catch (RpcFailure e) {
                Log.d(TAG, "getUserNames " + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "getUserNames " + e);
                // e.printStackTrace();
            } catch (Exception e) {
                Log.d(TAG, "getUserNames " + e);
                // e.printStackTrace();
            }
        }
        return null;
    }

	public Date getServerTime() throws Exception {
	    if(s!=null) {
	        return s.getTime().getTime();
	    } else {
	        throw new Exception("Not connected");
	    }
	}

    public Person getPersonStat(Integer arg0) {
        Person pers = null;
        try {
            pers = s.getPersonStat(arg0);
        } catch (RpcFailure e) {
            Log.d(TAG, "getUserNames " + e);
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "getUserNames " + e);
            //e.printStackTrace();
        }
        return pers;
    }

    public String getClientName(int sessionNo) {
        String clientName = "";
        try {
            byte[] clientBytes = s.getClientName(sessionNo);
            clientName = s.toString(clientBytes);
        } catch (RpcFailure e) {
            Log.d(TAG, "getClientName " + e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "getClientName " + e);
            //e.printStackTrace();
        }
        return clientName;
    }

    public String getClientVersion(int sessionNo) {
        String clientName = "";
        try {
            byte[] clientBytes = s.getClientVersion(sessionNo);
            clientName = s.toString(clientBytes);
        } catch (RpcFailure e) {
            Log.d(TAG, "getClientVersion " + e);
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "getClientVersion " + e);
            //e.printStackTrace();
        }
        return clientName;
    }

    public String getConnectionTime(int sessionNo) {
        KomTime ctime = null;
        try {
            ctime = s.getStaticSessionInfo(sessionNo).getConnectionTime();
            Date CreationTime = ctime.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm]");
            return sdf.format(CreationTime);
        } catch (RpcFailure e) {
            Log.d(TAG, "getConnectionTime " + e);
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "getConnectionTime " + e);
            //e.printStackTrace();
        }
        return "";
    }

    public Text getTextbyNo(int textNo) {
        Text text=null;
        if(s==null) {
            return null;
        }
        try {
            text=s.getText(textNo);
        } catch (RpcFailure e) {
            if(e.getError()==14) {
                Log.d(TAG, "komserver.getTextbyNo No such text#:" + e.getErrorStatus());
                
            } else {
                Log.d(TAG, "komserver.getTextbyNo new_text RpcFailure:" + e);
            }
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.getTextbyNo new_text IOException:" + e);
            // e.printStackTrace();
        }
        if(text==null){
            Log.d(TAG, "getTextbyNo could not get a text for "+textNo);
        } else {
            Log.d(TAG, "getTextbyNo returning a text for "+textNo);
        }
        return text;
    }
    
    public ConfInfo[] lookupName(String name, boolean wantPersons, boolean wantConfs) {
        ConfInfo[] text=null;
        if(s==null) {
            return null;
        }
        try {
            text=s.lookupName(name, wantPersons, wantConfs);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.lookupName new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.lookupName new_text IOException:" + e);
            // e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.d(TAG, "Ran out of index, trying to recover. name="+name+" wantPersons="+wantPersons+" wantConfs="+wantConfs);
            e.printStackTrace();
        }
        return text;
    }

    public TextStat getTextStat(int textNo, boolean refreshCache) {
        TextStat text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.getTextStat(textNo, refreshCache);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getTextStat trying to recover from RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.getTextStat trying to recover from IOException:" + e);
            // e.printStackTrace();
        } catch (ClassCastException e) {
            Log.d(TAG, "komserver.getTextStat trying to recover from ClassCastException:" + e);
        } catch (Exception e) {
            Log.d(TAG, "komserver.getTextStat trying to recover from Exception:" + e);
            e.printStackTrace();
        }
        return text;
    }
    
    public RpcCall doMarkAsRead(int confNo, int[] localTextNo) {
        RpcCall text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.doMarkAsRead(confNo, localTextNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.doMarkAsRead new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.doMarkAsRead new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }
 
    public nu.dll.lyskom.Conference getConfStat(int confNo) {
        nu.dll.lyskom.Conference text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.getConfStat(confNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getConfStat new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.getConfStat new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public byte[] getConfName(int confNo) {
        byte[] text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.getConfName(confNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getConfName new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.getConfName new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public String toString(byte[] buf) {
        String text = null;
        if ((s == null)||(buf==null)) {
            return null;
        }
        try {
            text = s.toString(buf);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.toString new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.toString new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public boolean isMemberOf(int confNo) {
        boolean text = false;
        if (s == null) {
            return false;
        }
        try {
            text = s.isMemberOf(confNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.isMemberOf new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.isMemberOf new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public List<Integer> getUnreadConfsListCached() {
        List<Integer> text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.getUnreadConfsListCached();
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getUnreadConfsListCached new_text RpcFailure:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public Membership queryReadTexts(int persNo, int confNo, boolean refresh) {
        Membership text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.queryReadTexts(persNo, confNo, refresh);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.queryReadTexts new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.queryReadTexts new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public UConference getUConfStat(int confNo) {
        UConference text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.getUConfStat(confNo);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getUConfStat new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.getUConfStat new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public TextMapping localToGlobal(int confNo, int firstLocalNo,
            int noOfExistingTexts) {
        TextMapping text = null;
        if (s == null) {
            return null;
        }
        try {
            text = s.localToGlobal(confNo, firstLocalNo, noOfExistingTexts);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.localToGlobal new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.localToGlobal new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public int createText(Text t, boolean autoreadmarkowntext) throws IOException {
        int text = 0;
        if (s == null) {
            return 0;
        }
        try {
            text = s.createText(t);
            if (text == 0) {
                Log.d(TAG, "createText did not get a textnumber");
            } else {
                if (autoreadmarkowntext) {
                    s.markAsRead(text);
                }
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.localToGlobal new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.localToGlobal new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public int getCurrentConference() {
        int text = 0;
        if (s == null) {
            return 0;
        }
        try {
            text = s.getCurrentConference();
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getCurrentConference new_text RpcFailure:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public List<TextInfo> nextUnreadTexts(int ConfNo) {
        List<TextInfo> ret_data = new ArrayList<TextInfo>();
        List<Integer> data = null;

        if (s == null) {
            return null;
        }
        try {
            data = s.nextUnreadTexts(ConfNo, false, 20);
            Iterator<Integer> iter = data.iterator();
            while(iter.hasNext()) {
                Integer textno = iter.next();
                Log.d(TAG, "nextUnreadTexts Next text: "+textno);
                TextInfo text = getKomText(textno);
                if (text != null) {
                    ret_data.add(text);
                } else {
                    Log.d(TAG, "nextUnreadTexts could not find textno "+textno);
                }
                Log.d(TAG, "nextUnreadTexts Author: "+text.getAuthor());
                Log.d(TAG, "nextUnreadTexts Date: "+text.getDate());
                Log.d(TAG, "nextUnreadTexts Subject: "+text.getSubject());
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "nextUnreadTexts " + e);
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "nextUnreadTexts " + e);
            //e.printStackTrace();
        }
        return ret_data;
    }

    public List<TextInfo> getMarkedTexts() {
        List<TextInfo> ret_data = new ArrayList<TextInfo>();
        Mark[] data = null;

        if (s == null) {
            return null;
        }
        try {
            data = s.getMarks();
            for(Mark i:data) {
                Integer textno = i.getText();
                Log.d(TAG, "getMarkedTexts Next text: "+textno);
                TextInfo text = getKomText(textno);
                if (text != null) {
                    ret_data.add(text);
                } else {
                    Log.d(TAG, "getMarkedTexts could not find textno "+textno);
                }
                Log.d(TAG, "getMarkedTexts Author: "+text.getAuthor());
                Log.d(TAG, "getMarkedTexts Date: "+text.getDate());
                Log.d(TAG, "getMarkedTexts Subject: "+text.getSubject());
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "getMarkedTexts " + e);
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "getMarkedTexts " + e);
            //e.printStackTrace();
        }
        return ret_data;
    }


    public int getUserId() {
        return re_userid;
    }

    public String getUserPassword() {
        return re_password;
    }

    public String getServer() {
        return re_server;
    }

    public void setUser(int userId, String userPSW, String server) {
        if((userId>0)&&(userPSW!=null)&&(userPSW.length()>0)){
            re_userid=userId;
            re_password=userPSW;
            re_server=server;
            Log.d(TAG, "setting userid:"+userId+" server:"+server);
        }
    }

    public boolean isConnected() {
		if ((s == null) || (!connected)) {
			return false;
		}
		return s.getConnected();
	}

    public void setConnected(boolean val) {
        connected = val;
        if (val) {
            //new ReconnectTask().execute();
        } else {
            if (s != null) {
                try {
                    s.disconnect(true);
                } catch (Exception e) {
                    Log.d(TAG, "setConnected False failed:" + e);
                }
                s = null;
            }
        }
    }
	
	public void error(String s) {
		Log.e("androkom", s);
	}

	public void debug(String s) {
		Log.d("androkom KomServer", s);
	}

	private Session s = null;

	private boolean connected = false;
	
	private int mLastTextNo = 0;
	HashMap<String, String> mElispUserAreaProps = null;
    HashMap<String, String> mCommonUserAreaProps = null;

	// This is the object that receives interactions from clients.
    //private final IBinder mBinder = new LocalBinder();

	private long lastActivate = 0;
	
	public HashSet<Integer> mPendingSentTexts;
	ConfInfo usernames[] = null;
	private int re_userid=0; // for reconnect, note: none of these are saved during screen rotation
	private String re_password=null; // for reconnect
	private String re_server=null; // for reconnect
    private int re_port=0; // for reconnect
	private boolean re_useSSL=true; // for reconnect
    private int re_cert_level=0; // for reconnect

	private String latestIMSender=""; // for creating new IM
	
	private boolean hidden_session = !RELEASE_BUILD;

	public AsyncMessages asyncMessagesHandler;
	public IMLogger imLogger;
}

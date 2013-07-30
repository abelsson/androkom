package org.lindev.androkom;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
	public static boolean RELEASE_BUILD = true;

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
                if (isConnected() && (!activeNetInfo.isConnected())
                        && (mHandler != null)) {
                    Message rmsg = new Message();
                    rmsg.what = Consts.MESSAGE_POPULATE;
                    mHandler.sendMessage(rmsg);
                }
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

        public TextInfo(Context context, int textNo, int confNo, String author, int authorno,
                String date, String all_headers, String visible_headers,
                String subject, String body, byte[] rawBody,
                int ShowHeadersLevel) {
            this.textNo = textNo;
            this.confNo = confNo;
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
                return new TextInfo(context, -1, -1, "", 0, "", "", "", "", context
                        .getString(R.string.all_read), null, 1);
            case ERROR_FETCHING_TEXT:
                Log.d(TAG, "createText ERROR_FETCHING_TEXT");
                return new TextInfo(context, -2, -1, "", 0, "", "", "", "", context
                        .getString(R.string.error_fetching_text), null, 1);
            case NO_PARENT:
                Log.d(TAG, "createText NO_PARENT");
                return new TextInfo(context, -1, -1, "", 0, "", "", "", "", context
                        .getString(R.string.error_no_parent), null, 1);
            default:
                Log.d(TAG, "createText default");
                return new TextInfo(context, -2, -1, "", 0, "", "", "", "", context
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

        public int getConfNo() {
            return confNo;
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
        private int confNo;
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

        try {
            if(slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    if (lks == null) {
                        lks = new Session();
                        lks.addRpcEventListener(this);
                    }
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "onCreate could not lock");
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    @Override
    public void onRebind(Intent arg0) 
    {
        Log.d(TAG, "onRebind");
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

        new LogoutTask().execute();

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

    public void nolock_logout() throws InterruptedException {
        Log.d(TAG, "KomServer logout");
        if (lks != null) {
            try {
                if (lks.getState() == Session.STATE_LOGIN)
                    lks.logout(true);
                Log.i(TAG, "logout: logged out");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "logout1 " + e);
                // e.printStackTrace();
            }

            try {
                Log.d(TAG, "logout: trying to disconnect");
                lks.disconnect(true);
                Log.d(TAG, "logout: disconnected");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "logout2 " + e);
                // e.printStackTrace();
            }

            try {
                lks.removeRpcEventListener(this);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "logout3 " + e);
                // e.printStackTrace();
            }
            Log.i(TAG, "logout disconnected and no listener");
        }
        Log.d(TAG, "logout reached end");
    }

    public void logout() throws InterruptedException {
        Log.d(TAG, "KomServer logout try lock");
        if (slock.tryLock(6, TimeUnit.SECONDS)) {
            try {
                nolock_logout();
            } finally {
                slock.unlock();
                Log.d(TAG, "KomServer logout Unlocked Session");
            }
        } else {
            Log.d(TAG, "logout could not lock");
        }
        Log.d(TAG, "logout reached end");
    }

    /**
     * When no need to wait for reconnect
     * 
     */
    private class ReconnectTask extends AsyncTask<KomToken, Void, Void> {
        protected void onPreExecute() {
            Log.d(TAG, "ReconnectTask.onPreExecute");
        }

        // worker thread (separate from UI thread)
        protected Void doInBackground(final KomToken... args) {
            Log.d(TAG, "ReconnectTask.doInBackground");
            try {
                reconnect();
            } catch (Exception e1) {
                Log.i(TAG, "Failed to reconnect exception:"+e1);
                //e1.printStackTrace();
            }
            Log.d(TAG, "ReconnectTask.doInBackground done");
            return null;
        }
    }

    public void reconnect() {
        Log.d(TAG, "reconnect() Logout old session");
        try {
            logout();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } // new LogoutTask().execute();

        Log.d(TAG, "reconnect() Initialize new session");

        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                Log.d(TAG, "reconnect got lock");
                try {
                    lks = new Session();
                    lks.addRpcEventListener(this);
                } finally {
                    slock.unlock();
                    Log.d(TAG, "reconnect unlocked");
                }
            } else {
                Log.d(TAG, "reconnect failed to get lock");
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "onCreate tryLock interrupted");
            e.printStackTrace();
        }

        if (re_userid > 0) {
            Log.d(TAG, "KomServer trying to login using id " + re_userid
                    + " on server " + re_server);
            try {
                login(re_userid, re_password, re_server, re_port, re_useSSL,
                        re_cert_level);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Can't reconnect because no userid. dumpStack:");
            Thread.dumpStack();
            try {
                logout();
                Log.d(TAG, "reconnect. Forced logout");
            } catch (InterruptedException e) {
                Log.d(TAG, "reconnect tried logout. got InterruptedException:"+e);
                e.printStackTrace();
            } catch (Exception e) {
                Log.d(TAG, "reconnect tried logout. got Exception:"+e);
                e.printStackTrace();
            }
            Log.d(TAG, "reconnect. Failed for now. Be right back.");
            // kill kill kill... Not really recommended but we should not be here anyway.
            //android.os.Process.killProcess(android.os.Process.myPid());
            //System.runFinalizersOnExit(true);
            //System.exit(0);
        }
    }

    /**
     * Connect to LysKOM server.
     * 
     * No need to get lock for calls to Session since lock is already obtained by login.
     * @param port 
     * 
     * @return 0 on success, non-zero on failure.
     */
    private synchronized int connect(String server, int port, boolean useSSL, int cert_level) 
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
            if (lks == null) {
                Log.d(TAG, "connect: no session!");
            }
            try {
                String hostName = MANUFACTURER.replace(' ', '_') + "_"
                        + MODEL.replace(' ', '_');
                lks.setClientHost(hostName);
            } catch (Exception e) {
                Log.i(TAG, "connect Got no terminalmodel");
            }

            try {
                AccountManager accountManager = AccountManager.get(this);
                Account[] accounts = accountManager
                        .getAccountsByType("com.google");
                String userName = accounts[0].name;
                int splitIndex = userName.indexOf("@");
                lks.setClientUser(userName.substring(0, splitIndex));
            } catch (Exception e) {
                Log.i(TAG, "connect Got no account");
            }

            if (lks != null) {
                Log.i(TAG, "connect lks.connect");
                if (lks.connect(
                        server,
                        port,
                        useSSL,
                        cert_level,
                        getBaseContext().getResources().openRawResource(
                                R.raw.root_keystore))) {
                    Log.i(TAG, "connect lks.connect done");
                    if (asyncMessagesHandler != null) {
                        lks.addAsynchMessageReceiver(asyncMessagesHandler);
                    } else {
                        Log.d(TAG, "connect asyncMessagesHandler==null");
                        Thread.dumpStack();
                        // logout();
                        return -2;
                    }
                } else {
                    Log.i(TAG, "connect lks.connect could not connect");
                    return -2;
                }
            } else {
                Log.d(TAG, "connect s==null");
                return -2;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "connect1 " + e);

            // e.printStackTrace();
            return -1;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "connect2 " + e);
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    public synchronized void disconnect() {
        try {
            Log.d(TAG, "mKom.disconnect tryLock");

            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    lks.disconnect(true);
                } catch (RpcFailure e) {
                    Log.d(TAG, "disconnect RpcFailure" + e);
                    // e.printStackTrace();
                } catch (Exception e) {
                    Log.d(TAG, "disconnect " + e);
                    // e.printStackTrace();
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.disconnect unlocked");
                }
            } else {
                Log.d(TAG, "mKom.disconnect could not lock");
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "disconnect tryLock interrupted");
            e.printStackTrace();
        }
    }

    /**
     * Fetch a username from userid
     * 
     * @param userid
     * @throws InterruptedException 
     */
    public String fetchUsername(int persNo) throws InterruptedException {
        String username = null;
        if (persNo > 0) {
            Log.d(TAG, "mKom.fetchUsername tryLock");
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                Log.d(TAG, "mKom.fetchUsername got Lock");
                try {
                    nu.dll.lyskom.Conference confStat = lks.getConfStat(persNo);
                    username = confStat.getNameString();
                } catch (Exception e) {
                    Log.d(TAG,
                            "fetchUsername caught exception from getConfStat"
                                    + e);
                    // e.printStackTrace();
                    username = getString(R.string.person) + persNo
                            + getString(R.string.does_not_exist);
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.fetchUsername unlocked");
                }
            } else {
                Log.d(TAG, "mKom.fetchUsername could not lock session");
            }
        } else {
            Log.d(TAG, "fetchUsername persNo=" + persNo);
            username = getString(R.string.anonymous);
        }
        return username;
    }
    
    /**
     * Fetch a list of persons online
     * @param mHandler 
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    public List<ConferenceInfo> fetchPersons(Handler mHandler, int who_type) throws IOException,
            InterruptedException {
        if (lks == null) {
            return null;
        }

        Set<Integer> friendsList = new HashSet<Integer>();

        ArrayList<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();
        if (who_type == 2) {
            friendsList = getFriends();
        }

        Log.d(TAG, "fetchPersons trying to lock");
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            Log.d(TAG, "fetchPersons got lock");
            try {
                DynamicSessionInfo[] persons = lks.whoIsOnDynamic(true, false,
                        30 * 60);

                for (int i = 0; i < persons.length; i++) {
                    int persNo = persons[i].getPerson();
                    if ((who_type == 1) || (friendsList.contains(persNo))) {
                        String username;
                        if (persNo > 0) {
                            try {
                                nu.dll.lyskom.Conference confStat = lks
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
                        Log.i(TAG, "fetchPersons (i=" + i + ") " + username
                                + " <" + persNo + ">");

                        ConferenceInfo info = new ConferenceInfo();
                        info.id = persNo;
                        info.name = username;
                        info.sessionNo = persons[i].session;

                        arr.add(info);
                        
                        Message msg = new Message();
                        msg.what = Consts.MESSAGE_PROGRESS;
                        msg.arg1 = (int)(((float)i)/((float)persons.length)*100.0);
                        mHandler.sendMessage(msg);

                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "fetchPersons caught an exception:" + e);
            } finally {
                Log.d(TAG, "fetchPersons finally unlock lock");
                slock.unlock();
            }
        } else {
            Log.d(TAG, "fetchPersons tryLock failed to get lock");
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
     * @param msgHandler 
     * @throws IOException 
     * @throws RpcFailure 
     * @throws UnsupportedEncodingException 
     * @throws InterruptedException 
     */
    public List<ConferenceInfo> fetchConferences(Handler msgHandler)
            throws UnsupportedEncodingException, RpcFailure, IOException,
            InterruptedException {
        List<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();
        Log.d(TAG, "mKom.fetchConferences tryLock");
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            Log.d(TAG, "mKom.fetchConferences got Lock");
            try {
                List<Integer> confList = lks.getMyUnreadConfsList(true);
                int arrIndex;
                for (int conf : confList) {
                    arrIndex = arr.size();
                    final ConferenceInfo info = new ConferenceInfo();
                    final String name = lks.toString(lks.getConfName(conf));
                    info.id = conf;
                    info.name = name;
                    info.numUnread = 0;
                    arr.add(info);
                    Message msg = new Message();
                    msg.what = Consts.MESSAGE_UPD_CONF;
                    msg.obj = new ArrayList<ConferenceInfo>(arr);
                    msgHandler.sendMessage(msg);

                    Log.i(TAG, name + " <" + conf + ">");
                    info.numUnread = lks.getUnreadCount(conf);
                    arr.set(arrIndex, info);
                    msg = new Message();
                    msg.what = Consts.MESSAGE_UPD_CONF;
                    msg.obj = new ArrayList<ConferenceInfo>(arr);
                    msgHandler.sendMessage(msg);
                }
            } finally {
                slock.unlock();
                Log.d(TAG, "mKom.fetchConferences unlocked");
            }
        } else {
            Log.d(TAG, "mKom.fetchConferences failed to lock");
        }
        return arr;
    }

    /**
     * Return name for given conference.
     * @throws InterruptedException 
     */
    public String getConferenceName(int conf) throws InterruptedException {
        Log.d(TAG, "mKom.getConferenceName id=" + conf);
        String confName = "";
        Log.d(TAG, "mKom.getConferenceName tryLock");
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            Log.d(TAG, "mKom.getConferenceName got Lock");
            try {
                confName = lks.toString(lks.getConfName(conf));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "getConferenceName " + e);

                // e.printStackTrace();
            } finally {
                slock.unlock();
                Log.d(TAG, "mKom.getConferenceName unlocked");
            }
            Log.d(TAG, "mKom.getConferenceName got " + confName);
        } else {
            Log.d(TAG, "mKom.getConferenceName failed to lock ");
        }
        return confName;
    }

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
        int confPres = 0;

        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    int confNo = lks.getCurrentConference();
                    if (confNo > 0) {
                        try {
                            confPres = lks.getConfStat(confNo)
                                    .getPresentation();
                        } catch (RpcFailure e) {
                            Log.d(TAG, "getConferencePres RpcFailure" + e);
                            // e.printStackTrace();
                        } catch (IOException e) {
                            Log.d(TAG, "getConferencePres IOException" + e);
                            // e.printStackTrace();
                        }
                    }
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "getConferencePres tryLock could not lock");
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "getConferencePres tryLock interrupted");
            e.printStackTrace();
        }
        return confPres;
    }

    /**
     * Return presentation text number for userid.
     * 
     * @throws InterruptedException
     */
    public int getUserPres(int userid) throws InterruptedException {
        int confNo = userid;
        int userPres = 0;

        if (confNo > 0) {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    userPres = lks.getConfStat(confNo).getPresentation();
                } catch (RpcFailure e) {
                    Log.d(TAG, "getConferencePres RpcFailure" + e);
                    // e.printStackTrace();
                } catch (IOException e) {
                    Log.d(TAG, "getConferencePres IOException" + e);
                    // e.printStackTrace();
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "getConferencePres could not lock");
            }
        }
        return userPres;
    }

    /**
     * Return number of unreads for current conference.
     * @throws InterruptedException 
     */
    public int getConferenceUnreadsNo() throws InterruptedException {
        int unreads = 0;

        Log.d(TAG, "getConferenceUnreadsNo");
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                if (lks != null) {
                    int confNo = lks.getCurrentConference();
                    if (confNo > 0) {
                        unreads = lks.getUnreadCount(confNo);
                        Log.d(TAG, "getConferenceUnreadsNo num unreads="+unreads);
                    } else {
                        Log.d(TAG, "getConferenceUnreadsNo no current conference");
                    }
                } else {
                    Log.d(TAG, "getConferenceUnreadsNo no session");
                }
            } catch (RpcFailure e) {
                Log.d(TAG, "getConferenceUnreadsNo RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "getConferenceUnreadsNo IOException:" + e);
                // e.printStackTrace();
            } catch (NullPointerException e) {
                Log.d(TAG, "getConferenceUnreadsNo NullPointerException:" + e);
                e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "getConferenceUnreadsNo failed to get lock");
        }
        Log.d(TAG, "getConferenceUnreadsNo done");
        return unreads;
    }

    /**
     * Set currently active conference.
     * 
     * @throws IOException
     * @throws RpcFailure
     * @throws InterruptedException 
     */
    public void setConference(final int confNo) throws RpcFailure, IOException, InterruptedException {
        Log.d(TAG, "mKom.setConference tryLock");

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                if (lks != null) {
                    Log.d(TAG, "setConference Byt till conf:" + confNo);
                    try {
                        lks.changeConference(confNo);
                        readMarker.clear();
                        //textFetcher.restartPrefetcher();
                    } catch (java.lang.NullPointerException e) {
                        Log.d(TAG,
                                "setConference Handled an NullpointerException");
                        Thread.dumpStack();
                    }
                } else {
                    Log.d(TAG, "setConference Ingen session");
                }
            } finally {
                slock.unlock();
                Log.d(TAG, "mKom.setConference unlocked");
            }
        } else {
            Log.d(TAG, "mKom.setConference could not lock");
        }
    }

    public void startPrefetcher() {
        textFetcher.startPrefetcher();
    }

    public void restartPrefetcher() {
        textFetcher.restartPrefetcher();
    }

    /*
     * Send message about user active to server. Will not send more than once
     * every 30 sek to keep from flooding server.
     */
    public void activateUser() throws InterruptedException, IOException, Exception {
        long long_now = System.currentTimeMillis();
        Log.d(TAG, "mKom.activateUser tryLock");

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            Log.d(TAG, "mKom.activateUser got lock");
            try {
                if (lks == null) {
                    throw new Exception("Not logged in");
                }
                if ((long_now - lastActivate) > (30 * 1000)) {
                    lks.doUserActive();
                    lastActivate = long_now;
                }
            } finally {
                slock.unlock();
                Log.d(TAG, "mKom.activateUser unlocked");
            }
        } else {
            Log.d(TAG, "mKom.activateUser could not get lock");
        }
    }

    /**
     * Log in to server.
     * 
     * @return Empty string on success, string describing failure otherwise
     * @throws InterruptedException 
     * @throws RpcFailure 
     */
    public String login(String username, String password, String server,
            int port, boolean useSSL, int cert_level) throws RpcFailure, InterruptedException {
        Log.d(TAG, "Trying to login username:" + username);
        Log.d(TAG, "Trying to login server:" + server);
        Log.d(TAG, "Trying to login port:" + port);
        Log.d(TAG, "Trying to login usessl:" + useSSL);
        Log.d(TAG, "Trying to login cert_level:" + cert_level);

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                Log.d(TAG, "login A Got lock");

                if (lks == null) {
                    Log.d(TAG, "login creating new session");
                    lks = new Session();
                    lks.addRpcEventListener(this);
                }
                Log.d(TAG, "LOGIN 1");

                if (!lks.getConnected()) {
                    Log.d(TAG, "LOGIN not connected, trying to reconnect");
                    if (connect(server, port, useSSL, cert_level) != 0) {
                        Log.d(TAG, "LOGIN failed to reconnect");
                        if (lks != null) {
                            lks.disconnect(true);
                            Log.d(TAG, "LOGIN lks disconnected");
                        }
                        lks = null;
                        return getString(R.string.error_could_not_connect);
                    }
                }
                Log.d(TAG, "LOGIN 2");
                usernames = new ConfInfo[0];
                usernames = lks.lookupName(username, true, false);
                Log.d(TAG, "LOGIN 3");
                if (usernames.length != 1) {
                    Log.d(TAG, "LOGIN 4");
                    return getString(R.string.error_ambigious_name);
                } else {
                    Log.d(TAG, "LOGIN 5");
                    // login as hidden
                    if (!lks.login(usernames[0].confNo, password,
                            hidden_session, false)) {
                        return getString(R.string.error_invalid_password);
                    }
                }
                Log.d(TAG, "LOGIN 6");

                lks.setClientVersion("Androkom", getVersionName());
                lks.setLatteName("AndroKOM " + getVersionName());

                Log.d(TAG, "LOGIN 7");

                re_userid = usernames[0].confNo;
                re_password = password;
                re_server = server;
                re_port = port;
                re_useSSL = useSSL;
                re_cert_level = cert_level;

                parseCommonUserArea();
                parseElispUserArea();

                Log.d(TAG, "LOGIN 8");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "LOGIN 9");
                e.printStackTrace();
            } finally {
                Log.d(TAG, "LOGIN 10");
                slock.unlock();
                Log.d(TAG, "LOGIN A unlocked");
            }
        } else {
            Log.d(TAG, "LOGIN could not lock");
        }
        return "";
    }
    
    /**
     * Log in to server.
     * 
     * @param port
     * 
     * @return Empty string on success, string describing failure otherwise
     * @throws InterruptedException 
     */
    public String login(int userid, String password, String server, int port,
            boolean useSSL, int cert_level) throws InterruptedException {
        Log.d(TAG, "login Trying to login userid:" + userid);
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            Log.d(TAG, "login B got lock");
            try {
                if (lks == null) {
                    Log.d(TAG, "login creating new session");

                    lks = new Session();
                    lks.addRpcEventListener(this);
                }
                usernames = new ConfInfo[0];
                try {
                    Log.d(TAG, "login checking connection");
                    if (!lks.getConnected()) {
                        Log.d(TAG, "login not connected");
                        if (connect(server, port, useSSL, cert_level) != 0) {
                            Log.d(TAG, "login Failed to connect");                            
                            try {
                                if (lks != null) {
                                    lks.disconnect(true);
                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                // e.printStackTrace();
                            }
                            lks = null;
                            return getString(R.string.error_could_not_connect);
                        } else {
                            Log.d(TAG, "login Succeded to connect");
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "login Failed to connect");
                    lks = null;
                    return getString(R.string.error_could_not_connect);
                }

                try {
                    Log.d(TAG, "login Logging in");
                    // login as hidden
                    if (!lks.login(userid, password, hidden_session, false)) {
                        return getString(R.string.error_invalid_password);
                    }
                    Log.d(TAG, "login Setting ClientVersion");
                    lks.setClientVersion("Androkom", getVersionName());
                    lks.setLatteName("AndroKOM " + getVersionName());
                    Log.d(TAG, "login done Setting ClientVersion");
                } catch (nu.dll.lyskom.RpcFailure e) {
                    Log.e(TAG, "Login.id Caught "
                            + e.getClass().getName());
                    Log.e(TAG, "Login.id Caught "
                            + e + "\n" + e.getStackTrace());
                    int errorCode = e.getError();
                    Log.e(TAG, "login.id got error from server: "+decodeKomErrorCode(errorCode));
                    nolock_logout();
                    lks = null;
                    // probably just a bad client programmer, trying to ignore
                } catch (Exception e) {
                    Log.e(TAG, "Login.id Caught "
                            + e.getClass().getName());
                    // Log.e("androkom", "Login.id Caught " +
                    // e.getClass().getName()+e.getStackTrace());
                    nolock_logout();
                    lks = null;
                    return "Unknown error: Exit client and restart";
                }
                Log.d(TAG, "login finished logging in");
                re_userid = userid;
                re_password = password;
                re_server = server;
                re_port = port;
                re_useSSL = useSSL;
                re_cert_level = cert_level;
            } finally {
                slock.unlock();
                Log.d(TAG, "login B unlocked");
            }
        } else {
            Log.d(TAG, "login could not lock");
        }
        Log.d(TAG, "login done");
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

    public int getVersionCode() {
        try {
            PackageInfo pinfo = getBaseContext().getPackageManager().getPackageInfo("org.lindev.androkom", 0);
            return pinfo.versionCode;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
        return -1;
      }
    }


    public void endast(int confNo, int no) {
        try {
            slock.tryLock(60, TimeUnit.SECONDS);
            try {
                lks.endast(confNo, no);
            } catch (RpcFailure e) {
                Log.d(TAG, "endast RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "endast IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "onCreate tryLock interrupted");
            e.printStackTrace();
        }
    }

    public void joinconference(int confNo) throws InterruptedException {
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                lks.joinConference(confNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "joinconference RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "joinconference IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "joinconference could not lock");
        }
    }

    public void leaveConference(int confNo) throws InterruptedException {
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                lks.leaveConference(confNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "leaveConference RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "leaveConference IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "leaveConference could not lock");
        }
    }

    public static String decodeKomErrorCode(int code) {
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

    public String subRecipient(int textNo, int confNo) throws InterruptedException {
        String result = "";

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                Log.d(TAG, "Remove confNo:" + confNo + " from textNo:" + textNo);
                lks.subRecipient(textNo, confNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "subRecipient RpcFailure:" + e);
                // e.printStackTrace();
                Log.d(TAG, "Error: " + e.getError());
                Log.d(TAG, "ErrorStatus: " + e.getErrorStatus());
                Log.d(TAG, "Message: " + e.getMessage());

                result = decodeKomErrorCode(e.getError());
            } catch (IOException e) {
                Log.d(TAG, "subRecipient IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "subRecipient could not lock");
        }
        return result;
    }

    /*
     * Add an aux item of type auxtype to text number textno with content
     * content. Note: textno must be pointing on an text not a
     * conference/person.
     */
    public void addAuxItem(int textno, int auxType, String content) throws InterruptedException {
        Log.d(TAG, "addAuxItem textno" + textno);
        Log.d(TAG, "addAuxItem axutype" + auxType);
        Log.d(TAG, "addAuxItem content" + content);

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                AuxItem auxItem = new AuxItem(auxType, content);
                AuxItem[] add_items = { auxItem };
                int[] del_items = {};

                lks.modifyAuxInfo(false, textno, del_items, add_items);
            } catch (Exception e) {
                Log.d(TAG, "Failed to add aux item. Exception:" + e);
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "addAuxItem failed to lock");
        }
    }

    public String subComment(int textNo, int commentNo) throws InterruptedException {
        String result = "";

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                Log.d(TAG, "Remove textNo:" + textNo + " from commentNo:"
                        + commentNo);
                lks.subComment(textNo, commentNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "subRecipient RpcFailure:" + e);
                // e.printStackTrace();
                Log.d(TAG, "Error: " + e.getError());
                Log.d(TAG, "ErrorStatus: " + e.getErrorStatus());
                Log.d(TAG, "Message: " + e.getMessage());

                result = decodeKomErrorCode(e.getError());
            } catch (IOException e) {
                Log.d(TAG, "subRecipient IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "subRecipient could not lock");
        }
        return result;
    }

    /**
     * Remove text from cache and reread from server 
     */
    public void updateText(final int textNo) {
        textFetcher.restartPrefetcher();
        //getKomText(textNo);
        try {
            getTextStat(textNo, true);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "updateText InterruptedExcpetion");
            e.printStackTrace();
        }
    }

    public void interruptPrefetcher() {
        textFetcher.interruptPrefetcher();
    }

    /**
     * Fetch next unread text, as a HTML formatted string. 
     */
    private final TextFetcher textFetcher = new TextFetcher(this);
    public TextInfo getKomText(final int textNo) {
        return textFetcher.getKomText(textNo);
    }

    public int getNextUnreadTextNo() {
        Log.d(TAG, "getNextUnreadTextNo");
        return textFetcher.getNextUnreadTextNo();
    }

    public TextInfo getNextUnreadText(final boolean peekQueue) {
        return textFetcher.getNextUnreadText(peekQueue);
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

    public void markText(int textNo) throws InterruptedException {
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                lks.markText(textNo, 100);
            } catch (RpcFailure e) {
                Log.d(TAG, "markText RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "markText IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "markText could not lock");
        }
    }

    public void unmarkText(int textNo) throws InterruptedException {
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                lks.unmarkText(textNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "unmarkText RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "unmarkText IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "unmarkText could not lock");
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

    private String[] getNextHollerith(String s) {
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
    public void parseCommonUserArea() {
        try {
            UserArea ua = null;

            try {
                Log.d(TAG, "parseCommonUserArea tryLock");
                if (slock.tryLock(60, TimeUnit.SECONDS)) {
                    try {
                        ua = lks.getUserArea();
                    } finally {
                        slock.unlock();
                        Log.d(TAG, "parseCommonUserArea unlocked");
                    }
                } else {
                    Log.d(TAG, "parseCommonUserArea failed to lock");
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "onCreate tryLock interrupted");
                e.printStackTrace();
            }
            if (ua != null) {
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
            }
        } catch (Exception e) {
            Log.d(TAG, "parseCommonUserArea " + e);
            // e.printStackTrace();
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
    public void parseElispUserArea() {
        try {
            UserArea ua = null;

            try {
                Log.d(TAG, "parseElispUserArea");
                if (slock.tryLock(60, TimeUnit.SECONDS)) {
                    try {
                        ua = lks.getUserArea();
                    } finally {
                        slock.unlock();
                        Log.d(TAG, "parseElispUserArea unlocked");
                    }
                } else {
                    Log.d(TAG, "parseElispUserArea could not lock");
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "onCreate tryLock interrupted");
                e.printStackTrace();
            }
            if (ua != null) {
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
            }
        } catch (Exception e) {
            Log.d(TAG, "parseElispUserArea " + e);
            // e.printStackTrace();
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

    public String addNewRecipientToText(int textNo, int confNo, int texttype) throws InterruptedException {
        String result = "";

        Log.d(TAG, "Add new recipient (null method)");
        Log.d(TAG, "-- textNo:" + textNo);
        Log.d(TAG, "-- confNo:" + confNo);
        Log.d(TAG, "-- texttype:" + texttype);
        
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                lks.addRecipient(textNo, confNo, texttype);
            } catch (RpcFailure e) {
                Log.d(TAG, "addNewRecipientToText " + e);
                // e.printStackTrace();
                result = decodeKomErrorCode(e.getError());
            } catch (IOException e) {
                Log.d(TAG, "addNewRecipientToText " + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "could not lock");
        }
        return result;
    }
    
    public String addNewCommentToText(int textNo, int commentNo) throws InterruptedException {
        String result = "";

        Log.d(TAG, "Add new comment (null method)");
        Log.d(TAG, "-- textNo:" + textNo);
        Log.d(TAG, "-- commentNo:" + commentNo);

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                lks.addComment(textNo, commentNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "addNewCommentToText " + e);
                // e.printStackTrace();
                result = decodeKomErrorCode(e.getError());
            } catch (IOException e) {
                Log.d(TAG, "addNewCommentToText " + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "addNewCommentToText could not lock");
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
            throws IOException, RpcFailure, InterruptedException {
        boolean res = false;

        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                res = lks.sendMessage(recipient, message, block);
                imLogger.sendMessage(recipient, message);
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "sendMessage could not lock");
        }
        return res;
    }

	public void setShowHeadersLevel(final int h) {
		textFetcher.setShowHeadersLevel(h);
	}

    public ConferenceInfo[] getUserNames() {
        if (lks != null) {
            try {
                if (usernames != null && usernames.length > 1) {
                    final ConferenceInfo[] items = new ConferenceInfo[usernames.length];
                    Log.d(TAG, "Ambigous name");
                    try {
                        Log.d(TAG, "getUserNames try lock");
                        if (slock.tryLock(60, TimeUnit.SECONDS)) {
                            Log.d(TAG, "getUserNames got lock");
                            try {
                                for (int i = 0; i < usernames.length; i++) {
                                    items[i] = new ConferenceInfo();
                                    items[i].name = lks.toString(lks
                                            .getConfName(usernames[i].confNo));
                                    items[i].id = usernames[i].confNo;
                                    Log.d(TAG, "Name " + i + ":" + items[i]);
                                }
                            } finally {
                                slock.unlock();
                                Log.d(TAG, "getUserNames unlock");
                            }
                        } else {
                            Log.d(TAG, "getUserNames could not lock");
                        }
                    } catch (InterruptedException e) {
                        Log.d(TAG, "onCreate tryLock interrupted");
                        e.printStackTrace();
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

    public Date getServerTime() throws InterruptedException {
        Date tiden = null;
        Log.d(TAG, "getServerTime");
        if (slock.tryLock(6, TimeUnit.SECONDS)) {
            Log.d(TAG, "getServerTime got lock");
            try {
                if (lks != null) {
                    tiden = lks.getTime().getTime();
                }
            } catch (IOException e) {
                Log.d(TAG, "getServerTime failed to get time, see trace:");
                e.printStackTrace();
            } finally {
                slock.unlock();
                Log.d(TAG, "getServerTime unlocked");
            }
        } else {
            Log.d(TAG, "getServerTime failed to get lock");
        }
        Log.d(TAG, "getServerTime done");
        return tiden;
    }

    public Person getPersonStat(Integer arg0) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        Person pers = null;
        Log.d(TAG, "getPersonStat id=" + arg0);
        Log.d(TAG, "getPersonStat try Lock "
                + (System.currentTimeMillis() - startTime));
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            Log.d(TAG, "getPersonStat got Lock "
                    + (System.currentTimeMillis() - startTime));
            try {
                pers = lks.getPersonStat(arg0);
                Log.d(TAG,
                        "getPersonStat got Stat "
                                + (System.currentTimeMillis() - startTime));
            } catch (RpcFailure e) {
                Log.d(TAG,
                        "getUserNames " + e + " "
                                + (System.currentTimeMillis() - startTime));
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG,
                        "getUserNames " + e + " "
                                + (System.currentTimeMillis() - startTime));
                // e.printStackTrace();
            } finally {
                slock.unlock();
                Log.d(TAG,
                        "getPersonStat unlock Lock "
                                + (System.currentTimeMillis() - startTime));
            }
        } else {
            Log.d(TAG,
                    "getPersonStat failed to lock "
                            + (System.currentTimeMillis() - startTime));
        }
        Log.d(TAG, "getPersonStat returning"
                + (System.currentTimeMillis() - startTime));
        return pers;
    }

    public String getClientName(int sessionNo) throws InterruptedException {
        String clientName = "";
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                byte[] clientBytes = lks.getClientName(sessionNo);
                clientName = lks.toString(clientBytes);
            } catch (RpcFailure e) {
                Log.d(TAG, "getClientName " + e);
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "getClientName " + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "getClientName could not lock");
        }
        return clientName;
    }

    public String getClientVersion(int sessionNo) throws InterruptedException {
        String clientName = "";
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                byte[] clientBytes = lks.getClientVersion(sessionNo);
                clientName = lks.toString(clientBytes);
            } catch (RpcFailure e) {
                Log.d(TAG, "getClientVersion " + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "getClientVersion " + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "getClientVersion could not lock");
        }
        return clientName;
    }

    public String getConnectionTime(int sessionNo) throws InterruptedException {
        KomTime ctime = null;
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                ctime = lks.getStaticSessionInfo(sessionNo).getConnectionTime();
            } catch (RpcFailure e) {
                Log.d(TAG, "getConnectionTime " + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "getConnectionTime " + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "getConnectionTime could not lock");
        }
        if (ctime != null) {
            Date CreationTime = ctime.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm]");
            return sdf.format(CreationTime);
        } else
            return "";
    }

    public Text getTextbyNo(int textNo) throws InterruptedException {
        Text text = null;
        if (lks == null) {
            return null;
        }
        Log.d(TAG, "getTextbyNo Trying to get text#: " + textNo);
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                text = lks.getText(textNo);
            } catch (RpcFailure e) {
                if (e.getError() == 14) {
                    Log.d(TAG,
                            "komserver.getTextbyNo No such text#:"
                                    + e.getErrorStatus());
                    throw (e);
                } else {
                    Log.d(TAG, "komserver.getTextbyNo new_text RpcFailure:" + e);
                }
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "komserver.getTextbyNo new_text IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "getTextbyNo could not lock");
        }

        if (text == null) {
            Log.d(TAG, "getTextbyNo could not get a text for " + textNo);
        } else {
            Log.d(TAG, "getTextbyNo returning a text for " + textNo);
        }
        return text;
    }
    
    public ConfInfo[] lookupName(String name, boolean wantPersons,
            boolean wantConfs) throws InterruptedException {
        ConfInfo[] text = null;
        if (lks == null) {
            return null;
        }

        Log.d(TAG, "komserver.lookupName tryLock");
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                text = lks.lookupName(name, wantPersons, wantConfs);
            } catch (RpcFailure e) {
                Log.d(TAG, "komserver.lookupName new_text RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "komserver.lookupName new_text IOException:" + e);
                // e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.d(TAG, "Ran out of index, trying to recover. name=" + name
                        + " wantPersons=" + wantPersons + " wantConfs="
                        + wantConfs);
                e.printStackTrace();
            } finally {
                slock.unlock();
                Log.d(TAG, "komserver.lookupName unlocked");
            }
        } else {
            Log.d(TAG, "komserver.lookupName could not lock");
        }
        return text;
    }

    public TextStat getTextStat(int textNo, boolean refreshCache) throws InterruptedException {
        TextStat text = null;
        if (lks == null) {
            return null;
        }
        Log.d(TAG, "getTextStat tryLock");
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            Log.d(TAG, "getTextStat got Lock");
            try {
                text = lks.getTextStat(textNo, refreshCache);
            } catch (RpcFailure e) {
                Log.d(TAG,
                        "komserver.getTextStat trying to recover from RpcFailure:"
                                + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG,
                        "komserver.getTextStat trying to recover from IOException:"
                                + e);
                // e.printStackTrace();
            } catch (ClassCastException e) {
                Log.d(TAG,
                        "komserver.getTextStat trying to recover from ClassCastException:"
                                + e);
                // } catch (Exception e) {
                // Log.d(TAG,
                // "komserver.getTextStat trying to recover from Exception:"
                // + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
                Log.d(TAG, "getTextStat unlocked");
            }
        } else {
            Log.d(TAG, "getTextStat failed Lock");
        }
        return text;
    }
    
    public void markAsRead(int confNo, int[] localTextNo) throws InterruptedException {
        if (lks == null) {
            Log.d(TAG, "komserver.markAsRead no session");
            return;
        }
        if (slock.tryLock(60, TimeUnit.SECONDS)) {
            try {
                lks.markAsRead(confNo, localTextNo);
            } catch (RpcFailure e) {
                Log.d(TAG, "komserver.markAsRead new_text RpcFailure:" + e);
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "komserver.markAsRead new_text IOException:" + e);
                // e.printStackTrace();
            } finally {
                slock.unlock();
            }
        } else {
            Log.d(TAG, "komserver.markAsRead could not lock");
        }
    }
 
    public nu.dll.lyskom.Conference getConfStat(int confNo)
            throws InterruptedException {
        nu.dll.lyskom.Conference text = null;
        if (lks == null) {
            return null;
        }
        try {
            //Log.d(TAG, "mKom.getConfStat tryLock");
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                //Log.d(TAG, "mKom.getConfStat got Lock");
                try {
                    text = lks.getConfStat(confNo);
                } finally {
                    slock.unlock();
                    //Log.d(TAG, "mKom.getConfStat unlocked");
                }
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getConfStat new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.getConfStat new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public byte[] getConfName(int confNo) throws InterruptedException {
        Log.d(TAG, "byte [] mKom.getConfName id=" + confNo);
        byte[] text = null;
        if (lks == null) {
            return null;
        }
        try {
            Log.d(TAG, "mKom.getConfName tryLock");
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                Log.d(TAG, "mKom.getConfName got Lock");
                try {
                    text = lks.getConfName(confNo);
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.getConfStat unlocked");
                }
            } else {
                Log.d(TAG, "komserver.getConfName could not lock");
            }
            Log.d(TAG, "komserver.getConfName got name:" + text);
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
        if ((lks == null)||(buf==null)) {
            return null;
        }
        try {
            text = lks.toString(buf);
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.toString new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.toString new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public boolean isMemberOf(int confNo) throws InterruptedException {
        boolean text = false;
        if (lks == null) {
            return false;
        }
        try {
            Log.d(TAG, "mKom.isMemberOf tryLock");
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.isMemberOf(confNo);
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.isMemberOf unlocked");
                }
            } else {
                Log.d(TAG, "mKom.isMemberOf could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.isMemberOf new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.isMemberOf new_text IOException:" + e);
            // e.printStackTrace();
        } catch (java.lang.NullPointerException e) {
            Log.d(TAG, "komserver.isMemberOf new_text NullPointerException:"
                    + e);
        }
        return text;
    }

    public List<Integer> getUnreadConfsListCached() throws InterruptedException {
        List<Integer> text = null;
        if (lks == null) {
            return null;
        }
        try {
            Log.d(TAG, "mKom.getUnreadConfsListCached tryLock");
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.getUnreadConfsListCached();
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.getUnreadConfsListCached unlocked");
                }
            } else {
                Log.d(TAG, "mKom.getUnreadConfsListCached could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG,
                    "komserver.getUnreadConfsListCached new_text RpcFailure:"
                            + e);
            // e.printStackTrace();
        }
        return text;
    }

    public Membership queryReadTexts(int persNo, int confNo, boolean refresh) throws InterruptedException {
        Membership text = null;
        if (lks == null) {
            return null;
        }
        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.queryReadTexts(persNo, confNo, refresh);
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "komserver.queryReadTexts could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.queryReadTexts new_text RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.queryReadTexts new_text IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public UConference getUConfStat(int confNo) throws InterruptedException {
        UConference text = null;
        if (lks == null) {
            return null;
        }
        try {
            Log.d(TAG, "mKom.getUConfStat tryLock");

            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.getUConfStat(confNo);
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.getUConfStat unlocked");
                }
            } else {
                Log.d(TAG, "mKom.getUConfStat could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getUConfStat RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.getUConfStat IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public TextMapping localToGlobal(int confNo, int firstLocalNo,
            int noOfExistingTexts) throws InterruptedException {
        TextMapping text = null;
        if (lks == null) {
            return null;
        }
        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.localToGlobal(confNo, firstLocalNo,
                            noOfExistingTexts);
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "komserver.localToGlobal could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.localToGlobal RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.localToGlobal IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public TextMapping localToGlobalReverse(int confNo, int firstLocalNo,
            int noOfExistingTexts) throws InterruptedException {
        TextMapping text = null;
        if (lks == null) {
            return null;
        }
        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.localToGlobalReverse(confNo, firstLocalNo,
                            noOfExistingTexts);
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "komserver.localToGlobalReverse could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.localToGlobalReverse RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.localToGlobalReverse IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public TextMapping mapCreatedTextsReverse(int confNo, int firstLocalNo,
            int noOfExistingTexts) throws InterruptedException {
        TextMapping text = null;
        if (lks == null) {
            return null;
        }
        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.mapCreatedTextsReverse(confNo, firstLocalNo,
                            noOfExistingTexts);
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "komserver.mapCreatedTextsReverse could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.mapCreatedTextsReverse RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.mapCreatedTextsReverse IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public List<TextInfo> populateSeeAgain(final SeeAgainTextList seeAgainTextList, int confNo, int numTexts, boolean douser) {
        List<TextInfo> ret_data = new ArrayList<TextInfo>();
        TextMapping data  = null;

        try {
            try {
                if (douser) {
                    data = mapCreatedTextsReverse(confNo, 0, numTexts);
                } else {
                    data = localToGlobalReverse(confNo, 0, numTexts);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (data != null) {
                Log.d(TAG,
                        "populateSeeAgain found number of texts: "
                                + data.size());
                        seeAgainTextList.setPBMax(data.size());
            } else {
                Log.d(TAG, "populateSeeAgain found null: ");
            }
            int counter = 0;
            while (data != null && data.hasMoreElements()) {
                final int globalNo = (Integer) data.nextElement();
                final int localNo = data.local();

                Log.d(TAG, "populateSeeAgain Next text: "+globalNo);
                TextInfo text = getKomText(globalNo);
                if (text != null) {
                    ret_data.add(text);
                    Log.d(TAG, "populateSeeAgain Author: "+text.getAuthor());
                    Log.d(TAG, "populateSeeAgain Date: "+text.getDate());
                    Log.d(TAG, "populateSeeAgain Subject: "+text.getSubject());
                } else {
                    Log.d(TAG, "populateSeeAgain could not find textno "+globalNo);
                }
                counter++;
                seeAgainTextList.setPBprogress(counter);
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "populateSeeAgain " + e);
            //e.printStackTrace();
        }
        return ret_data;
   }
    
    public int createText(Text t, boolean autoreadmarkowntext)
            throws IOException, InterruptedException {
        int text = 0;
        if (lks == null) {
            return 0;
        }
        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    text = lks.createText(t);
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "createText did not lock 1");
            }
            if (text == 0) {
                Log.d(TAG, "createText did not get a textnumber");
            } else {
                if (autoreadmarkowntext) {
                    if (slock.tryLock(60, TimeUnit.SECONDS)) {
                        try {
                            lks.markAsRead(text);
                        } finally {
                            slock.unlock();
                        }
                    } else {
                        Log.d(TAG, "createText could not lock 2");
                    }
                }
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.createText RpcFailure:" + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "komserver.createText IOException:" + e);
            // e.printStackTrace();
        }
        return text;
    }

    public int getCurrentConference() throws InterruptedException {
        int confNo = 0;
        try {
            Log.d(TAG, "mKom.getCurrentConference tryLock");

            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                if (lks == null) {
                    Log.d(TAG, "mKom.getCurrentConference No Session");
                    return 0;
                }
                try {
                    confNo = lks.getCurrentConference();
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.getCurrentConference unlocked");
                }
            } else {
                Log.d(TAG, "mKom.getCurrentConference could not lock");
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "komserver.getCurrentConference new_text RpcFailure:"
                    + e);
            // e.printStackTrace();
        }
        return confNo;
    }

    public List<TextInfo> nextUnreadTexts(int ConfNo) throws InterruptedException {
        List<TextInfo> ret_data = new ArrayList<TextInfo>();
        List<Integer> data = null;

        if (lks == null) {
            return null;
        }
        try {
            Log.d(TAG, "mKom.nextUnreadTexts tryLock");

            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    data = lks.nextUnreadTexts(ConfNo, false, 20);
                } finally {
                    slock.unlock();
                    Log.d(TAG, "mKom.nextUnreadTexts unlocked");
                }
            } else {
                Log.d(TAG, "mKom.nextUnreadTexts could not lock");
            }
            if(data == null) {
                return null;
            }
            Iterator<Integer> iter = data.iterator();
            while (iter.hasNext()) {
                Integer textno = iter.next();
                Log.d(TAG, "nextUnreadTexts Next text: " + textno);
                TextInfo text = getKomText(textno);
                if (text != null) {
                    ret_data.add(text);
                } else {
                    Log.d(TAG, "nextUnreadTexts could not find textno "
                            + textno);
                }
                Log.d(TAG, "nextUnreadTexts Author: " + text.getAuthor());
                Log.d(TAG, "nextUnreadTexts Date: " + text.getDate());
                Log.d(TAG, "nextUnreadTexts Subject: " + text.getSubject());
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "nextUnreadTexts " + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "nextUnreadTexts " + e);
            // e.printStackTrace();
        }
        return ret_data;
    }

    public <PopulateMarkedTextsTask> List<TextInfo> getMarkedTexts(
            MarkedTextList markedTextList) throws InterruptedException {
        List<TextInfo> ret_data = new ArrayList<TextInfo>();
        Mark[] data = null;

        if (lks == null) {
            return null;
        }
        try {
            if (slock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    data = lks.getMarks();
                } finally {
                    slock.unlock();
                }
            } else {
                Log.d(TAG, "populateSeeAgain could not lock");
            }
            if (data != null) {
                Log.d(TAG, "populateSeeAgain found number of texts: "
                        + data.length);
                markedTextList.setPBMax(data.length);
            } else {
                Log.d(TAG, "populateSeeAgain found null: ");
            }
            int counter = 0;
            for (Mark i : data) {
                Integer textno = i.getText();
                Log.d(TAG, "getMarkedTexts Next text: " + textno);
                TextInfo text = getKomText(textno);
                if (text != null) {
                    ret_data.add(text);
                } else {
                    Log.d(TAG, "getMarkedTexts could not find textno " + textno);
                }
                Log.d(TAG, "getMarkedTexts Author: " + text.getAuthor());
                Log.d(TAG, "getMarkedTexts Date: " + text.getDate());
                Log.d(TAG, "getMarkedTexts Subject: " + text.getSubject());
                counter++;
                markedTextList.setPBprogress(counter);
            }
        } catch (RpcFailure e) {
            Log.d(TAG, "getMarkedTexts " + e);
            // e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "getMarkedTexts " + e);
            // e.printStackTrace();
        }
        return ret_data;
    }

    /*
     * Remove contents of "Paranthesis" in kom-name
     */
    public String stripParanthesis(String str) {
        String retval = str;
        int firstP = str.lastIndexOf('(');
        int lastP = str.indexOf(')', firstP);
        if ((firstP >= 0) && (lastP >= 0) && (lastP > firstP)) {
            retval = str.substring(0, firstP)
                    + str.substring(lastP, str.length() - 1);
            retval = stripParanthesis(retval);
        }
        return retval;
    }
    
    void setCurrentTextList(List<TextInfo> Texts) {
        mTexts = Texts;
    }

    List<TextInfo> getCurrentTextList() {
        return (mTexts);
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

    public int getServerPortNo() {
        return re_port;
    }

    public boolean getUseSSL() {
        return re_useSSL;
    }

    public int getCertLevel() {
        return re_cert_level;
    }

    public void setUser(int userId, String userPSW, String server, int port,
            boolean useSSL, int cert_level) {
        if ((userId > 0) && (userPSW != null) && (userPSW.length() > 0)
                && (server.length() > 0) && (port > 0)) {
            re_userid = userId;
            re_password = userPSW;
            re_server = server;
            re_port = port;
            re_useSSL = useSSL;
            re_cert_level = cert_level;
            Log.d(TAG, "setting userid:" + userId + " server:" + server);
        } else {
            Log.d(TAG, "setUser got bad params! " + userId + " server:"
                    + server + " port:" + port);
            Thread.dumpStack();
            try {
                logout();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        boolean val = false;
        if (!connected) {
            return val;
        }
        try {
            if (lks != null) {
                val = lks.getConnected();
            }
        } catch (Exception e) {
            Log.d(TAG, "isConnected caught exception:" + e);
        }
        return val;
    }

    public void setConnected(boolean val) {
        connected = val;
        if (val) {
            if (lks == null || lks.getState() == Session.STATE_DISCONNECTED) {
                Log.d(TAG,
                        "mKom.setConnected set connected failed. trying to reconnect");
                new ReconnectTask().execute();
            }
        } else {
            try {
                Log.d(TAG, "mKom.setConnected tryLock");
                if (slock.tryLock(60, TimeUnit.SECONDS)) {
                    Log.d(TAG, "mKom.setConnected got Lock");
                    try {
                        if (lks != null) {
                            lks.disconnect(true);
                        } else {
                            Log.d(TAG,
                                    "mKom.setConnected no session to disconnect");
                        }
                    } finally {
                        slock.unlock();
                        Log.d(TAG, "mKom.setConnected unlocked");
                    }
                } else {
                    Log.d(TAG, "mKom.setConnected could not lock");
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "onCreate tryLock interrupted");
                e.printStackTrace();
            } catch (Exception e) {
                Log.d(TAG, "setConnected False failed:" + e);
                lks = null;
            }
        }
    }
    
	public void error(String s) {
		Log.e("androkom", s);
	}

	public void debug(String s) {
		Log.d("androkom KomServer", s);
	}

	/**
     * Logcat to file
     */
    public void dumpLog() {
        if (ConferencePrefs.getDumpLog(getBaseContext())) {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("logcat -d");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            File tempFile = null;
            FileOutputStream fos = null;
            File sdDir = Environment.getExternalStorageDirectory();
            final String EXPORT_DIR_NAME = sdDir.getAbsolutePath()+"/Android/data/org.lindev.androkom/files";
            final String EXPORT_FILE_NAME = EXPORT_DIR_NAME+"/androkom.log";
            Log.d(TAG, "dumpLOG: "+EXPORT_FILE_NAME);
            try {
                tempFile = new File(EXPORT_DIR_NAME);
                tempFile.mkdirs();
                tempFile = new File(EXPORT_FILE_NAME);
                tempFile.createNewFile();
                fos = new FileOutputStream(tempFile);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return;
            }
            BufferedOutputStream out = new BufferedOutputStream(fos);

            String newline = "\n";
            byte[] nlbytes = newline.getBytes();
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    out.write(line.getBytes());
                    out.write(nlbytes);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                out.close();
                bufferedReader.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void setClientMessageHandler(Handler clientHandler) {
        mHandler = clientHandler;
    }

	private Session lks = null;
	private volatile Lock slock = new ReentrantLock();
	
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

    private String mConfName = "";

    private List<TextInfo> mTexts;
    
	private String latestIMSender=""; // for creating new IM
	
	private boolean hidden_session = !RELEASE_BUILD;

	public AsyncMessages asyncMessagesHandler;
	private static Handler mHandler=null;
	public IMLogger imLogger;
}

package org.lindev.androkom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lysator.lattekom.AuxItem;
import org.lysator.lattekom.ConfInfo;
import org.lysator.lattekom.Membership;
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

/**
 * A service which keeps the Lattekom session object and all
 * LysKOM stuff for the various activities in the app.
 * 
 * @author henrik
 *
 */
public class KomServer extends Service 
{

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

    @Override
    public void onCreate() 
    {
        super.onCreate();

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
     * Fetch next unread text. 
     * 
     */
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

    /**
     * Log in to server. 
     * 
     * @return Empty string on success, string describing failure otherwise
     */
    public String login(String username, String password, String server) 
    {
        if (!s.getConnected()) {
            if (connect(server) != 0)
                return "Couldn't connect to server";
        }

        ConfInfo usernames[] = new ConfInfo[0];
        try {
            usernames = s.lookupName(username, true, false);
            if (usernames.length != 1) {            
                return "Invalid/ambigious username";
            } else {
                // login as hidden
                if (!s.login(usernames[0].confNo, password, true)) {
                    return "Invalid password";
                }
            }
        } catch (Exception e) {
            Log.e("androkom", "Caught " + e.getClass().getName());
            return "Unknown error";
        }
        return "";
    }

    /**
     * Display the next unread text in a TextView. 
     * 
     * TODO: This should not interact directly with GUI components.
     * 
     * @return text number displayed
     */
    public int displayText(final TextView tv) 
    {
        try {
            final int textNo = s.nextUnreadText(false);             
            if (textNo < 0) {
                tv.setText("All read");
                s.nextUnreadConference(true);
            } 
            else {
                final Text text = s.getText(textNo);
                final String username = s.getConfStat(text.getAuthor()).getNameString();

                final String str = "<b>Author: "+username+ 
                "<br/>Subject: " + text.getSubjectString() + 
                "</b><br/>" + text.getBodyString();

                tv.setText(Html.fromHtml(str), TextView.BufferType.SPANNABLE);

                s.markAsRead(textNo);
                return textNo;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
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
            s.createText(text);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Session s;

    // This is the object that receives interactions from clients. 
    private final IBinder mBinder = new LocalBinder();

}

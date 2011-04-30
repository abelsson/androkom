package org.lindev.androkom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import nu.dll.lyskom.AsynchMessage;
import nu.dll.lyskom.AsynchMessageReceiver;
import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.ConfInfo;
import nu.dll.lyskom.Hollerith;
import nu.dll.lyskom.KomToken;
import nu.dll.lyskom.Membership;
import nu.dll.lyskom.RpcEvent;
import nu.dll.lyskom.RpcEventListener;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Session;
import nu.dll.lyskom.Text;
import nu.dll.lyskom.UserArea;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


/**
 * A service which keeps the Lattekom session object and all
 * LysKOM stuff for the various activities in the app.
 * 
 * @author henrik
 *
 */
public class KomServer extends Service implements RpcEventListener, AsynchMessageReceiver, nu.dll.lyskom.Log
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
    		this.setTextNo(textNo);
    		this.setAuthor(author);
    		this.setDate(date);
    		this.setSubject(subject);
    		this.setBody(body);
    	}
    	
    	public void setAuthor(String author) {
			this.author = author;
		}
		
		public String getAuthor() {
			return author;
		}

		public void setBody(String body) {
			this.body = body;
		}

		public String getBody() {
			return body;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getSubject() {
			return subject;
		}

		public void setDate(String date) {
			this.date = date;
		}

		public String getDate() {
			return date;
		}

		public void setTextNo(int textNo) {
			this.textNo = textNo;
		}

		public int getTextNo() {
			return textNo;
		}

		private int textNo;
		private String date;
    	private String subject;
    	private String body;
    	private String author;
    }
    
    public KomServer() {
    	//System.setProperty("lattekom.debug", "true");
        System.setProperty("lattekom.enable-prefetch", "true"); 
        Session.setLog(this);
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
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "onDestroy "+e);
            e.printStackTrace();
        }

        s.removeRpcEventListener(this);        
        s = null;

    }

    void reconnect() {
    	Log.d(TAG, "KomServer trying to reconnect");
        try {
            if (s.getState() == Session.STATE_LOGIN)
                s.logout(true);
            Log.i("androkom","logged out");

            if (s.getState() == Session.STATE_CONNECTED)
                s.disconnect(false);

            Log.i("androkom","disconnected");           
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "onDestroy "+e);
            e.printStackTrace();
        }

        s.removeRpcEventListener(this);
        s = null;
    	
        s = new Session();
        s.addRpcEventListener(this);
        //s.addAsynchMessageReceiver(this);

        Log.d(TAG, "KomServer trying to login using "+re_userid+" "+re_server);
        login(re_userid, re_password, re_server);
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
            s.addAsynchMessageReceiver(this);
        } catch (IOException e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "connect1 "+e);

            e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
        	Log.d(TAG, "fetchConferences1 "+e);
            e.printStackTrace();
            reconnect();
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "fetchConferences2 "+e);
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
        	Log.d(TAG, "getConferenceName "+e);

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
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "setConference "+e);

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
    	try {
    		if (!s.getConnected()) {
    			if (connect(server) != 0)
    				return "Couldn't connect to server";
    		}
        } catch (Exception e) {
            Log.e("androkom", "Login.name connect Caught " + e.getClass().getName()+":"+e+":"+e.getCause());
            e.printStackTrace();
            return "Unknown error";
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
            }
        } catch (Exception e) {
            Log.e("androkom", "Login.name Caught " + e.getClass().getName()+":"+e+":"+e.getCause());
            e.printStackTrace();
            return "Unknown error";
        }
        try {
            s.setClientVersion("Androkom", getVersionName());
            s.setLatteName("AndroKOM " + getVersionName());
        } catch (Exception e) {
        	Log.e("androkom", "Login.name2 Caught " + e.getClass().getName()+":"+e+":"+e.getCause());
        	e.printStackTrace();
        	return "Unknown error";
        }
        re_userid = usernames[0].confNo;
        re_password = password;
        re_server = server;

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
            Log.e("androkom", "Login.id Caught " + e.getClass().getName()+e.getStackTrace());
            return "Unknown error";
        }
        re_userid = userid;
        re_password = password;
        re_server = server;
        
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
				return getKomText(arr[0]);
			}
		} catch (RpcFailure e) {
			// TODO Auto-generated catch block
        	Log.d(TAG, "getParentToText "+e);

			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
        	Log.d(TAG, "getParentToText "+e);
			e.printStackTrace();
		}
    	reconnect();
		return new TextInfo(-1, "", "", "", "[error fetching parent text]");
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
            
            return getKomText(mLastTextNo);                                

        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "getNextUnreadText "+e);
            e.printStackTrace();
        }
    	reconnect();

        return new TextInfo(-1, "", "", "", "[error fetching unread text]");
    }


    /**
     * Fetch next unread text, as a HTML formatted string. 
     */
    public TextInfo getKomText(int textNo)
    {
        try {
            Text text = s.getText(textNo);
            String username;
            int authorid = text.getAuthor();
            if (authorid > 0) {
            	try {
            		nu.dll.lyskom.Conference confStat = s.getConfStat(authorid);
            		username = confStat.getNameString();
                } catch (Exception e) {
                	username = "Person "+authorid+" finns inte";
                }
            } else {
            	username = "anonymous";
            }
            String CreationTimeString = text.getCreationTimeString();
            String SubjectString = null;
            try {
            	SubjectString = text.getSubjectString();
            } catch (UnsupportedEncodingException e) {
            	Log.d(TAG, "UnsupportedEncodingException"+e);
            	SubjectString = text.getSubjectString8();
            }
            String BodyString = null;
            try {
            	BodyString = text.getBodyString();
            } catch (UnsupportedEncodingException e) {
            	Log.d(TAG, "UnsupportedEncodingException"+e);
            	BodyString = text.getBodyString8();
            }
            
            docacheAllComments(text);
            return new TextInfo(textNo, username, CreationTimeString, SubjectString, BodyString);
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "getTextAsHTML "+e);

            e.printStackTrace();
        }
        return new TextInfo(-1, "", "", "", "[Error fetching text]");

    }

    /**
     * Attempt to retrieve all comments for a text.
     */
    private class cacheAllCommentsTask extends AsyncTask<Text, Integer, Void> {
        protected void onPreExecute() {

        }

        protected Void doInBackground(Text... text) 
        {
        		int[] comments = text[0].getComments();
        		if (comments.length>0) {
        			Log.d(TAG, "Text#"+text[0].getNo()+" has "+comments.length+" comments");
        			for(int i=0; i<comments.length; i++) {
        				try {
        					Log.d(TAG, "Trying to cache text "+comments[i]);
        					s.getText(comments[i]);
        				} catch (RpcFailure e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				} catch (IOException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
        			}
        		} else {
        			Log.d(TAG, "No comments to cache on text#"+text[0].getNo());
        			try {
        				int[] recipients = text[0].getRecipients();
        				if (recipients.length > 0) {
        					List<Integer> list = s.nextUnreadTexts(recipients[0], false, 3);
        					if (list.size()>0) {
        						for (int i=0; i<list.size(); i++) {
                					Log.d(TAG, "Trying to cache text "+list.get(i));
                					s.getText(list.get(i));
        						}
        					} else {
        						Log.d(TAG, "No more unread in conf"+recipients[0]);
        					}
						} else {
							Log.d(TAG, "No recipients");
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
				return null;
        }

        @SuppressWarnings("unused")
		protected void onPostExecute(final String result) 
        { 

        }

    }

    private void docacheAllComments(Text text)
    {
        new cacheAllCommentsTask().execute(text);
    }
     
    public void markText(int textNo)
    {
    	try {
			s.markText(textNo, 100);
		} catch (RpcFailure e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void unmarkText(int textNo)
    {
    	try {
			s.unmarkText(textNo);
		} catch (RpcFailure e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	public void markTextAsRead(int textNo)
    {
    	Text text;
		try {
			text = s.getText(textNo);
			// TODO: Should batch these up and send in a group, instead of many separate requests.
			int recipents[] = text.getRecipients();						
			for(int i=0;i<recipents.length;i++) {
				int confNo = recipents[i];
				int[] localTextNo = { text.getLocal(confNo) };
				s.doMarkAsRead(confNo, localTextNo); 
			}
			
			int ccrecipients[] = text.getCcRecipients();
			for(int i=0;i<ccrecipients.length;i++) {
				int confNo = ccrecipients[i];
				int[] localTextNo = { text.getLocal(confNo) };
				s.doMarkAsRead(confNo, localTextNo); 
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
        try {
			if(!s.isMemberOf(s.getCurrentConference())) {
				text.addRecipient(re_userid);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Failed testing membership");
			e1.printStackTrace();
		}
        
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
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "createText "+e);

            e.printStackTrace();
        }
    }

    /**
     * Create a text, in reply to another text.
     */
    public void createText(String subject, String body, int inReplyTo, boolean copyRecipients) 
    {
        Text text = new Text();

        if (inReplyTo != -1)
            text.addCommented(inReplyTo);

        if ( (inReplyTo != -1) && (copyRecipients) ) {
            Text orgtext=null;
			try {
				orgtext = s.getText(inReplyTo);
			} catch (RpcFailure e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(orgtext != null) {
				int[] receps = orgtext.getRecipients();
				Log.d(TAG, "Found no of recipients:"+receps.length);
				for(int i=0; i < receps.length; i++) {
					Log.d(TAG, "adding recipient:"+receps[i]);
					try {
						text.addRecipient(receps[i]);
					} catch (java.lang.IllegalArgumentException e) {
						Log.d(TAG, "recipient already added");
					}
				}
			}
        }
        
        try {
			if(!s.isMemberOf(s.getCurrentConference())) {
				text.addRecipient(re_userid);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Failed testing membership");
			e1.printStackTrace();
		} catch (java.lang.IllegalArgumentException e) {
			Log.d(TAG, "recipient already added");
		}

        
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
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "createText "+e);

            e.printStackTrace();
        }
    }

    /**
     * Get a list of conferencenames matching a string
     */
    public ConfInfo[] getConferences(String name) 
    {
        // find ConfNo
        ConfInfo[] conferences=null;
		try {
			conferences = s.lookupName(name, false, true);
		} catch (RpcFailure e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return conferences;
    }

    /**
     * Get a list of usernames matching a string
     */
    public ConfInfo[] getUsers(String name) 
    {
        // find ConfNo
        ConfInfo[] conferences=null;
		try {
			conferences = s.lookupName(name, true, false);
		} catch (RpcFailure e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return conferences;
    }

    /**
     * Create a text
     */
    public ConfInfo[] createText(int recipient_type, String recipient, String subject, String body) 
    {
        Text text = new Text();
        ConfInfo[] conferences;
        
        // find ConfNo
        switch(recipient_type) {
        case 1 :
            conferences=getConferences(recipient);
        	break;
        case 2 :
            conferences=getUsers(recipient);
        	break;
        default:
        	Log.d(TAG, "create text unknown recipient_type:"+recipient_type);
        	return null;
        }
		if ((conferences == null) || (conferences.length < 1)
				|| (conferences.length > 1)) {
        	Log.d(TAG, "Could not find uniq ConfNo");
        	return conferences;
        }
    	int confno = 0;
    	confno = conferences[0].getNo();
    	Log.d(TAG, "creating text in confno:"+confno);
        text.addRecipient(confno);
        try {
			if(!s.isMemberOf(confno)) {
				text.addRecipient(re_userid);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Failed testing membership");
			e1.printStackTrace();
		}

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
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "createText "+e);

            e.printStackTrace();
        }
    	return conferences;
    }

    /**
     * Create a text
     */
    public void createText(int recipient_type, int confno, String subject, String body) 
    {
        Text text = new Text();

        text.addRecipient(confno);
        try {
			if(!s.isMemberOf(confno)) {
				text.addRecipient(re_userid);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Failed testing membership");
			e1.printStackTrace();
		}

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
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "createText "+e);

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
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "parseElispUserArea "+e);

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

    public void rpcEvent(RpcEvent e) {
        if (mPendingSentTexts.contains(e.getId())) {
            Log.i("androkom", "Got reply for created text " + e.getId());

            if (!e.getSuccess()) {
                /* TODO: handle error here */;
            	Log.d(TAG, "rpcEvent failed "+e);
            }
        }

    }

    public void asynchMessage(AsynchMessage m) {
        // TODO Auto-generated method stub
    	Log.d(TAG, "asynchMessage:"+m);
    	
    	if (asyncHandler != null) {
    		mMessage = m;
    		doGetMessage();
    	} else {
    		Log.d(TAG, "got async but no asyncHandler");
    	}
    }

    public void setasynchandler(Handler h) {
    	asyncHandler = h;
    }

    /**
     * Attempt to receive async message to user.
     */
    private class getMessageTask extends AsyncTask<Void, Integer, String> {
    	AsynchMessage message;

        protected void onPreExecute() {
            this.message = mMessage;
        }

        protected String doInBackground(final Void... args) 
        {
            	int confno;
            	String name;
            	Hollerith msgH;

            	KomToken[] params = message.getParameters();
            	
            	Message msg = new Message();
        		msg.what = message.getNumber();
        		Bundle b = new Bundle();
                int textno;
				switch(msg.what) {
                case nu.dll.lyskom.Asynch.login :
                	confno = params[0].intValue();
                	name = getConferenceName(confno);
                    b.putString("name", ""+name);
                	break;
                case nu.dll.lyskom.Asynch.logout :
                	confno = params[0].intValue();
                	name = getConferenceName(confno);
                    b.putString("name", ""+name);
                	break;
                case nu.dll.lyskom.Asynch.new_name :
                    msgH = (Hollerith) params[1];
                    b.putString("oldname", ""+msgH.getContentString());
                    msgH = (Hollerith) params[2];
                    b.putString("newname", ""+msgH.getContentString());
                	break;
                case nu.dll.lyskom.Asynch.send_message :
                	confno = params[1].intValue();
                	name = getConferenceName(confno);
                    b.putString("from", ""+name);
                	confno = params[0].intValue();
                	name = getConferenceName(confno);
                    b.putString("to", ""+name);
                    msgH = (Hollerith) params[2];
                    b.putString("msg", ""+msgH.getContentString());
                	break;
                case nu.dll.lyskom.Asynch.new_text_old : 
                	confno = params[0].intValue();
                	Log.d(TAG, "New text created:"+confno);
                	break;
                case nu.dll.lyskom.Asynch.i_am_on:
                	Log.d(TAG, "Should probably update cached data (i_am_on).");
                	break;
                case nu.dll.lyskom.Asynch.sync_db:
                	Log.d(TAG, "Database sync. Tell user about service interruption?");
                	break;
                case nu.dll.lyskom.Asynch.leave_conf:
                	Log.d(TAG, "No longer member of a conference.");
                	break;
                case nu.dll.lyskom.Asynch.rejected_connection:
                	Log.d(TAG, "Lyskom is full, please make space.");
                	break;
                case nu.dll.lyskom.Asynch.deleted_text:
                	Log.d(TAG, "Text deleted.");
                	break;
                case nu.dll.lyskom.Asynch.new_text:
                	Log.d(TAG, "New text created.");
                	textno = params[0].intValue();
                	Log.d(TAG, "Trying to cache text "+textno);
					try {
						s.getText(textno);
					} catch (RpcFailure e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	break;
                case nu.dll.lyskom.Asynch.new_recipient:
                	Log.d(TAG, "New recipient added to text.");
                	break;
                case nu.dll.lyskom.Asynch.sub_recipient:
                	Log.d(TAG, "Recipient removed from text.");
                	break;
                case nu.dll.lyskom.Asynch.new_membership:
                	Log.d(TAG, "New recipient added to text.");
                	break;
                default:
                	Log.d(TAG, "Unknown async message received#"+msg.what);
                }
                msg.setData(b);
        		asyncHandler.sendMessage(msg);

        		return getString(R.string.No_server_selected);
        }

        protected void onPostExecute(final String result) 
        { 

        }
    }

    private void doGetMessage()
    {
        new getMessageTask().execute();
    }

    public boolean sendMessage(int recipient, String message, boolean block)
    throws IOException, RpcFailure {
    	return s.sendMessage(recipient, message, block);
    }
    
    public ConferenceInfo[] getUserNames() {
    	try {
    		if (usernames != null && usernames.length > 1) {
    			final ConferenceInfo[] items = new ConferenceInfo[usernames.length];
    			Log.d(TAG, "Ambigous name");
    			for(int i=0; i <usernames.length; i++) {   				
    				items[i] = new ConferenceInfo();
    				items[i].name=s.toString(s.getConfName(usernames[i].confNo));
    				items[i].id = usernames[i].confNo;
    				Log.d(TAG, "Name "+i+":"+items[i]);
    			}
    			return items;
    		}

    	} catch (UnsupportedEncodingException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (RpcFailure e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	return null;
    }

    public boolean isConnected() {
    	if (s==null) {
    		return false;
    	}
        return s.getConnected();
    }
    
    public void error(String s) {
    	Log.e("androkom", s);
    }
    
    public void debug(String s) {
    	Log.d("androkom", s);
    }
    
    private Session s=null;

    private int mLastTextNo=0;
    HashMap<String, String> mUserAreaProps=null;

    AsynchMessage mMessage; // temp storage for async message
    
    // This is the object that receives interactions from clients. 
    private final IBinder mBinder = new LocalBinder();

    private HashSet<Integer> mPendingSentTexts;
    ConfInfo usernames[];
    private int re_userid; //for reconnect
    private String re_password; // for reconnect
    private String re_server; // for reconnect

	Handler asyncHandler=null;
}

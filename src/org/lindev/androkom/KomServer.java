package org.lindev.androkom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.ConfInfo;
import nu.dll.lyskom.DynamicSessionInfo;
import nu.dll.lyskom.RpcEvent;
import nu.dll.lyskom.RpcEventListener;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Session;
import nu.dll.lyskom.Text;
import nu.dll.lyskom.UserArea;

import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;
import org.lindev.androkom.WhoIsOn.populatePersonsTask;

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
public class KomServer extends Service implements RpcEventListener, nu.dll.lyskom.Log
{
	public static final String TAG = "Androkom";
	public static boolean RELEASE_BUILD = false;

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
        public int numUnread;

        @Override
        public String toString() 
        {
            return name + " <" + id + ">";
        }
    }

    /**
     * Small helper class to manage texts.
     */
    public static class TextInfo
    {
    	public TextInfo() { }

    	public TextInfo(int textNo, String author, String date, String headers, String subject, String body)
    	{
    		this.setTextNo(textNo);
    		this.setAuthor(author);
    		this.setDate(date);
    		this.setHeaders(headers);
    		this.setSubject(subject);
    		this.setBody(body);
    	}
    	
    	public void setAuthor(String author) {
			this.author = author;
		}
		
		public String getAuthor() {
			return author;
		}

		public void setHeaders(String headers) {
			this.headers = headers;
		}

		public void setBody(String body) {
			this.body = body;
		}

		public String getBody() {
			return body;
		}

		public String getHeaders() {
			return headers;
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
    	private String headers;
    	private String body;
    	private String author;
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
        
        asyncMessagesHandler = new AsyncMessages(getApp(), this);
        asyncMessagesHandler.subscribe(asyncMessagesHandler.new MessageToaster());
        
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

    App getApp() 
    {
        return (App) getApplication();
    }

    /**
     * Called upon destruction of the service. If we're logged in,
     * we want to log out and close the connection now.
     */
    @Override
    public void onDestroy() 
    {

        // Tell the user we stopped.
        Toast.makeText(this, getString(R.string.komserver_stopped), Toast.LENGTH_SHORT).show();

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

        super.onDestroy();
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
            s.addAsynchMessageReceiver(asyncMessagesHandler);
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
     * Fetch a list of persons online
     * @param populatePersonsTask 
     */
    public List<ConferenceInfo> fetchPersons(populatePersonsTask populatePersonsT)
    {
        ArrayList<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();
        try {
            DynamicSessionInfo[] persons = s.whoIsOnDynamic(true, false, 30*60);

            for (int i = 0; i < persons.length; i++) {
                int persNo = persons[i].getPerson();
                String username;
                if (persNo > 0) {
                	try {
                		nu.dll.lyskom.Conference confStat = s.getConfStat(persNo);
                		username = confStat.getNameString();
                		populatePersonsT.updateProgress((int) ((i / (float) persons.length) * 100));
                    } catch (Exception e) {
                    	username = getString(R.string.person)+persNo+
                    	getString(R.string.does_not_exist);
                    }
                } else {
                	username = getString(R.string.anonymous);
                }
                Log.i("androkom", username + " <" + persNo + ">");

                ConferenceInfo info = new ConferenceInfo();
                info.id = persNo;
                info.name = username;

                arr.add(info);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "fetchPersons1 "+e);
            e.printStackTrace();
            reconnect();
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "fetchPersons2 "+e);
            e.printStackTrace();
        }

        return arr;
    }
    
    /**
     * Add a new subscriber who's interested in asynchronous messages.
     */
    void addAsyncSubscriber(AsyncMessageSubscriber sub)
    {
    	asyncMessagesHandler.subscribe(sub);
    }
    
    /**
     * Add a new subscriber who's interested in asynchronous messages.
     */
    void removeAsyncSubscriber(AsyncMessageSubscriber sub)
    {
    	asyncMessagesHandler.unsubscribe(sub);
    }

    /**
     * Fetch a list of conferences with unread texts.
     */
    public List<ConferenceInfo> fetchConferences()
    {
        List<ConferenceInfo> arr = new ArrayList<ConferenceInfo>();

        try
        {
            for (int conf : s.getMyUnreadConfsList(true))
            {
                  String name = s.toString(s.getConfName(conf));
                  Log.i(TAG, name + " <" + conf + ">");

                  ConferenceInfo info = new ConferenceInfo();
                  info.id = conf;
                  info.name = name;
                  info.numUnread = s.getUnreadCount(conf);

                  arr.add(info);
            }
        }
        catch (IOException e)
        {
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
     * Return presentation text number for current conference.
     */
	public int getConferencePres() {
		int confNo = s.getCurrentConference();
		if (confNo > 0) {
			try {
				return s.getConfStat(confNo).getPresentation();
			} catch (RpcFailure e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return 0;
	}

    /**
     * Set currently active conference.
     */
    public void setConference(int confNo) 
    {
        try {
            s.changeConference(confNo);
            textFetcher.restartPrefetcher();
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
                if (!s.login(usernames[0].confNo, password, hidden_session, false)) {
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
        	if (!s.login(userid, password, hidden_session, false)) {
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

    public void endast(int confNo, int no) {
    	try {
			s.endast(confNo, no);
		} catch (RpcFailure e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public Session getSession()
    {
        return s;
    }

    /**
     * Fetch next unread text, as a HTML formatted string. 
     */
    private final TextFetcher textFetcher = new TextFetcher(this);
    public TextInfo getKomText(final int textNo)
    {
        final TextInfo text = textFetcher.getText(textNo);
        textFetcher.doCacheRelevant(textNo);
        return text;
    }

    public TextInfo getNextUnreadText()
    {
        final TextInfo text = textFetcher.getNextUnreadText();
        textFetcher.doCacheRelevant(text.getTextNo());
        return text;
    }

    public TextInfo getParentToText(final int textNo) {
        final TextInfo text = textFetcher.getParentToText(textNo);
        textFetcher.doCacheRelevant(text.getTextNo());
        return text;
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

        int currConf = s.getCurrentConference();
        try {
        	// only add current Conf if comments are allowed
			if(s.getConfStat(currConf).getConfInfo().confType.original()) {
				Log.d(TAG, "Conf is original, add super conf instead");
				text.addRecipient(s.getConfStat(currConf).getSuperConf());
			} else {
				Log.d(TAG, "Conf is not original, ok to add");
				text.addRecipient(currConf);
			}
		} catch (RpcFailure e2) {
			// TODO Auto-generated catch block
			Log.d(TAG, "createText failed to add conf");
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			Log.d(TAG, "createText failed to add conf");
			e2.printStackTrace();
		}
        try {
			if(!s.isMemberOf(currConf)) {
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
				        try {
				        	// only add Conf if comments are allowed
							if(s.getConfStat(receps[i]).getConfInfo().confType.original()) {
								Log.d(TAG, "Conf is original, add super conf instead");
								text.addRecipient(s.getConfStat(receps[i]).getSuperConf());
							} else {
								Log.d(TAG, "Conf is not original, ok to add");
								text.addRecipient(receps[i]);
							}
						} catch (RpcFailure e2) {
							// TODO Auto-generated catch block
							Log.d(TAG, "createText failed to add conf");
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							Log.d(TAG, "createText failed to add conf");
							e2.printStackTrace();
						}
					} catch (java.lang.IllegalArgumentException e) {
						Log.d(TAG, "recipient already added");
					}
				}
			}
        }
        
        try {
			int[] receps = text.getRecipients();
			boolean userIsMemberOfSomeConf = false;
			for(int i=0; i < receps.length; i++) {
				if(s.isMemberOf(receps[i])) {
					userIsMemberOfSomeConf = true;
				}
			}
			if(!userIsMemberOfSomeConf) {
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

    public boolean sendMessage(int recipient, String message, boolean block)
    throws IOException, RpcFailure {
    	return s.sendMessage(recipient, message, block);
    }

    public void setShowFullHeaders(final boolean h) {
        textFetcher.setShowFullHeaders(h);
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

    // This is the object that receives interactions from clients. 
    private final IBinder mBinder = new LocalBinder();

    private HashSet<Integer> mPendingSentTexts;
    ConfInfo usernames[];
    private int re_userid; //for reconnect
    private String re_password; // for reconnect
    private String re_server; // for reconnect

    private boolean hidden_session = !RELEASE_BUILD;
    
    AsyncMessages asyncMessagesHandler;
}

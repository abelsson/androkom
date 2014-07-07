package org.lindev.androkom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nu.dll.lyskom.Person;

import org.lindev.androkom.gui.IMConversationList;
import org.lindev.androkom.gui.TextCreator;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Show a list of persons logged on.
 * 
 * @author jonas
 *
 */
public class WhoIsOn extends ListActivity implements ServiceConnection
{
	public static final String TAG = "Androkom WhoIsOn";
	private KomServer mKom;

	/**
     * Instantiate activity.  
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.whoison);

		mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                consumeMessage(msg);
            }
        };
        
		who_type = (Integer) getIntent().getExtras().get("who_type");

        // Set Window Title
        switch(who_type) {
        	case 1 : setTitle(getString(R.string.seewhoison_label));
        	         break;
        	case 2 : setTitle(getString(R.string.seefriendsison_label));
        	         break;
        	default: setTitle("Title of window");
        }
        
        mAdapter = new ArrayAdapter<ConferenceInfo>(this, R.layout.whoison_person);
        setListAdapter(mAdapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        getApp().doBindService(this);
    }
    
    /**
     * Update on visible
     */
    @Override
	public void onResume() {
        Log.d(TAG, "onResume");
		super.onResume();

		//new populatePersonsTask().execute();
	}
    
    /**
     * Pause
     */
    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause");
        super.onPause();
    }
    
    @Override
    protected void onDestroy() 
    {
        getApp().doUnbindService(this);
        super.onDestroy();
    }
    
    /**
     * Called when a person has been clicked. Switch to send message or mail?
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        selected_user = ((ConferenceInfo) l.getItemAtPosition(position)).name;
        final int selected_user_id = ((ConferenceInfo) l.getItemAtPosition(position)).id;
        selected_sessionNo = ((ConferenceInfo) l.getItemAtPosition(position)).sessionNo;

        Log.d(TAG, "onListItemClick selected_user_id = " + selected_user_id);
        Log.d(TAG, "onListItemClick selected_sessionNo = " + selected_sessionNo);

        AlertDialog.Builder builder = new AlertDialog.Builder(WhoIsOn.this);
        builder.setTitle(getString(R.string.pick_an_action));
        String vals[] = {
                getString(R.string.createnewmail_label),
                getString(R.string.create_new_IM),
                getString(R.string.person_stat_label)
                };
        builder.setSingleChoiceItems(vals, -1,
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int item)
                    {
                        dialog.cancel();
                        Intent intent = null;
                        switch (item)
                        {
                        case 0: // mail
                            Log.d(TAG, "Trying to create mail");
                            intent = new Intent(getBaseContext(), TextCreator.class);
                            intent.putExtra(TextCreator.INTENT_IS_MAIL, true);
                            intent.putExtra(TextCreator.INTENT_RECIPIENT, selected_user);
                            startActivity(intent);
                            finish();
                            break;
                        case 1: // IM
                            Log.d(TAG, "Trying to create IM");
                            intent = new Intent(getBaseContext(), IMConversationList.class);
                            intent.putExtra(Consts.INTENT_CONVERSATION_LIST_RECIPIENT, selected_user);
                            startActivity(intent);
                            finish();
                            break;
                        case 2: // Person info
                            Message msg = new Message();
                            msg.what = Consts.MESSAGE_PERS_INFO;
                            msg.arg1 = selected_user_id;
                            mHandler.sendMessage(msg);
                            break;
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }    

    /**
     * Attempt to get all persons online.
     */
    public class populatePersonsTask extends AsyncTask<Void, Integer, Integer> {
        protected void onPreExecute() {
            Log.d(TAG, "populatePersonsTask onPreExecute 1");
            PopulateProgressDialog = new ProgressDialog(WhoIsOn.this);
            PopulateProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            PopulateProgressDialog.setCancelable(true);
            PopulateProgressDialog.setIndeterminate(false);
            PopulateProgressDialog.setMessage(getString(R.string.loading));
            PopulateProgressDialog.setMax(100);
            PopulateProgressDialog.show();
            Log.d(TAG, "populatePersonsTask onPreExecute 2");
        }

		protected Integer doInBackground(Void... param) {
            Log.d(TAG, "populatePersonsTask doInBackground 1");
    		int i=0;
    		/*
    		while ((mKom == null)&&(i<100)) {
    			i++;
    			try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
		            Log.d(TAG, "populatePersonsTask background interrupted:"+e);
		            //e.printStackTrace();
				}
    		}*/
    		Log.d(TAG, "populatePersonsTask slept "+i);
	        tPersons = fetchPersons(who_type);
            Log.d(TAG, "populatePersonsTask doInBackground 2");
			return 1;
		}

        void changeMax(int val) {
            PopulateProgressDialog.setMax(val);
        }
        
		void updateProgress(int percent) {
            Log.d(TAG, "populatePersonsTask updateProgress "+percent);
			publishProgress(percent);
		}
		
		protected void onProgressUpdate(Integer... i) {
            Log.d(TAG, "populatePersonsTask onProgressUpdate");
            PopulateProgressDialog.setProgress(i[0]);
		}

		protected void onPostExecute(final Integer result) 
        { 
            Log.d(TAG, "populatePersonsTask onPostExecute 1");
            PopulateProgressDialog.dismiss();
            mPersons = tPersons;
            populatePersons();
            Log.d(TAG, "populatePersonsTask onPostExecute 2");
        }

    }

    /**
     * Refresh list of persons.
     */
    private void populatePersons() 
    {
        Log.d(TAG, "populatePersons 1");
        mAdapter.clear();
        Log.d(TAG, "populatePersons 2");

        if (mPersons != null && (!mPersons.isEmpty())) {
            Log.d(TAG, "populatePersons 3");
        	for(ConferenceInfo elem : mPersons) {
        		mAdapter.add(elem);
        	}
            Log.d(TAG, "populatePersons 4");
        	mAdapter.notifyDataSetChanged();
        } else {
            Log.d(TAG, "populatePersons 5");
        	// TODO: Do something here?
        	Log.d(TAG, "populatePersons failed, no Persons");
        	Log.d(TAG, "mPersons is null:"+(mPersons==null));
        	if(mPersons!=null) {
            	Log.d(TAG, "mPersons is empty:"+mPersons.isEmpty());
        	}
            Log.d(TAG, "populatePersons 6");
        }
        Log.d(TAG, "populatePersons 7");
    }
 
    private List<ConferenceInfo> fetchPersons(int whoType) {
    	List<ConferenceInfo> retlist = null;
        Log.d(TAG, "fetchPersons 1");
    	
		try {
			if (mKom != null) {
				if (mKom.isConnected()) {
					retlist = mKom.fetchPersons(mHandler, whoType);
				} else {
					Log.d(TAG, "Can't fetch persons when no connection");
					//mKom.reconnect();
				}
			} else {
				Log.d(TAG, "mKom is null");
			}
		} catch (Exception e) {
			Log.d(TAG, "fetchPersons failed:" + e);
			//e.printStackTrace();
		}
        Log.d(TAG, "fetchPersons 2");
		return retlist;
    }

    /**
     * Attempt to get all persons online.
     */
    public class person_info_task extends AsyncTask<Integer, Void, Person> {
        private final ProgressDialog dialog = new ProgressDialog(WhoIsOn.this);
        private String clientName = "";
        private String clientVersion = "";
        private String userName = "";
        private String connectionTime = "";
        
        protected void onPreExecute() {
            Log.d(TAG, "person_info_task onPreExecute 1");
            this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(false);
            this.dialog.setMessage(getString(R.string.loading));
            //this.dialog.show();
            Log.d(TAG, "person_info_task onPreExecute 2");
        }

        protected Person doInBackground(Integer... arg0) {
            userName = "(unknown)";

            Log.d(TAG, "person_info_task stat on " + arg0[0]);
            Person pers = null;
            try {
                pers = mKom.getPersonStat(arg0[0]);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                Log.d(TAG, "person_info_task InterruptedException 1");
            }
            Log.d(TAG, "person_info_task doInBackground 1");
            try {
                userName = mKom.fetchUsername(arg0[0]);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "person_info_task InterruptedException 2");
            }
            Log.d(TAG, "person_info_task doInBackground 2");
            try {
                clientName = mKom.getClientName(selected_sessionNo);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "person_info_task InterruptedException 3");
            }
            Log.d(TAG, "person_info_task doInBackground 3");
            try {
                clientVersion = mKom.getClientVersion(selected_sessionNo);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "person_info_task InterruptedException 4");
            }
            Log.d(TAG, "person_info_task doInBackground 4");
            try {
                connectionTime = mKom.getConnectionTime(selected_sessionNo);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "person_info_task InterruptedException 5");
            }
            Log.d(TAG, "person_info_task doInBackground 5");
            return pers;
        }

        protected void onPostExecute(final Person pers) {
            Log.d(TAG, "person_info_task onPostExecute 1");
            //this.dialog.dismiss();
            String pstat = "";

            pstat += "Name: " + userName + "\n";
            Log.d(TAG, "person_info_task onPostExecute 2");
            Date CreationTime = pers.getLastLogin().getTime();
            Log.d(TAG, "person_info_task onPostExecute 3");
            SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm]");
            pstat += "Last login: " + sdf.format(CreationTime) + "\n";
            pstat += "Time present: " + pers.getTotalTimePresent() + "\n";
            pstat += "Number of sessions: " + pers.getSessions() + "\n";
            pstat += "Created lines: " + pers.getCreatedLines() + "\n";

            if(selected_sessionNo > 0) {
                pstat += "Client name: " + clientName + "\n";
                pstat += "Client version: " + clientVersion + "\n";
                pstat += "Connection time: " + connectionTime + "\n";                
            } else {
                pstat += "No session no: "+selected_sessionNo;
            }
            Log.d(TAG, "person_info_task onPostExecute 4");
            
            new AlertDialog.Builder(WhoIsOn.this).setTitle("Person info!")
                    .setMessage(pstat).setNeutralButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                }
                            }).show();
            Log.d(TAG, "person_info_task onPostExecute 5");

        }
    }

    protected void consumeMessage(final Message msg) {
        switch (msg.what) {
        case Consts.MESSAGE_PERS_INFO:
            new person_info_task().execute(msg.arg1);
            break;
        case Consts.MESSAGE_POPULATE:
            new populatePersonsTask().execute();
            break;
        case Consts.MESSAGE_PROGRESS:
            PopulateProgressDialog.setProgress(msg.arg1);
            break;
        default:
            Log.d(TAG, "consumeMessage ERROR unknown msg.what=" + msg.what);
            return;
        }
    }

    App getApp() 
    {
        return (App)getApplication();
    }
    
	public void onServiceConnected(ComponentName name, IBinder service) {
        mKom = ((LocalBinder<KomServer>) service).getService();
        Message msg = new Message();
        msg.what = Consts.MESSAGE_POPULATE;
        mHandler.sendMessage(msg);
	}

	public void onServiceDisconnected(ComponentName name) {
		mKom = null;		
	}
    private List<ConferenceInfo> tPersons;
    private List<ConferenceInfo> mPersons;
    private ArrayAdapter<ConferenceInfo> mAdapter;
    
    private String selected_user = null;
    private int selected_sessionNo = 0;
    
    private int who_type = 0; // type 1 = all, type 2 = friends

    private static Handler mHandler=null;
    private ProgressDialog PopulateProgressDialog = null;
 }
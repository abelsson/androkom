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
import android.os.IBinder;
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
        getApp().doBindService(this);
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
    }
    
    /**
     * Update on visible?
     */
    @Override
	public void onResume() {
		super.onResume();

		new populatePersonsTask().execute();
	}
    
    /**
     * Pause
     */
    @Override
    public void onPause()
    {
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
                            intent.putExtra(IMConversationList.INTENT_CONVERSATION_LIST_RECIPIENT, selected_user);
                            startActivity(intent);
                            finish();
                            break;
                        case 2: // Person info
                            new person_info_task().execute(selected_user_id);
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
        private final ProgressDialog dialog = new ProgressDialog(WhoIsOn.this);

        protected void onPreExecute() {
			this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(false);
            this.dialog.setMessage(getString(R.string.loading));
            this.dialog.setMax(1);
            this.dialog.show();
        }

		protected Integer doInBackground(Void... param) {
    		int i=0;
    		while ((mKom == null)&&(i<100)) {
    			i++;
    			try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
		            Log.d(TAG, "populatePersonsTask background interrupted:"+e);
		            //e.printStackTrace();
				}
    		}
    		Log.d(TAG, "populatePersonsTask slept "+i);
	        tPersons = fetchPersons(this, who_type);
			return 1;
		}

        void changeMax(int val) {
            this.dialog.setMax(val);
        }
        
		void updateProgress(int percent) {
			publishProgress(percent);
		}
		
		protected void onProgressUpdate(Integer... i) {
			this.dialog.setProgress(i[0]);
		}

		protected void onPostExecute(final Integer result) 
        { 
            this.dialog.dismiss();
            mPersons = tPersons;
            populatePersons();
        }

    }

    /**
     * Refresh list of persons.
     */
    private void populatePersons() 
    {
        mAdapter.clear();

        if (mPersons != null && (!mPersons.isEmpty())) {
        	for(ConferenceInfo elem : mPersons) {
        		mAdapter.add(elem);
        	}

        	mAdapter.notifyDataSetChanged();
        } else {
        	// TODO: Do something here?
        	Log.d(TAG, "populatePersons failed, no Persons");
        	Log.d(TAG, "mPersons is null:"+(mPersons==null));
        	if(mPersons!=null) {
            	Log.d(TAG, "mPersons is empty:"+mPersons.isEmpty());
        	}
        }
    }
 
    private List<ConferenceInfo> fetchPersons(populatePersonsTask populatePersonsT, int whoType) {
    	List<ConferenceInfo> retlist = null;
    	
		try {
			if (mKom != null) {
				if (mKom.isConnected()) {
					retlist = mKom.fetchPersons(populatePersonsT, whoType);
				} else {
					Log.d(TAG, "Can't fetch persons when no connection");
					mKom.reconnect();
				}
			} else {
				Log.d(TAG, "mKom is null");
			}
		} catch (Exception e) {
			Log.d(TAG, "fetchPersons failed:" + e);
			//e.printStackTrace();
		}
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
            this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(false);
            this.dialog.setMessage(getString(R.string.loading));
            this.dialog.show();
        }

        protected Person doInBackground(Integer... arg0) {
            Log.d(TAG, "person_info_task stat on " + arg0);
            Person pers = mKom.getPersonStat(arg0[0]);
            userName = mKom.fetchUsername(arg0[0]);
            clientName = mKom.getClientName(selected_sessionNo);
            clientVersion = mKom.getClientVersion(selected_sessionNo);
            connectionTime = mKom.getConnectionTime(selected_sessionNo);
            return pers;
        }

        protected void onPostExecute(final Person pers) {
            this.dialog.dismiss();
            String pstat = "";

            pstat += "Name: " + userName + "\n";
            Date CreationTime = pers.getLastLogin().getTime();
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
            
            new AlertDialog.Builder(WhoIsOn.this).setTitle("Person info!")
                    .setMessage(pstat).setNeutralButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                }
                            }).show();

        }
    }

    App getApp() 
    {
        return (App)getApplication();
    }
    
	public void onServiceConnected(ComponentName name, IBinder service) {
		mKom = ((KomServer.LocalBinder)service).getService();		
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

 }
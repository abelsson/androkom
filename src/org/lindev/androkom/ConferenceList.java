package org.lindev.androkom;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import nu.dll.lyskom.KomToken;

import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;
import org.lindev.androkom.gui.IMConversationList;
import org.lindev.androkom.gui.MessageLog;
import org.lindev.androkom.gui.TextCreator;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Show a list of all conferences with unread texts.
 * 
 * @author henrik
 * 
 */
public class ConferenceList extends ListActivity implements AsyncMessageSubscriber, ServiceConnection {
	public static final String TAG = "Androkom ConferenceList";

	/**
	 * Instantiate activity.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Log.d(TAG, "onCreate");
		// Use a custom layout file
		setContentView(R.layout.main);

        getApp().doBindService(this);

        if (savedInstanceState != null) {
            Log.d(TAG, "Got a bundle");
            restoreBundle(savedInstanceState);
        }
		
		mEmptyView = (TextView) findViewById(android.R.id.empty);
		mAdapter = new ArrayAdapter<String>(this, R.layout.conflistconf);
		setListAdapter(mAdapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
	}

	/**
	 * While activity is active, keep a timer running to periodically refresh
	 * the list of conferences with unread messages.
	 */
	@Override
	public void onResume() {
		super.onResume();

        Log.d(TAG, "onResume");
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				// Must populate list in UI thread.
				runOnUiThread(new Runnable() {
					public void run() {
						new PopulateConferenceTask().execute();
					}

				});
			}

		}, 0, 60000);

	}

	/**
	 * If activity is no longer active, cancel periodic updates.
	 */
	@Override
	public void onPause() {
		super.onPause();
        Log.d(TAG, "onPause");
		mTimer.cancel();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mKom != null) {
            mKom.removeAsyncSubscriber(this);
        }
        getApp().doUnbindService(this);
    }

	/**
	 * Called when a conference has been clicked. Switch to Conference activity,
	 * passing the ID along.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Toast.makeText(getApplicationContext(), ((TextView) v).getText(),
				Toast.LENGTH_SHORT).show();

		activateUser();

		Intent intent = new Intent(this, Conference.class);
		intent.putExtra("conference-id", mConferences.get((int) id).id);
		startActivity(intent);
	}

	/**
	 * Show options menu. Currently does nothing useful.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.conferencelist, menu);
		return true;
	}

	/**
	 * Called when user has selected a menu item from the menu button popup.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		Log.d(TAG, "onOptionsItemSelected");
		activateUser();

		// Handle item selection
		switch (item.getItemId()) {

		case R.id.menu_settings_id:
			Log.d(TAG, "Starting menu");
			startActivity(new Intent(this, ConferencePrefs.class));
			return true;

		case R.id.menu_createnewtext_id:
			intent = new Intent(this, TextCreator.class);
			startActivity(intent);
			return true;

		case R.id.menu_createnewmail_id:
			intent = new Intent(this, TextCreator.class);
			intent.putExtra(TextCreator.INTENT_IS_MAIL, true);
			startActivity(intent);
			return true;

		case R.id.menu_messaging_id:
			intent = new Intent(this, IMConversationList.class);
			startActivity(intent);
			return true;

		case R.id.menu_seewhoison_id:
			seewhoison(1);
			return true;

		case R.id.menu_seefriendsison_id:
			seewhoison(2);
			return true;

		case R.id.menu_endast_id:
			intent = new Intent(this, Endast.class);
			startActivity(intent);
			return true;
			
        case R.id.menu_joinconference_id:
            intent = new Intent(this, JoinConference.class);
            startActivity(intent);
            return true;
            
        case R.id.menu_logout_id:
            mKom.logout();
            Log.i(TAG, "User opted back to login");
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return true;
            
		case R.id.menu_message_log_id:
		    intent = new Intent(this, MessageLog.class);
		    startActivity(intent);
		    return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case android.view.KeyEvent.KEYCODE_X:
			XDialog dialog = new XDialog(this);
			dialog.show();
			return true;
		case android.view.KeyEvent.KEYCODE_Q:
		case 4: // back in emulator
			finish();
		default:
			Log.d(TAG, "onKeyup unknown key:" + keyCode + " " + event);
		}
		return false;
	}

	protected void seewhoison(int type) {
		Intent intent = new Intent(this, WhoIsOn.class);
		intent.putExtra("who_type", type);
		startActivity(intent);
	}

	private class PopulateConferenceTask extends
			AsyncTask<Void, Void, List<ConferenceInfo>> {
		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
		}

		// worker thread (separate from UI thread)
		@Override
		protected List<ConferenceInfo> doInBackground(final Void... args) {
            if((mKom!=null) && (!mKom.isConnected())) {
                mKom.reconnect();
            }
			return fetchConferences();
		}

		@Override
        protected void onPostExecute(final List<ConferenceInfo> fetched) {
            setProgressBarIndeterminateVisibility(false);

            mAdapter.clear();
            mConferences = fetched;

            if (mConferences != null && (!mConferences.isEmpty())) {
                for (ConferenceInfo elem : mConferences) {
                    String str = "(" + elem.numUnread + ") " + elem.name;
                    mAdapter.add(str);
                }

                mAdapter.notifyDataSetChanged();
            } else {
                Log.d(TAG, "populateConferences failed, no Conferences");
                Log.d(TAG, "mConferences is null:" + (mConferences == null));
                if (mConferences != null) {
                    Log.d(TAG, "mConferences is empty:"
                            + mConferences.isEmpty());
                }
                // String currentDateTimeString = new Date().toLocaleString();
                // mEmptyView.setText(getString(R.string.no_unreads) + "\n"
                // + currentDateTimeString + "\n"
                // + getString(R.string.local_time));
                if ((mKom != null) && (mKom.isConnected())) {
                    String currentDateTimeString = null;
                    try {
                        currentDateTimeString = mKom.getServerTime().toString();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        Log.d(TAG, "Populate lost connection");
                        // e.printStackTrace();
                        mKom.logout();
                        mEmptyView.setText("Not connected");
                    }
                    mEmptyView.setText(getString(R.string.no_unreads) + "\n"
                            + currentDateTimeString + "\n"
                            + getString(R.string.server_time));
                } else {
                    mEmptyView.setText("Not connected\nPlease wait");
                }
                if((mKom!=null) && (!mKom.isConnected())) {
                    if(mKom.getUserId()>0) {
                        Log.d(TAG, "Got userid, trying to reconnect");
                        new reconnectTask();
                    } else {
                        Log.d(TAG, "No userid, bailing out");
                        finish();
                    }
                }
            }
		}

	}

	private List<ConferenceInfo> fetchConferences() {
		List<ConferenceInfo> retlist = null;

		try {
			App app = getApp();
			if (app != null) {
				if (mKom != null) {
					if (mKom.isConnected()) {
						retlist = mKom.fetchConferences();
					} else {
						Log.d(TAG, "Can't fetch conferences when no connection");
						mKom.reconnect();
					}
				} else {
				    Log.d(TAG, "mKom==null");
				}
			} else {
			    Log.d(TAG, "app == null");
			}
		} catch (Exception e) {
			Log.d(TAG, "fetchConferences failed:" + e);
			//e.printStackTrace();
			mKom.logout();
		}
		return retlist;
	}

    public void activateUser() {
        new ActivateUserTask().execute();
    }

    /**
     * No need to wait for activate
     * 
     */
    private class ActivateUserTask extends AsyncTask<KomToken, Void, Void> {
        protected void onPreExecute() {
            Log.d(TAG, "LoadMessageTask.onPreExecute");
        }

        // worker thread (separate from UI thread)
        protected Void doInBackground(final KomToken... args) {
            try {
                mKom.activateUser();
            } catch (Exception e1) {
                Log.i(TAG, "Failed to activate user exception:"+e1);
                //e1.printStackTrace();
                mKom.logout();
            }
            return null;
        }
    }

    private class cacheNamesTask extends
            AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "cacheNamesTask 1");
        }

        // worker thread (separate from UI thread)
        @Override
        protected Void doInBackground(final Void... args) {
            try {
                List<ConferenceInfo> pers = mKom.fetchPersons(null, 1);
                if(pers != null) {
                    Log.d(TAG, "cacheNamesTask num persons = " + pers.size());
                } else {
                    Log.d(TAG, "cacheNamesTask num persons = null");                    
                }
            } catch (IOException e) {
                Log.d(TAG, "cacheNamesTask got IOException:" + e);
                //e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute() {
            Log.d(TAG, "cacheNamesTask 2");
        }
    }
    
	App getApp() {
		return (App) getApplication();
	}


    private class reconnectTask extends AsyncTask<Void, Void, Integer> {
        protected void onPreExecute() {
            Log.d(TAG, "starting reconnectTask");
            setProgressBarIndeterminateVisibility(true);
        }

        // worker thread (separate from UI thread)
        @Override
        protected Integer doInBackground(final Void... args) {
            mKom.reconnect();
            return 0;
        }

        protected void onPostExecute(Integer foo) {
            Log.d(TAG, "reconnect done");
        }

    }

	public void asyncMessage(Message msg) {
		if (msg.what == nu.dll.lyskom.Asynch.new_text) {
			Log.d(TAG, "New text created, update unread list");
		
			runOnUiThread(new Runnable() {
				public void run() {					
					new PopulateConferenceTask().execute();
				}
			});
		}
		
	}

    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "ConfList onServiceConnected");
        mKom = ((KomServer.LocalBinder) service).getService();
        mKom.addAsyncSubscriber(this);
        if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
            mKom.setUser(re_userId, re_userPSW, re_server);
        } else {
            if(mKom==null) {
                Log.d(TAG, "mKom == null");
            }
            if(re_userId<1){
                Log.d(TAG, "no userId");
            }
            if(re_userPSW==null){
                Log.d(TAG, "null password");
            } else {
                if(re_userPSW.length()<1){
                    Log.d(TAG, "short password");
                }
            }
        }
        new cacheNamesTask().execute();
    }

	public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "ConfList onServiceDisconnected");
		mKom=null;
	}
	
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Conference onSaveInstanceState");
        super.onSaveInstanceState(outState);

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        if (mKom != null) {
            int userId = mKom.getUserId();
            if (userId > 0) {
                Log.d(TAG, "Store userid:"+userId);
                outState.putInt("UserId", userId);
                outState.putString("UserPSW", mKom.getUserPassword());
                outState.putString("UserServer", mKom.getServer());
            } else {
                Log.d(TAG, "No userid to store");
            }
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
        restoreBundle(savedInstanceState);
    }

    private void restoreBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d(TAG, "ConferenceList restoreBundle got a bundle");
            // Restore UI state from the savedInstanceState.
            // This bundle has also been passed to onCreate.
            re_userId = savedInstanceState.getInt("UserId");
            re_userPSW = savedInstanceState.getString("UserPSW");
            re_server = savedInstanceState.getString("UserServer");
            if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
                mKom.setUser(re_userId, re_userPSW, re_server);
            } else {
                if(mKom==null) {
                    Log.d(TAG, "mKom == null");
                }
                if(re_userId<1){
                    Log.d(TAG, "no userId");
                }
                if(re_userPSW==null){
                    Log.d(TAG, "null password");
                } else {
                    if(re_userPSW.length()<1){
                        Log.d(TAG, "short password");
                    }
                }
            }
        }        
    }

    private List<ConferenceInfo> mConferences;
	private ArrayAdapter<String> mAdapter;
	private Timer mTimer;

	private int re_userId = 0;
    private String re_userPSW = null;
    private String re_server = null;

    TextView mEmptyView;
	KomServer mKom=null;
}

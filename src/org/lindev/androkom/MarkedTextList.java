package org.lindev.androkom;

import java.util.List;
import org.lindev.androkom.KomServer.TextInfo;
import org.lindev.androkom.gui.IMConversationList;
import org.lindev.androkom.gui.MessageLog;
import org.lindev.androkom.gui.TextCreator;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Show a list of marked texts.
 * 
 * 
 */
public class MarkedTextList extends ListActivity implements ServiceConnection {
	public static final String TAG = "Androkom MarkedTextList";

	/**
	 * Instantiate activity.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate");
		// Use a custom layout file
		setContentView(R.layout.main);

        if (savedInstanceState != null) {
            Log.d(TAG, "Got a bundle");
            restoreBundle(savedInstanceState);
        }
		
		mEmptyView = (TextView) findViewById(android.R.id.empty);
		mAdapter = new ArrayAdapter<String>(this, R.layout.conflistconf);
		setListAdapter(mAdapter);

		mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                consumeMessage(msg);
            }
        };
        
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		getApp().doBindService(this);

		context = this;
	}

	/**
	 * Repopulate list when resumed.
	 * 
	 */
	@Override
	public void onResume() {
		super.onResume();

        Log.d(TAG, "onResume");

	}

	/**
	 * If activity is no longer active, cancel periodic updates.
	 */
	@Override
	public void onPause() {
		super.onPause();
        Log.d(TAG, "onPause");
	}

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        getApp().doUnbindService(this);
        super.onDestroy();
    }

	/**
	 * Called when a text has been clicked. Switch to Conference activity,
	 * passing the ID along.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Toast.makeText(getApplicationContext(), ((TextView) v).getText(),
				Toast.LENGTH_SHORT).show();

		activateUser();
		
        mKom.setCurrentTextList(mTexts);
        
        Intent intent = new Intent(this, Conference.class);
        intent.putExtra("conference-id", mConfNo);
        intent.putExtra("textListIndex", (int)id);
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


    void updateView(List<TextInfo> fetched) {
        mAdapter.clear();
        mTexts = fetched;

        if (mTexts != null && (!mTexts.isEmpty())) {
            for (TextInfo elem : mTexts) {
                String str = elem.getDate() + " " + elem.getAuthor() + "\n"
                        + elem.getSubject();
                mAdapter.add(str);
            }

            mAdapter.notifyDataSetChanged();
        } else {
            Log.d(TAG, "populateMarkedTexts failed, no Texts");
            Log.d(TAG, "mConferences is null:" + (mTexts == null));
            if (mTexts != null) {
                Log.d(TAG, "mConferences is empty:" + mTexts.isEmpty());
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
                    mEmptyView.setText(getString(R.string.not_connected));
                }
                mEmptyView.setText(getString(R.string.no_unreads) + "\n"
                        + currentDateTimeString + "\n"
                        + getString(R.string.server_time));
            } else {
                mEmptyView.setText(getString(R.string.not_connected));
            }
            if ((mKom != null) && (!mKom.isConnected())) {
                if (mKom.getUserId() > 0) {
                    Log.d(TAG, "Got userid, trying to reconnect");
                    new reconnectTask();
                } else {
                    Log.d(TAG, "No userid, bailing out");
                    finish();
                }
            }
        }
    }

    private List<TextInfo> fetchTextHeaders() {
		List<TextInfo> retlist = null;

		try {
			App app = getApp();
			if (app != null) {
				if (mKom != null) {
					if (mKom.isConnected()) {
						retlist = mKom.getMarkedTexts(this);
					} else {
						Log.d(TAG, "Can't fetch conferences when no connection");
					}
				} else {
				    Log.d(TAG, "mKom==null");
				}
			} else {
			    Log.d(TAG, "app == null");
			}
		} catch (Exception e) {
			Log.d(TAG, "fetchConferences failed:" + e);
			e.printStackTrace();
			//mKom.logout();
		}
		return retlist;
	}

    protected void consumeMessage(Message msg) {
        final TextInfo text;
        switch (msg.what) {

        case Consts.MESSAGE_TYPE_ACTIVATEUSER:
            try {
                mKom.activateUser();
            } catch (Exception e1) {
                Log.i(TAG, "Failed to activate user exception:" + e1);
                // e1.printStackTrace();
                mKom.logout();
            }
            break;

        case Consts.MESSAGE_TYPE_POPULATE_MARKEDTEXTSLIST:
            runOnUiThread(new Runnable() {
                public void run() {
                    progressBar = new ProgressDialog(context);
                    progressBar.setMessage("Loading texts");
                    progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressBar.show();
                }
            });

            Thread backgroundThread = new Thread(new Runnable() {
                public void run() {
                    final List<TextInfo> headers = fetchTextHeaders();
                    if(headers != null) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                updateView(headers);
                                progressBar.dismiss();
                            }
                        });
                    }
                }
            });
            backgroundThread.start();
            break;
        default:
            Log.d(TAG, "consumeMessage ERROR unknown msg.what=" + msg.what);
            return;
        }
    }

    public void setPBMax(final int size) {
        runOnUiThread(new Runnable() {
            public void run() {
                progressBar.setMax(size);
            }
        });
    }

    public void setPBprogress(final int size) {
        runOnUiThread(new Runnable() {
            public void run() {
                progressBar.setProgress(size);
            }
        });
    }


    public void activateUser() {
        Message msgout = new Message();
        msgout.what = Consts.MESSAGE_TYPE_ACTIVATEUSER;
        mHandler.sendMessage(msgout);
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
            //mKom.reconnect();
            return 0;
        }

        protected void onPostExecute(Integer foo) {
            Log.d(TAG, "reconnect done");
        }

    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "ConfList onServiceConnected");
        mKom = ((LocalBinder<KomServer>) service).getService();
        if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
            mKom.setUser(re_userId, re_userPSW, re_server, re_port, re_useSSL, re_cert_level);
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
        //new populateSeeAgainTextsTask().execute();
        Message msg = new Message();
        msg.what = Consts.MESSAGE_TYPE_POPULATE_MARKEDTEXTSLIST;
        mHandler.sendMessage(msg);
    }

	public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "ConfList onServiceDisconnected");
		mKom=null;
	}
	
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        if ((re_userId > 0) && (re_userPSW != null)
                && (re_userPSW.length() > 0)) {
            outState.putInt("UserId", re_userId);
            outState.putString("UserPSW", re_userPSW);
            outState.putString("UserServer", re_server);
            outState.putInt("UserServerPortNo", re_port);
            outState.putBoolean("UserUseSSL", re_useSSL);
            outState.putInt("UserCertLevel", re_cert_level);
        } else {
            if (mKom != null) {
                int userId = mKom.getUserId();
                if (userId > 0) {
                    Log.d(TAG, "Store userid:" + userId);
                    outState.putInt("UserId", userId);
                    outState.putString("UserPSW", mKom.getUserPassword());
                    outState.putString("UserServer", mKom.getServer());
                    outState.putInt("UserServerPortNo", mKom.getServerPortNo());
                    outState.putBoolean("UserUseSSL", mKom.getUseSSL());
                    outState.putInt("UserCertLevel", mKom.getCertLevel());
                } else {
                    Log.d(TAG, "No userid to store");
                }
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
            re_port = savedInstanceState.getInt("UserServerPortNo");
            re_useSSL = savedInstanceState.getBoolean("UserUseSSL");
            re_cert_level = savedInstanceState.getInt("UserCertLevel");
            if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
                mKom.setUser(re_userId, re_userPSW, re_server, re_port, re_useSSL, re_cert_level);
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

    Context context=null;
    ProgressDialog progressBar;
    
    private List<TextInfo> mTexts;
	private ArrayAdapter<String> mAdapter;
	private int mConfNo;
	
	private int re_userId = 0;
    private String re_userPSW = null;
    private String re_server = null;
    private int re_port=0; // for reconnect
    private boolean re_useSSL=true; // for reconnect
    private int re_cert_level=0; // for reconnect

    TextView mEmptyView;
    private static Handler mHandler=null;
    KomServer mKom=null;
}

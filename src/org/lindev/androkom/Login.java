package org.lindev.androkom;

import nu.dll.lyskom.RpcFailure;

import org.lindev.androkom.gui.IMConversationList;
import org.lindev.androkom.gui.ImgTextCreator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Login dialog. Shows username and password text editors, 
 * and a button to log in. Will save username and password 
 * for the next session.
 * 
 * @author henrik
 *
 */
public class Login extends Activity implements ServiceConnection
{
	public static final String TAG = "Androkom Login";
	private boolean loginFailed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // Use this when bumping to SdkVersion to 9
        if(!KomServer.RELEASE_BUILD) {
         // Activate StrictMode
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                //.detectAll()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork() 
                 // alternatively .detectAll() for all detectable problems
                .penaltyLog()
                //.penaltyDeath()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                 .detectLeakedSqlLiteObjects()
                // .detectLeakedClosableObjects()  // API-level 11
                // alternatively .detectAll() for all detectable problems
                 //.detectAll()
                .penaltyLog()
                //.penaltyDeath()
                .build());
        }
       
        setContentView(R.layout.login);

        // if this is from the share menu
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                // Get resource path from intent callee
                share_uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                Log.d(TAG, "Called from Share");
            }
        } else if (Intent.ACTION_SENDTO.equals(action)) {
            String recipdata = intent.getData().getSchemeSpecificPart();
            sendto_im_recipient = recipdata.replaceFirst("//[lL]yskom/", "");
            Log.d(TAG, "ACTION_SENDTO "+sendto_im_recipient);
        } else if (getApp().getUsers() > 0) {
            Log.d(TAG, "onCreate Too many users (" + getApp().getUsers()
                    + "). Starting ConfList");
            if (!KomServer.RELEASE_BUILD) {
                Toast.makeText(this, "Already logged in: "+getApp().getUsers(), Toast.LENGTH_SHORT)
                        .show();
            }
            //getApp().resetUsers();
            Intent clintent = new Intent(this, ConferenceList.class);
            startActivity(clintent);
            finish();
            return;
        }

        getApp().doBindService(this);

        titleWidget = (TextView) findViewById(R.id.akl_tw_title);
        
        mUsername = (EditText) findViewById(R.id.akl_username);
        mPassword = (EditText) findViewById(R.id.akl_password);

        Button loginButton = (Button) findViewById(R.id.akl_login);

        getPrefs();

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doLogin(); }
        });        
        
        try {
            Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memoryInfo);

            String memMessage = String.format(
                    "Memory: Pss=%.2f MB, Private=%.2f MB, Shared=%.2f MB",
                    memoryInfo.getTotalPss() / 1024.0,
                    memoryInfo.getTotalPrivateDirty() / 1024.0,
                    memoryInfo.getTotalSharedDirty() / 1024.0);
            Log.d(TAG, memMessage);
            double max = Runtime.getRuntime().maxMemory();
            Log.d(TAG, "max memory:" + max);
        } catch (Exception e) {
            Log.d(TAG, "Catched exception " + e);
            e.printStackTrace();
        }
        Log.d(TAG, "onCreate done");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState(Bundle savedInstanceState)");
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mHandler = new CustomHandler(this);
        
        Log.d(TAG, "onResume done");
    }

    @Override
	protected void onDestroy() {
        super.onDestroy();
	    Log.d(TAG, "onDestroy");
        getApp().doUnbindService(this);
	}

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory");
    }

    @Override
    public void finish() {
        super.finish();
        Log.d(TAG, "finish");
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "onWindowFocusChanged: " + hasFocus);

        if (hasFocus) {
            activityVisible = true;

            Log.d(TAG, "onWindowFocusChanged Checking prefs");
            // autologin
            if (Prefs.getAutologin(this) && !(Prefs.getUseOISafe(this))
                    && (!loginFailed) && (getPsw().length() > 0)) {
                Log.d(TAG, "onWindowFocusChanged do auto login");
                doLogin();
            } else {
                if ((mHandler != null) && (getPsw().length() > 0)) {
                    Message msg = new Message();
                    msg.what = Consts.MESSAGE_SET_LOGIN_FOCUS;
                    mHandler.sendMessage(msg);
                }
            }
        }
        Log.d(TAG, "onWindowFocusChanged done");
    }

    @Override
	protected void onPause() {	    
	    super.onPause();
        Log.d(TAG, "onPause");
	    activityVisible = false;
	}

    @Override
	protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        activityVisible = false;
        
        Log.d(TAG, "onStop  getChangingConfigurations: "+ getChangingConfigurations());
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed()");
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
    }
    
    void otherAction() {
        if((share_uri != null) && (mKom!=null) && (mKom.isConnected())) {
            Intent img_intent = new Intent(Login.this, ImgTextCreator.class);
            img_intent.putExtra("bild_uri", share_uri.toString());
            img_intent.putExtra("BitmapImage", (Bitmap)null);
            startActivity(img_intent);
            finish();
        }        
        if((sendto_im_recipient != null) && (mKom!=null) && (mKom.isConnected())) {
            Intent intent = new Intent(this, IMConversationList.class);
            intent.putExtra(Consts.INTENT_CONVERSATION_LIST_RECIPIENT, sendto_im_recipient);
            startActivity(intent);
            finish();
        }        
    }

    private void getPrefs() {
        Log.d(TAG, "getPrefs");
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                Log.d(TAG, "getPrefs BG");

                SharedPreferences prefs = getPreferences(MODE_PRIVATE);

                mUsername.setText(prefs.getString("username", ""));
                mPassword.setText(getPsw());

                Log.d(TAG, "getPrefs BG done");
            }
        });
        backgroundThread.start();

        Log.d(TAG, "getPrefs done");
    }
    
    private String getPsw() {
    	String password;
    	
        Log.d(TAG, "getPsw Checking prefs");
    	if(Prefs.getUseOISafe(this)) {
    		Intent i = new Intent();
    		i.setAction("org.openintents.action.GET_PASSWORD");
    		i.putExtra("org.openintents.extra.UNIQUE_NAME", "AndroKom");
    		i.putExtra("org.openintents.extra.UNIQUE_NAME", "AndroKom");
    		try {
    			startActivityForResult(i, 17);
    		} catch (ActivityNotFoundException e) {
    			//Toast.makeText(this,
    			//		getString(R.string.error_oisafe_not_found),
    			//		Toast.LENGTH_LONG).show();
    			Log.e(TAG, "failed to get password from OISafe(1)");
    		} catch (java.lang.SecurityException e) {
                Toast.makeText(this,
                        getString(R.string.error_oisafe_not_error),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "failed to get password from OISafe(2)");
    		}
    		Log.d(TAG, "Finished activity for result");
    		password = "";
    	} else {
    		Log.d(TAG, "GET PREFS PASSWORD");
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        	password = prefs.getString("password", "");
    	}
    	return password;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode != RESULT_CANCELED) {
    		mUsername.setText(data.getStringExtra("org.openintents.extra.USERNAME"));
    		mPassword.setText(data.getStringExtra("org.openintents.extra.PASSWORD"));
            Log.d(TAG, "onActivityResult Checking prefs");
    		if(Prefs.getAutologin(this)) {
    			doLogin();
    		}
    	} else {
    		Log.d(TAG, "onActivityResult: no result");
    	}
    }
    
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_settings_id :
			startActivity(new Intent(this, Prefs.class));
			return true;
		case R.id.menu_clearpsw_id :
            doClearPsw();
            return true;
		case R.id.save_oisafe_psw_id:
    		savePsw(mUsername.getText().toString(), mPassword.getText().toString());
    		return true;
		default:
				Log.d(TAG, "Unknown menu selected");
		}
		return false;
	}

    /**
     * Attempt to log in to server. If unsuccessful, show an 
     * alert. Otherwise save username and password for successive sessions.
     */
    private class LoginTask extends AsyncTask<Void, Integer, String> {
        private ProgressDialog dialog = null;

        String username;
        String password;

        protected void onPreExecute() {
            Log.d(TAG, "LoginTask onPreExecute 1");
            try {
                dialog = new ProgressDialog(Login.this);
                
                this.dialog.setCancelable(true);
                this.dialog.setIndeterminate(true);
                this.dialog.setMessage("Logging in...");
                this.dialog.show();

                this.username = mUsername.getText().toString().trim();
                this.password = mPassword.getText().toString();
            } catch (BadTokenException e) {
                Log.d(TAG, "LoginTask onPreExecute caught BadTokenException: "
                        + e);
                e.printStackTrace();
                finish();
            }
            Log.d(TAG, "LoginTask onPreExecute 2");
        }

        protected String doInBackground(final Void... args) 
        {
            Log.d(TAG, "LoginTask doInBackground 1");
            if( (username == null) ||
                    (password == null) ||
                    (username.length()<1) ||
                    (password.length() < 1)) {
                return "Username and password needed";
            }
            Log.d(TAG, "LoginTask doInBackground 2");
            
            //Context context = getBaseContext();
            Context context = getApp();
			String server = Prefs.getServer(context);
			int port = Prefs.getPortno(context);
			boolean useSSL = Prefs.getUseSSL(context);
			int cert_level = Prefs.getCertLevel(context);
        	if(server.equals("@")) {
            	server = Prefs.getOtherServer(context);        	
        	}
        	Log.d(TAG, "LoginTask Connecting to "+server);
        	if(server.length()>0) {
        		if(selectedUser>0) {
                    Log.d(TAG, "LoginTask login userid");
                    mKom.reconnect();
        			String msg = "Login failed 1 (default)";
                    try {
                        msg = mKom.login(selectedUser, password, server, port, useSSL, cert_level);
                    } catch (InterruptedException e) {
                        msg = "Login failed 1 (Interrupted)";
                        e.printStackTrace();
                        if(mKom != null) {
                            try {
                                mKom.logout();
                            } catch (InterruptedException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
                    }
        			selectedUser=0;
                    Log.d(TAG, "LoginTask logged in userid");
            		return msg;
        		} else {
        		    String result = "Login failed 2 (default)";
                    Log.d(TAG, "LoginTask login username");
        		    
        		    try {
                        mKom.reconnect();
        		        result = mKom.login(username, password, server, port, useSSL, cert_level);
        		    }
        		    catch(NullPointerException e) {
        		        result = "Login failed 3 (nullpointer)";
                        if(mKom != null) {
                            try {
                                mKom.logout();
                            } catch (InterruptedException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
        		    } catch (RpcFailure e) {
                        result = "Login failed 4 (RpcFailure)";
                        e.printStackTrace();
                        if(mKom != null) {
                            try {
                                mKom.logout();
                            } catch (InterruptedException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
                    } catch (InterruptedException e) {
                        result = "Login failed 5 (Interrupted)";
                        e.printStackTrace();
                        if(mKom != null) {
                            try {
                                mKom.logout();
                            } catch (InterruptedException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
                    }
                    Log.d(TAG, "LoginTask logged in userid?");
            		return result;
        		}
        	}
       		return getString(R.string.No_server_selected);
        }

        protected void onPostExecute(final String result) {
            Log.d(TAG, "LoginTask onPostExecute 1");
            try {
                this.dialog.dismiss();
            } catch (Exception e) {
                Log.d(TAG, "Failed to dismiss dialog " + e);
            }
            if (mKom == null) {
                try {
                    // Got no komserver
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            Login.this);
                    builder.setMessage("No connection process.")
                            .setCancelable(false)
                            .setNegativeButton(
                                    getString(R.string.alert_dialog_ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                    loginFailed = true;
                } catch (Exception e) {
                    Log.d(TAG, "Something bad happened:" + e);
                    finish();
                }
                Log.d(TAG, "LoginTask onPostExecute 2");
                return;
            }

            if (result.length() > 0) {
                // Login failed, check why
                final ConferenceInfo[] users = mKom.getUserNames();
                if (users != null && users.length > 1) {
                    // Ambiguous name
                    selectedUser = 0;
                    // Check for exact match
                    for (ConferenceInfo user : users) {
                        if ((username != null) && (user != null)
                                && (user.name != null)) {
                            if (user.name.compareToIgnoreCase(username) == 0) {
                                Log.d(TAG, "Exact username found, id: "
                                        + user.id);
                                selectedUser = user.id;
                                // Restart task by message
                                Message msg = new Message();
                                msg.obj = selectedUser;
                                msg.what = 1;
                                mHandler.sendMessage(msg);
                            }
                        }
                    }
                    if (selectedUser == 0) {
                        // Exact match not found
                        // Go back to GUI, let user select and then
                        // restart task
                        Message msg = new Message();
                        msg.obj = users;
                        msg.what = 2;
                        mHandler.sendMessage(msg);
                    }
                } else {
                    try {
                        // User not found or such error
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                Login.this);
                        builder.setMessage(result)
                                .setCancelable(false)
                                .setNegativeButton(
                                        getString(R.string.alert_dialog_ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int id) {
                                                dialog.cancel();
                                            }
                                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                        loginFailed = true;
                    } catch (Exception e) {
                        Log.d(TAG, "Something bad happened:" + e);
                        finish();
                    }
                }
            } else {
                Log.d(TAG, "will store password");
                // Login succeded: Store psw, start new activity and kill this.
                SharedPreferences settings = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username", username);

                // Context context = getBaseContext();
                Context context = getApp();
                if (Prefs.getSavePsw(context)) {
                    if (Prefs.getUseOISafe(context)) {
                        // Can't work with OISafe here
                    } else {
                        editor.putString("password", password);
                    }
                }

                // Commit the edits!
                editor.commit();

                if (share_uri != null) {
                    Message msg = new Message();
                    msg.what = Consts.MESSAGE_INTENT_IMGTEXTCREATOR;
                    mHandler.sendMessage(msg);
                } else if (sendto_im_recipient != null) {
                    Message msg = new Message();
                    msg.what = Consts.MESSAGE_INTENT_SENDTOIM;
                    mHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = Consts.MESSAGE_INTENT_CONFLIST;
                    mHandler.sendMessage(msg);
                }
            }
            Log.d(TAG, "LoginTask onPostExecute 3");
        }
    }

    private void doLogin()
    {
        new LoginTask().execute();
    }

    protected void consumeLoginMessage(Message msg) {
        Intent intent;
        //Context context = getBaseContext();
        final Context context = this;
        
        switch (msg.what) {
        case 1:  // Name resolved, just try login again
            doLogin();
            break;
            
        case 2: // Ambiguous name, resolve it
            final ConferenceInfo[] users = (ConferenceInfo[]) msg.obj;

            if (activityVisible) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        Login.this);
                builder.setTitle(getString(R.string.pick_a_name));
                String[] vals = new String[users.length];
                for (int i = 0; i < users.length; i++)
                    vals[i] = users[i].name;
                builder.setSingleChoiceItems(vals, -1,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                Toast.makeText(context,
                                        users[item].name, Toast.LENGTH_SHORT)
                                        .show();
                                dialog.cancel();
                                selectedUser = users[item].id;
                                Log.d(TAG, "Selected user:" + selectedUser
                                        + ":" + new String(users[item].name));
                                doLogin();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
            break;
            
        case Consts.MESSAGE_INTENT_CONFLIST:
            intent = new Intent(context, ConferenceList.class);
            startActivity(intent);
            finish();
            break;

        case Consts.MESSAGE_INTENT_IMGTEXTCREATOR:
            intent = new Intent(context, ImgTextCreator.class);
            intent.putExtra("bild_uri", share_uri.toString());
            startActivity(intent);
            finish();
            break;

        case Consts.MESSAGE_INTENT_SENDTOIM:
            intent = new Intent(this, IMConversationList.class);
            intent.putExtra(Consts.INTENT_CONVERSATION_LIST_RECIPIENT, sendto_im_recipient);
            startActivity(intent);
            finish();
            break;

        case Consts.MESSAGE_SET_LOGIN_FOCUS:
            Log.d(TAG, "consumeLoginMessage Hide keyboard");
            
            InputMethodManager inputMethodManager = (InputMethodManager)  this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if(inputMethodManager != null) {
                //View focus = this.getCurrentFocus();
                View focus = getWindow().getDecorView().getRootView();
                if(focus != null) {
                    inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                } else {
                    Log.d(TAG, "consumeLoginMessage No view");
                }
            } else {
                Log.d(TAG, "consumeLoginMessage No inputmgr");
            }
            
            break;
            
        default:
            Log.d(TAG, "consumeLoginMessage ERROR unknown msg.what=" + msg.what);
        }
    }

    private void doClearPsw()
    {
        Log.d(TAG, "doClearPsw");
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("password");
        editor.commit();

        if (Prefs.getUseOISafe(this)) {
    		Intent i = new Intent();
    		i.putExtra("org.openintents.extra.UNIQUE_NAME", "AndroKom");
    		i.putExtra("org.openintents.extra.USERNAME", "");
    		i.putExtra("org.openintents.extra.PASSWORD", "");
    		i.setAction("org.openintents.action.SET_PASSWORD");
    		try {
    			startActivityForResult(i, 17);
    		} catch (ActivityNotFoundException e) {
    			Toast.makeText(this,
    			        getString(R.string.error_oisafe_not_found),
    					Toast.LENGTH_LONG).show();
    			Log.e(TAG, "failed to store password in OISafe");
            } catch (java.lang.SecurityException e) {
                Toast.makeText(this,
                        getString(R.string.error_oisafe_not_error),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "failed to store password in OISafe(2)");
            }
    		Log.d(TAG, "password cleared in OISafe");        	
        }
        
        mPassword.setText("");
    }
    
    private void savePsw(String username, String password) {
		Log.d(TAG, "Trying to store password in OISafe");
		Intent i = new Intent();
		i.putExtra("org.openintents.extra.UNIQUE_NAME", "AndroKom");
		i.putExtra("org.openintents.extra.USERNAME", username);
		i.putExtra("org.openintents.extra.PASSWORD", password);
		i.setAction("org.openintents.action.SET_PASSWORD");
		try {
			startActivityForResult(i, 17);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this,
			        getString(R.string.error_oisafe_not_found),
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "failed to store password in OISafe");
		}
		Log.d(TAG, "password stored in OISafe");
    }

    App getApp() 
    {
        return (App)getApplication();
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected");
        try {
            mKom = ((LocalBinder<KomServer>) service).getService();

            String rawTitle = getString(R.string.login_title);
            String fs = String.format(rawTitle,
                               ""+mKom.getVersionName()+" ("+mKom.getVersionCode()+")");
            titleWidget.setText(Html.fromHtml(fs));

            dumpLog();
            otherAction();
        } catch (Exception e) {
            Log.d(TAG, "Exception: " + e);
            e.printStackTrace();
        }
        Log.d(TAG, "onServiceConnected done");
    }

    void dumpLog() {
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                mKom.dumpLog();
            }
        });
        backgroundThread.start();
    }
    
	public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "onServiceDisconnected");
		mKom = null;		
	}
	
    private static class CustomHandler extends Handler {
        private Login activity;

        public CustomHandler(Login activity) {
            super();
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                activity.consumeLoginMessage(msg);
            } catch (Exception e) {
                Log.d(TAG, "handleMessage failed to consume:"+e);
                e.printStackTrace();
            }
        }
    }

    private int selectedUser=0;
    private EditText mUsername;
    private EditText mPassword;	
    private TextView titleWidget;
	private KomServer mKom = null;
	private Uri share_uri=null;
	private String sendto_im_recipient=null;
	private static Handler mHandler=null;
	private boolean activityVisible=false;
}

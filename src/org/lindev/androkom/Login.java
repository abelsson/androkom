package org.lindev.androkom;

import nu.dll.lyskom.RpcFailure;

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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.Button;
import android.widget.EditText;
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
        }

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);

        Button loginButton = (Button) findViewById(R.id.login);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        mUsername.setText(prefs.getString("username", ""));
        mPassword.setText(getPsw());

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                consumeLoginMessage(msg);
            }
        };

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doLogin(); }
        });        
        getApp().doBindService(this);
        
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);

        String memMessage = String.format(
            "Memory: Pss=%.2f MB, Private=%.2f MB, Shared=%.2f MB",
            memoryInfo.getTotalPss() / 1024.0,
            memoryInfo.getTotalPrivateDirty() / 1024.0,
            memoryInfo.getTotalSharedDirty() / 1024.0);
        Log.d(TAG, memMessage);
        double max = Runtime.getRuntime().maxMemory();
        Log.d(TAG, "max memory:"+max);
    }
    
    @Override
	protected void onDestroy() {
	    Log.d(TAG, "onDestroy");
        getApp().doUnbindService(this);
		super.onDestroy();
	}
   
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		Log.d(TAG, "onWindowFocusChanged");

		// autologin
		if (hasFocus && Prefs.getAutologin(getBaseContext())
				&& !(Prefs.getUseOISafe(getBaseContext()))
				&& (!loginFailed)
				&& (getPsw().length() > 0)) {
			doLogin();
		}
	}

    void create_image() {
        if((share_uri != null) && (mKom!=null) && (mKom.isConnected())) {
            Intent img_intent = new Intent(Login.this, ImgTextCreator.class);
            img_intent.putExtra("bild_uri", share_uri.toString());
            img_intent.putExtra("BitmapImage", (Bitmap)null);
            startActivity(img_intent);
            finish();
        }        
    }

    private String getPsw() {
    	String password;
    	
    	if(Prefs.getUseOISafe(getBaseContext())) {
    		Intent i = new Intent();
    		i.setAction("org.openintents.action.GET_PASSWORD");
    		i.putExtra("org.openintents.extra.UNIQUE_NAME", "AndroKom");
    		i.putExtra("org.openintents.extra.UNIQUE_NAME", "AndroKom");
    		try {
    			startActivityForResult(i, 17);
    		} catch (ActivityNotFoundException e) {
    			Toast.makeText(getBaseContext(),
    					getString(R.string.error_oisafe_not_found),
    					Toast.LENGTH_LONG).show();
    			Log.e(TAG, "failed to get password from OISafe(1)");
    		} catch (java.lang.SecurityException e) {
                Toast.makeText(getBaseContext(),
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
    		if(Prefs.getAutologin(getBaseContext())) {
    			doLogin();
    		}
    	} else {
    		Log.d(TAG, "no result");
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
        }

        protected String doInBackground(final Void... args) 
        {
            if( (username == null) ||
                    (password == null) ||
                    (username.length()<1) ||
                    (password.length() < 1)) {
                return "Username and password needed";
            }
            Log.d(TAG, "LoginTask doInBackground ");
			String server = Prefs.getServer(getBaseContext());
			int port = Prefs.getPortno(getBaseContext());
			boolean useSSL = Prefs.getUseSSL(getBaseContext());
			int cert_level = Prefs.getCertLevel(getBaseContext());
        	if(server.equals("@")) {
            	server = Prefs.getOtherServer(getBaseContext());        	
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

        protected void onPostExecute(final String result) 
        { 
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
            }
            else {
            	// Login succeded: Store psw, start new activity and kill this.
                SharedPreferences settings = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username", username);

                Log.d(TAG, "will store password");
                if(Prefs.getSavePsw(getBaseContext())) {
                	if(Prefs.getUseOISafe(getBaseContext())) {
                		//Can't work with OISafe here
                	} else {
                		editor.putString("password", password);
                	}		
                }

                // Commit the edits!
                editor.commit();

                if (share_uri == null) {
                    Message msg = new Message();
                    msg.what = Consts.MESSAGE_INTENT_CONFLIST;
                    mHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = Consts.MESSAGE_INTENT_IMGTEXTCREATOR;
                    mHandler.sendMessage(msg);
                }
            }
        }
    }

    private void doLogin()
    {
        new LoginTask().execute();
    }

    protected void consumeLoginMessage(Message msg) {
        Intent intent;
        Context context = getBaseContext();
        
        switch (msg.what) {
        case 1:  // Name resolved, just try login again
            doLogin();
            break;
            
        case 2: // Ambiguous name, resolve it
            final ConferenceInfo[] users = (ConferenceInfo[]) msg.obj;

            AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
            builder.setTitle(getString(R.string.pick_a_name));
            String[] vals = new String[users.length];
            for (int i = 0; i < users.length; i++)
                vals[i] = users[i].name;
            builder.setSingleChoiceItems(vals, -1,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            Toast.makeText(getApplicationContext(),
                                    users[item].name, Toast.LENGTH_SHORT)
                                    .show();
                            dialog.cancel();
                            selectedUser = users[item].id;
                            Log.d(TAG, "Selected user:" + selectedUser + ":"
                                    + new String(users[item].name));
                            doLogin();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            break;
            
        case Consts.MESSAGE_INTENT_CONFLIST:
            //Context context = Login.this;
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
        default:
            Log.d(TAG, "consumeLoginMessage ERROR unknown msg.what=" + msg.what);
        }
    }

    private void doClearPsw()
    {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("password");
        editor.commit();

        if (Prefs.getUseOISafe(getBaseContext())) {
    		Intent i = new Intent();
    		i.putExtra("org.openintents.extra.UNIQUE_NAME", "AndroKom");
    		i.putExtra("org.openintents.extra.USERNAME", "");
    		i.putExtra("org.openintents.extra.PASSWORD", "");
    		i.setAction("org.openintents.action.SET_PASSWORD");
    		try {
    			startActivityForResult(i, 17);
    		} catch (ActivityNotFoundException e) {
    			Toast.makeText(getBaseContext(),
    			        getString(R.string.error_oisafe_not_found),
    					Toast.LENGTH_LONG).show();
    			Log.e(TAG, "failed to store password in OISafe");
            } catch (java.lang.SecurityException e) {
                Toast.makeText(getBaseContext(),
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
			Toast.makeText(getBaseContext(),
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
        try {
            mKom = ((LocalBinder<KomServer>) service).getService();
            mKom.dumpLog();
            create_image();
        } catch (Exception e) {
            Log.d(TAG, "Exception: " + e);
            e.printStackTrace();
        }
    }

	public void onServiceDisconnected(ComponentName name) {
		mKom = null;		
	}
	
    private int selectedUser=0;
    private EditText mUsername;
    private EditText mPassword;	
	private KomServer mKom = null;
	private Uri share_uri=null;
	private static Handler mHandler=null;
}

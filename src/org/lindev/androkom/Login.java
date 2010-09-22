package org.lindev.androkom;

import org.lysator.lattekom.ConfInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
public class Login extends Activity 
{
	public static final String TAG = "Androkom Login";
	private boolean loginFailed = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        getApp().doBindService();

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);

        Button loginButton = (Button) findViewById(R.id.login);

        SharedPreferences prefs =  getPreferences(MODE_PRIVATE);

        mUsername.setText(prefs.getString("username", ""));
        mPassword.setText(getPsw());

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doLogin(); }
        });
    }

    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
        
        if (hasFocus && Prefs.getAutologin(getBaseContext())
        		&& !(Prefs.getUseOISafe(getBaseContext()))
        		&& (!loginFailed)
    			&& (getPsw().length()>0)) {
        	doLogin();
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
    					"OISafe not found",
    					Toast.LENGTH_LONG).show();
    			Log.e(TAG, "failed to store password in OISafe");
    		}
    		Log.d(TAG, "Finished activity for result");
    		password = "";
    	} else {
    		Log.d(TAG, "GET PREFS PASSWORD");
            SharedPreferences prefs =  getPreferences(MODE_PRIVATE);

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
     * Attempt to log in to "kom.lysator.liu.se". If unsuccessful, show an 
     * alert. Otherwise save username and password for successive sessions.
     */
    private class LoginTask extends AsyncTask<Void, Integer, String> {
        private final ProgressDialog dialog = new ProgressDialog(Login.this);

        String username;
        String password;

        protected void onPreExecute() {
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(true);
            this.dialog.setMessage("Logging in...");
            this.dialog.show();

            this.username = mUsername.getText().toString();
            this.password = mPassword.getText().toString();
        }

        protected String doInBackground(final Void... args) 
        {
        	String server = Prefs.getServer(getBaseContext());
        	if(server.equals("@")) {
            	server = Prefs.getOtherServer(getBaseContext());        	
        	}
        	Log.d(TAG, "Connecting to "+server);
        	if(server.length()>0) {
        		if(selectedUser>0) {
        			String msg = getApp().getKom().login(selectedUser, password, server);
        			selectedUser=0;
            		return msg;
        		} else {
            		return getApp().getKom().login(username, password, server);
        		}
        	}
       		return getString(R.string.No_server_selected);
        }

        protected void onPostExecute(final String result) 
        { 
            this.dialog.dismiss();
                       
            if (result.length() > 0) {
            	// Login failed, check why
            	users = getApp().getKom().getUserNames();
            	if (users != null) {
            		if (users.length > 1) {
            			Log.d(TAG, "Ambigous name");
            			final CharSequence[] items = new CharSequence[users.length];
            			for(int i=0; i <users.length; i++) {
            				items[i]=new String(users[i].confName);
            				Log.d(TAG, "Name "+i+":"+items[i]);
            			}
            			AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
            			builder.setTitle("Pick a name");
            			builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            				public void onClick(DialogInterface dialog, int item) {
            					Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
            					dialog.cancel();
            					selectedUser = users[item].confNo;
            					Log.d(TAG, "Selected user:"+selectedUser+":"+new String(users[item].confName));
            					doLogin();
            				}
            			});
            			AlertDialog alert = builder.create();
            			alert.show();
            		}
            	} else {
            		AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
            		builder.setMessage(result)
            		.setCancelable(false)
            		.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
            			public void onClick(DialogInterface dialog, int id) {
            				dialog.cancel();
            			}
            		});
            		AlertDialog alert = builder.create();
            		alert.show();
            		loginFailed = true;
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

                Intent intent = new Intent(Login.this, ConferenceList.class);
                startActivity(intent);
                finish();
            }
        }
    }

    private void doLogin()
    {
        new LoginTask().execute();
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
    					"OISafe not found",
    					Toast.LENGTH_LONG).show();
    			Log.e(TAG, "failed to store password in OISafe");
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
					"OISafe not found",
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "failed to store password in OISafe");
		}
		Log.d(TAG, "password stored in OISafe");
    }

    App getApp() 
    {
        return (App)getApplication();
    }
    private int selectedUser=0;
    private ConfInfo[] users;
    private EditText mUsername;
    private EditText mPassword;
}

package org.lindev.androkom;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
        mPassword.setText(prefs.getString("password", ""));

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doLogin(); }
        });
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
            return getApp().getKom().login(username, password, "kom.lysator.liu.se");                
        }

        protected void onPostExecute(final String result) 
        { 
            this.dialog.dismiss();
                       
            if (result.length() > 0) {
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
            }
            else {

                SharedPreferences settings = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username", username);
                editor.putString("password", password);

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


    App getApp() 
    {
        return (App)getApplication();
    }
    private EditText mUsername;
    private EditText mPassword;
}

package org.lindev.androkom;

import org.lysator.lattekom.ConfInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity showing a dialog in which the user can create 
 * new texts. Currently assumes that each text is in reply 
 * to another text.
 * 
 */
public class CreateNewText extends Activity 
{

    /**
     * Create activity. Just a plain old dialog with
     * a subject, body and cancel and post buttons.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.createnewtext);

        mRecipient = (EditText) findViewById(R.id.recipient);
        mSubject = (EditText) findViewById(R.id.subject);
        mBody = (EditText) findViewById(R.id.body);

        Button confirmButton = (Button) findViewById(R.id.send);
        Button cancelButton = (Button) findViewById(R.id.cancel);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doCreateText(); }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { cancelText(); }
        });
    }

    /**
     * Request a new message be posted.
     */
    /*
    private void createText()
    {
        String recipient = mRecipient.getText().toString();
        String subject = mSubject.getText().toString();
        String body = mBody.getText().toString();
        ConfInfo[] conferences = getApp().getKom().createText(recipient, subject, body);
        if (conferences == null) {
        	Toast.makeText(getBaseContext(),
        			"Could not create text",
        			Toast.LENGTH_LONG)
        			.show();
			Log.e(TAG, "failed to create new text (null)");
        } else {
        	if (conferences.length != 1) {
    			Log.d(TAG, "Ambigous name");
    			final CharSequence[] items = new CharSequence[conferences.length];
    			for(int i=0; i <conferences.length; i++) {
    				items[i]=new String(conferences[i].getNameString());
    				Log.d(TAG, "Name "+i+":"+items[i]);
    			}
    			AlertDialog.Builder builder = new AlertDialog.Builder(CreateNewText.this);
    			builder.setTitle("Pick a name");
    			builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int item) {
    					Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    					dialog.cancel();
    					selectedConference = conferences[item].confNo;
    					Log.d(TAG, "Selected user:"+selectedConference+":"+new String(conferences[item].getNameString()));
    					doLogin();
    				}
    			});
    			AlertDialog alert = builder.create();
    			alert.show();
        	} else {
            	Toast.makeText(getBaseContext(),
            			"Created text",
            			Toast.LENGTH_SHORT)
            			.show();
        	}
        }
        finish();
    }
*/
    /**
     * Attempt to create text
     */
    private class CreateTextTask extends AsyncTask<Void, Integer, String> {
        private final ProgressDialog dialog = new ProgressDialog(CreateNewText.this);

        String recipient;
        String subject;
        String textbody;
        ConfInfo[] conferences = null;
        
        protected void onPreExecute() {
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(true);
            this.dialog.setMessage("Creating text...");
            this.dialog.show();

            this.recipient = mRecipient.getText().toString();
            this.subject = mSubject.getText().toString();
            this.textbody = mBody.getText().toString();
        }

        protected String doInBackground(final Void... args) 
        {
        	Log.d(TAG, "Trying to create text ");
        	if (recipientNo == 0) {
        		Log.d(TAG, "Create text using string");
        		conferences = getApp().getKom().createText(recipient, subject, textbody);
        		return "fail";
        	} else {
        		Log.d(TAG, "Create text using id");
        		getApp().getKom().createText(recipientNo, subject, textbody);
        		return ""; //TODO: check for fail
        	}
        }

        protected void onPostExecute(final String result) 
        { 
            this.dialog.dismiss();

            if (result.length() > 0) {
            	// Login failed, check why
            	if (conferences != null) {
            		if (conferences.length > 1) {
            			Log.d(TAG, "Ambigous name");
            			final CharSequence[] items = new CharSequence[conferences.length];
            			for(int i=0; i <conferences.length; i++) {
            				items[i]=new String(conferences[i].getNameString());
            				Log.d(TAG, "Name "+i+":"+items[i]);
            			}
            			AlertDialog.Builder builder = new AlertDialog.Builder(CreateNewText.this);
            			builder.setTitle("Pick a name");
            			builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            				public void onClick(DialogInterface dialog, int item) {
            					Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
            					dialog.cancel();
            					recipientNo = conferences[item].confNo;
            					Log.d(TAG, "Selected confNo:"+recipientNo+":"+new String(conferences[item].getNameString()));
            					doCreateText();
            				}
            			});
            			AlertDialog alert = builder.create();
            			alert.show();
            		}
            	} else {
            		AlertDialog.Builder builder = new AlertDialog.Builder(CreateNewText.this);
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
            }
            else {
            	// Create succeeded, just die
                finish();
            }
        }
    }

    /**
     * New text canceled. Just finish up and die.
     */
    private void cancelText()
    {
        finish();
    }

    private void doCreateText()
    {
        new CreateTextTask().execute();
    }

    App getApp() 
    {
        return (App)getApplication();
    }

    private EditText mRecipient;
    private EditText mSubject;
    private EditText mBody;

    int recipientNo=0;

	public static final String TAG = "Androkom CreateNewText";
}

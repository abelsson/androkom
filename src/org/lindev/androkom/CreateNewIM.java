package org.lindev.androkom;

import java.io.IOException;

import nu.dll.lyskom.ConfInfo;
import nu.dll.lyskom.RpcFailure;

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
 * new IM.
 * 
 */
public class CreateNewIM extends Activity 
{

    /**
     * Create activity. Just a plain old dialog with
     * a recipient, body and cancel and post buttons.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.createnew_im);

        mRecipient = (EditText) findViewById(R.id.recipient);
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
     * Attempt to create text
     */
    private class CreateTextTask extends AsyncTask<Void, Integer, String> {
        private final ProgressDialog dialog = new ProgressDialog(CreateNewIM.this);

        String textbody;
        
        protected void onPreExecute() {
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(true);
            this.dialog.setMessage("Creating text...");
            this.dialog.show();

            this.textbody = mBody.getText().toString();
        }

        protected String doInBackground(final Void... args) 
        {
        	Log.d(TAG, "Trying to create IM ");
        	try {
				return ((Boolean)getApp().getKom().sendMessage(recipientNo, textbody, true)).toString();
			} catch (RpcFailure e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "RpcFailure");
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "IOException");
				e.printStackTrace();
			}
			return "fail";
        }

        protected void onPostExecute(final String result) 
        { 
            this.dialog.dismiss();
            if (result.equalsIgnoreCase("true")) {
            	// Create succeeded, just die
                finish();
            } else {
            	Log.d(TAG, "Failed to create IM");
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
    	Log.d(TAG, "Trying to resolve name "+mRecipient.getText().toString());
    	if (recipientNo != 0) {
        	new CreateTextTask().execute();
    	} else {
    		ConfInfo[] users = getApp().getKom().getUsers(mRecipient.getText().toString());
    		ConfInfo[] confs = getApp().getKom().getConferences(mRecipient.getText().toString());
    		final ConfInfo[] conferences = new ConfInfo[users.length + confs.length];
    		if(users.length>0) {
    			for(int i=0; i<users.length; i++) {
    				conferences[i] = users[i];
    			}
    		}
    		if(confs.length>0) {
    			for(int i=0; i<confs.length; i++) {
    				conferences[i+users.length] = confs[i];
    			}
    		}
    		if (conferences != null) {
    			if (conferences.length > 1) {
    				Log.d(TAG, "Ambigous name");
    				final CharSequence[] items = new CharSequence[conferences.length];
    				for(int i=0; i <conferences.length; i++) {
    					items[i]=new String(conferences[i].getNameString());
    					Log.d(TAG, "Name "+i+":"+items[i]);
    				}
    				AlertDialog.Builder builder = new AlertDialog.Builder(CreateNewIM.this);
    				builder.setTitle(getString(R.string.pick_a_name));
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
    			} else if (conferences.length < 1) {
    				Log.e(TAG, "No such recipient:"+mRecipient.getText().toString());
    			} else {
    				recipientNo = conferences[0].confNo;
    		    	new CreateTextTask().execute();
    			}
    		}
    	}
    }

    App getApp() 
    {
        return (App)getApplication();
    }

    private EditText mRecipient;
    private EditText mBody;

    int recipientNo=0;

	public static final String TAG = "Androkom CreateNewIM";
}

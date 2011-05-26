package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;

import org.lindev.androkom.KomServer.ConferenceInfo;

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
 * "Endast" dialog. 
 * 
 * @author jonas
 *
 */
public class Endast extends Activity 
{
	public static final String TAG = "Androkom";

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.endast);
        
        Button endastButton = (Button) findViewById(R.id.do_endast);
        endastButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doendast(); }
        });

    }

	void doendast() {
		EditText confNameView = (EditText) findViewById(R.id.confname);
		String confName = confNameView.getText().toString();
		final ConfInfo[] confs = getApp().getKom().getConferences(confName);

		EditText numTextsView = (EditText) findViewById(R.id.num_texts);
		String textString = numTextsView.getText().toString();
		final int texts = Integer.parseInt(textString);

		if (texts > 0) {
			if (confs.length == 1) {
				Log.d(TAG, "doendast confname=" + confName);
				Log.d(TAG, "doendast texts=" + texts);
				getApp().getKom().endast(confs[0].getNo(), texts);
				finish();
			} else if (confs.length > 1) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Endast.this);
				builder.setTitle(getString(R.string.pick_a_name));
				String[] vals = new String[confs.length];
				for (int i = 0; i < confs.length; i++)
					vals[i] = confs[i].getNameString();
				builder.setSingleChoiceItems(vals, -1,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								Toast.makeText(getApplicationContext(),
										confs[item].getNameString(),
										Toast.LENGTH_SHORT).show();
								dialog.cancel();
								int selectedConf = confs[item].getNo();
								Log.d(TAG, "Selected user:"
										+ selectedConf
										+ ":"
										+ new String(confs[item]
												.getNameString()));
								getApp().getKom().endast(selectedConf, texts);
								finish();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			} else {
				Toast.makeText(getBaseContext(), getString(R.string.no_conference_error),
						Toast.LENGTH_SHORT).show();			
			}
		} else {
			Toast.makeText(getBaseContext(), "zero texts is not implemented",
					Toast.LENGTH_SHORT).show();
		}
	}
    
    App getApp() 
    {
        return (App)getApplication();
    }
    
    private int selectedUser=0;
    private EditText mConfName;
    private EditText mTexts;
}

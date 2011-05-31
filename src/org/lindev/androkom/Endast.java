package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
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
public class Endast extends Activity implements ServiceConnection
{
	public static final String TAG = "Androkom";
	private KomServer mKom;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.endast);
        getApp().doBindService(this);
        
        Button endastButton = (Button) findViewById(R.id.do_endast);
        endastButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doEndast(); }
        });

    }
    
    @Override
    public void onDestroy()
    {
    	getApp().doUnbindService(this);
    	super.onDestroy();
    }

	void doEndast() {
		EditText confNameView = (EditText) findViewById(R.id.confname);
		String confName = confNameView.getText().toString();
		final ConfInfo[] confs = mKom.getConferences(confName);

		EditText numTextsView = (EditText) findViewById(R.id.num_texts);
		String textString = numTextsView.getText().toString();
		final int texts = Integer.parseInt(textString);

		if (texts > 0) {
			if (confs.length == 1) {
				Log.d(TAG, "doendast confname=" + confName);
				Log.d(TAG, "doendast texts=" + texts);
				mKom.endast(confs[0].getNo(), texts);
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
								mKom.endast(selectedConf, texts);
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
    

	public void onServiceConnected(ComponentName name, IBinder service) {
		mKom = ((KomServer.LocalBinder)service).getService();		
	}

	public void onServiceDisconnected(ComponentName name) {
		mKom = null;		
	}
	
    private int selectedUser=0;
    private EditText mConfName;
    private EditText mTexts;
}

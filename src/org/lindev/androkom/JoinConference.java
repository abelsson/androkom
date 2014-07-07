package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;

import org.lindev.androkom.LookupNameTask.LookupType;
import org.lindev.androkom.LookupNameTask.RunOnSuccess;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Join conference dialog. 
 * 
 * @author jonas
 *
 */
public class JoinConference extends Activity implements ServiceConnection {
    public static final String TAG = "Androkom JoinConference";

    private KomServer mKom;
    private EditText mConfName;

    private class JoinConferenceTask extends AsyncTask<Integer, Void, Void> {
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(JoinConference.this);
            mDialog.setCancelable(false);
            mDialog.setIndeterminate(true);
            mDialog.setMessage(getString(R.string.joinconference_label));
            mDialog.show();
        }

        @Override
        protected Void doInBackground(final Integer... args) {
            try {
                mKom.joinconference(args[0]);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void arg) {
            mDialog.dismiss();
            JoinConference.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joinconference);

        mConfName = (EditText) findViewById(R.id.confname);
        Button joinButton = (Button) findViewById(R.id.do_joinconference);
        joinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doJoinConference();
            }
        });
        getApp().doBindService(this);
    }
    
    @Override
    public void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    void doJoinConference() {
        final String confName = mConfName.getText().toString();

        new LookupNameTask(this, mKom, confName, LookupType.LOOKUP_CONFERENCES, new RunOnSuccess() {
            public void run(final ConfInfo conf) {
                Toast.makeText(getApplicationContext(), conf.getNameString(), Toast.LENGTH_SHORT).show();
                new JoinConferenceTask().execute(conf.getNo());
            }
        }).execute();
    }

    App getApp() {
        return (App) getApplication();
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        mKom = ((LocalBinder<KomServer>) service).getService();
    }

    public void onServiceDisconnected(ComponentName name) {
        mKom = null;
    }
}

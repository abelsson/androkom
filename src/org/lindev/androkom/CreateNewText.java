package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;

import org.lindev.androkom.LookupRecipientTask.RunOnSuccess;

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

/**
 * Activity showing a dialog in which the user can create 
 * new texts.
 * 
 */
public class CreateNewText extends Activity implements ServiceConnection {
    private KomServer mKom;

    /**
     * Create activity. Just a plain old dialog with
     * a recipient, subject, body and cancel and post buttons.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.createnewtext);

        getApp().doBindService(this);
        recipient_type = (Integer) getIntent().getExtras().get("recipient_type");
        String recipient = (String) getIntent().getExtras().get("recipient");

        // Set Window Title
        switch(recipient_type) {
        case 1 : setTitle(getString(R.string.create_new_text));
            break;
        case 2 : setTitle(getString(R.string.create_new_mail));
            break;
        default: setTitle("Title of window");
        }

        mRecipient = (EditText) findViewById(R.id.recipient);
        mSubject = (EditText) findViewById(R.id.subject);
        mBody = (EditText) findViewById(R.id.body);

        if (recipient != null) {
            mRecipient.setText(recipient);
        }

        Button confirmButton = (Button) findViewById(R.id.send);
        Button cancelButton = (Button) findViewById(R.id.cancel);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doCreateText(); }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { cancelText(); }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApp().doUnbindService(this);
    }

    /**
     * New text canceled. Just finish up and die.
     */
    private void cancelText() {
        finish();
    }

    private class SendTextTask extends AsyncTask<Object, Void, Void> {
        private final ProgressDialog dialog = new ProgressDialog(CreateNewText.this);

        @Override
        protected void onPreExecute() {
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            dialog.setMessage("Creating text ...");
            dialog.show();
        }

        @Override
        protected Void doInBackground(final Object... args) {
            final int recipient = (Integer) args[0];
            final String subject = (String) args[1];
            final String body = (String) args[2];
            mKom.createText(recipient_type, recipient, subject, body);
            return null;
        }

        @Override
        protected void onPostExecute(final Void obj) {
            dialog.dismiss();
            CreateNewText.this.finish();
        }
    }

    private void doCreateText() {
        final String recipient = mRecipient.getText().toString();
        final String subject = mSubject.getText().toString();
        final String body = mBody.getText().toString();
        new LookupRecipientTask(this, mKom, recipient, recipient_type, new RunOnSuccess() {
            public void run(final ConfInfo conf) {
                new SendTextTask().execute(conf.confNo, subject, body);
            }
        }).execute();
    }

    App getApp() {
        return (App)getApplication();
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        mKom = ((KomServer.LocalBinder)service).getService();
    }

    public void onServiceDisconnected(ComponentName name) {
        mKom = null;
    }

    private EditText mRecipient;
    private EditText mSubject;
    private EditText mBody;
    private int recipient_type = 0; // type 1 = text/inlägg, type 2 = brev/mail

    public static final String TAG = "Androkom CreateNewText";
}

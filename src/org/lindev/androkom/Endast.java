package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;

import org.lindev.androkom.LookupRecipientTask.RunOnSuccess;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
public class Endast extends Activity implements ServiceConnection {
    public static final String TAG = "Androkom";

    private KomServer mKom;
    private EditText mConfName;
    private EditText mTexts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.endast);
        getApp().doBindService(this);
        
        mConfName = (EditText) findViewById(R.id.confname);
        mTexts = (EditText) findViewById(R.id.num_texts);
        Button endastButton = (Button) findViewById(R.id.do_endast);
        endastButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doEndast();
            }
        });
    }

    @Override
    public void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    void doEndast() {
        final String confName = mConfName.getText().toString();
        final String numTextsStr = mTexts.getText().toString();
        final int numTexts;

        try {
            numTexts = Integer.valueOf(numTextsStr);
        }
        catch (final NumberFormatException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.not_a_number) + numTextsStr);
            builder.setPositiveButton(getString(R.string.alert_dialog_ok), null);
            builder.create().show();
            return;
        }
        if (numTexts < 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.must_not_be_negative));
            builder.setPositiveButton(getString(R.string.alert_dialog_ok), null);
            builder.create().show();
            return;
        }

        new LookupRecipientTask(this, mKom, confName, LookupRecipientTask.LOOKUP_CONFERENCES, new RunOnSuccess() {
            public void run(final ConfInfo conf) {
                Toast.makeText(getApplicationContext(), conf.getNameString(), Toast.LENGTH_SHORT).show();
                mKom.endast(conf.getNo(), numTexts);
                Endast.this.finish();
            }
        }).execute();
    }

    App getApp() {
        return (App) getApplication();
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        mKom = ((KomServer.LocalBinder)service).getService();
    }

    public void onServiceDisconnected(ComponentName name) {
        mKom = null;
    }
}

package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;

import org.lindev.androkom.LookupNameTask.LookupType;
import org.lindev.androkom.LookupNameTask.RunOnSuccess;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

/**
 * Dialog to set options for See Again (återse). 
 * 
 * @author jonas
 *
 */
public class SeeAgainTexts extends Activity implements ServiceConnection {
    public static final String TAG = "Androkom SeeAgainTexts";

    private KomServer mKom;
    private EditText mConfName;
    private EditText mTexts;
    private RadioButton mradioUserButton;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seeagain);

        mConfName = (EditText) findViewById(R.id.confname);
        mTexts = (EditText) findViewById(R.id.num_texts);
        mradioUserButton = (RadioButton) findViewById(R.id.radio_user);
        
        Button seeagainButton = (Button) findViewById(R.id.do_seeagain);
        seeagainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doSeeAgain();
            }
        });
        getApp().doBindService(this);
    }

    @Override
    public void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    void doSeeAgain() {
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

        new LookupNameTask(this, mKom, confName, LookupType.LOOKUP_BOTH,
                new RunOnSuccess() {
                    public void run(final ConfInfo conf) {
                        Intent intent = new Intent(SeeAgainTexts.this,
                                SeeAgainTextList.class);
                        intent.putExtra("conference-id", conf.getNo());
                        intent.putExtra("numTexts", numTexts);
                        intent.putExtra("douser", mradioUserButton.isChecked());
                        if (conf.getNo() > 0) {
                            if (numTexts > 0) {
                                startActivity(intent);
                                finish();
                            } else {
                                Log.d(TAG, "No numTexts");
                            }
                        } else {
                            Log.d(TAG, "No confNo");
                        }
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

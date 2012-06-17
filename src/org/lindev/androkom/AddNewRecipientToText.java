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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Add new recipient dialog. 
 * 
 * @author jonas
 *
 */
public class AddNewRecipientToText extends Activity implements ServiceConnection {
    public static final String INTENT_TEXTNO = "textno";

    public static final String TAG = "Androkom AddNewRecipientToText";

    private KomServer mKom;
    private EditText mConfName;

    private class AddNewRecipientToTextTask extends AsyncTask<Integer, Void, String> {
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(AddNewRecipientToText.this);
            mDialog.setCancelable(false);
            mDialog.setIndeterminate(true);
            mDialog.setMessage(getString(R.string.addnewrecipienttotext_title));
            mDialog.show();
        }

        @Override
        protected String doInBackground(final Integer... args) {
            return mKom.addNewRecipientToText(mTextNo, args[0], mTextType);
        }

        @Override
        protected void onPostExecute(final String arg) {
            mDialog.dismiss();
            if (arg != "") {
                Toast.makeText(getApplicationContext(),
                        arg, Toast.LENGTH_SHORT)
                        .show();                                
            }
            AddNewRecipientToText.this.finish();
        }
    }

    public class MyOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) {
          mTextType = pos;
        }

        public void onNothingSelected(AdapterView parent) {
          // Do nothing.
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addnewrecipienttotext);

        mTextNo = (Integer) getIntent().getExtras().get(INTENT_TEXTNO);

        mConfName = (EditText) findViewById(R.id.confname);
        
        Spinner spinner = (Spinner) findViewById(R.id.textType_id);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.texttype_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
        
        Button doButton = (Button) findViewById(R.id.do_addnewrecipienttotext);
        doButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doAddNewRecipientToText();
            }
        });
        getApp().doBindService(this);
    }

    @Override
    public void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    void doAddNewRecipientToText() {
        final String confName = mConfName.getText().toString();

        new LookupNameTask(this, mKom, confName, LookupType.LOOKUP_CONFERENCES, new RunOnSuccess() {
            public void run(final ConfInfo conf) {
                Toast.makeText(getApplicationContext(), conf.getNameString(), Toast.LENGTH_SHORT).show();
                new AddNewRecipientToTextTask().execute(conf.getNo());
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
    
    int mTextNo = 0;
    int mTextType = 0;
}

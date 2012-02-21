package org.lindev.androkom.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nu.dll.lyskom.ConfInfo;
import nu.dll.lyskom.Conference;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Session;
import nu.dll.lyskom.Text;

import org.lindev.androkom.App;
import org.lindev.androkom.KomServer;
import org.lindev.androkom.LookupNameTask;
import org.lindev.androkom.LookupNameTask.LookupType;
import org.lindev.androkom.LookupNameTask.RunOnSuccess;
import org.lindev.androkom.R;
import org.lindev.androkom.text.CreateTextTask;
import org.lindev.androkom.text.CreateTextTask.CreateTextRunnable;
import org.lindev.androkom.text.Recipient;
import org.lindev.androkom.text.Recipient.RecipientType;
import org.lindev.androkom.text.SendTextTask;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

public class TextCreator extends TabActivity implements ServiceConnection {
    public static final String TAG = "Androkom TextCreator";

    private static final String TEXT_TAB_TAG = "text-tab-tag";
    private static final String RECIPIENTS_TAB_TAG = "recipients-tab-tag";

    private static final String INTENT_INITIAL_RECIPIENTS_ADDED = "initial-recipients-added";
    public static final String INTENT_REPLY_TO = "in-reply-to";
    public static final String INTENT_SUBJECT = "subject-line";
    public static final String INTENT_RECIPIENT = "recipient";
    public static final String INTENT_IS_MAIL = "is-mail";

    private KomServer mKom = null;
    private List<Recipient> mRecipients;
    private ArrayAdapter<Recipient> mAdapter;
    private int mReplyTo;
    private EditText mSubject;
    private EditText mBody;
    private double mLat, mLon, mPrecision=-1;
    LocationManager mlocManager=null;
    LocationListener mlocListener=null;
    
    public class CopyRecipientsTask extends
            AsyncTask<Integer, Void, List<Recipient>> {
        @Override
        protected List<Recipient> doInBackground(final Integer... args) {
            final List<Recipient> recipients = new ArrayList<Recipient>();

            try {
                final Text text = mKom.getTextbyNo(args[0]);
                if (text == null) {
                    return null;
                }
                for (int recip : text.getRecipients()) {
                    final Conference confStat = mKom.getConfStat(recip);
                    if (confStat != null) {
                        if (confStat.getConfInfo().confType.original()) {
                            recip = confStat.getSuperConf();
                        }
                        final String name = mKom.toString(mKom.getConfName(recip));
                        if (name != null) {
                            recipients.add(new Recipient(mKom
                                    .getApplicationContext(), recip, name,
                                    RecipientType.RECP_TO));
                        }
                    }
                }
            } catch (final RpcFailure e) {
                return null;
            }
            return recipients;
        }

        @Override
        public void onPostExecute(final List<Recipient> recipients) {
            if (recipients != null) {
                for (final Recipient recipient : recipients) {
                    add(recipient);
                }
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_new_text_layout);

        /* Use the LocationManager class to obtain GPS locations */

        mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mlocListener = new MyLocationListener();
        mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
        
        initializeCommon();
        initializeRecipients();
        initializeTabs();
        initializeButtons();

        getApp().doBindService(this);
    }

    @Override
    protected void onDestroy() {
        if(mlocManager != null) {
            mlocManager.removeUpdates(mlocListener);
        }
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    private void initializeCommon() {
        mSubject = (EditText) findViewById(R.id.subject);
        mBody = (EditText) findViewById(R.id.body);
        mReplyTo = getIntent().getIntExtra(INTENT_REPLY_TO, -1);
        if (mReplyTo > 0) {
            setTitle(getString(R.string.creator_comment_to) + mReplyTo);
        }
        else {
            setTitle(getString(R.string.creator_new_text));
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeRecipients() {
        mAdapter = new ArrayAdapter<Recipient>(this, R.layout.message_log);
        mRecipients = (List<Recipient>) getLastNonConfigurationInstance();
        if (mRecipients == null) {
            mRecipients = new ArrayList<Recipient>();
        }
        for (final Recipient recipient : mRecipients) {
            mAdapter.add(recipient);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void initializeTabs() {
        final TabHost tabHost = getTabHost();
        final TabSpec textTab = tabHost.newTabSpec(TEXT_TAB_TAG);
        textTab.setIndicator(getString(R.string.creator_text_title));
        textTab.setContent(R.id.create_text);
        tabHost.addTab(textTab);

        final TabSpec recipientsTab = tabHost.newTabSpec(RECIPIENTS_TAB_TAG);
        recipientsTab.setIndicator(getString(R.string.creator_recipents_title));
        recipientsTab.setContent(R.id.recipients_view);
        tabHost.addTab(recipientsTab);

        final ListView recipientsView = (ListView) findViewById(R.id.recipients);
        recipientsView.setAdapter(mAdapter);
        recipientsView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(final AdapterView<?> av, final View view, final int position, final long id) {
                final Recipient recipient = (Recipient) av.getItemAtPosition(position);
                showRemoveRecipientDialog(recipient);
            }
        });
    }

    private void initializeButtons() {
        final Button toButton = (Button) findViewById(R.id.add_to);
        final Button ccButton = (Button) findViewById(R.id.add_cc);
        final Button bccButton = (Button) findViewById(R.id.add_bcc);
        final Button sendButton = (Button) findViewById(R.id.send);
        final Button cancelButton = (Button) findViewById(R.id.cancel);

        toButton.setEnabled(false);
        ccButton.setEnabled(false);
        bccButton.setEnabled(false);
        sendButton.setEnabled(false);
        cancelButton.setEnabled(true);

        final View.OnClickListener buttonClickListener = new View.OnClickListener() {
            public void onClick(final View view) {
                if (view == toButton) {
                    showAddRecipientDialog(RecipientType.RECP_TO, LookupType.LOOKUP_BOTH);
                }
                else if (view == ccButton) {
                    showAddRecipientDialog(RecipientType.RECP_CC, LookupType.LOOKUP_BOTH);
                }
                else if (view == bccButton) {
                    showAddRecipientDialog(RecipientType.RECP_BCC, LookupType.LOOKUP_BOTH);
                }
                else if (view == sendButton) {
                    sendMessage();
                }
                else if (view == cancelButton) {
                    finish();
                }
            }
        };

        toButton.setOnClickListener(buttonClickListener);
        ccButton.setOnClickListener(buttonClickListener);
        bccButton.setOnClickListener(buttonClickListener);
        sendButton.setOnClickListener(buttonClickListener);
        cancelButton.setOnClickListener(buttonClickListener);
    }

    private void enableButtons() {
        ((Button) findViewById(R.id.add_to)).setEnabled(true);
        ((Button) findViewById(R.id.add_cc)).setEnabled(true);
        ((Button) findViewById(R.id.add_bcc)).setEnabled(true);
        ((Button) findViewById(R.id.send)).setEnabled(true);
    }

    /**
     * The menu key has been pressed, instantiate the requested
     * menu.
     */
    @Override 
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.textcreator, menu);
        return true;
    }

    /**
     * Called when user has selected a menu item from the 
     * menu button popup. 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        try {
            mKom.activateUser();
        } catch (Exception e1) {
            Log.d(TAG, "onOptionsItem caught exception, bailing out");
            // e1.printStackTrace();
            mKom.logout();
        }

        // Handle item selection
        switch (item.getItemId()) {

        case R.id.menu_insertlocation_id:
            Log.i(TAG, "insertlocation");
            if(mPrecision > 0) {
                String textToInsert = "<geo:"+mLat+","+mLon+";u="+mPrecision+">"; // ref RFC580
                int start = mBody.getSelectionStart();
                int end = mBody.getSelectionEnd();
                mBody.getText().replace(Math.min(start, end), Math.max(start, end),
                        textToInsert, 0, textToInsert.length());

            } else {
                Toast.makeText(getApplicationContext(),
                        "No location", Toast.LENGTH_SHORT)
                        .show();                                
            }
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showRemoveRecipientDialog(final Recipient recipient) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.creator_remove_recipient)+"\n"
                + recipient.recipientStr + "?");
        builder.setNegativeButton(getString(R.string.no), null);
        builder.setPositiveButton(getString(R.string.yes), new OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                remove(recipient);
            }
        });
        builder.create().show();
    }

    private void showAddRecipientDialog(final RecipientType type, final LookupType lookupType) {
        final EditText input = new EditText(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.creator_add_recipient));
        builder.setView(input);
        builder.setNegativeButton(getString(R.string.alert_dialog_cancel), null);
        builder.setPositiveButton(getString(R.string.alert_dialog_ok), new OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final String recip = input.getText().toString();
                new LookupNameTask(TextCreator.this, mKom, recip, lookupType, new RunOnSuccess() {
                    public void run(final ConfInfo conf) {
                        add(new Recipient(mKom.getApplicationContext(), conf.getNo(), conf.getNameString(), type));
                    }
                }).execute();
            }
        });
        builder.create().show();
    }

    private void sendMessage() {
        final String subject = mSubject.getText().toString();
        final String body = mBody.getText().toString();
        if (mRecipients.isEmpty()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.creator_no_recipients));
            builder.setPositiveButton(getString(R.string.alert_dialog_ok), null);
            builder.create().show();
            return;
        }

        new CreateTextTask(this, mKom, subject, body, mLat, mLon, mPrecision, mReplyTo, mRecipients, new CreateTextRunnable() {
            public void run(final Text text) {
                new SendTextTask(TextCreator.this, mKom, text, new Runnable() {
                    public void run() {
                        finish();
                    }
                }).execute();
            }
        }).execute();
    }

    private void remove(final Recipient recipient) {
        mRecipients.remove(recipient);
        mAdapter.remove(recipient);
        mAdapter.notifyDataSetChanged();
    }

    private void add(final Recipient recipient) {
        for (final Recipient recpt : mRecipients) {
            if (recpt.recipientId == recipient.recipientId) {
                Log.d(TAG, "Remove old recipient");
                remove(recpt);
            }
        }
        mRecipients.add(recipient);
        mAdapter.add(recipient);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mRecipients;
    }

    private void addInitialRecipients() {
        final String subject = getIntent().getStringExtra(INTENT_SUBJECT);
        final int recipient = getIntent().getIntExtra(INTENT_RECIPIENT, -1);
        final boolean isMail = getIntent().getBooleanExtra(INTENT_IS_MAIL, false);

        if (mReplyTo > 0) {
            new CopyRecipientsTask().execute(mReplyTo);
        }
        else if (isMail) {
            add(new Recipient(mKom.getApplicationContext(), mKom.getUserId(), mKom.getConferenceName(mKom.getUserId()), RecipientType.RECP_TO));
            showAddRecipientDialog(RecipientType.RECP_TO, LookupType.LOOKUP_USERS);
        }
        else {
            showAddRecipientDialog(RecipientType.RECP_TO, LookupType.LOOKUP_BOTH);
        }

        if (subject != null) {
            mSubject.setText(subject);
        }
        if (recipient > 0) {
            add(new Recipient(mKom.getApplicationContext(), recipient, mKom.getConferenceName(recipient), RecipientType.RECP_TO));
        }
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mKom = ((KomServer.LocalBinder) service).getService();
        if (!getIntent().getBooleanExtra(INTENT_INITIAL_RECIPIENTS_ADDED, false)) {
            addInitialRecipients();
            getIntent().putExtra(INTENT_INITIAL_RECIPIENTS_ADDED, true);
        }
        enableButtons();
    }

    public void onServiceDisconnected(final ComponentName name) {
        mKom = null;
    }

    private App getApp() {
        return (App) getApplication();
    }

    /* Class My Location Listener */
    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location loc) {
            mLat = loc.getLatitude();
            mLon = loc.getLongitude();
            mPrecision = loc.getAccuracy();
            String Text = "My current location is: " + "Latitud = "
                    + loc.getLatitude() + "Longitud = " + loc.getLongitude();
            Log.i(TAG, Text);
        }

        public void onProviderDisabled(String provider) {
            Log.i(TAG, "Gps Disabled");
        }

        public void onProviderEnabled(String provider) {
            Log.i(TAG, "Gps Enabled");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "Gps status changed");
        }

    }/* End of Class MyLocationListener */
}

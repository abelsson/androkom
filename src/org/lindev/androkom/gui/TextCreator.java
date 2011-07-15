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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TextCreator extends TabActivity implements ServiceConnection {
    private static final String TEXT_TAB_TAG = "text-tab-tag";
    private static final String RECIPIENTS_TAB_TAG = "recipients-tab-tag";

    private static final String INTENT_STATE_CONFIGURED = "initial-state-configured";
    public static final String INTENT_REPLY_TO = "in-reply-to";
    public static final String INTENT_SUBJECT = "subject-line";

    private KomServer mKom = null;
    private List<Recipient> mRecipients;
    private ArrayAdapter<Recipient> mAdapter;
    private int mReplyTo;
    private EditText mSubject;
    private EditText mBody;

    public class CopyRecipientsTask extends AsyncTask<Integer, Void, List<Recipient>> {
        @Override
        protected List<Recipient> doInBackground(final Integer... args) {
            final Session s = mKom.getSession();
            final List<Recipient> recipients = new ArrayList<Recipient>();

            try {
                final Text text = s.getText(args[0]);
                if (text == null) {
                    return null;
                }
                for (int recip : text.getRecipients()) {
                    final Conference confStat = s.getConfStat(recip);
                    if (confStat.getConfInfo().confType.original()) {
                        recip = confStat.getSuperConf();
                    }
                    final String name = s.toString(s.getConfName(recip));
                    recipients.add(new Recipient(recip, name, RecipientType.RECP_TO));
                }
            }
            catch (final RpcFailure e) {
                return null;
            }
            catch (final IOException e) {
                return null;
            }
            return recipients;
        }

        @Override
        public void onPostExecute(final List<Recipient> recipients) {
            for (final Recipient recipient : recipients) {
                mRecipients.add(recipient);
                mAdapter.add(recipient);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_new_text_layout);

        mSubject = (EditText) findViewById(R.id.subject);
        mBody = (EditText) findViewById(R.id.body);
        mAdapter = new ArrayAdapter<Recipient>(this, R.layout.message_log);
        mRecipients = (List<Recipient>) getLastNonConfigurationInstance();
        if (mRecipients == null) {
            mRecipients = new ArrayList<Recipient>();
        }

        initialize();
        getApp().doBindService(this);
    }

    @Override
    protected void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    private void initialize() {
        final TabHost tabHost = getTabHost();
        final TabSpec textTab = tabHost.newTabSpec(TEXT_TAB_TAG);
        textTab.setIndicator("Text");
        textTab.setContent(R.id.create_text);
        tabHost.addTab(textTab);

        final TabSpec recipientsTab = tabHost.newTabSpec(RECIPIENTS_TAB_TAG);
        recipientsTab.setIndicator("Recipients");
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

        final Button toButton = (Button) findViewById(R.id.add_to);
        final Button ccButton = (Button) findViewById(R.id.add_cc);
        final Button sendButton = (Button) findViewById(R.id.send);
        final Button cancelButton = (Button) findViewById(R.id.cancel);

        final View.OnClickListener buttonClickListener = new View.OnClickListener() {
            public void onClick(final View view) {
                if (view == toButton) {
                    showAddRecipientDialog(RecipientType.RECP_TO);
                }
                else if (view == ccButton) {
                    showAddRecipientDialog(RecipientType.RECP_CC);
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
        sendButton.setOnClickListener(buttonClickListener);
        cancelButton.setOnClickListener(buttonClickListener);

        for (final Recipient recip : mRecipients) {
            mAdapter.add(recip);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void showRemoveRecipientDialog(final Recipient recipient) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove recipient \n" + recipient.recipientStr + "?");
        builder.setNegativeButton("No", null);
        builder.setPositiveButton("Yes", new OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                mRecipients.remove(recipient);
                mAdapter.remove(recipient);
                mAdapter.notifyDataSetChanged();
            }
        });
        builder.create().show();
    }

    private void showAddRecipientDialog(final RecipientType type) {
        final EditText input = new EditText(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add recipient");
        builder.setView(input);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final String recip = input.getText().toString();
                new LookupNameTask(TextCreator.this, mKom, recip, LookupNameTask.LOOKUP_BOTH, new RunOnSuccess() {
                    public void run(final ConfInfo conf) {
                        final Recipient recipient = new Recipient(conf.getNo(), conf.getNameString(), type);
                        mRecipients.add(recipient);
                        mAdapter.add(recipient);
                        mAdapter.notifyDataSetChanged();
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
            builder.setTitle("There are no recipients");
            builder.setPositiveButton(getString(R.string.alert_dialog_ok), null);
            builder.create().show();
            return;
        }
        new CreateTextTask(this, mKom, subject, body, mReplyTo, mRecipients, new CreateTextRunnable() {
            public void run(final Text text) {
                new SendTextTask(TextCreator.this, mKom, text, new Runnable() {
                    public void run() {
                        finish();
                    }
                }).execute();
            }
        }).execute();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mRecipients;
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mKom = ((KomServer.LocalBinder) service).getService();
        mReplyTo = getIntent().getIntExtra(INTENT_REPLY_TO, -1);
        if (!getIntent().getBooleanExtra(INTENT_STATE_CONFIGURED, false)) {
            getIntent().putExtra(INTENT_STATE_CONFIGURED, true);
            final String subject = getIntent().getStringExtra(INTENT_SUBJECT);
            if (subject != null) {
                mSubject.setText(subject);
            }
            if (mReplyTo <= 0) {
                showAddRecipientDialog(RecipientType.RECP_TO);
            }
            else {
                new CopyRecipientsTask().execute(mReplyTo);
            }
        }
        if (mReplyTo <= 0) {
            setTitle("Create new text");
        }
        else {
            setTitle("Create comment to " + mReplyTo);
        }
    }

    public void onServiceDisconnected(final ComponentName name) {
        mKom = null;
    }

    private App getApp() {
        return (App) getApplication();
    }
}

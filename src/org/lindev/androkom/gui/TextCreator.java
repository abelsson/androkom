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

        initializeCommon();
        initializeRecipients();
        initializeTabs();
        initializeButtons();

        getApp().doBindService(this);
    }

    @Override
    protected void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    private void initializeCommon() {
        mSubject = (EditText) findViewById(R.id.subject);
        mBody = (EditText) findViewById(R.id.body);
        mReplyTo = getIntent().getIntExtra(INTENT_REPLY_TO, -1);
        if (mReplyTo > 0) {
            setTitle("Create comment to " + mReplyTo);
        }
        else {
            setTitle("Create new text");
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
    }

    private void initializeButtons() {
        final Button toButton = (Button) findViewById(R.id.add_to);
        final Button ccButton = (Button) findViewById(R.id.add_cc);
        final Button sendButton = (Button) findViewById(R.id.send);
        final Button cancelButton = (Button) findViewById(R.id.cancel);

        final View.OnClickListener buttonClickListener = new View.OnClickListener() {
            public void onClick(final View view) {
                if (view == toButton) {
                    showAddRecipientDialog(RecipientType.RECP_TO, LookupNameTask.LOOKUP_BOTH);
                }
                else if (view == ccButton) {
                    showAddRecipientDialog(RecipientType.RECP_CC, LookupNameTask.LOOKUP_BOTH);
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
    }

    private void showRemoveRecipientDialog(final Recipient recipient) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove recipient \n" + recipient.recipientStr + "?");
        builder.setNegativeButton("No", null);
        builder.setPositiveButton("Yes", new OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                remove(recipient);
            }
        });
        builder.create().show();
    }

    private void showAddRecipientDialog(final RecipientType type, final int lookupType) {
        final EditText input = new EditText(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add recipient");
        builder.setView(input);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final String recip = input.getText().toString();
                new LookupNameTask(TextCreator.this, mKom, recip, lookupType, new RunOnSuccess() {
                    public void run(final ConfInfo conf) {
                        add(new Recipient(conf.getNo(), conf.getNameString(), type));
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

    private void remove(final Recipient recipient) {
        mRecipients.remove(recipient);
        mAdapter.remove(recipient);
        mAdapter.notifyDataSetChanged();
    }

    private void add(final Recipient recipient) {
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
            add(new Recipient(mKom.getUserId(), mKom.getConferenceName(mKom.getUserId()), RecipientType.RECP_TO));
            showAddRecipientDialog(RecipientType.RECP_TO, LookupNameTask.LOOKUP_USERS);
        }
        else {
            showAddRecipientDialog(RecipientType.RECP_TO, LookupNameTask.LOOKUP_BOTH);
        }

        if (subject != null) {
            mSubject.setText(subject);
        }
        if (recipient > 0) {
            add(new Recipient(recipient, mKom.getConferenceName(recipient), RecipientType.RECP_TO));
        }
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mKom = ((KomServer.LocalBinder) service).getService();
        if (!getIntent().getBooleanExtra(INTENT_INITIAL_RECIPIENTS_ADDED, false)) {
            addInitialRecipients();
            getIntent().putExtra(INTENT_INITIAL_RECIPIENTS_ADDED, true);
        }
    }

    public void onServiceDisconnected(final ComponentName name) {
        mKom = null;
    }

    private App getApp() {
        return (App) getApplication();
    }
}

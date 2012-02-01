package org.lindev.androkom.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

public class ImgTextCreator extends TabActivity implements ServiceConnection {
    public static final String TAG = "Androkom ImgTextCreator";

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
                    recipients.add(new Recipient(mKom.getApplicationContext(), recip, name, RecipientType.RECP_TO));
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
        setContentView(R.layout.create_new_imgtext_layout);

        initializeCommon();
        initializeRecipients();
        initializeTabs();
        initializeButtons();
        initializeImg();
        
        getApp().doBindService(this);
    }

    @Override
    protected void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    private void initializeCommon() {
        mSubject = (EditText) findViewById(R.id.subject);
        mReplyTo = getIntent().getIntExtra(INTENT_REPLY_TO, -1);
        if (mReplyTo > 0) {
            setTitle(getString(R.string.creator_comment_to) + mReplyTo);
        }
        else {
            setTitle(getString(R.string.creator_new_text));
        }
    }

    private void initializeImg() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String action = intent.getAction();

        // if this is from the share menu
        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                try {
                    // Get resource path from intent callee
                    Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

                    // Query gallery for camera picture via
                    // Android ContentResolver interface
                    ContentResolver cr = getContentResolver();
                    InputStream is = cr.openInputStream(uri);
                    // Get binary bytes for encode
                    imgdata = getBytesFromFile(is);

                    ImageView imgView = (ImageView) findViewById(R.id.imageView1);
                    
                    Bitmap bmImg = BitmapFactory.decodeByteArray(imgdata, 0, imgdata.length);
                    if(bmImg != null) {
                        imgView.setImageBitmap(bmImg);
                        double imgHeight = bmImg.getHeight();
                        double imgWidth = bmImg.getWidth();
                        double scaleFactor;
                        int maxLength=200;
                        if((imgHeight>maxLength)||(imgWidth>maxLength)) {
                            Log.d(TAG, "Too big image:"+imgHeight+" x "+imgWidth);
                            if(imgHeight>imgWidth) {
                                scaleFactor = imgHeight/maxLength; 
                            } else {
                                scaleFactor = imgWidth/maxLength;                                 
                            }
                            Log.d(TAG, "scaleFactor:"+scaleFactor);
                            int newHeight = (int) (imgHeight / scaleFactor);
                            int newWidth = (int) (imgWidth / scaleFactor);
                            Log.d(TAG, "newHeight:"+newHeight);
                            Log.d(TAG, "newWidth:"+newWidth);
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmImg, newWidth, newHeight, true);
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, buffer);
                            buffer.flush();
                            imgdata = buffer.toByteArray();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to decode image", Toast.LENGTH_LONG).show();
                    }
                    return;
                } catch (Exception e) {
                    Log.e(this.getClass().getName(), e.toString());
                }

            } else if (extras.containsKey(Intent.EXTRA_TEXT)) {
                return;
            }
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
            // e1.printStackTrace();
            Log.d(TAG, "onOptionsItem caught exception, bailing out");
            mKom.logout();
        }

        // Handle item selection
        switch (item.getItemId()) {

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
                new LookupNameTask(ImgTextCreator.this, mKom, recip, lookupType, new RunOnSuccess() {
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
        if (mRecipients.isEmpty()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.creator_no_recipients));
            builder.setPositiveButton(getString(R.string.alert_dialog_ok), null);
            builder.create().show();
            return;
        }

        new CreateTextTask(this, mKom, subject, imgdata, mReplyTo, mRecipients, new CreateTextRunnable() {
            public void run(final Text text) {
                new SendTextTask(ImgTextCreator.this, mKom, text, new Runnable() {
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

    public static byte[] getBytesFromFile(InputStream is) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();

            return buffer.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return null;
        }
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

    byte[] imgdata = null;
}

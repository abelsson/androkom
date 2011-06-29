package org.lindev.androkom.gui;

import java.util.Observable;
import java.util.Observer;

import nu.dll.lyskom.ConfInfo;

import org.lindev.androkom.App;
import org.lindev.androkom.KomServer;
import org.lindev.androkom.R;
import org.lindev.androkom.im.IMLogger;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class IMConversationList extends ListActivity implements ServiceConnection, Observer, OnClickListener {
    public static final String TAG = "Androkom";

    private static final int MAX_CONVERSATIONS = 50;
    private static final int BACKGROUND_COLOR_ALL_READ = Color.BLACK;
    private static final int BACKGROUND_COLOR_UNREAD = 0xff303060;

    private KomServer mKom = null;
    private IMLogger mIMLogger = null;
    private Cursor mCursor = null;

    private Button mSendButton = null;
    private EditText mRecipientField = null;
    private EditText mMessageField = null;

    public static class IMConvListView extends ListView {
        private final IMConversationList mIMConversationList;

        public IMConvListView(final Context context, final AttributeSet attr) {
            super(context, attr);
            mIMConversationList = (IMConversationList) context;
        }

        @Override
        protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
            mIMConversationList.updateView(false);
        }
    }

    private class IMConvListCursorAdapter extends CursorAdapter {
        private IMConvListCursorAdapter(final Context context, final Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.message_log, null);
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            final int convId = cursor.getInt(cursor.getColumnIndex(IMLogger.COL_CONV_ID));
            final String convStr = cursor.getString(cursor.getColumnIndex(IMLogger.COL_CONV_STR));
            final int numMsg = cursor.getInt(cursor.getColumnIndex(IMLogger.COL_NUM_MSG));
            final int latestMsg = cursor.getInt(cursor.getColumnIndex(IMLogger.COL_LATEST_MSG));
            final int latestSeen = cursor.getInt(cursor.getColumnIndex(IMLogger.COL_LATEST_SEEN));

            final TextView tv = (TextView) view;
            if (latestMsg == latestSeen) {
                tv.setBackgroundColor(BACKGROUND_COLOR_ALL_READ);
            }
            else {
                tv.setBackgroundColor(BACKGROUND_COLOR_UNREAD);
            }
            tv.setText("(" + numMsg + ") " + convStr + " <" + convId + ">");
        }
    }

    private class ResolveRecipientTask extends AsyncTask<String, Void, ConfInfo[]> {
        private final ProgressDialog dialog = new ProgressDialog(IMConversationList.this);
        private String mRecip;
        private String mMsg;

        @Override
        protected void onPreExecute() {
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            dialog.setMessage("Resolving recipient ...");
            dialog.show();
        }

        protected ConfInfo[] doInBackground(final String... args) {
            mRecip = args[0];
            mMsg = args[1];

            final ConfInfo[] users = mKom.getUsers(mRecip);
            final ConfInfo[] confs = mKom.getConferences(mRecip);
            final ConfInfo[] possibleRecip = new ConfInfo[users.length + confs.length];
            System.arraycopy(users, 0, possibleRecip, 0, users.length);
            System.arraycopy(confs, 0, possibleRecip, users.length, confs.length);
            return possibleRecip;
        }

        protected void onPostExecute(final ConfInfo[] possibleRecip) {
            if (possibleRecip.length == 0) {
                Toast.makeText(getApplicationContext(), "No such recipient: " + mRecip, Toast.LENGTH_SHORT).show();
            }
            else if (possibleRecip.length == 1) {
                new SendMessageTask().execute(possibleRecip[0], mMsg);
            }
            else {
                final String[] items = new String[possibleRecip.length];
                for (int i = 0; i < items.length; ++i) {
                    items[i] = possibleRecip[i].getNameString();
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(IMConversationList.this);
                builder.setTitle(getString(R.string.pick_a_name));
                builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int item) {
                        new SendMessageTask().execute(possibleRecip[item], mMsg);
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
            dialog.dismiss();
        }
    }

    private class SendMessageTask extends AsyncTask<Object, Void, ConfInfo> {
        private final ProgressDialog dialog = new ProgressDialog(IMConversationList.this);

        @Override
        protected void onPreExecute() {
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            dialog.setMessage("Sending message ...");
            dialog.show();
        }

        @Override
        protected ConfInfo doInBackground(final Object... args) {
            final ConfInfo conf = (ConfInfo) args[0];
            final String msg = (String) args[1];
            try {
                mKom.sendMessage(conf.confNo, msg, true);
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }
            return conf;
        }

        @Override
        protected void onPostExecute(final ConfInfo conf) {
            if (conf != null) {
                final Intent intent = new Intent(IMConversationList.this, IMConversation.class);
                intent.putExtra(IMConversation.INTENT_CONVERSATION_ID, conf.confNo);
                intent.putExtra(IMConversation.INTENT_CONVERSATION_STR, conf.getNameString());
                mRecipientField.setText("");
                mMessageField.setText("");
                startActivity(intent);
            }
            dialog.dismiss();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.im_conversation_list_layout);

        mSendButton = (Button) findViewById(R.id.send);
        mRecipientField = (EditText) findViewById(R.id.recipient);
        mMessageField = (EditText) findViewById(R.id.message);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final String recipient = extras.getString("recipient");
            if (recipient != null) {
                mRecipientField.setText(recipient);
            }
        }

        mSendButton.setOnClickListener(this);

        getApp().doBindService(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIMLogger != null) {
            mIMLogger.addObserver(this);
            updateView(true);
        }
    }

    @Override
    public void onPause() {
        if (mIMLogger != null) {
            mIMLogger.deleteObserver(this);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mIMLogger != null) {
            mCursor.close();
        }
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    public void onClick(final View view) {
        if (view == mSendButton && mIMLogger != null) {
            final String recipient = mRecipientField.getText().toString();
            final String msg = mMessageField.getText().toString();
            new ResolveRecipientTask().execute(recipient, msg);
        }
    }

    private void updateView(final boolean requery) {
        if (mIMLogger == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            public void run() {
                if (requery) {
                    mCursor.requery();
                }
                getListView().setSelection(getListView().getCount() - 1);
            }
        });
    }

    public void update(final Observable observable, final Object data) {
        if (observable == mIMLogger) {
            updateView(true);
        }
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int position, final long id) {
        final Cursor cursor = (Cursor) listView.getItemAtPosition(position);
        final int convId = cursor.getInt(cursor.getColumnIndex(IMLogger.COL_CONV_ID));
        final String convStr = cursor.getString(cursor.getColumnIndex(IMLogger.COL_CONV_STR));

        final Intent intent = new Intent(this, IMConversation.class);
        intent.putExtra(IMConversation.INTENT_CONVERSATION_ID, convId);
        intent.putExtra(IMConversation.INTENT_CONVERSATION_STR, convStr);

        startActivity(intent);
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mKom = ((KomServer.LocalBinder) service).getService();
        mIMLogger = mKom.imLogger;
        mCursor = mIMLogger.getConversations(MAX_CONVERSATIONS);
        setListAdapter(new IMConvListCursorAdapter(this, mCursor));
        mIMLogger.addObserver(this);
        updateView(false);
    }

    public void onServiceDisconnected(final ComponentName name) {
        mIMLogger = null;
    }

    private App getApp() {
        return (App) getApplication();
    }
}

package org.lindev.androkom.gui;

import java.util.Observable;
import java.util.Observer;

import nu.dll.lyskom.ConfInfo;

import org.lindev.androkom.App;
import org.lindev.androkom.KomServer;
import org.lindev.androkom.LocalBinder;
import org.lindev.androkom.LookupNameTask;
import org.lindev.androkom.LookupNameTask.LookupType;
import org.lindev.androkom.LookupNameTask.RunOnSuccess;
import org.lindev.androkom.R;
import org.lindev.androkom.im.IMLogger;
import org.lindev.androkom.im.SendMessageTask;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class IMConversationList extends ListActivity implements ServiceConnection, Observer, OnClickListener {
    public static final String TAG = "Androkom IMConversationList";

    private static final int MAX_CONVERSATIONS = 50;
    private static final int BACKGROUND_COLOR_ALL_READ = Color.BLACK;
    private static final int BACKGROUND_COLOR_UNREAD = 0xff303060;

    public static final String INTENT_CONVERSATION_LIST_RECIPIENT = "recipient-str";

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
            final int numUnseen = cursor.getInt(cursor.getColumnIndex(IMLogger.COL_NUM_UNSEEN));

            final TextView tv = (TextView) view;
            if (numUnseen == 0) {
                tv.setBackgroundColor(BACKGROUND_COLOR_ALL_READ);
                tv.setText(convStr + " <" + convId + ">");
            }
            else {
                tv.setBackgroundColor(BACKGROUND_COLOR_UNREAD);
                tv.setText("(" + numUnseen + ") " + convStr + " <" + convId + ">");
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.im_conversation_list_layout);

        mSendButton = (Button) findViewById(R.id.send);
        mRecipientField = (EditText) findViewById(R.id.recipient);
        mMessageField = (EditText) findViewById(R.id.message);

        mSendButton.setOnClickListener(this);

        getApp().doBindService(this);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        mRecipientField.setText("");
        mMessageField.setText("");
        initialize(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIMLogger != null) {
            mIMLogger.addObserver(this);
            updateView(true);
        }
    }

    @Override
    protected void onPause() {
        if (mIMLogger != null) {
            mIMLogger.deleteObserver(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mIMLogger != null) {
            mCursor.close();
        }
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.imconversationlist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.imconversationlist_menu_id:
            clearAllHistory();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void clearAllHistory() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(IMConversationList.this);
        builder.setTitle(getString(R.string.im_clear_history_q));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int which) {
                mIMLogger.clearAllHistory();
            }
        });
        builder.setNegativeButton(getString(R.string.no), null);
        builder.create().show();
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

    public void onClick(final View view) {
        if (mIMLogger == null || view != mSendButton) {
            return;
        }
        final String recipient = mRecipientField.getText().toString();
        final String msg = mMessageField.getText().toString();

        new LookupNameTask(this, mKom, recipient, LookupType.LOOKUP_BOTH, new RunOnSuccess() {
            public void run(final ConfInfo conf) {
                new SendMessageTask(IMConversationList.this, mKom, conf.getNo(), conf.getNameString(), msg, new Runnable() {
                    public void run() {
                        final Intent intent = new Intent(IMConversationList.this, IMConversation.class);
                        intent.putExtra(IMConversation.INTENT_CONVERSATION_ID, conf.getNo());
                        intent.putExtra(IMConversation.INTENT_CONVERSATION_STR, conf.getNameString());
                        mRecipientField.setText("");
                        mMessageField.setText("");
                        startActivity(intent);
                    }
                }).execute();
            }
        }).execute();
    }

    private void initialize(final Intent intent) {
        if (intent.hasExtra(INTENT_CONVERSATION_LIST_RECIPIENT)) {
            final String recipient = intent.getStringExtra(INTENT_CONVERSATION_LIST_RECIPIENT);
            intent.removeExtra(INTENT_CONVERSATION_LIST_RECIPIENT);
            if (recipient != null) {
                mRecipientField.setText(recipient);
            }
        }

        mIMLogger = mKom.imLogger;
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mIMLogger.getConversations(MAX_CONVERSATIONS);
        setListAdapter(new IMConvListCursorAdapter(this, mCursor));
        mIMLogger.addObserver(this);
        updateView(false);
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mKom = ((LocalBinder<KomServer>) service).getService();
        initialize(getIntent());
    }

    public void onServiceDisconnected(final ComponentName name) {
        mIMLogger = null;
    }

    private App getApp() {
        return (App) getApplication();
    }
}

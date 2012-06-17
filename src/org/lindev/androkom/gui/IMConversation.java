package org.lindev.androkom.gui;

import java.util.Observable;
import java.util.Observer;

import org.lindev.androkom.App;
import org.lindev.androkom.KomServer;
import org.lindev.androkom.LocalBinder;
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
import android.os.Message;
import android.provider.BaseColumns;
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

public class IMConversation extends ListActivity implements ServiceConnection, Observer, OnClickListener {
    public static final String TAG = "Androkom IMConversation";

    private static final int MAX_MESSAGES = 50;
    private static final int BACKGROUND_COLOR_READ = Color.BLACK;
    private static final int BACKGROUND_COLOR_UNREAD = 0xff303060;
    private static final String LATEST_SEEN = "latest-seen";

    public static final String INTENT_CONVERSATION_ID = "conversation-id";
    public static final String INTENT_CONVERSATION_STR = "conversation-str";

    private KomServer mKom = null;
    private IMLogger mIMLogger = null;
    private int mConvId = -1;
    private String mConvStr = null;
    private Cursor mCursor = null;
    private int mLatestSeen = -1;

    private Button mSendButton = null;
    private EditText mTextField = null;

    public static class IMConvListView extends ListView {
        private final IMConversation mIMConversation;

        public IMConvListView(final Context context, final AttributeSet attr) {
            super(context, attr);
            mIMConversation = (IMConversation) context;
        }

        @Override
        protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
            mIMConversation.updateView(false);
        }
    }

    private class IMConvCursorAdapter extends CursorAdapter {
        private IMConvCursorAdapter(final Context context, final Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.message_log, null);
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            final int msgId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            final int fromId = cursor.getInt(cursor.getColumnIndex(IMLogger.COL_FROM_ID));
            final String fromStr = cursor.getString(cursor.getColumnIndex(IMLogger.COL_FROM_STR));
            final String msg = cursor.getString(cursor.getColumnIndex(IMLogger.COL_MSG));

            final TextView tv = (TextView) view;
            if (msgId <= mLatestSeen) {
                tv.setBackgroundColor(BACKGROUND_COLOR_READ);
            }
            else {
                tv.setBackgroundColor(BACKGROUND_COLOR_UNREAD);
            }

            final StringBuilder sb = new StringBuilder();
            if (fromId == mKom.getUserId()) {
                sb.append(getString(R.string.im_you));
            }
            else {
                sb.append(fromStr).append(getString(R.string.im_prompt));
            }
            sb.append(msg);
            tv.setText(sb.toString());
        }
    }

    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLatestSeen = -1;
        if (savedInstanceState != null && savedInstanceState.containsKey(LATEST_SEEN)) {
            mLatestSeen = savedInstanceState.getInt(LATEST_SEEN);
        }

        setContentView(R.layout.im_conversation_layout);
        mSendButton = (Button) findViewById(R.id.send);
        mTextField = (EditText) findViewById(R.id.message);

        mSendButton.setOnClickListener(this);
        getApp().doBindService(this);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        mLatestSeen = -1;
        mTextField.setText("");
        initialize(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIMLogger != null) {
            mIMLogger.addObserver(this);
            updateView(true);
            mIMLogger.updateLatestSeen(mConvId);
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
        inflater.inflate(R.menu.imconversation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.imconversation_menu_id:
            clearHistory();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void clearHistory() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(IMConversation.this);
        builder.setTitle(getString(R.string.im_clear_history_q));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int which) {
                mIMLogger.clearConversationHistory(mConvId);
            }
        });
        builder.setNegativeButton(getString(R.string.yes), null);
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

    public void update(final Observable observable, final Object obj) {
        if (observable != mIMLogger) {
            return;
        }
        final Message msg = (Message) obj;
        switch (msg.what) {
        case IMLogger.NEW_MESSAGE:
            if (mConvId == msg.getData().getInt(IMLogger.MESSAGE_CONV_ID)) {
                // New message in this conversation
                updateView(true);
                mIMLogger.updateLatestSeen(mConvId);
            }
            break;
        case IMLogger.HISTORY_CLEARED:
            if (!msg.getData().containsKey(IMLogger.MESSAGE_CONV_ID)) {
                // All history cleared
                updateView(true);
            }
            else if (mConvId == msg.getData().getInt(IMLogger.MESSAGE_CONV_ID)) {
                // This conversation cleared
                updateView(true);
            }
            break;
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        outState.putInt(LATEST_SEEN, mLatestSeen);
        super.onSaveInstanceState(outState);
    }

    public void onClick(final View view) {
        if (mIMLogger == null || view != mSendButton) {
            return;
        }
        final String msg = mTextField.getText().toString();

        new SendMessageTask(this, mKom, mConvId, mConvStr, msg, new Runnable() {
            public void run() {
                mTextField.setText("");
            }
        }).execute();
    }

    private void initialize(final Intent intent) {
        mConvId = intent.getIntExtra(INTENT_CONVERSATION_ID, 0);
        mConvStr = intent.getStringExtra(INTENT_CONVERSATION_STR);
        setTitle(mConvStr);

        mIMLogger = mKom.imLogger;
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mIMLogger.getMessages(mConvId, MAX_MESSAGES);
        if (mLatestSeen < 0) {
            mLatestSeen = mIMLogger.getLatestSeen(mConvId);
        }
        mIMLogger.updateLatestSeen(mConvId);
        setListAdapter(new IMConvCursorAdapter(this, mCursor));
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

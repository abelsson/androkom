package org.lindev.androkom.gui;

import java.util.Observable;
import java.util.Observer;

import org.lindev.androkom.App;
import org.lindev.androkom.KomServer;
import org.lindev.androkom.R;
import org.lindev.androkom.im.IMLogger;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class IMConversationList extends ListActivity implements ServiceConnection, Observer {
    public static final String TAG = "Androkom";

    private static final int MAX_CONVERSATIONS = 50;

    private IMLogger mIMLogger = null;
    private CursorAdapter mAdapter = null;
    private Cursor mCursor = null;

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

            final TextView tv = (TextView) view;
            tv.setText("(" + numMsg + ") " + convStr + " <" + convId + ">");
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_main);
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

    private void updateView(final boolean requery) {
        if (mIMLogger == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            public void run() {
                if (requery) {
                    mCursor.requery();
                    mAdapter.notifyDataSetChanged();
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
        intent.putExtra("conversation-id", convId);
        intent.putExtra("conversation-str", convStr);

        startActivity(intent);
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mIMLogger = ((KomServer.LocalBinder) service).getService().imLogger;
        mCursor = mIMLogger.getConversations(MAX_CONVERSATIONS);
        mAdapter = new IMConvListCursorAdapter(this, mCursor);
        mIMLogger.addObserver(this);
        setListAdapter(mAdapter);
        updateView(false);
    }

    public void onServiceDisconnected(final ComponentName name) {
        mIMLogger = null;
    }

    private App getApp() {
        return (App) getApplication();
    }
}

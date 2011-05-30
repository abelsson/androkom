package org.lindev.androkom;

import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.widget.ArrayAdapter;

public class MessageLog extends ListActivity implements AsyncMessageSubscriber, ServiceConnection
{
    public static final String TAG = "Androkom";

    private ArrayAdapter<String> mAdapter;
    private int mLogIndex;
    private KomServer mKom;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.message_main);
        mAdapter = new ArrayAdapter<String>(this, R.layout.message_log);
        setListAdapter(mAdapter);
        mLogIndex = 0;

        getApp().doBindService(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mKom != null) {
            mKom.addAsyncSubscriber(this);
            update();
        }
    }

    @Override
    public void onPause() {
        if (mKom != null) {
            mKom.removeAsyncSubscriber(this);
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    private void update() {
        if (mKom != null) {
            final AsyncMessages am = mKom.asyncMessagesHandler;

            while (mLogIndex < am.getLog().size()) {
                final Message msg = am.getLog().get(mLogIndex++);
                String msgStr = am.messageAsString(msg);
                mAdapter.add(msgStr);
            }

            final int count = getListView().getCount();
            if (count > 0) {
                mAdapter.notifyDataSetChanged();
                getListView().setSelection(count - 1);
            }
        }
    }

    public void asyncMessage(final Message msg) {
        update();
    }

    App getApp() {
        return (App) getApplication();
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mKom = ((KomServer.LocalBinder) service).getService();
        mKom.addAsyncSubscriber(this);
        update();
    }

    public void onServiceDisconnected(final ComponentName name) {
        mKom = null;
        mLogIndex = 0;
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
    }
}

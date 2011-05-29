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
    private AsyncMessages mAsyncMessages;
    private int mLogIndex;
    private KomServer mKom;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.message_main);
        getApp().doBindService(this);
        mAdapter = new ArrayAdapter<String>(this, R.layout.message_log);
        setListAdapter(mAdapter);
        mLogIndex = 0;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mAsyncMessages != null)
        {
            mAsyncMessages.subscribe(this);
            update();
        }
    }

    @Override
    public void onPause()
    {
        mAsyncMessages.unsubscribe(this);
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    private void update()
    {
        while (mLogIndex < mAsyncMessages.getLog().size())
        {
            final Message msg = mAsyncMessages.getLog().get(mLogIndex++);
            String msgStr = mAsyncMessages.messageAsString(msg);
            mAdapter.add(msgStr);
        }

        final int count = getListView().getCount();
        if (count > 0)
        {
            mAdapter.notifyDataSetChanged();
            getListView().smoothScrollToPosition(count - 1);
        }
    }

    public void asyncMessage(Message msg)
    {
        update();
    }

    App getApp()
    {
        return (App) getApplication();
    }

    public void onServiceConnected(ComponentName name, IBinder service)
    {
        mKom = ((KomServer.LocalBinder) service).getService();
        mAsyncMessages = mKom.asyncMessagesHandler;
        mAsyncMessages.subscribe(this);
        update();
    }

    public void onServiceDisconnected(ComponentName name)
    {
        mKom = null;
    }
}

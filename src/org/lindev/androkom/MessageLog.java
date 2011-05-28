package org.lindev.androkom;

import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Message;
import android.widget.ArrayAdapter;

public class MessageLog extends ListActivity implements AsyncMessageSubscriber
{
    public static final String TAG = "Androkom";

    private ArrayAdapter<String> mAdapter;
    private AsyncMessages asyncMessages;
    private int logIndex;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.message_main);
        mAdapter = new ArrayAdapter<String>(this, R.layout.message_log);
        setListAdapter(mAdapter);
        asyncMessages = ((App) getApplication()).getKom().asyncMessagesHandler;
        logIndex = 0;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        asyncMessages.subscribe(this);
        update();
    }

    @Override
    public void onPause()
    {
        asyncMessages.unsubscribe(this);
        super.onPause();
    }

    private void update()
    {
        while (logIndex < asyncMessages.getLog().size())
        {
            final Message msg = asyncMessages.getLog().get(logIndex++);
            String msgStr = asyncMessages.messageAsString(msg);
            mAdapter.add(msgStr);
        }

        mAdapter.notifyDataSetChanged();
        getListView().smoothScrollToPosition(getListView().getCount() - 1);
    }

    public void asyncMessage(Message msg)
    {
        update();
    }
}

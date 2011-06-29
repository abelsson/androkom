package org.lindev.androkom.im;

import java.util.Observable;
import java.util.Observer;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.R;
import org.lindev.androkom.gui.IMConversation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

public class IMNotification implements Observer {
    private final KomServer mKom;
    private final IMLogger mIMLogger;
    private final NotificationManager mNotificationManager;

    public IMNotification(final KomServer kom) {
        this.mKom = kom;
        this.mIMLogger = kom.imLogger;
        this.mNotificationManager = (NotificationManager) mKom.getSystemService(Context.NOTIFICATION_SERVICE);
        mIMLogger.addObserver(this);
    }

    private void newMessage(final int convId, final String convStr, final int fromId, final String fromStr,
            final int toId, final String toStr, final String msg) {
        final int myId = mKom.getUserId();
        final String tickerText = fromStr + ": " + msg;
        final long when = System.currentTimeMillis();
        final Notification notification = new Notification(R.drawable.icon, tickerText, when);
        notification.defaults |= Notification.DEFAULT_SOUND;

        String contentText = msg;
        if (toId != myId) {
            contentText = fromStr + ": " + contentText;
        }
        final Intent notificationIntent = new Intent(mKom, IMConversation.class);
        notificationIntent.putExtra(IMConversation.INTENT_CONVERSATION_ID, convId);
        notificationIntent.putExtra(IMConversation.INTENT_CONVERSATION_STR, convStr);
        PendingIntent contentIntent = PendingIntent.getActivity(mKom, 0, notificationIntent, 0);

        notification.setLatestEventInfo(mKom.getApplicationContext(), convStr, contentText, contentIntent);
        mNotificationManager.notify(convId, notification);
    }

    private void unreadUpdated(final int convId) {
        mNotificationManager.cancel(convId);
    }

    public void update(final Observable observable, final Object obj) {
        if (observable == mIMLogger) {
            final Message msg = (Message) obj;
            final Bundle data = msg.getData();
            switch (msg.what) {
            case IMLogger.NEW_MESSAGE:
            {
                final int convId = data.getInt(IMLogger.MESSAGE_CONV_ID);
                final String convStr = data.getString(IMLogger.MESSAGE_CONV_STR);
                final int fromId = data.getInt(IMLogger.MESSAGE_FROM_ID);
                final String fromStr = data.getString(IMLogger.MESSAGE_FROM_STR);
                final int toId = data.getInt(IMLogger.MESSAGE_TO_ID);
                final String toStr = data.getString(IMLogger.MESSAGE_TO_STR);
                final String body = data.getString(IMLogger.MESSAGE_BODY);
                newMessage(convId, convStr, fromId, fromStr, toId, toStr, body);
                break;
            }
            case IMLogger.UNREAD_UPDATE:
            {
                final int convId = data.getInt(IMLogger.MESSAGE_CONV_ID);
                unreadUpdated(convId);
                break;
            }
            }
        }
    }
}

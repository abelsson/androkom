package org.lindev.androkom.im;

import java.util.Observable;
import java.util.Observer;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.R;
import org.lindev.androkom.gui.IMConversation;
import org.lindev.androkom.gui.IMConversationList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

public class IMNotification implements Observer {
    private static final int NOTIFICATION_ID = 4711;

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

        final String tickerText = fromStr + ": " + msg;
        final String contentTitle;
        final String contentText;
        final Intent notificationIntent;

        final int unseenConvs = mIMLogger.numConversationsWithUnseen();
        if (unseenConvs > 1) {
            final int unseenMessages = mIMLogger.numUnseenMessages();
            contentTitle = "New Messages";
            contentText = unseenMessages + " new messages in " + unseenConvs + " conversations.";
            notificationIntent = new Intent(mKom, IMConversationList.class);
        }
        else {
            final int unseenInConv = mIMLogger.numUnseenInConversation(convId);
            contentTitle = convStr;
            if (unseenInConv > 1) {
                contentText = unseenInConv + " new messages.";
            }
            else if (toId == mKom.getUserId()) {
                contentText = msg;
            }
            else {
                contentText = fromStr + ": " + msg;
            }
            notificationIntent = new Intent(mKom, IMConversation.class);
            notificationIntent.putExtra(IMConversation.INTENT_CONVERSATION_ID, convId);
            notificationIntent.putExtra(IMConversation.INTENT_CONVERSATION_STR, convStr);
        }

        final PendingIntent contentIntent = PendingIntent.getActivity(mKom, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final Notification notification = new Notification(R.drawable.icon, tickerText, System.currentTimeMillis());
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(mKom.getApplicationContext(), contentTitle, contentText, contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void update(final Observable observable, final Object obj) {
        if (observable != mIMLogger) {
            return;
        }
        if (!(obj instanceof Message) || ((Message) obj).what != IMLogger.NEW_MESSAGE) {
            return;
        }

        final Bundle data = ((Message) obj).getData();
        final int convId = data.getInt(IMLogger.MESSAGE_CONV_ID);
        final String convStr = data.getString(IMLogger.MESSAGE_CONV_STR);
        final int fromId = data.getInt(IMLogger.MESSAGE_FROM_ID);
        final String fromStr = data.getString(IMLogger.MESSAGE_FROM_STR);
        final int toId = data.getInt(IMLogger.MESSAGE_TO_ID);
        final String toStr = data.getString(IMLogger.MESSAGE_TO_STR);
        final String body = data.getString(IMLogger.MESSAGE_BODY);

        if (fromId != mKom.getUserId()) {
            newMessage(convId, convStr, fromId, fromStr, toId, toStr, body);
        }
    }
}

package org.lindev.androkom.im;

import java.util.Observable;

import org.lindev.androkom.AsyncMessages;
import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;
import org.lindev.androkom.KomServer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;

public class IMLogger extends Observable implements AsyncMessageSubscriber {
    public static final String TAG = "Androkom IMLogger";

    private final SQLHelper dbHelper;
    private final KomServer mKom;
    private final Object mWriteLock;

    private static final String TABLE_MSG = "table_msg";
    private static final String TABLE_CONV = "table_conv";

    private static final String INDEX_MSG_MY_ID_CONV_ID = "index_msg_my_id_conv_id";
    private static final String INDEX_CONV_MY_ID_CONV_ID = "index_conv_my_id_conv_id";

    public static final String COL_CONV_ID = "col_conv_id";
    public static final String COL_MY_ID = "col_my_id";

    public static final String COL_CONV_STR = "col_conv_str";
    public static final String COL_NUM_MSG = "col_num_msgs";
    public static final String COL_LATEST_MSG = "col_latest_im";
    public static final String COL_LATEST_SEEN = "col_latest_seen";
    public static final String COL_NUM_UNSEEN = "col_num_unseen";

    public static final String COL_FROM_ID = "col_from_id";
    public static final String COL_TO_ID = "col_to_id";
    public static final String COL_FROM_STR = "col_from_str";
    public static final String COL_TO_STR = "col_to_str";
    public static final String COL_TIMESTAMP = "col_timestamp";
    public static final String COL_MSG = "col_msg";

    public static final int NEW_MESSAGE = 1;
    public static final int UNREAD_UPDATE = 2;
    public static final int HISTORY_CLEARED = 3;

    public static final String MESSAGE_CONV_ID = "message-conv-id";
    public static final String MESSAGE_CONV_STR = "message-conv-str";
    public static final String MESSAGE_FROM_ID = "message-from-id";
    public static final String MESSAGE_FROM_STR = "message-from-str";
    public static final String MESSAGE_TO_ID = "message-to-id";
    public static final String MESSAGE_TO_STR = "message-to-str";
    public static final String MESSAGE_BODY = "message-body";

    private class SQLHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "lyskom_im_log.db";
        private static final int DATABASE_VERSION = 6;

        public SQLHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            String sql;
            sql = "CREATE TABLE " + TABLE_MSG + " (" +
                  BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                  COL_MY_ID + " INTEGER NOT NULL, " +
                  COL_CONV_ID + " INTEGER NOT NULL, " +
                  COL_FROM_ID + " INTEGER NOT NULL, " +
                  COL_FROM_STR + " TEXT NOT NULL, " +
                  COL_TO_ID + " INTEGER NOT NULL, " +
                  COL_TO_STR + " TEXT NOT NULL, " +
                  COL_TIMESTAMP + " INTEGER NOT NULL, " +
                  COL_MSG + " TEXT NOT NULL)";
            db.execSQL(sql);

            sql = "CREATE INDEX " + INDEX_MSG_MY_ID_CONV_ID + " ON " +
                  TABLE_MSG + " (" + COL_MY_ID + ", " + COL_CONV_ID + ")";
            db.execSQL(sql);

            sql = "CREATE TABLE " + TABLE_CONV + " (" +
                  BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                  COL_MY_ID + " INTEGER NOT NULL, " +
                  COL_CONV_ID + " INTEGER NOT NULL, " +
                  COL_CONV_STR + " TEXT NOT NULL, " +
                  COL_NUM_MSG + " INTEGER NOT NULL, " +
                  COL_LATEST_MSG + " INTEGER NOT NULL, " +
                  COL_LATEST_SEEN + " INTEGER NOT NULL, " +
                  COL_NUM_UNSEEN + " INTEGER NOT NULL, " +
                  COL_TIMESTAMP + " INTEGER NOT NULL, " +
                  "UNIQUE (" + COL_MY_ID + ", " + COL_CONV_ID + "))";
            db.execSQL(sql);

            sql = "CREATE INDEX " + INDEX_CONV_MY_ID_CONV_ID + " ON " +
                  TABLE_CONV + " (" + COL_MY_ID + ", " + COL_CONV_ID + ")";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            // This is the Easy Ugly Fix. Deal with it or implement it yourself. :)
            db.execSQL("DROP TABLE " + TABLE_MSG);
            db.execSQL("DROP TABLE " + TABLE_CONV);
            onCreate(db);
        }
    }

    public IMLogger(final KomServer kom) {
        this.mKom = kom;
        this.dbHelper = new SQLHelper(mKom);
        this.mWriteLock = new Object();
    }

    public void close() {
        dbHelper.close();
    }

    private static final String INSERT_CONV = "INSERT OR IGNORE INTO " + TABLE_CONV + " (" + COL_MY_ID + ", " +
            COL_CONV_ID + ", " + COL_CONV_STR + ", " + COL_NUM_MSG + ", " + COL_LATEST_MSG + ", " +
            COL_LATEST_SEEN + ", " + COL_NUM_UNSEEN + ", " + COL_TIMESTAMP + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_CONV = "UPDATE " + TABLE_CONV + " SET " + COL_CONV_STR + " = ?, " +
            COL_NUM_MSG + " = " + COL_NUM_MSG + " + 1, " + COL_LATEST_MSG + " = ?, " + COL_NUM_UNSEEN + " = " +
            COL_NUM_UNSEEN + " + 1, " + COL_TIMESTAMP + " = ? WHERE " + COL_MY_ID + " = ? AND " + COL_CONV_ID + " = ?";

    private void logIM(final Integer myId, final Integer fromId, final String fromStr, final Integer toId,
            final String toStr, final Integer convId, final String convStr, final String msg) {
        final Long timestamp = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(COL_MY_ID, myId);
        values.put(COL_CONV_ID, convId);
        values.put(COL_FROM_ID, fromId);
        values.put(COL_FROM_STR, fromStr);
        values.put(COL_TO_ID, toId);
        values.put(COL_TO_STR, toStr);
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_MSG, msg);

        synchronized (mWriteLock) {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Insert the message into the message table
            final Long rowId = db.insert(TABLE_MSG, null, values);

            // Create a new empty record for the conversation if it doesn't already exist
            final Integer zero = Integer.valueOf(0);
            final Object insertArgs[] = { myId, convId, "", zero, zero, zero, zero, zero };
            db.execSQL(INSERT_CONV, insertArgs);

            // Update conversation record
            final Object[] updateArgs = { convStr, rowId, timestamp, myId, convId };
            db.execSQL(UPDATE_CONV, updateArgs);
        }

        // Notify observers that the database has changed.
        final Bundle data = new Bundle();
        data.putInt(MESSAGE_CONV_ID, convId);
        data.putString(MESSAGE_CONV_STR, convStr);
        data.putInt(MESSAGE_FROM_ID, fromId);
        data.putString(MESSAGE_FROM_STR, fromStr);
        data.putInt(MESSAGE_TO_ID, toId);
        data.putString(MESSAGE_TO_STR, toStr);
        data.putString(MESSAGE_BODY, msg);
        final Message message = new Message();
        message.what = NEW_MESSAGE;
        message.setData(data);
        setChanged();
        notifyObservers(message);
    }

    private static final String[] SELECT_CONV = { BaseColumns._ID, COL_CONV_ID, COL_CONV_STR, COL_NUM_UNSEEN };
    private static final String ORDER_BY_CONV = COL_LATEST_MSG;

    public Cursor getConversations(final int max) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.query(TABLE_CONV, SELECT_CONV, null, null, null, null, ORDER_BY_CONV, null);
    }

    private static final String SELECT_MSG = BaseColumns._ID + ", " + COL_FROM_ID + ", " + COL_FROM_STR + ", " +
            COL_TO_STR + ", " + COL_MSG;
    private static final String WHERE = COL_MY_ID + " = ? AND " + COL_CONV_ID + " = ?";
    private static final String SUBQUERY_MSG = "SELECT " + SELECT_MSG + " FROM " + TABLE_MSG + " WHERE " + WHERE +
            " ORDER BY " + BaseColumns._ID + " DESC LIMIT ?";
    private static final String QUERY_MSG = "SELECT " + SELECT_MSG + " FROM (" + SUBQUERY_MSG + ") ORDER BY " +
            BaseColumns._ID + " ASC";

    public Cursor getMessages(final int convId, final int max) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String[] args = { Integer.toString(mKom.getUserId()), Integer.toString(convId), Integer.toString(max) };
        return db.rawQuery(QUERY_MSG, args);
    }

    private static final String[] COLS_LATEST_SEEN = { COL_LATEST_SEEN };

    public int getLatestSeen(final int convId) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String[] whereArgs = { Integer.toString(mKom.getUserId()), Integer.toString(convId) };
        final Cursor cursor = db.query(TABLE_CONV, COLS_LATEST_SEEN, WHERE, whereArgs, null, null, null);
        int latestSeen = 0;
        if (cursor.moveToFirst()) {
            latestSeen = cursor.getInt(cursor.getColumnIndex(COL_LATEST_SEEN));
        }
        cursor.close();
        return latestSeen;
    }

    private static final String QUERY_UPDATE_LATEST = "UPDATE " + TABLE_CONV + " SET " + COL_LATEST_SEEN  + " = " +
            COL_LATEST_MSG + ", " + COL_NUM_UNSEEN + " = 0 WHERE " + WHERE;

    public void updateLatestSeen(final int convId) {
        final Object[] args = { Integer.valueOf(mKom.getUserId()), Integer.valueOf(convId) };
        synchronized (mWriteLock) {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL(QUERY_UPDATE_LATEST, args);
        }

        // Notify observers that the database has changed.
        final Bundle data = new Bundle();
        data.putInt(MESSAGE_CONV_ID, convId);
        final Message message = new Message();
        message.what = UNREAD_UPDATE;
        message.setData(data);
        setChanged();
        notifyObservers(message);
    }

    private static final String QUERY_NUM_UNSEEN = "SELECT SUM(" + COL_NUM_UNSEEN + ") FROM " + TABLE_CONV +
            " WHERE " + COL_MY_ID + " = ?";

    public int numUnseenMessages() {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String[] args = { Integer.toString(mKom.getUserId()) };
        final Cursor cursor = db.rawQuery(QUERY_NUM_UNSEEN, args);
        int numUnread = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            numUnread = cursor.getInt(0);
        }
        cursor.close();
        return numUnread;
    }

    private static final String[] COLS_UNSEEN_IN_CONV = { COL_NUM_UNSEEN };

    public int numUnseenInConversation(final int convId) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String[] whereArgs = { Integer.toString(mKom.getUserId()), Integer.toString(convId) };
        final Cursor cursor = db.query(TABLE_CONV, COLS_UNSEEN_IN_CONV, WHERE, whereArgs, null, null, null);
        int numUnseen = 0;
        if (cursor.moveToFirst()) {
            numUnseen = cursor.getInt(cursor.getColumnIndex(COL_NUM_UNSEEN));
        }
        cursor.close();
        return numUnseen;
    }

    private static final String QUERY_UNSEEN_CONVS = "SELECT COUNT(" + BaseColumns._ID + ") FROM " + TABLE_CONV +
            " WHERE " + COL_MY_ID + " = ? AND " + COL_NUM_UNSEEN + " > 0";

    public int numConversationsWithUnseen() {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String[] args = { Integer.toString(mKom.getUserId()) };
        final Cursor cursor = db.rawQuery(QUERY_UNSEEN_CONVS, args);
        int numConvWithUnseen = 0;
        if (cursor.moveToFirst()) {
            numConvWithUnseen = cursor.getInt(0);
        }
        cursor.close();
        return numConvWithUnseen;
    }

    public void clearConversationHistory(final int convId) {
        final String[] whereArgs = { Integer.toString(mKom.getUserId()), Integer.toString(convId) };
        synchronized (mWriteLock) {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(TABLE_MSG, WHERE, whereArgs);
            db.delete(TABLE_CONV, WHERE, whereArgs);
        }
        // Notify observers that the database has changed.
        final Bundle data = new Bundle();
        data.putInt(MESSAGE_CONV_ID, convId);
        final Message message = new Message();
        message.what = HISTORY_CLEARED;
        message.setData(data);
        setChanged();
        notifyObservers(message);
    }

    private final String CLEAR_ALL_HISTORY_WHERE = COL_MY_ID + " = ?";

    public void clearAllHistory() {
        final String[] whereArgs = { Integer.toString(mKom.getUserId()) };
        synchronized (mWriteLock) {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(TABLE_MSG, CLEAR_ALL_HISTORY_WHERE, whereArgs);
            db.delete(TABLE_CONV, CLEAR_ALL_HISTORY_WHERE, whereArgs);
        }
        // Notify observers that the database has changed.
        final Message message = new Message();
        message.what = HISTORY_CLEARED;
        setChanged();
        notifyObservers(message);
    }

    public void asyncMessage(final Message msg) {
        if (msg.what != nu.dll.lyskom.Asynch.send_message) {
            return;
        }

        final int myId = mKom.getUserId();

        final int fromId = msg.getData().getInt(AsyncMessages.ASYNC_MESSAGE_FROM_ID);
        final String fromStr = msg.getData().getString(AsyncMessages.ASYNC_MESSAGE_FROM);

        final int toId = msg.getData().getInt(AsyncMessages.ASYNC_MESSAGE_TO_ID);
        final String toStr = msg.getData().getString(AsyncMessages.ASYNC_MESSAGE_TO);

        final int convId;
        final String convStr;

        final String msgStr = msg.getData().getString(AsyncMessages.ASYNC_MESSAGE_MSG);

        // If we received a message from the server about a message we sent ourselves, it shouldn't be stored as that
        // is already stored when it was sent.
        if (fromId == myId) {
            return;
        }

        // For personal messages, conversations are tracked by the other person (fromId in this context)
        // For group messages, they are tracked by the conference sent to (toId in this context)
        if (toId == myId) {
            convId = fromId;
            convStr = fromStr;
        }
        else {
            convId = toId;
            convStr = toStr;
        }

        logIM(myId, fromId, fromStr, toId, toStr, convId, convStr, msgStr);
    }

    public void sendMessage(final int toId, final String msgStr) {
        final int myId = mKom.getUserId();
        final int fromId = myId;
        String fromStr = null;
        try {
            fromStr = mKom.getConferenceName(fromId);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "sendMessage InterruptedException 1");
        }
        String toStr = null;
        try {
            toStr = mKom.getConferenceName(toId);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "sendMessage InterruptedException 2");
        }
        final int convId = toId;
        final String convStr = toStr;

        logIM(myId, fromId, fromStr, toId, toStr, convId, convStr, msgStr);
    }
}

package org.lindev.androkom.im;

import java.util.Observable;

import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;
import org.lindev.androkom.KomServer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Message;
import android.provider.BaseColumns;

public class IMLogger extends Observable implements AsyncMessageSubscriber {
    private final SQLHelper dbHelper;
    private final KomServer mKom;

    private static final String TABLE_MSG = "table_msg";
    private static final String TABLE_CONV = "table_conv";

    private static final String INDEX_MY_ID_CONV_ID = "index_my_id_conv_id";

    public static final String COL_CONV_ID = "col_conv_id";
    public static final String COL_MY_ID = "col_my_id";

    public static final String COL_CONV_STR = "col_conv_str";
    public static final String COL_NUM_MSG = "col_num_msgs";
    public static final String COL_LATEST_MSG = "col_latest_im";

    public static final String COL_FROM_ID = "col_from_id";
    public static final String COL_TO_ID = "col_to_id";
    public static final String COL_FROM_STR = "col_from_str";
    public static final String COL_TO_STR = "col_to_str";
    public static final String COL_TIMESTAMP = "col_timestamp";
    public static final String COL_MSG = "col_msg";

    private class SQLHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "lyskom_im_log.db";
        private static final int DATABASE_VERSION = 1;

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
                  COL_MSG + " TEXT NOT NULL);";
            db.execSQL(sql);

            sql = "CREATE INDEX " + INDEX_MY_ID_CONV_ID + " ON " +
                  TABLE_MSG + " (" + COL_MY_ID + ", " + COL_CONV_ID + ");";
            db.execSQL(sql);

            sql = "CREATE TABLE " + TABLE_CONV + " (" +
                  BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                  COL_MY_ID + " INTEGER NOT NULL, " +
                  COL_CONV_ID + " INTEGER NOT NULL, " +
                  COL_CONV_STR + " TEXT NOT NULL, " +
                  COL_NUM_MSG + " INTEGER NOT NULL, " +
                  COL_LATEST_MSG + " INTEGER NOT NULL, " +
                  "UNIQUE (" + COL_MY_ID + ", " + COL_CONV_ID + "));";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        }
    }

    public IMLogger(final KomServer kom) {
        this.mKom = kom;
        this.dbHelper = new SQLHelper(mKom);
    }

    public void close() {
        dbHelper.close();
    }

    private static final String UPDATE_CONV =
            "UPDATE " + TABLE_CONV + " SET " +
            COL_CONV_STR + " = ?, " +
            COL_NUM_MSG + " = " + COL_NUM_MSG + " + 1, " +
            COL_LATEST_MSG + " = ? WHERE " +
            COL_MY_ID + " = ? AND " +
            COL_CONV_ID + " = ?;";

    private void logIM(final int myId, final int fromId, final String fromStr, final int toId, final String toStr,
            final int convId, final String convStr, final String msg) {

        final SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Insert the message into the message table
        ContentValues values = new ContentValues();
        values.put(COL_MY_ID, myId);
        values.put(COL_CONV_ID, convId);
        values.put(COL_FROM_ID, fromId);
        values.put(COL_FROM_STR, fromStr);
        values.put(COL_TO_ID, toId);
        values.put(COL_TO_STR, toStr);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_MSG, msg);
        final long rowId = db.insert(TABLE_MSG, null, values);

        // Create a new empty record for the conversation if it doesn't already exist
        values = new ContentValues();
        values.put(COL_MY_ID, myId);
        values.put(COL_CONV_ID, convId);
        values.put(COL_CONV_STR, "");
        values.put(COL_NUM_MSG, 0);
        values.put(COL_LATEST_MSG, 0);
        db.insertWithOnConflict(TABLE_CONV, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        // Update conversation record
        final Object[] updateArgs =
            { convStr, Long.valueOf(rowId), Integer.valueOf(myId), Integer.valueOf(convId) };
        db.execSQL(UPDATE_CONV, updateArgs);

        // Notify observers that the database has changed. Send the conversation id as argument
        setChanged();
        notifyObservers(Integer.valueOf(convId));
    }

    private static final String[] SELECT_CONV = { BaseColumns._ID, COL_CONV_ID, COL_CONV_STR, COL_NUM_MSG };
    private static final String ORDER_BY_CONV = COL_LATEST_MSG;

    public Cursor getConversations(final int max) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String limit = Integer.toString(max);
        return db.query(TABLE_CONV, SELECT_CONV, null, null, null, null, ORDER_BY_CONV, null);
    }

    private static final String[] SELECT_MSG = { BaseColumns._ID, COL_FROM_STR, COL_TO_STR, COL_MSG };
    private static final String WHERE_MSG = COL_MY_ID + " = ? AND " + COL_CONV_ID + " = ?";
    private static final String ORDER_BY_MSG = BaseColumns._ID;

    public Cursor getMessages(final int convId, final int max) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final String[] whereArgs = { Integer.toString(mKom.getUserId()), Integer.toString(convId) };
        final String limit = Integer.toString(max);
        return db.query(TABLE_MSG, SELECT_MSG, WHERE_MSG, whereArgs, null, null, ORDER_BY_MSG, null);
    }

    public void asyncMessage(final Message msg) {
        if (msg.what != nu.dll.lyskom.Asynch.send_message) {
            return;
        }

        final int myId = mKom.getUserId();

        final int fromId = msg.getData().getInt("from-id");
        final String fromStr = msg.getData().getString("from");

        final int toId = msg.getData().getInt("to-id");
        final String toStr = msg.getData().getString("to");

        final int convId;
        final String convStr;

        final String msgStr = msg.getData().getString("msg");

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
        final String fromStr = mKom.getConferenceName(fromId);
        final String toStr = mKom.getConferenceName(toId);
        final int convId = toId;
        final String convStr = toStr;

        logIM(myId, fromId, fromStr, toId, toStr, convId, convStr, msgStr);
    }
}

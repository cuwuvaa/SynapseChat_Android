package com.example.synapsechat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME    = "chat.db";
    private static final int    DB_VERSION = 1;

    public static final String TABLE_MESSAGES  = "messages";
    public static final String COL_ID          = "id";
    public static final String COL_SESSION_ID  = "session_id";
    public static final String COL_SENDER      = "sender";
    public static final String COL_TEXT        = "text";
    public static final String COL_TIMESTAMP   = "timestamp";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_MESSAGES + " ("
                + COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SESSION_ID + " TEXT, "
                + COL_SENDER     + " TEXT, "
                + COL_TEXT       + " TEXT, "
                + COL_TIMESTAMP  + " INTEGER"
                + ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    /** Вставить новое сообщение в БД */
    public void insertMessage(String sessionId, String sender, String text, long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SESSION_ID, sessionId);
        cv.put(COL_SENDER,     sender);
        cv.put(COL_TEXT,       text);
        cv.put(COL_TIMESTAMP,  timestamp);
        db.insert(TABLE_MESSAGES, null, cv);
        long row = db.insert(TABLE_MESSAGES, null, cv);
        if (row == -1) {
            Log.e("DBHelper", "Insert failed for session " + sessionId);
        }
    }

    /** Получить все сообщения для данной сессии, отсортированные по времени */
    public List<ChatMessage> getMessagesForSession(String sessionId) {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_MESSAGES,
                null,
                COL_SESSION_ID + " = ?",
                new String[]{ sessionId },
                null, null,
                COL_TIMESTAMP + " ASC"
        );

        if (c.moveToFirst()) {
            do {
                String sender    = c.getString(c.getColumnIndexOrThrow(COL_SENDER));
                String text      = c.getString(c.getColumnIndexOrThrow(COL_TEXT));
                long   timestamp = c.getLong  (c.getColumnIndexOrThrow(COL_TIMESTAMP));
                list.add(new ChatMessage(sessionId, sender, text, timestamp));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    /** Модель для хранения строк из БД */
    public static class ChatMessage {
        public final String sessionId;
        public final String sender;
        public final String text;
        public final long   timestamp;

        public ChatMessage(String sessionId,
                           String sender,
                           String text,
                           long timestamp) {
            this.sessionId = sessionId;
            this.sender    = sender;
            this.text      = text;
            this.timestamp = timestamp;
        }
    }
}

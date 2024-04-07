package com.example.baitaplon.helpers;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.baitaplon.models.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "message.db";
    private static final int DATABASE_VERSION = 1;

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS messages ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sender_id TEXT, " +
                "receiver_id TEXT, " +
                "message TEXT, " +
                "timestamp TEXT, " +
                "image TEXT, " +
                "file_url TEXT, " +
                "file_name TEXT, " +
                "location TEXT " +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Xử lý nâng cấp database (nếu cần)
    }

    public void addMessage(String senderId, String receiverId, String message, String timestamp, String image, String fileUrl, String fileName, String location) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("sender_id", senderId);
        values.put("receiver_id", receiverId);
        values.put("message", message);
        values.put("timestamp", timestamp);
        values.put("image", image);
        values.put("file_url", fileUrl);
        values.put("file_name", fileName);
        values.put("location", fileName);

        db.insert("messages", null, values);
        db.close();
    }
    @SuppressLint("Range")
    public List<Message> getAllMessages() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query("messages", null, null, null, null, null, "timestamp ASC");

        List<Message> messages = new ArrayList<>();
        while (cursor.moveToNext()) {
            Message message = new Message();
            message.setSenderId(cursor.getString(cursor.getColumnIndex("sender_id")));
            message.setReceiverId(cursor.getString(cursor.getColumnIndex("receiver_id")));
            message.setMessage(cursor.getString(cursor.getColumnIndex("message")));
            message.setDateTime(cursor.getString(cursor.getColumnIndex("timestamp")));
            message.setImage(cursor.getString(cursor.getColumnIndex("image")));
            message.setFileURL(cursor.getString(cursor.getColumnIndex("file_url")));
            message.setFileName(cursor.getString(cursor.getColumnIndex("file_name")));
            message.setLocation(cursor.getString(cursor.getColumnIndex("location")));
            messages.add(message);
        }

        cursor.close();
        return messages;
    }





}


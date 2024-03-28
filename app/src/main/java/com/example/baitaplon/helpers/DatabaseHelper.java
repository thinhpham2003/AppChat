package com.example.baitaplon.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "chat_database";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SENDER_ID = "senderId";
    public static final String COLUMN_RECEIVER_ID = "receiverId";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_DATE_TIME = "dateTime";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_FILE_URL = "fileURL";
    public static final String COLUMN_FILE_NAME = "fileName";
    public static final String COLUMN_CONVERSION_ID = "conversionId";
    public static final String COLUMN_CONVERSION_NAME = "conversionName";
    public static final String COLUMN_CONVERSION_IMAGE = "conversionImage";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SENDER_ID + " TEXT,"
                + COLUMN_RECEIVER_ID + " TEXT,"
                + COLUMN_MESSAGE + " TEXT,"
                + COLUMN_DATE_TIME + " TEXT,"
                + COLUMN_IMAGE + " TEXT,"
                + COLUMN_FILE_URL + " TEXT,"
                + COLUMN_FILE_NAME + " TEXT,"
                + COLUMN_CONVERSION_ID + " TEXT,"
                + COLUMN_CONVERSION_NAME + " TEXT,"
                + COLUMN_CONVERSION_IMAGE + " TEXT"
                + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }
}

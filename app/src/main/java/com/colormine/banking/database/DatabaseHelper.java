package com.colormine.banking.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ColorMineBank.db";
    private static final int DATABASE_VERSION = 1;

    // Users Table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_BALANCE = "balance";

    // Transactions Table
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_TXN_ID = "txn_id";
    public static final String COL_USER_EMAIL = "user_email";
    public static final String COL_TYPE = "type"; // "INCOME" or "EXPENSE"
    public static final String COL_AMOUNT = "amount";
    public static final String COL_TITLE = "title";
    public static final String COL_DATE = "date";
    public static final String COL_CATEGORY = "category";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_BALANCE + " REAL DEFAULT 5000.00)";
        db.execSQL(createUsersTable);

        String createTxnTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COL_TXN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_EMAIL + " TEXT, " +
                COL_TYPE + " TEXT, " +
                COL_AMOUNT + " REAL, " +
                COL_TITLE + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_CATEGORY + " TEXT)";
        db.execSQL(createTxnTable);
        
        // Insert dummy data for demo purposes
        db.execSQL("INSERT INTO " + TABLE_USERS + " (name, email, password, balance) VALUES ('Sarah Johnson', 'sarah@example.com', 'password123', 8450.50)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    // User Methods
    public boolean registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public String getUserName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_NAME}, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        return "User";
    }
}

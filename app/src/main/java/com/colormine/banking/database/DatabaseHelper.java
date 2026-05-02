package com.colormine.banking.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ColorMineBank.db";
    private static final int DATABASE_VERSION = 2; // Incremented version for new column

    // Users Table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_BALANCE = "balance";
    public static final String COLUMN_IS_ADMIN = "is_admin"; // New column for real admin login

    // Transactions Table
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_TXN_ID = "txn_id";
    public static final String COL_USER_EMAIL = "user_email";
    public static final String COL_TYPE = "type";
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
                COLUMN_BALANCE + " REAL DEFAULT 5000.00, " +
                COLUMN_IS_ADMIN + " INTEGER DEFAULT 0)";
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
        
        // Default Admin Account
        db.execSQL("INSERT INTO " + TABLE_USERS + " (name, email, password, balance, is_admin) " +
                "VALUES ('System Admin', 'admin@colormine.com', 'admin123', 0.0, 1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_IS_ADMIN + " INTEGER DEFAULT 0");
            db.execSQL("INSERT INTO " + TABLE_USERS + " (name, email, password, balance, is_admin) " +
                    "VALUES ('System Admin', 'admin@colormine.com', 'admin123', 0.0, 1)");
        }
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

    public int checkUserType(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_IS_ADMIN}, 
                COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ?", 
                new String[]{email, password}, null, null, null);
        
        int type = -1; // Not found
        if (cursor != null && cursor.moveToFirst()) {
            type = cursor.getInt(0); // 1 for admin, 0 for user
            cursor.close();
        }
        return type;
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

    public double getUserBalance(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_BALANCE}, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            double balance = cursor.getDouble(0);
            cursor.close();
            return balance;
        }
        return 0.0;
    }

    public boolean updateBalance(String email, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BALANCE, amount);
        return db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email}) > 0;
    }

    // Transaction Methods
    public boolean addTransaction(String email, String type, double amount, String title, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_EMAIL, email);
        values.put(COL_TYPE, type);
        values.put(COL_AMOUNT, amount);
        values.put(COL_TITLE, title);
        values.put(COL_CATEGORY, category);
        
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
        values.put(COL_DATE, date);

        long result = db.insert(TABLE_TRANSACTIONS, null, values);
        return result != -1;
    }

    // Admin Methods
    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_IS_ADMIN + " = 0", null);
    }

    public Cursor searchUsers(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_IS_ADMIN + " = 0 AND (" +
                COLUMN_NAME + " LIKE ? OR " + COLUMN_EMAIL + " LIKE ?)";
        return db.rawQuery(sql, new String[]{"%" + query + "%", "%" + query + "%"});
    }

    public boolean updateUser(int id, String name, String email, double balance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_BALANCE, balance);
        return db.update(TABLE_USERS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteUser(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_USERS, COLUMN_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public Cursor getUserTransactions(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_TRANSACTIONS, null, COL_USER_EMAIL + "=?", 
                new String[]{email}, null, null, COL_DATE + " DESC");
    }

    public Cursor getAllTransactions() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_TRANSACTIONS, null, null, null, null, null, COL_DATE + " DESC");
    }
}

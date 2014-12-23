
package com.itcc.smartswitch.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.itcc.smartswitch.utils.Constant;


public class SmartSwitchDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "LauncherDatabaseHelper";
    private static final String CREATE_DOWNLOAD_TABLE = "CREATE TABLE IF NOT EXISTS "
            + Constant.TABLE_DOWNLOAD
            + " ( "
            + Constant.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Constant.COLUMN_DOWNLOAD_FILE_NAME + " TEXT,"
            + Constant.COLUMN_DOWNLOAD_DESTINATION + " TEXT,"
            + Constant.COLUMN_DOWNLOAD_URL + " TEXT,"
            + Constant.COLUMN_DOWNLOAD_MIME_TYPE + " TEXT,"
            + Constant.COLUMN_DOWNLOAD_TOTAL_SIZE + " INTEGER NOT NULL DEFAULT 0,"
            + Constant.COLUMN_DOWNLOAD_CURRENT_SIZE + " INTEGER,"
            + Constant.COLUMN_DOWNLOAD_STATUS + " INTEGER,"
            + Constant.COLUMN_DOWNLOAD_DATE + " INTEGER,"
            + Constant.COLUMN_DOWNLOAD_TITLE + " TEXT, "
            + Constant.COLUMN_DOWNLOAD_DESCRIPTION + " TEXT, "
            + Constant.COLUMN_DOWNLOAD_WIFIONLY + " INTEGER NOT NULL DEFAULT -1"
            + ");";

    SmartSwitchDatabaseHelper(Context context) {
        super(context, Constant.DATABASE_NAME, null, Constant.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DOWNLOAD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
    }

    /**
     * Build a query string that will match any row where the column matches
     * anything in the values list.
     */
    static String buildOrWhereString(String column, int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(column).append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
    }
    
}

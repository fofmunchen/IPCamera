package com.rockchip.tutk.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by qiujian on 2017/5/12.
 */

public class DBOpenHelper extends SQLiteOpenHelper {

    public DBOpenHelper(Context context) {
        super(context, "data.db", null, 1);
    }

    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table UserInfo ("
                + "uid varchar(20),"
                + "account varchar(20),"
                + "password varchar(20),"
                + "constraint pk_UserInfo primary key (uid,account)"
                + ")");


        ContentValues values = new ContentValues();

        values.put("uid", "DXYA9H6MU3ZCBMPGUHY1");
        values.put("account", "admin");
        values.put("password", "888888");

        //sqLiteDatabase.insert("UserInfo", null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}

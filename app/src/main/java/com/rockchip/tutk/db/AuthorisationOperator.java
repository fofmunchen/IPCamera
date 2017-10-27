package com.rockchip.tutk.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qiujian on 2017/5/12.
 */

public class AuthorisationOperator {
    Context context;
    DBOpenHelper helper;
    SQLiteDatabase db;

    String TABLE_NAME="UserInfo";

    public AuthorisationOperator(Context context) {
        super();
        this.context = context;
        helper = new DBOpenHelper(context);
    }

    public long addRecord(UserInfo userInfo) {
        db = helper.getWritableDatabase();
        long rowId = 0;
        ContentValues values = new ContentValues();
        values.put("uid", userInfo.getUid());
        values.put("account", userInfo.getAccount());
        values.put("password", userInfo.getPassword());
        rowId = db.insert(TABLE_NAME, null, values);
        db.close();
        return rowId;
    }

    public int updateRecord(UserInfo userInfo, String uid,String account) {
        db = helper.getWritableDatabase();
        int lines = 0;
        ContentValues values = new ContentValues();
        values.put("uid", userInfo.getUid());
        values.put("account", userInfo.getAccount());
        values.put("password", userInfo.getPassword());
        lines = db.update(TABLE_NAME, values, "uid=? and account=?", new String[]{uid,account,});
        db.close();
        return lines;
    }

    public int deleteRecord(String uid) {
        db = helper.getWritableDatabase();
        int lines = 0;
        db.delete(TABLE_NAME, "uid=?", new String[]{uid});
        db.close();
        return lines;
    }

    public Cursor findRecord() {
        db = helper.getWritableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, "uid DESC");
        return cursor;
    }

    public List<UserInfo> findByUid(String uid) {
        List<UserInfo> userInfoList = new ArrayList<UserInfo>();
        UserInfo scoreData;
        db = helper.getWritableDatabase();
        String sql = "select * from UserInfo where uid=?";
        Cursor cursor = db.rawQuery(sql, new String[]{uid});
        while (cursor.moveToNext()) {
            scoreData = new UserInfo();
            scoreData.setUid(cursor.getString(cursor.getColumnIndex("uid")));
            scoreData.setAccount(cursor.getString(cursor.getColumnIndex("account")));
            scoreData.setPassword(cursor.getString(cursor.getColumnIndex("password")));
            userInfoList.add(scoreData);
        }
        cursor.close();
        return userInfoList;
    }
}

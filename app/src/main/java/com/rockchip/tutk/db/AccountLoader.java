package com.rockchip.tutk.db;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import com.rockchip.tutk.dialog.DeviceLoginDialog;


/**
 * Created by qiujian on 2017/5/12.
 */

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AccountLoader extends SimpleCursorLoader {

    ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
    private Context context;

    public AccountLoader(Context context) {
        super(context);
        this.context = context;

    }

    @Override
    public Cursor loadInBackground() {
        AuthorisationOperator authorisationOperator=new AuthorisationOperator(context);
        SQLiteDatabase database= authorisationOperator.helper.getWritableDatabase();
        //需要的是_id 否则会报错，所以这里要重命名一下
        Cursor cursor = database.rawQuery("SELECT uid AS _id,account FROM UserInfo", null);
        if (database != null) {
            if (cursor != null) {
                //注册一下这个观察者
                cursor.registerContentObserver(mObserver);
                //这边也要注意 一定要监听这个uri的变化。但是如果你这个uri没有对应的provider的话
                //记得在你操作数据库的时候 通知一下这个uri
                cursor.setNotificationUri(context.getContentResolver(), DeviceLoginDialog.uri);
            }
        }
        return cursor;
    }
}
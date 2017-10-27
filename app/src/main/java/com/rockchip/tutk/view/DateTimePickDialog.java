package com.rockchip.tutk.view;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.TextView;

/**
 * Created by waha on 2017/4/1.
 */

public class DateTimePickDialog implements DatePickerDialog.OnDateSetListener {
    private Context mContext;
    private TextView txtView;
    private DatePickerDialog mDialog;

    public DateTimePickDialog(Context context) {
        mContext = context;
    }

    public void showDialog(TextView view) {
        if (null != mDialog && mDialog.isShowing()) {
            mDialog.cancel();
            mDialog = null;
        }
        txtView = view;
        String date = txtView.getText().toString();//yyyy-MM-dd
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        mDialog = new DatePickerDialog(mContext, this, year, month-1, day);
        mDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        if (null != mDialog && mDialog.isShowing()) {
            mDialog.cancel();
            mDialog = null;
        }
        if (null != txtView) {
            String strMonth = month < 9 ? "0" + (month + 1) : "" + (month + 1);
            String strDay = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
            txtView.setText(year + "-" + strMonth + "-" + strDay);
        }
    }
}

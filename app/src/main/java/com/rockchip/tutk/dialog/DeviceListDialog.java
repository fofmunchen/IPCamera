package com.rockchip.tutk.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;


import com.rockchip.tutk.constants.Constants;
import com.rockchip.tutk.R;
import com.rockchip.tutk.adapter.DeviceListAdapter;
import com.rockchip.tutk.utils.UserManager;

@SuppressLint("NewApi")
public class DeviceListDialog extends Dialog implements View.OnClickListener{
	private Context mContext;
	private ListView mMenuList;
	private LinearLayout layout_return;
	public DeviceListDialog(Context context) {
		super(context, R.style.AppTheme);
		mContext = context;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_devicelist);
		mMenuList = (ListView)findViewById(R.id.list_dialog_typelist);
		mMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int status = UserManager.getUserLoginStatus();
				if(status == Constants.LoginStatus.OK) {
					Intent intent = new Intent(getContext(), com.realtek.simpleconfig.MainActivity.class);
					getContext().startActivity(intent);
					DeviceListDialog.this.dismiss();
				}
				else {
					DeviceListDialog.this.dismiss();
					LoginDialog dialog = new LoginDialog(mContext);
					dialog.show();
				}
            }
        });
        DeviceListAdapter adapter = new DeviceListAdapter(mContext);
        mMenuList.setAdapter(adapter);
		layout_return = (LinearLayout)findViewById(R.id.btn_title_return) ;
		layout_return.setOnClickListener(this);
	}
	
	 
	@Override
	public void onClick(View v)
	{
		int id = v.getId();
		switch (id) {
			case R.id.btn_title_return: {
				this.dismiss();
				break;
			}
		}
	}

	
    @Override
	public void onBackPressed() {
        this.dismiss();
	}

}

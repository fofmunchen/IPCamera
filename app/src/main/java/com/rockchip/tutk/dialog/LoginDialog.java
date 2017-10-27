package com.rockchip.tutk.dialog;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rockchip.tutk.R;

import org.w3c.dom.Text;

@SuppressLint("NewApi")
public class LoginDialog extends Dialog implements View.OnClickListener{
	private Context mContext;
	public LoginDialog(Context context) {
			super(context,R.style.AppTheme);
		mContext = context;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_login);
		((TextView)findViewById(R.id.txt_login_register)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LoginDialog.this.dismiss();
				RegisterDialog dialog = new RegisterDialog(mContext);
				dialog.show();
			}
		});
		((TextView)findViewById(R.id.txt_phone)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LoginDialog.this.dismiss();
				PhoneLoginDialog dialog = new PhoneLoginDialog(mContext);
				dialog.show();
			}
		});
	}
	
	
	@Override
	public void onClick(View v) {
		LoginDialog.this.dismiss();
		switch(v.getId())
		{

		}
	}
	@Override
	public void onBackPressed() {
		this.dismiss();
	}
}

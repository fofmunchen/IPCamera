package com.rockchip.tutk.dialog;


import java.util.Timer;
import java.util.TimerTask;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.rockchip.tutk.R;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class TipDialog extends Dialog{
	private long mExitTime;
	private String string = null;
	private Timer  mTimer = new Timer(true); 
	private TimerTask mTimerTask;
	public TipDialog(Context context,String str) {
		super(context,R.style.Translucent_NoTitle);
		string = str;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_tip_succ);
		((TextView)findViewById(R.id.txt_tip_succ)).setText(string);
	    mTimerTask = new TimerTask() 
	    {
	    	public void run()
		    {
	    		dismissit();
		    }         
		 };
     	mTimer.schedule(mTimerTask, 2500);
	}
	public void showit()
	{
		try {
			this.show();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public void dismissit()
	{
		try {
			this.dismiss();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}

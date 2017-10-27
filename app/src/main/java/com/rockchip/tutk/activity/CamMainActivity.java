package com.rockchip.tutk.activity;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.TUTKServer;
import com.rockchip.tutk.constants.GlobalValue;
import com.rockchip.tutk.fragment.FragmentCamera;
import com.rockchip.tutk.fragment.FragmentGallery;
import com.rockchip.tutk.fragment.FragmentShare;
import com.rockchip.tutk.fragment.FragmentUser;
import com.rockchip.tutk.utils.L;
import com.rockchip.tutk.utils.UserManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class CamMainActivity extends FragmentActivity implements TUTKServer.Listener {

    private ViewPager viewPager;                            // 页面内容
    //    private ImageView imageView;                            // 滑动的图片
    private LinearLayout btn_camera, btn_photo, btn_live,btn_find,btn_self;     // 选项名称
    private ImageButton img_camera, img_photo, img_live,img_find,img_self;     // 选项名称
    private TextView txt_camera,txt_photo, txt_live,txt_find,txt_self;     // 选项名称
    private List<Fragment> fragments;                       //标题列表
    //    private int offset = 0;                                 // 动画图片偏移量
    private int currIndex = 0;                              // 当前页卡编号
    //    private int bmpW;                                       // 动画图片宽度
    private int selectedColor, unSelectedColor;
    /**
     * 页面总数 *
     */
    private static final int pageSize = 5;
    private int PAGEINDEX = -1;


    public String TAG = "TUTK_MAIN";

    public static String UID = "DXYA9H6MU3ZCBMPGUHY1";
    public static String UID2 = "F5PUBH7MPLF4BMPGUH6J";
    public  Set<String> mStringSet;
    TUTKServer mHelperService;
   // FragmentTabHost mTabHost;
    FragmentCamera cameraFragment;
    private ServiceConnection mHelperServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mHelperService = ((TUTKServer.LocalBinder) service).getService();
            mHelperService.setClientActivity(CamMainActivity.this);
            mHelperService.addListener(CamMainActivity.this);
            if (cameraFragment != null) {
                cameraFragment.OnServiceBind(mHelperService);
            }
            if (mStringSet != null) {
                Object[] objects = mStringSet.toArray();
                for (int i = 0; i < objects.length; i++) {
                    String uid = objects[i].toString();
                    if (uid.trim().length() == 20) {
                        GlobalValue.deviceUID = uid;
                        Intent intent = new Intent(TUTKManager.ACTION_TUTK_DEVICE_ATTACHED);
                        intent.putExtra("UID", uid);
                        sendBroadcast(intent);
                    }
                }
            }
//            else{
//                Intent intent = new Intent(TUTKManager.ACTION_TUTK_DEVICE_ATTACHED);
//                GlobalValue.deviceUID = "1234567";
//                intent.putExtra("UID", "1234567");
//                sendBroadcast(intent);
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mHelperService.removeListener(CamMainActivity.this);
            mHelperService = null;
        }
    };

    private void doBindHelperService() {
        bindService(new Intent(getApplicationContext(), TUTKServer.class),
                mHelperServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindHelperService() {
        if (mHelperService != null) {
            mHelperService.setClientActivity(null);
            unbindService(mHelperServiceConnection);
        }
    }
    @Override
    public void deviceAdded(TUTKDevice device) {

    }

    @Override
    public void deviceRemoved(TUTKDevice device) {

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cammain);
        UserManager.init(CamMainActivity.this);
        this.mStringSet = UserManager.getUserDeviceList();
        Log.i("wz", "get local device list is " + this.mStringSet);
        initView();
        doBindHelperService();
    }

    /** 初始化当前标题和未被选择的字体的颜色     */
    private void initView() {
        selectedColor = getResources().getColor(R.color.main_bottombtn_focus);
        unSelectedColor = getResources().getColor(R.color.main_bottombtn_normal);

//        InitImageView();
        InitTextView();
        InitViewPager();

        img_camera.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_camera_focus));
        txt_camera.setTextColor(selectedColor);
    }

    /**
     * 初始化Viewpager页
     */
    private void InitViewPager() {
        viewPager = (ViewPager) findViewById(R.id.id_viewpager);
        fragments = new ArrayList<Fragment>();
        cameraFragment = new FragmentCamera();
        fragments.add(cameraFragment);
        fragments.add(new FragmentGallery());
        //       fragments.add(new FragmentShare());
        fragments.add(new FragmentShare());
        fragments.add(new FragmentUser());
        viewPager.setAdapter(new myPagerAdapter(getSupportFragmentManager(),
                fragments));
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
    }

    /**
     * 初始化顶部三个字体按钮
     */
    private void InitTextView() {
        btn_camera = (LinearLayout)findViewById(R.id.id_tab_bottom_camera);
        btn_photo = (LinearLayout)findViewById(R.id.id_tab_bottom_photo);
//        btn_live = (LinearLayout)findViewById(R.id.id_tab_bottom_live);
        btn_find = (LinearLayout)findViewById(R.id.id_tab_bottom_find);
        btn_self = (LinearLayout)findViewById(R.id.id_tab_bottom_self);

        btn_camera.setOnClickListener(new TabBtnClickListener(0));
        btn_photo.setOnClickListener(new TabBtnClickListener(1));
//        btn_live.setOnClickListener(new TabBtnClickListener(2));
        btn_find.setOnClickListener(new TabBtnClickListener(2));
        btn_self.setOnClickListener(new TabBtnClickListener(3));

        img_camera = (ImageButton)findViewById(R.id.btn_tab_bottom_camera);
        img_photo = (ImageButton)findViewById(R.id.btn_tab_bottom_photo);
//        img_live = (ImageButton)findViewById(R.id.btn_tab_bottom_live);
        img_find = (ImageButton)findViewById(R.id.btn_tab_bottom_find);
        img_self = (ImageButton)findViewById(R.id.btn_tab_bottom_self);

        txt_camera = (TextView)findViewById(R.id.txt_tab_bottom_camera);
        txt_photo = (TextView)findViewById(R.id.txt_tab_bottom_photo);
        //       txt_live = (TextView)findViewById(R.id.txt_tab_bottom_live);
        txt_find = (TextView)findViewById(R.id.txt_tab_bottom_find);
        txt_self = (TextView)findViewById(R.id.txt_tab_bottom_self);
    }



    /**
     * 初始化动画，这个就是页面滑动时，下面的横线也滑动的效果，在这里需要计算一些数据
     */

 /*   private void InitImageView() {
        imageView = (ImageView) findViewById(R.id.cursor);
        bmpW = BitmapFactory.decodeResource(getResources(),
                R.drawable.tab_selected_bg).getWidth();// 获取图片宽度
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels;// 获取分辨率宽度
        offset = (screenW / pageSize - bmpW) / 2;// 计算偏移量--(屏幕宽度/页卡总数-图片实际宽度)/2
        // = 偏移量
        Matrix matrix = new Matrix();
        matrix.postTranslate(offset, 0);
        imageView.setImageMatrix(matrix);// 设置动画初始位置
    }*/

    /**
     * 定义内部类三个文字点击按钮监听
     */
    private class TabBtnClickListener implements OnClickListener {
        private int index = 0;

        public TabBtnClickListener(int i) {
            index = i;
        }

        public void onClick(View v) {
            L.i(1,"pageindex is "+index);
            if(PAGEINDEX != index)
            {
                viewPager.setCurrentItem(index);
            }
        }

    }

    /**
     * 为选项卡绑定监听器
     */
    public class MyOnPageChangeListener implements OnPageChangeListener {

//        int one = offset * 2 + bmpW;// 页面1 -> 页面2 偏移量
//        int two = one * 2;// 页面1 -> 页面3 偏移量

        public void onPageScrollStateChanged(int index) {
        }

        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        public void onPageSelected(int index) {
//            Animation animation = new TranslateAnimation(one * currIndex, one * index, 0, 0);// 显然这个比较简洁，只有一行代码。
//            currIndex = index;
//            animation.setFillAfter(true);// True:图片停在动画结束位置
//            animation.setDuration(300);
//            imageView.startAnimation(animation);
            resetBtn();
            PAGEINDEX = index;
            switch (index) {
                case 0:
                    img_camera.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_camera_focus));
                    txt_camera.setTextColor(selectedColor);
                    break;
                case 1:
                    img_photo.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_photo_focus));
                    txt_photo.setTextColor(selectedColor);
                    break;
     /*           case 2:
                	img_live.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_live_focus));
                	txt_live.setTextColor(selectedColor);
                    break;*/
                case 2:
                    img_find.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_find_focus));
                    txt_find.setTextColor(selectedColor);
                    break;
                case 3:
                    img_self.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_me_focus));
                    txt_self.setTextColor(selectedColor);
                    break;
            }
        }
    }

    /**
     * 定义内部类适配器
     */
    private class myPagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragmentList;

        public myPagerAdapter(FragmentManager fm, List<Fragment> fragmentList) {
            super(fm);
            this.fragmentList = fragmentList;
        }

        /**
         * 得到每个页面
         */
        @Override
        public Fragment getItem(int arg0) {
            return (fragmentList == null || fragmentList.size() == 0) ? null
                    : fragmentList.get(arg0);
        }

        /**
         * 每个页面的title
         */
        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }

        /**
         * 页面的总个数
         */
        @Override
        public int getCount() {
            return fragmentList == null ? 0 : fragmentList.size();
        }
    }

    /**
     * 清除掉所有的选中状态。
     */
    private void resetBtn()
    {
        img_camera.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_camera_normal));
        img_photo.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_photo_normal));
//		img_live.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_live_normal));
        img_find.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_find_normal));
        img_self.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_me_normal));
        txt_camera.setTextColor(unSelectedColor);
        txt_photo.setTextColor(unSelectedColor);
//		txt_live.setTextColor(unSelectedColor);
        txt_find.setTextColor(unSelectedColor);
        txt_self.setTextColor(unSelectedColor);
    }

}
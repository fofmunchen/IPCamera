package com.rockchip.tutk.activity;

import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceFragment;

import com.rockchip.tutk.R;

public class SettingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new InnerFragment()).commit();
    }

    public static class InnerFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_setting);
        }
    }
}

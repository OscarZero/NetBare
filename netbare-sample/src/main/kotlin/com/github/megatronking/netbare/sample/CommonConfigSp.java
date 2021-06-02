package com.github.megatronking.netbare.sample;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.github.megatronking.netbare.sample.deskfloating.DragTableButton;


public class CommonConfigSp {
    private static final String TAG = "commoncfg-debug";

    public static final String DEVICEID = "deviceId";

    /**
     * 存储common config 的SP名称
     */
    private static final String SP_NAME = "common_config_store";

    private Context mContext;
    private SharedPreferences mPref;

    private static CommonConfigSp commonConfigSp;

    private DragTableButton mSys_view;

    private CommonConfigSp() {

    }

    public void init(Context context) {
        mContext = context;
        mPref = mContext.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
    }


    public static CommonConfigSp getInstance() {
        if (commonConfigSp == null) {
            commonConfigSp = new CommonConfigSp();
        }
        return commonConfigSp;
    }

    /**
     * 清除数据
     */
    public void clear() {
        SharedPreferences.Editor editor = mPref.edit();
        if (editor == null) {
            return;
        }
        editor.clear();
        editor.apply();
    }

    public void put(String key, String value) {
        mPref.edit().putString(key, value).apply();
    }

    public void put(String key, int value) {
        mPref.edit().putInt(key, value).apply();
    }

    public void put(String key, long value) {
        mPref.edit().putLong(key, value).apply();
    }

    public void put(String key, float value) {
        mPref.edit().putFloat(key, value).apply();
    }


    ///

    public String get(String key, String value) {
        return mPref.getString(key, value);
    }

    public int get(String key, int value) {
        return mPref.getInt(key, value);
    }

    public long get(String key, long value) {
        return mPref.getLong(key, value);
    }

    public float get(String key, float value) {
        return mPref.getFloat(key, value);
    }

    public void setDeskObject(DragTableButton dtb){
        mSys_view = dtb;
    }
    public DragTableButton getFloatButton(){
        return mSys_view;
    }
}

package com.github.megatronking.netbare.sample.packge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.megatronking.netbare.sample.util.Logger;
import com.github.megatronking.netbare.sample.util.MessageEvent;

import org.greenrobot.eventbus.EventBus;

public class AppinstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction())) {
                String packageName = intent.getDataString();
                String[] packagers = packageName.split(":");
                packageName = packagers[1];

                boolean result = AppUtils.getInstance().addApp(packageName);
                Logger.e("install app "+packageName+"/ result "+result);

                EventBus.getDefault().post(new MessageEvent(packageName,MessageEvent.UPDATEAPP_INSTALL));
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                String packageName = intent.getDataString();
                String[] packagers = packageName.split(":");
                packageName = packagers[1];
                boolean result = AppUtils.getInstance().removeApp(packageName);
                Logger.e("uninstall  app "+packageName+"/ result "+result);
                EventBus.getDefault().post(new MessageEvent(packageName,MessageEvent.UPDATEAPP_REMOVE));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

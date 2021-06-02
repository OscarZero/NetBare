package com.github.megatronking.netbare.sample.packge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.github.dfqin.grantor.PermissionListener;
import com.github.dfqin.grantor.PermissionsUtil;
import com.github.megatronking.netbare.sample.CommonConfigSp;
import com.github.megatronking.netbare.sample.MainActivity;
import com.github.megatronking.netbare.sample.util.Logger;
import com.github.megatronking.netbare.sample.util.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.ACTIVITY_SERVICE;

public class AppUtils {

    private HashSet<String> appsHash = new HashSet<>();
    private HashMap<String,AppInfo> applistMap = new HashMap<>();
    private static volatile AppUtils instance = null;
    private OkHttpClient okHttpClient = new OkHttpClient();
    private AppInfo currentAppinfo;
    public static String HOST = "https://jbzsbw.com/";
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private AppUtils(){
    }

    public static AppUtils getInstance() {
        if (instance == null) {
            synchronized (AppUtils.class) {
                if (instance == null) {
                    instance = new AppUtils();
                }
            }
        }
        return instance;
    }
    public String stampToDate(long stamp){
        return format.format(new Date(stamp));
    }

    public AppInfo getCurrentAppinfo() {
        return currentAppinfo;
    }

    public void setCurrentAppinfo(AppInfo currentAppinfo) {
        this.currentAppinfo = currentAppinfo;
    }

    public void loadInstallApp(Context context){
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES);

        for (PackageInfo info : installedPackages) {
            String packageName = info.packageName;
            String appName = (String) info.applicationInfo.loadLabel(packageManager);
            if(TextUtils.isEmpty(packageName)){
                continue;
            }
            appsHash.add(packageName);
        }
    }
    public boolean addApp(String packageName){
        if(TextUtils.isEmpty(packageName)){
            return false;
        }
        return appsHash.add(packageName);
    }
    public boolean removeApp(String packageName){
        if(TextUtils.isEmpty(packageName)){
            return false;
        }
        return appsHash.remove(packageName);
    }
    public boolean isOpen(String packageName){
        return appsHash.contains(packageName);
    }

    public AppInfo getAppinfo(String packageName){
//        ArrayList<AppInfo> list = new ArrayList<>(applistMap.values());
        return applistMap.get(packageName);
    }
    public void updateAppinfoMap(AppInfo appInfo){
        applistMap.put(appInfo.getPackageName(),appInfo);
    }

    public void clearAppListMap(){
        applistMap.clear();
    }

    public ArrayList<AppInfo> getApplist(){
        ArrayList<AppInfo> listApp =  new ArrayList<>(applistMap.values());
        Collections.sort(listApp, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                if(o1.getCreateDate() == o2.getCreateDate()){
                    return 0;
                }
                if(o1.getCreateDate() <= 0){
                    return 1;
                }
                if(o2.getCreateDate() <= 0){
                    return -1;
                }
                if(o1.getCreateDate() <= o2.getCreateDate()){
                    return -1;
                }
                return 1;
            }
        });
        return listApp;
    }


    //=================================================================================================
    public  void openBrowser(Context context,String url){
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(HOST+url));
        // 注意此处的判断intent.resolveActivity()可以返回显示该Intent的Activity对应的组件名
        // 官方解释 : Name of the component implementing an activity that can display the intent
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            final ComponentName componentName = intent.resolveActivity(context.getPackageManager());
            // 打印Log   ComponentName到底是什么
            Logger.e("componentName = " + componentName.getClassName());
            context.startActivity(Intent.createChooser(intent, "请选择浏览器"));
        } else {
            Toast.makeText(context.getApplicationContext(), "请下载浏览器", Toast.LENGTH_SHORT).show();
        }
    }
    //  文件保存路径
    private String mSavePath;
    private String mSaveDataPath;
    private String datafileName = "netdata.txt";
    //  判断是否停止
    private boolean mIsCancel = false;
    //  下载名称
    private String downloadname="download-app.apk";

    public void downloadAPK(Activity activity,final String apk_file_url, final Handler handler){
        if (PermissionsUtil.hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            downloadAPK(apk_file_url,handler);
        } else {
            PermissionsUtil.requestPermission(activity, new PermissionListener() {
                @Override
                public void permissionGranted(@NonNull String[] permissions) {
                    downloadAPK(apk_file_url,handler);
                }


                @Override
                public void permissionDenied(@NonNull String[] permissions) {
                }
            }, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }
    }
    /**
     * 下载APk
     * @param apk_file_url
     */
    private void downloadAPK(final String apk_file_url, final Handler handler) {


        Request request = new Request.Builder()
                .url(HOST+apk_file_url)
                .get() //默认就是GET请求，可以不写
                .build();
        Call call  = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.sendEmptyMessage(MainActivity.UPDATE_FAILE);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;//输入流
                FileOutputStream fos = null;//输出流
                try {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                        String sdPath = Environment.getExternalStorageDirectory() + "/";
                        mSavePath = sdPath + "oil";
                        File dir = new File(mSavePath);
                        if (!dir.exists()){
                            dir.mkdir();
                        }
                        is = response.body().byteStream();//获取输入流
                        long total = response.body().contentLength();//获取文件大小
                        if(is != null){
                            Log.d("SettingPresenter", "onResponse: 不为空");
                            File apkFile = new File(mSavePath, downloadname);
                            fos = new FileOutputStream(apkFile);
                            byte[] buf = new byte[1024 * 2];
                            int ch = -1;
                            int count = 0;
                            while ((ch = is.read(buf)) != -1) {
                                fos.write(buf, 0, ch);
                                count += ch;
                                int process = (int) (((float)count/total) * 100);
                                // 更新进度条
                                Message message = Message.obtain();
                                message.obj = process;
                                message.what = MainActivity.UPDATE_PROGRESS;
                                handler.sendMessage(message);
                            }

                        }
                        // 下载完成
                        if(fos != null){
                            fos.flush();
                            fos.close();
                        }
                        handler.sendEmptyMessage(MainActivity.UPDATE_INSTALL);
                    }
                } catch (Exception e) {
                    handler.sendEmptyMessage(MainActivity.UPDATE_FAILE);
                    Log.d("SettingPresenter",e.toString());
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


    }

    /**
     * 安装Apk
     */
    public void installAPK(Context mContext) {
        File apkFile = new File(mSavePath, downloadname);
        if (!apkFile.exists()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
//      安装完成后，启动app（源码中少了这句话）

        if (null != apkFile) {
            try {
                //兼容7.0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileProvider", apkFile);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    //兼容8.0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        boolean hasInstallPermission = mContext.getPackageManager().canRequestPackageInstalls();
                        if (!hasInstallPermission) {
                            startInstallPermissionSettingActivity(mContext);
                            return;
                        }
                    }
                } else {
                    intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                if (mContext.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                    mContext.startActivity(intent);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void startInstallPermissionSettingActivity(Context mContext) {
        //注意这个是8.0新API
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public void bring2Front(Context context)
    {
        ActivityManager activtyManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activtyManager.getRunningTasks(3);
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfos)
        {
            if (context.getPackageName().equals(runningTaskInfo.topActivity.getPackageName()))
            {
                activtyManager.moveTaskToFront(runningTaskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
                return;
            }
        }
    }
    /**
     * 将本应用置顶到最前端
     * 当本应用位于后台时，则将它切换到最前端
     *
     * @param context
     */
    public void setTopApp(Context context) {
        try {
            Logger.e("settopapp start -----------");
            /**获取ActivityManager*/
            ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                List<ActivityManager.AppTask> list = activityManager.getAppTasks();
                for (ActivityManager.AppTask appTask : list){
                    appTask.moveToFront();
                    break;
                }
            }else {
                /**获得当前运行的task(任务)*/
                List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.getRunningTasks(100);
                for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                    /**找到本应用的 task，并将它切换到前台*/
                    if (taskInfo.topActivity.getPackageName().equals(context.getPackageName())) {
                        activityManager.moveTaskToFront(taskInfo.id, 0);
                        break;
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Logger.e("settopapp err "+e.getMessage());
        }
    }
//    public void setTopApp(Context context) {
//        if (!isRunningForeground(context)) {
//            /**获取ActivityManager*/
//            ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
//
//            /**获得当前运行的task(任务)*/
//            List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.getRunningTasks(100);
//            for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
//                /**找到本应用的 task，并将它切换到前台*/
//                if (taskInfo.topActivity.getPackageName().equals(context.getPackageName())) {
//                    activityManager.moveTaskToFront(taskInfo.id, 0);
//                    break;
//                }
//            }
//        }
//    }
    /**
     * 判断本应用是否已经位于最前端
     *
     * @param context
     * @return 本应用已经位于最前端时，返回 true；否则返回 false
     */
    public static boolean isRunningForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfoList = activityManager.getRunningAppProcesses();
        /**枚举进程*/
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfoList) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(context.getApplicationInfo().processName)) {
                    return true;
                }
            }
        }
        return false;
    }

    //获取本地app列表
    public void loadApplist(String url, final Handler handler){
        final Request request = new Request.Builder()
                .url(HOST+url)
                .get() //默认就是GET请求，可以不写
                .build();
        Call call  = okHttpClient.newCall(request);
        Logger.e("333 = "+HOST+url);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e("loadlist faile "+e.getMessage());
                handler.sendEmptyMessage(MainActivity.UPDATEPG);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    String jsonString = response.body().string();
                    try {
                        Logger.e("jsonstring "+jsonString);
                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONArray list = jsonObject.getJSONArray("list");
                        for (int i = 0; i < list.length(); i++) {
//                        for (int i = 0; i < 2; i++) {
                            AppInfo appInfo = AppInfo.parse(list.getJSONObject(i).toString());
                            Logger.e("appinfo "+appInfo.toString());
                            boolean isopen = AppUtils.getInstance().isOpen(appInfo.getPackageName());
                            appInfo.setAction(isopen ? "打开":"下载");
//                            listApp.add(appInfo);
                            applistMap.put(appInfo.getPackageName(),appInfo);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                handler.sendEmptyMessage(MainActivity.UPDATEPG);

            }
        });
    }
    //===================tcp==================================
    HashSet<String> hostMap = new HashSet<>();
    public void addTCPUDPRespone(String host, int port,String method){
        if(!hostMap.add(host+port) || httpMap.containsKey(host+port)){
//            Logger.e("--------add tcp dup    "+host+":"+port);
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("host",host);
            jsonObject.put("port",port);
            jsonObject.put("method",method);

        } catch (Exception e) {
            e.printStackTrace();
        }
        addResponse(host+port,jsonObject,false);
    }
    public void addHttpIdPort(String ip,int port){
        httpMap.put(ip+port,ip+port);
    }

//===========================upload josn=====================================================
    HashMap<String,JSONObject> response = new HashMap<>();
    HashMap<String,String> httpMap = new HashMap<>();
    int currentsize = 0;
    public JSONObject updateResponseTojson(String id,String responseJson){

        JSONObject jsonObject = response.get(id);
        try {
            String res = jsonObject.optString("response");
            if(TextUtils.isEmpty(res)){
                jsonObject.put("response",responseJson);
            }else {
                res = res + responseJson;
                jsonObject.put("response",res);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
    public void addResponse(String id,JSONObject jsonObj,boolean upload){
        try {
            if(jsonObj != null){
                response.put(id,jsonObj);
                currentsize += jsonObj.toString().length();
                if(httpMap.containsKey(id)){
                    response.remove(id);
                    currentsize = currentsize - jsonObj.toString().length();
                    Logger.e("-------addRespone resize  ------  "+currentsize);
                }
            }
            Logger.e("-------addRespone size   "+currentsize);
            if(currentsize > maxsize || upload){
                currentsize = 0;
                if(jsonArray ==null){
                    addResponeJson();
                }
                for (String o : response.keySet()) {
                    if(httpMap.containsKey(o)){
                        continue;
                    }
                    Logger.e("========upload   ========  key=" + o + " value=" + response.get(o));
                    jsonArray.put(response.get(o));
                }
                hostMap.clear();
                jsonObject.put("data",jsonArray);
                String jsonstr= jsonObject.toString();
                Logger.e("post json "+jsonstr);
                jsonArray = null;
                response.clear();
                uploadData(jsonstr);
            }
        }catch (Exception e){
            e.printStackTrace();
            jsonArray = null;
            Logger.e("addrespone err "+e.getMessage());
        }
    }
    JSONArray jsonArray = null;
    JSONObject jsonObject = null;
    int maxsize = 1024 * 1024;
//    public void addRespone(JSONObject json,boolean upload){
//        try {
//            if(jsonArray ==null){
//                addResponeJson();
//            }
//            if(json != null){
//                jsonArray.put(json);
//
//            }
//            Logger.e("--------addRespone size   "+jsonArray.toString().length());
//            if(jsonArray.toString().length() > maxsize || upload){
//                try {
//                    hostMap.clear();
//                    jsonObject.put("data",jsonArray);
//                    String jsonstr= jsonObject.toString();
//                    Logger.e("post json "+jsonstr);
//                    jsonArray = null;
//                    uploadData(jsonstr);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }catch (Exception e){
//            e.printStackTrace();
//            jsonArray = null;
//            Logger.e("addrespone err "+e.getMessage());
//        }
//    }
    public void addResponeJson(){
        if(jsonArray != null){
            return;
        }
        jsonArray = new JSONArray();
        jsonObject = new JSONObject();
        try {
            jsonObject.put("device",CommonConfigSp.getInstance().get(CommonConfigSp.DEVICEID,""));
            jsonObject.put("app",currentAppinfo.getUuid());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addTxtToFileBuffered(String content) {
        //在文本文本中追加内容
        BufferedWriter out = null;
        try {

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String sdPath = Environment.getExternalStorageDirectory() + "/";
//                      文件保存路径
                mSavePath = sdPath + "oil";
                File dir = new File(mSavePath);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                File datafile = new File(mSavePath, datafileName);
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(datafile, true)));
                out.newLine();//换行
                out.write(content);
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cleanDataFile(){
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String sdPath = Environment.getExternalStorageDirectory() + "/";
                mSavePath = sdPath + "oil";
                File dir = new File(mSavePath);
                if (!dir.exists()) {
                    return;
                }
                File datafile = new File(mSavePath, datafileName);
                datafile.delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void uploadData(String json){

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        OkHttpClient okHttpClient = new OkHttpClient();

        Request request = new Request.Builder()
                .url(HOST+"api/upload/data")
                .post(RequestBody.create(mediaType, json))
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e( "upload onFailure: " + e.getMessage());
//                EventBus.getDefault().post(new MessageEvent("failed",MessageEvent.UPLOAD_FAILED));
                EventBus.getDefault().post(new MessageEvent("failed",MessageEvent.UPLOAD_SUCCESS));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logger.e( "upload success: "+response.protocol() + " " +response.code() + " " + response.message());
                Headers headers = response.headers();
                for (int i = 0; i < headers.size(); i++) {
                    Logger.e( headers.name(i) + ":" + headers.value(i));
                }
                Logger.e( "upload success onResponse: " + response.body().string());
                EventBus.getDefault().post(new MessageEvent("success ",MessageEvent.UPLOAD_SUCCESS));
            }
        });
    }



}

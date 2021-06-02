package com.github.megatronking.netbare.sample.packge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Objects;

public class AppInfo {


    public static AppInfo parse(String json){
        AppInfo appInfo = new AppInfo();
        try {
            JSONObject jsonObject = new JSONObject(json);
            appInfo.uuid = jsonObject.optString("uuid");
            appInfo.name = jsonObject.optString("name");
            appInfo.packageName = jsonObject.optString("packageName");
            appInfo.memo = jsonObject.optString("memo");
            appInfo.deHttps = jsonObject.optBoolean("deHttps");
            appInfo.startTime = jsonObject.optInt("startTime");
            appInfo.stopTime = jsonObject.optInt("stopTime");
//            appInfo.packageName = "com.mostalk.im";
//            appInfo.startTime = 1500;
//            appInfo.stopTime = 500;

            appInfo.downlaodUrl = jsonObject.optString("downloadUrl");
            appInfo.createDate = jsonObject.optLong("createDate");
            JSONArray jsonArray = jsonObject.optJSONArray("whiteList");
            for (int i = 0; i < jsonArray.length(); i++) {
                String url = (String) jsonArray.get(i);
                appInfo.whiteList.add(url);
            }
            JSONArray blackListjsonArray = jsonObject.optJSONArray("blackList");
            for (int i = 0; i < blackListjsonArray.length(); i++) {
                String url = (String) blackListjsonArray.get(i);
                appInfo.blackList.add(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appInfo;
    }

    private String uuid;
    private String name;
    private String packageName;
    private String memo;
    private boolean deHttps;
    private int startTime;
    private int  stopTime;
    private String downlaodUrl;
    private long createDate;
    public String action;
    private HashSet<String> whiteList = new HashSet<>();
    private HashSet<String> blackList = new HashSet<>();

    public boolean isWhite(String url){
        return whiteList.contains(url);
    }
    public boolean isBlack(String url){
        return blackList.contains(url);
    }

    public HashSet<String> getWhiteList(){return this.whiteList;}
    public HashSet<String> getBlackList(){return this.blackList;}

    public String getAction(){
        return action;
    }
    public void setAction(String action){
        this.action = action;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public boolean getDeHttps() {
        return deHttps;
    }

    public void setDeHttps(boolean deHttps) {
        this.deHttps = deHttps;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getStopTime() {
        return stopTime;
    }

    public void setStopTime(int stopTime) {
        this.stopTime = stopTime;
    }

    public String getDownlaodUrl() {
        return downlaodUrl;
    }

    public void setDownlaodUrl(String downlaodUrl) {
        this.downlaodUrl = downlaodUrl;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                ", memo='" + memo + '\'' +
                ", deHttps='" + deHttps + '\'' +
                ", startTime=" + startTime +
                ", stopTime=" + stopTime +
                ", downlaodUrl='" + downlaodUrl + '\'' +
                ", createDate='" + createDate + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppInfo appInfo = (AppInfo) o;

        if (startTime != appInfo.startTime) return false;
        if (stopTime != appInfo.stopTime) return false;
        if (createDate != appInfo.createDate) return false;
        if (!Objects.equals(uuid, appInfo.uuid)) return false;
        if (!Objects.equals(name, appInfo.name)) return false;
        if (!Objects.equals(packageName, appInfo.packageName))
            return false;
        if (!Objects.equals(memo, appInfo.memo)) return false;
        if (!Objects.equals(deHttps, appInfo.deHttps))
            return false;
        return Objects.equals(downlaodUrl, appInfo.downlaodUrl);
    }

//    @Override
//    public int hashCode() {
//        int result = uuid != null ? uuid.hashCode() : 0;
//        result = 31 * result + (name != null ? name.hashCode() : 0);
//        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
//        result = 31 * result + (memo != null ? memo.hashCode() : 0);
//        result = 31 * result + (deHttps != null ? deHttps.hashCode() : 0);
//        result = 31 * result + startTime;
//        result = 31 * result + stopTime;
//        result = 31 * result + (downlaodUrl != null ? downlaodUrl.hashCode() : 0);
//        result = 31 * result + (int) (createDate ^ (createDate >>> 32));
//        return result;
//    }
}

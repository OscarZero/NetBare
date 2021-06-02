package com.github.megatronking.netbare.sample;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.megatronking.netbare.sample.packge.AppInfo;
import com.github.megatronking.netbare.sample.packge.AppUtils;
import com.github.megatronking.netbare.sample.util.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AppListAdapter extends BaseAdapter {

    private List<AppInfo> list;
    private Context mContext;
    private OnItemClickListener listener;
    private interface OnItemClickListener{
        void onItemClick(int postion,AppInfo info,boolean isOpen);
    }
    public AppListAdapter(Context context,List<AppInfo> data, OnItemClickListener listener){

        this.listener = listener;
        list = data;
        mContext = context;
    }
    @Override
    public int getCount() {
        if (list == null) {
            return 0;
        }
        return list.size();

    }
    /**
     * 添加列表项
     * @param position
     * @param appInfo
     */
    public void add(int position, AppInfo appInfo){
        if (null == list) {
            list = new LinkedList<>();
        }
        list.add(position,appInfo);
        notifyDataSetChanged();
    }

    /**
     * 更新列表内容
     * @param data
     */
    public void update(List<AppInfo> data){
        if (null == list) {
            list = new LinkedList<>();
        }
        List<AppInfo> temp = new ArrayList<>(data);
        list.clear();
        list.addAll(temp);

        Logger.e("update data "+list.size());
        notifyDataSetChanged();
    }

    /**
     * 更新列表项
     * @param position
     * @param appInfo
     */
    public void update(int position,AppInfo appInfo){
        if(list != null && position < list.size()) {
            list.set(position, appInfo);
        }
        notifyDataSetChanged();
    }

    /**
     * 移除指定列表项
     * @param position
     */
    public void remove(int position) {
        if(list != null && 0 != getCount()) {
            list.remove(position);
        }
        notifyDataSetChanged();
    }

    /**
     * 清空列表数据
     */
    public void clear() {
        if (list != null) {
            list.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public Object getItem(int position) {
        if (list == null) {
            return null;
        }
        return list.get(position);

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = View.inflate(mContext,R.layout.item_select_package, null);
            viewHolder = new ViewHolder();
            viewHolder.baseItem = (RelativeLayout) convertView.findViewById(R.id.list_item_bg);
            viewHolder.appname = (TextView) convertView.findViewById(R.id.app_name);
            viewHolder.lasttime = (TextView) convertView.findViewById(R.id.lasttime);
            viewHolder.timeinterval = (TextView) convertView.findViewById(R.id.time_interval);
            viewHolder.timetotal = (TextView) convertView.findViewById(R.id.time_total);
            viewHolder.action = (Button) convertView.findViewById(R.id.action);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final AppInfo appInfo = list.get(position);
        Logger.e("getview  name "+appInfo.getName());
        viewHolder.appname.setText(appInfo.getName());
        viewHolder.lasttime.setText(AppUtils.getInstance().stampToDate(appInfo.getCreateDate()));
        viewHolder.timeinterval.setText("启动间隔 "+appInfo.getStartTime()+"秒");
        viewHolder.timetotal.setText("检测时长 "+appInfo.getStopTime()+"秒");
        final boolean isopen = AppUtils.getInstance().isOpen(appInfo.getPackageName());
        viewHolder.action.setText(isopen ? "读取":"下载");
//        viewHolder.action.setVisibility(isopen ?View.GONE:View.VISIBLE);
//        viewHolder.action.setEnabled(!isopen);
        viewHolder.action.setVisibility(View.VISIBLE);
        viewHolder.action.setEnabled(true);
        viewHolder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null){
                    listener.onItemClick(position,appInfo,isopen);
                }
            }
        });
        viewHolder.baseItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(mContext,appInfo.getPackageName(),0).show();
            }
        });
        // 将这个处理好的view返回
        return convertView;

    }

    private static class ViewHolder {
        public RelativeLayout baseItem;
        public TextView appname;
        public TextView lasttime;
        public TextView timeinterval;
        public TextView timetotal;
        public Button action;
    }


}

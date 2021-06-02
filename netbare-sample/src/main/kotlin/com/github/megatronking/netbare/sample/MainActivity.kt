package com.github.megatronking.netbare.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import android.widget.CompoundButton
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareConfig
import com.github.megatronking.netbare.NetBareListener
import com.github.megatronking.netbare.http.HttpInjectInterceptor
import com.github.megatronking.netbare.http.HttpInterceptorFactory
import com.github.megatronking.netbare.http.HttpVirtualGatewayFactory
import com.github.megatronking.netbare.sample.deskfloating.DragTableButton
import com.github.megatronking.netbare.sample.deskfloating.SuspendUtils
import com.github.megatronking.netbare.sample.packge.AppInfo
import com.github.megatronking.netbare.sample.packge.AppUtils
import com.github.megatronking.netbare.sample.packge.AppinstallReceiver
import com.github.megatronking.netbare.sample.util.*
import com.github.megatronking.netbare.ssl.JKS
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.util.*
import kotlin.concurrent.timerTask


class MainActivity : AppCompatActivity(), NetBareListener {

    companion object {
        internal const val REQUEST_CODE_PREPARE = 1
        internal const val REQUEST_CODE = 100
        internal const val UPDATEPG = 2
        internal const val UPDATE_PROGRESS = 3
        internal const val UPDATE_INSTALL = 4
        internal const val UPDATE_FAILE = 5
        internal const val COUNTDOWN_NEXT = 6
        internal const val COUNTDOWN_UPLOAD = 7
        internal const val LOAD_TIMER = 8
    }

    //常数
    var countDownSecond: Long = 3 * 1000

    //自動執行判斷
    var isAutoMonitoring: Boolean = false;
    var isHandChangeMonitorState: Boolean = false;

    //手动停止时使用
    private var isRunNetBare = false
    private lateinit var mNetBare: NetBare
    private lateinit var pg: ProgressBar
    private lateinit var packageListView: ListView
    private lateinit var adapter: AppListAdapter
    private lateinit var appinstallReceiver: AppinstallReceiver

    private lateinit var mRefreshButton: Button
    private lateinit var customProgress: CustomProgress

    private lateinit var deviceIdTextView: TextView
    private lateinit var idChangeButton: Button
    private lateinit var autoCheckBox: CheckBox


    //避免重複刷新
    private var isRefreshBackendData = false


    private var mSys_view: DragTableButton? = null

    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {

            Logger.e("msgwaht  = ${msg.what}")
            when (msg.what) {
                UPDATEPG -> {//更新列表
                    pg.visibility = View.GONE
                    adapter.update(AppUtils.getInstance().applist)
                    if (isAutoMonitoring) {
                        Log.e("TOA", "isAutoMonitoring = true")
                    } else {
                        if (!isHandChangeMonitorState) {
                            autoCheckBox.isChecked = true
                        }
                    }
                }
                UPDATE_PROGRESS -> {//更新下载进度
                    customProgress.setMessage(msg.obj.toString() + "%")
                }
                UPDATE_INSTALL -> {//安装apk
                    customProgress.dismiss()
                    AppUtils.getInstance().installAPK(this@MainActivity)
                }
                UPDATE_FAILE -> {//隐藏下载进度
                    customProgress.dismiss()
                }
                COUNTDOWN_NEXT -> {//测试结束，停止vpn
                    mNetBare.stop()
                }
                COUNTDOWN_UPLOAD -> {//vpn停止后上传数据，kill掉测试app
                    var appinfo = AppUtils.getInstance().currentAppinfo
                    Logger.e("countdown next app ${appinfo.name}")
                    appinfo.createDate = System.currentTimeMillis()
                    AppUtils.getInstance().updateAppinfoMap(appinfo)
                    adapter.update(AppUtils.getInstance().applist)
                    AppUtils.getInstance().addResponse("", null, true)
                    killApp(appinfo)
                }
                LOAD_TIMER -> {//轮训计时启动监测
                    if (autoCheckBox.isChecked) {
                        prepareNetBare(false)
                    }
                }
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMoonEvent(messageEvent: MessageEvent) {
        Logger.e("msg ${messageEvent.message} ,type ${messageEvent.type}")
        when (messageEvent.type) {
            MessageEvent.UPDATEAPP_INSTALL -> {//安装或者卸载app通知
                var appinfo = AppUtils.getInstance().getAppinfo(messageEvent.message)
                appinfo.action = "打开"
                AppUtils.getInstance().updateAppinfoMap(appinfo);
                adapter.update(AppUtils.getInstance().applist)
            }
            MessageEvent.UPDATEAPP_REMOVE -> {//安装或者卸载app通知
                var appinfo = AppUtils.getInstance().getAppinfo(messageEvent.message)
                appinfo.action = "下载"
                AppUtils.getInstance().updateAppinfoMap(appinfo)
                adapter.update(AppUtils.getInstance().applist)
            }
            MessageEvent.UPLOAD_SUCCESS -> {
                if (!isRunNetBare) {
                    Logger.e("UPLOAD_SUCCESS isRunnetbare $isRunNetBare")
                    return
                }
                prepareNetBare(false)
            }
            MessageEvent.UPLOAD_FAILED -> {
                Toast.makeText(this@MainActivity, "上传失败", Toast.LENGTH_SHORT).show()
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mNetBare = NetBare.get()
        isRunNetBare = false
        var deviceId = CommonConfigSp.getInstance().get(CommonConfigSp.DEVICEID, "")

        deviceIdTextView = findViewById(R.id.deviceTextView)
        idChangeButton = findViewById(R.id.changeDeviceIdBtn)
        autoCheckBox = findViewById(R.id.autoCheckBox)

        //deviceid textview show
        deviceIdTextView.text = deviceId
        //id change btn click
        idChangeButton.setOnClickListener {
            inputDeviceIdDialog()
        }
        //CheckBox狀態改變觸發動作
        autoCheckBox.setOnCheckedChangeListener(checkBoxOnCheckedChange)


        mRefreshButton = findViewById(R.id.refresh_btn)
        mRefreshButton.setOnClickListener {
            deviceId = CommonConfigSp.getInstance().get(CommonConfigSp.DEVICEID, "")
            if (deviceId.isNullOrEmpty()) {
                inputDeviceIdDialog()
                return@setOnClickListener;
            }
            if (!isRefreshBackendData) {
                isRefreshBackendData = true;
                Toast.makeText(
                    applicationContext,
                    R.string.reloadingBackendData,
                    Toast.LENGTH_SHORT
                ).show()
                Handler().postDelayed({
                    isRefreshBackendData = false
                }, 3000)
                refreshBackenData()
            } else {
                Toast.makeText(
                    applicationContext,
                    R.string.frequentReloadBackendData,
                    Toast.LENGTH_SHORT
                ).show()
            }

        }


        // 监听NetBare服务的启动和停止
        mNetBare.registerNetBareListener(this)

        pg = findViewById(R.id.pg)
        packageListView = findViewById(R.id.package_list)
        adapter = AppListAdapter(this, AppUtils.getInstance().applist) { postion, info, isopen ->

            Logger.e("isopen $isopen/ postion $postion/ info $info")
            if (isopen) {
                var device = CommonConfigSp.getInstance().get(CommonConfigSp.DEVICEID, "")
                if (TextUtils.isEmpty(device)) {
                    inputDeviceIdDialog()
                } else {
                    AppUtils.getInstance().currentAppinfo = info;
                    val intent = Intent(this, TargetObjectActivity::class.java).apply {
                        putExtra("uuid", info.uuid)
                        putExtra("name", info.name)
                        putExtra("packageName", info.packageName)
                        putExtra("memo", info.memo)
                        putExtra("deHttps", info.deHttps)
                        putExtra("startTime", info.startTime)
                        putExtra("stopTime", info.stopTime)
                        putExtra("downlaodUrl", info.downlaodUrl)
                        putExtra("createDate", info.createDate)
                        putExtra("action", info.action)
                        putExtra("whiteList", info.whiteList)
                        putExtra("blackList", info.blackList)

                    }
                    startActivity(intent)
                }

            } else {
                AppUtils.getInstance().openBrowser(this, info.downlaodUrl)
            }

        }

        packageListView.adapter = adapter

        appinstallReceiver = AppinstallReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED")
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED")
        intentFilter.addDataScheme("package")
        registerReceiver(appinstallReceiver, intentFilter) // 注册广播接收器
        EventBus.getDefault().register(this)
        startService(Intent(this, ForegroundService::class.java))
        setDeskFloatButton()

        if (deviceId.isNullOrEmpty()) {
            inputDeviceIdDialog()
        } else {
            refreshBackenData()
        }

    }

    override fun onResume() {
        super.onResume()
        adapter.update(AppUtils.getInstance().applist)
    }

    override fun onPause() {
        super.onPause()
        Logger.e("===========######========================")
//        handler.postDelayed({
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("netbare://goods:8888/goodsDetail?goodsId=10011002"))
//            startActivity(intent)
//            openApp("com.netbare.sample")
//            AppUtils.getInstance().setTopApp(this)
//            AppUtils.getInstance().bring2Front(this)
//        },2000)
    }

    override fun onStop() {
        super.onStop()
        Logger.e("===========######====stop====================")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.e("===========######==destory======================")
        mNetBare.unregisterNetBareListener(this)
        stopAll()
        unregisterReceiver(appinstallReceiver)
        EventBus.getDefault().unregister(this)
        handler.removeCallbacksAndMessages(null)
        SuspendUtils.removeWindowView(mSys_view, this)
    }

    override fun onServiceStarted() {
        Logger.e("onServiceStarted $isRunNetBare")
        runOnUiThread {
            if (!isRunNetBare) {
                return@runOnUiThread
            }
            openApp(AppUtils.getInstance().currentAppinfo.packageName)
            countDownNext(AppUtils.getInstance().currentAppinfo)
        }
    }

    override fun onServiceStopped() {
        runOnUiThread {
            Logger.e("onServiceStopped------------ $isRunNetBare")

            handler.sendEmptyMessageDelayed(COUNTDOWN_UPLOAD, 2000)
            AppUtils.getInstance().setTopApp(this)
        }


    }

    private fun countDownNext(info: AppInfo) {
        var time = info.stopTime * 1000;
        Logger.e("countDownNext   $time");
        handler.sendEmptyMessageDelayed(COUNTDOWN_NEXT, time.toLong())
        info.action = "读取中"
        AppUtils.getInstance().updateAppinfoMap(info)
        adapter.update(AppUtils.getInstance().applist)
    }

    private fun openApp(packageName: String) {
        val intent2: Intent = packageManager.getLaunchIntentForPackage(packageName)
        val classNameString = intent2.component.className //得到app类名

        val intent = Intent()
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        intent.component = ComponentName(packageName, classNameString)
        startActivity(intent)
    }

    private fun killApp(info: AppInfo) {
        try {
            Logger.e("kill app start===========")
            val mActivityManager: ActivityManager =
                getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            mActivityManager.killBackgroundProcesses(info.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("kill app fail ======== ${e.message}")
        }
    }

    private fun prepareNetBare(force: Boolean) {
        handler.removeMessages(LOAD_TIMER)

        if (!autoCheckBox.isChecked) {
            stopAll()
            return
        }
        setDeskBtnState(true)
        var listapp = AppUtils.getInstance().applist
        listapp.forEach {
            var startdata = System.currentTimeMillis() - it.createDate - it.startTime * 1000
            if (AppUtils.getInstance().isOpen(it.packageName) && (startdata > 0 || force)) {
                Logger.e("apinfo ${it.toString()}")
                AppUtils.getInstance().currentAppinfo = it
                // 安装自签证书
                if (!JKS.isInstalled(this, App.JSK_ALIAS)) {
                    try {
                        JKS.install(this, App.JSK_ALIAS, App.JSK_ALIAS)
                    } catch (e: IOException) {
                        // 安装失败
                    }
                    stopAll()
                    return
                }
                // 配置VPN
                val intent = NetBare.get().prepare()
                if (intent != null) {
                    startActivityForResult(intent, REQUEST_CODE_PREPARE)
                    stopAll()
                    return
                }
//                NetBareConfig.defaultHttpConfig(App.getInstance().getJSK(),
//                        -                interceptorFactories())
                // 启动NetBare服务
                var builder = NetBareConfig.defaultConfig().newBuilder()
                builder.addAllowedApplication(it.packageName)
                builder.addPackterListener { host, port, method, data ->
                    Logger.e("packterlistener ---------host= $host  port=$port  method=$method")
                    UIThreadHandler.post {
                        AppUtils.getInstance().addTCPUDPRespone(host, port, method)
                    }
                }
                var jks = if (it.deHttps) {
                    App.getInstance().getJSK()
                } else {
                    null
                }
                CommonConfigSp.getInstance().put(it.uuid, System.currentTimeMillis())
                builder.setVirtualGatewayFactory(
                    HttpVirtualGatewayFactory(
                        jks,
                        interceptorFactories()
                    )
                )
                isRunNetBare = true
                mNetBare.start(builder.build())
                return
            }
        }
        handler.sendEmptyMessageDelayed(LOAD_TIMER, 2000)
        Logger.e("timer +++++");
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PREPARE) {
            prepareNetBare(false)
        }
    }

    private fun interceptorFactories(): List<HttpInterceptorFactory> {
//        InterceptorFactory
        // 拦截器范例1：打印请求url
//        val interceptor1 = HttpUrlPrintInterceptor.createFactory()
        //黑名单拦截使用
        val injector = HttpInjectInterceptor.createFactory(RejectHttpInjector())
        // 注入器范例1：替换百度首页logo
//        val injector1 = HttpInjectInterceptor.createFactory(BaiduLogoInjector())
//        // 注入器范例2：修改发朋友圈定位
//        val injector2 = HttpInjectInterceptor.createFactory(WechatLocationInjector())
        // 可以添加其它的拦截器，注入器
        // ...
        HttpInjectInterceptor.blackList = AppUtils.getInstance().currentAppinfo.blackList;
        return listOf(injector)
    }


    /**
     * 设置桌面悬浮框
     */
    private fun setDeskFloatButton() {
        SuspendUtils.canDrawOverlays(this, REQUEST_CODE)
        mSys_view = DragTableButton(this)
        CommonConfigSp.getInstance().setDeskObject(mSys_view)
        setDeskBtnState(false);
        mSys_view!!.setOnClickListener(View.OnClickListener {
//            Toast.makeText(this@MainActivity, "桌面悬浮框", Toast.LENGTH_SHORT).show()
            AppUtils.getInstance().setTopApp(this)
        })
        SuspendUtils.showDragTableButton(mSys_view, this)
    }

    private fun setDeskBtnState(isPlaying: Boolean) {
        if (isPlaying) {
            mSys_view!!.setBackgroundResource(R.drawable.monitoring_bg)
        } else {
            mSys_view!!.setBackgroundResource(R.drawable.float_bg)
        }
    }

    private var isExit: Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            val handler = Handler()

            if ((!isExit)) {
                isExit = true
                Toast.makeText(applicationContext, "再按一次退出游戏", Toast.LENGTH_LONG).show()
                handler.postDelayed({ isExit = false }, 1000 * 2) //x秒后没按就取消

            } else {
                finish()
                System.exit(0)
            }
        }
        return false
    }


    private fun inputDeviceIdDialog() {
        val alertDialog = AlertDialog.Builder(this@MainActivity)
        val v: View =
            layoutInflater.inflate(R.layout.set_custom_dialog_layout_with_button, null)
        alertDialog.setView(v)
        val btOK: Button = v.findViewById(R.id.button_ok)
        val btC: Button = v.findViewById(R.id.buttonCancel)
        val editText = v.findViewById<EditText>(R.id.ededed)
        var deviceId = CommonConfigSp.getInstance().get(CommonConfigSp.DEVICEID, "")
        editText.setText(deviceId)
        val dialog = alertDialog.create()
        dialog.show()
        btOK.setOnClickListener { v1 ->
            var deviceId = editText.text.toString()
            if (editText.text.toString().isNullOrEmpty()) {
                val twoDialog = AlertDialog.Builder(this@MainActivity)
                twoDialog.setTitle("请输入DeviceId")
                twoDialog.setPositiveButton(
                    "好的"
                ) { dialog1: DialogInterface?, which: Int -> }
                twoDialog.show()
            } else {
                CommonConfigSp.getInstance().put(CommonConfigSp.DEVICEID, deviceId)
                refreshBackenData()
                deviceIdTextView.text = deviceId
                dialog.dismiss()
            }
        }
        btC.setOnClickListener { v1 ->

            dialog.dismiss()
        }
    }

    private val checkBoxOnCheckedChange =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->

            var deviceId = CommonConfigSp.getInstance().get(CommonConfigSp.DEVICEID, "")
            if (deviceId.isNullOrEmpty()) {
                inputDeviceIdDialog()
                autoCheckBox.isChecked = false
                return@OnCheckedChangeListener
            }

            val timer = object : CountDownTimer(countDownSecond, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    Log.e("TTAA", millisUntilFinished.toString())
                    customProgress.setMessage((millisUntilFinished / 1000).toString())
                }

                override fun onFinish() {
                    //開始監測
                    prepareNetBare(true)
                    customProgress.dismiss();
                }
            }
            //buttonView 為目前觸發此事件的 CheckBox, isChecked 為此 CheckBox 目前的選取狀態
            if (isChecked) //等於 buttonView.isChecked()
            {
                customProgress = CustomProgress.show(this, "10", true) {
                    customProgress.dismiss();
                    timer.cancel()
                    isHandChangeMonitorState = true
                    autoCheckBox.isChecked = false
                }
                if (isHandChangeMonitorState) {
                    isHandChangeMonitorState = false
                }

                timer.start()
//                Toast.makeText(
//                    applicationContext,
//                    buttonView.text.toString() + " 被選取",
//                    Toast.LENGTH_SHORT
//                ).show()
            } else {
                stopAll()
            }
        }


    private fun refreshBackenData() {
        var deviceId = CommonConfigSp.getInstance().get(CommonConfigSp.DEVICEID, "")
        AppUtils.getInstance().clearAppListMap()
        adapter.update(AppUtils.getInstance().applist)
        ThreadProxy.getInstance().execute {
            AppUtils.getInstance().loadInstallApp(this)
            AppUtils.getInstance().loadApplist("api/app/list?deviceId=" + deviceId, handler)
        }
    }

    private fun stopAll() {
        autoCheckBox.isChecked = false
        isHandChangeMonitorState = true
        isAutoMonitoring = false
        setDeskBtnState(false)
        mNetBare.stop();
        handler.removeMessages(LOAD_TIMER)
    }

}

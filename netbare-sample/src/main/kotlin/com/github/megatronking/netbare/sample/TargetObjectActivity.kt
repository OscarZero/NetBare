package com.github.megatronking.netbare.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.widget.EditText
import android.widget.TextView
import android.view.KeyEvent
import android.view.View
import android.widget.*
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


class TargetObjectActivity : AppCompatActivity() ,NetBareListener{


    //NetBare相關宣告
    //手动停止时使用
    private var isRunNetBare = false
    private lateinit var mNetBare : NetBare

    //UI宣告
    private lateinit var appNameTextView : TextView
    private lateinit var packageNameTextView : TextView
    private lateinit var monitorTimeEditText:EditText
    private lateinit var whiteListTextView: TextView
    private lateinit var blackListTextView: TextView
    private lateinit var monitorButton: Button


    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {

            Logger.e("msgwaht  = ${msg.what}")
            when(msg.what){

                MainActivity.COUNTDOWN_NEXT -> {//测试结束，停止vpn
                    mNetBare.stop()
                    CommonConfigSp.getInstance().floatButton.setBackgroundResource(R.drawable.float_bg)
                }
                MainActivity.COUNTDOWN_UPLOAD -> {//vpn停止后上传数据，kill掉测试app
                    var appinfo = AppUtils.getInstance().currentAppinfo
                    Logger.e("TOAcountdown next app ${appinfo.name}")
                    AppUtils.getInstance().addResponse("", null, true)
                    killApp(appinfo)
                }
            }

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_object)

        //NetBare
        mNetBare = NetBare.get()
        isRunNetBare = false
        // 监听NetBare服务的启动和停止
        mNetBare.registerNetBareListener(this)

        //UI VIEW
        var uuid = intent.getStringExtra("uuid");
        var name = intent.getStringExtra("name");
        var packageName = intent.getStringExtra("packageName");
        var memo = intent.getStringExtra("memo");
        var deHttps = intent.getBooleanExtra("deHttps",false);
        var startTime = intent.getIntExtra("startTime",0);
        var stopTime = intent.getIntExtra("stopTime",0);
        var downlaodUrl = intent.getStringExtra("downlaodUrl");
        var createDate = intent.getIntExtra("createDate",0);
        var action = intent.getStringExtra("action");
        var whiteList = intent.getSerializableExtra("whiteList");
        var blackList = intent.getSerializableExtra("blackList");

//
//
//
        appNameTextView = findViewById(R.id.appname_title)
        appNameTextView.setText(name)
        packageNameTextView = findViewById(R.id.packagename_title)
        packageNameTextView.setText(packageName)
        monitorTimeEditText = findViewById(R.id.secondEditText)
        monitorTimeEditText.setText(stopTime.toString())
        whiteListTextView = findViewById(R.id.whiteTextArea)
        whiteListTextView.movementMethod = ScrollingMovementMethod.getInstance()
        whiteListTextView.setText(whiteList.toString())
        whiteListTextView.scrollTo(0, 0);
        blackListTextView = findViewById(R.id.blackTextArea)
        blackListTextView.movementMethod = ScrollingMovementMethod.getInstance()
        blackListTextView.setText(blackList.toString())
        blackListTextView.scrollTo(0, 0);

        monitorButton = findViewById(R.id.monitor_btn)
        monitorButton.setText(R.string.monitorBtnStartText)
        monitorButton.setOnClickListener {

            var targetSecond = monitorTimeEditText.text.toString().toIntOrNull()?:30
            if(targetSecond<0){
                AlertDialog.Builder(this)
                    .setMessage(R.string.monitorTimeTooShortTip)
                    .setTitle("Error")
                    .setPositiveButton("OK",null)
                    .show()
                return@setOnClickListener
            }

            if (mNetBare.isActive) {
                isRunNetBare = false
                mNetBare.stop()
                CommonConfigSp.getInstance().floatButton.setBackgroundResource(R.drawable.float_bg)
            } else{
                startNetBare()
            }

//            //開起監測目標app
//            val intent2: Intent = packageManager.getLaunchIntentForPackage(packageName)
//            val classNameString = intent2.component.className //得到app类名
//
//            val intent = Intent()
//            intent.action = Intent.ACTION_MAIN
//            intent.addCategory(Intent.CATEGORY_LAUNCHER)
//            intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
//                    or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
//            intent.component = ComponentName(packageName, classNameString)
//            startActivity(intent)

        }








    }



    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Logger.e("TOA===========######========================")
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
        Logger.e("TOA===========######====stop====================")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.e("TOA===========######==destory======================")
        mNetBare.unregisterNetBareListener(this)
        mNetBare.stop()
        CommonConfigSp.getInstance().floatButton.setBackgroundResource(R.drawable.float_bg)
        EventBus.getDefault().unregister(this)
    }

    override fun onServiceStarted() {
        Logger.e("TOAonServiceStarted $isRunNetBare")
        runOnUiThread {
            monitorButton.setText(R.string.monitorBtnStopText)
            if(!isRunNetBare){
                return@runOnUiThread
            }
            var sTime :Int = monitorTimeEditText.text.toString().toInt();
            if(AppUtils.getInstance().currentAppinfo.stopTime != sTime){
                AppUtils.getInstance().currentAppinfo.stopTime = sTime
            }
            var time = AppUtils.getInstance().currentAppinfo.stopTime * 1000;
            Logger.e("countDownNext   $time");
            handler.sendEmptyMessageDelayed(MainActivity.COUNTDOWN_NEXT, time.toLong())
            openApp(AppUtils.getInstance().currentAppinfo.packageName)
        }
    }

    override fun onServiceStopped() {
        runOnUiThread {
            monitorButton.setText(R.string.monitorBtnStartText)
            Logger.e("TOAonServiceStopped------------ $isRunNetBare")
            handler.sendEmptyMessageDelayed(MainActivity.COUNTDOWN_UPLOAD, 2000)
            AppUtils.getInstance().setTopApp(this)
        }
    }


    private fun openApp(packageName: String){
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
    private fun killApp(info: AppInfo){
        try {
            Logger.e("TOAkill app start===========")
            val mActivityManager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            mActivityManager.killBackgroundProcesses(info.packageName)
        }catch (e: Exception){
            e.printStackTrace()
            Logger.e("TOAkill app fail ======== ${e.message}")
        }
    }
    private fun startNetBare() {

        CommonConfigSp.getInstance().floatButton.setBackgroundResource(R.drawable.monitoring_bg)
        var monitorTime = monitorTimeEditText.text;
        Logger.e("TOAapinfo ${AppUtils.getInstance().currentAppinfo.packageName}")
        // 安装自签证书
        if (!JKS.isInstalled(this, App.JSK_ALIAS)) {
            try {
                JKS.install(this, App.JSK_ALIAS, App.JSK_ALIAS)
            } catch (e: IOException) {
                // 安装失败
            }
            return
        }
        // 配置VPN
        val intent = NetBare.get().prepare()
        if (intent != null) {
            startActivityForResult(intent, MainActivity.REQUEST_CODE_PREPARE)
            return
        }
        var builder =  NetBareConfig.defaultConfig().newBuilder()
        builder.addAllowedApplication(AppUtils.getInstance().currentAppinfo.packageName)
        builder.addPackterListener { host, port, method, data ->
            Logger.e("TOApackterlistener ---------host= $host  port=$port  method=$method")
            UIThreadHandler.post {
                AppUtils.getInstance().addTCPUDPRespone(host, port, method)
            }
        }
        var jks = if(AppUtils.getInstance().currentAppinfo.deHttps){
            App.getInstance().getJSK()
        }else{
            null
        }
        Logger.e("TOAAAA")
        CommonConfigSp.getInstance().put(AppUtils.getInstance().currentAppinfo.uuid, System.currentTimeMillis())
                builder.setVirtualGatewayFactory(
                    HttpVirtualGatewayFactory(
                        jks,
                        interceptorFactories()
                    )
                )
        isRunNetBare = true
        mNetBare.start(builder.build())

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == MainActivity.REQUEST_CODE_PREPARE) {
            startNetBare()
        }
    }

    private fun interceptorFactories() : List<HttpInterceptorFactory> {
        val injector = HttpInjectInterceptor.createFactory(RejectHttpInjector())
        HttpInjectInterceptor.blackList = AppUtils.getInstance().currentAppinfo.blackList;
        return listOf(injector)
    }
}
package com.github.megatronking.netbare.sample.util

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.AnimationDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.github.megatronking.netbare.sample.R

class CustomProgress :Dialog{
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    /**
     * 当窗口焦点改变时调用
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        val imageView = findViewById<View>(R.id.spinnerImageView) as ImageView
        // 获取ImageView上的动画背景
        val spinner = imageView.getBackground() as AnimationDrawable
        // 开始动画
        spinner.start()
    }

    /**
     * 给Dialog设置提示信息
     *
     * @param message
     */
    fun setMessage(message: CharSequence?) {
        if (message != null && message.length > 0) {
            findViewById<View>(R.id.SpinnerMessage).setVisibility(View.VISIBLE)
            val txt = findViewById<View>(R.id.SpinnerMessage) as TextView
            txt.text = message
            txt.invalidate()
        }
    }

    companion object {
        /**
         * 弹出自定义ProgressDialog
         *
         * @param context
         * 上下文
         * @param message
         * 提示
         * @param cancelable
         * 是否按返回键取消
         * @param cancelListener
         * 按下返回键监听
         * @return
         */
        fun show(
                context: Context,
                message: CharSequence?,
                cancelable: Boolean,
                cancelListener: DialogInterface.OnCancelListener?
        ): CustomProgress {
            val dialog = CustomProgress(context, R.style.Custom_Progress)
            dialog.setTitle("")
            dialog.setContentView(R.layout.progress_custom)
            if (message == null || message.length == 0) {
                dialog.findViewById<View>(R.id.SpinnerMessage).setVisibility(View.GONE)
            } else {
                val txt = dialog.findViewById<View>(R.id.SpinnerMessage) as TextView
                txt.text = message
            }
            // 按返回键是否取消
            dialog.setCancelable(cancelable)
            // 监听返回键处理
            dialog.setOnCancelListener(cancelListener)
            // 设置居中
            dialog.window!!.attributes.gravity = Gravity.CENTER
            val lp = dialog.window!!.attributes
            // 设置背景层透明度
            lp.dimAmount = 0.2f
            dialog.window!!.attributes = lp
            // dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            dialog.show()
            return dialog
        }
    }
}
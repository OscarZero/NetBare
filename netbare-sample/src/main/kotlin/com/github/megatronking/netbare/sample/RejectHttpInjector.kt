package com.github.megatronking.netbare.sample

import android.util.Log
import com.github.megatronking.netbare.http.HttpRequest
import com.github.megatronking.netbare.http.HttpResponse
import com.github.megatronking.netbare.injector.BlockedHttpInjector
import com.github.megatronking.netbare.sample.packge.AppUtils
import com.github.megatronking.netbare.sample.util.Logger
import java.nio.ByteBuffer

class RejectHttpInjector :BlockedHttpInjector(){
    override fun sniffRequest(request: HttpRequest): Boolean {
        var url = "";//request.url()
        var host = request.host();
        Logger.e("RejectHttpInjector---@---sniffRequest host= ${request.host()}  url= $url ")
        var currentAppinfo = AppUtils.getInstance().currentAppinfo
        if(currentAppinfo != null && currentAppinfo.isBlack(host)){
            return true
        }
        return false
    }

    override fun sniffResponse(response: HttpResponse): Boolean {
        return false
    }

    override fun intercept(response: HttpResponse, buffer: ByteBuffer, index: Int, content: String) {
        Logger.e("response =====RejectHttpInjector==response--- body :"+response.id()+"/ "+index+"/ "+content)
        var jsonObject = AppUtils.getInstance().updateResponseTojson(response.id(),content)
        AppUtils.getInstance().addHttpIdPort(response.ip(),response.port())
        AppUtils.getInstance().addResponse(response.id(),jsonObject,false)
    }

    override fun intercept(request: HttpRequest, buffer: ByteBuffer, index: Int, content: String) {
        Logger.e("requeset =====RejectHttpInjector==111111  request--- body :"+request.id()+"/ "+index+"/ "+content)
        AppUtils.getInstance().addHttpIdPort(request.ip(),request.port())
        AppUtils.getInstance().addResponse(request.id(),request.updateRequestTojson(content),false)
    }

}
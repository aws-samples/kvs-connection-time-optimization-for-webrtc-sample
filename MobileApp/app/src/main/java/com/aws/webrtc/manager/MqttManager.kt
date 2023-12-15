//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.manager

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import com.aws.webrtc.util.ConstantUtil
import com.github.lzyzsd.jsbridge.BridgeWebView
import com.github.lzyzsd.jsbridge.BridgeWebViewClient
import com.github.lzyzsd.jsbridge.CallBackFunction

class MqttManager(context: Context) {

    private val TAG = ConstantUtil.PREFIX + this.javaClass.simpleName

    private var mWebView: BridgeWebView? = null
    private var mContext: Context? = null

    private lateinit var mConnectivityManager: ConnectivityManager
    private lateinit var mNetworkCallback: NetworkCallback

    init {
        val bridgeWebView = BridgeWebView(context)
        mWebView = bridgeWebView
        mContext = context
    }

    /** Called when the following actions finished:
     *  1. Sign in (credential + identityId)
     *  2. Get Client config (IotAtsEndpoint)
     *  3. Add target
     * */
    fun startMqtt() {
        Log.i(TAG, "startMqtt")
        initSettings()
        registerHandler()
        loadUrl()
        registerNetworkCallback()
        if (UserStateManager.getInstance().isMqttConnected.value != true) {
            JsAndroidCommManager.getInstance().connectMQTT()
        }
        Log.i(TAG, "MQTT start")
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initSettings() {
        mWebView!!.webChromeClient = WebViewChromeClient()
        mWebView!!.webViewClient = BridgeWebViewClient(mWebView)
        val settings = mWebView!!.settings
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
    }

    private fun loadUrl() {
        Log.d(TAG, "loadUrl")
        mWebView!!.loadUrl("file:///android_asset/iot_core/index.html")
    }

    private inner class WebViewChromeClient : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            Log.d(TAG, "(Iot WebView) ${consoleMessage.message()}")
            return super.onConsoleMessage(consoleMessage)
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress == 100) {
                Log.i(TAG, "onProgressChanged, newProgress (MQTT) = $newProgress")
            }
        }
    }

    private fun registerHandler() {
        mWebView!!.registerHandler("getAuthSession") { data: String, responseCallback: CallBackFunction? ->
            Log.d(TAG, data)
            if (responseCallback != null) {
                sendAuthSessionToJs(responseCallback)
            }
        }

        mWebView!!.registerHandler("getIotAtsEndpoint") { data: String, responseCallback: CallBackFunction? ->
            Log.d(TAG, data)
            if (responseCallback != null) {
                sendIotAtsEndpointToJs(responseCallback)
            }
        }

        mWebView!!.registerHandler("getIdentityId") { data: String, responseCallback: CallBackFunction? ->
            Log.d(TAG, data)
            if (responseCallback != null) {
                sendIdentityIdToJs(responseCallback)
            }
        }

        JsAndroidCommManager.getInstance().bridgeWebView = mWebView
    }

    private fun sendIdentityIdToJs(callback: CallBackFunction) {
        JsAndroidCommManager.getInstance().getIdentityId(callback)
    }

    private fun sendAuthSessionToJs(callback: CallBackFunction) {
        JsAndroidCommManager.getInstance().getAuthSession(callback, false)
    }

    private fun sendIotAtsEndpointToJs(callback: CallBackFunction) {
        JsAndroidCommManager.getInstance().getIotAtsEndpoint(callback)
    }

    private fun registerNetworkCallback() {
        // register DefaultNetworkCallback to observe current network status.
        mConnectivityManager = mContext?.getSystemService(ConnectivityManager::class.java)!!
        mNetworkCallback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "network is on available")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "network is on lost")
            }
        }
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback)
    }
}
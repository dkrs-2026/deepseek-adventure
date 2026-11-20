package com.uusse.deepseekadventure.core.network

import android.content.Context
import android.net.ConnectivityManager

/** 弱网/断网检测，供 UI 给出明确提示 */
class NetworkMonitor(context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Suppress("DEPRECATION")
    val isOnline: Boolean
        get() = cm.activeNetworkInfo?.isConnected == true
}

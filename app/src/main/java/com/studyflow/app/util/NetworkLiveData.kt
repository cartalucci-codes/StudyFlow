package com.studyflow.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData

/** true while the phone has an internet connection - drives the "You're offline" banner. */
class NetworkLiveData(context: Context) : LiveData<Boolean>() {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = postValue(true)
        override fun onLost(network: Network) = postValue(isConnectedNow())
    }

    override fun onActive() {
        value = isConnectedNow()
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    override fun onInactive() {
        connectivityManager.unregisterNetworkCallback(callback)
    }

    private fun isConnectedNow(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}

package io.nubhub.candlestick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(MainActivity.TAG,"intent action:"+intent?.action.toString())
    }
}
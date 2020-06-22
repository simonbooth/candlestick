package io.nubhub.candlestick

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.result.Result
import com.google.firebase.iid.FirebaseInstanceId
import com.google.android.gms.tasks.OnCompleteListener
import com.google.gson.Gson
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class Device(
    var DeviceId: String = "",
    var FirebaseToken: String = "",
    var PairingCode: String = "",
    var SubscriberId: String = ""
)

class NubHubService : Service() {

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null
    val PREFS_FILENAME = "io.nubhub.candlestick.prefs"
    var prefs: SharedPreferences? = null
    private var broadcaster: LocalBroadcastManager? = null


    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
        broadcaster = LocalBroadcastManager.getInstance(this)

    }

    public fun getSubscriberInfo(token:String?){
    prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
    var currentState = Device()
    currentState.FirebaseToken = token.toString()
    currentState.DeviceId = prefs?.getString("deviceId", "").toString()

    Log.d(MainActivity.TAG,"calling nubhub:"+ currentState.FirebaseToken+"," + currentState.DeviceId)

    Fuel.post("https://candlestick.nubhub.io/api")
        .jsonBody(Gson().toJson(currentState).toString())
        .responseObject<Device> { _, _, result ->
            Log.d(MainActivity.TAG,"response from nubhub:"+  (result as Result.Success).value.toString())
            val intent = Intent("CandlestickInit")
            intent.putExtra("firebaseToken", currentState.FirebaseToken)
            intent.putExtra(
                "pairingCode",
                (result as Result.Success).value.PairingCode.toString()
            )
            broadcaster!!.sendBroadcast(intent)
            prefs?.edit()?.putString("deviceId",(result as Result.Success).value.DeviceId.toString())?.apply()
        }


    // send the token to the server, the server should return a short pairing code (which can rotate) if not paired
    // display the short code on the screen.
    // OR
    // server returns meta info : Display Name, Background Pic, Shareable ID Number
    // display background pic, overlay your Display Name and shareable ID number
    // db : device-token | Device Name
    // db : shareable ID | [device-tokens] | Person Name
    // db : pairing code | token | expiration (change token every 5 mins, expire after 10 mins)
    // web : setup screen : New user (Enter pairing Code), Existing user (Enter pairing Code + shareable ID)
    // web : invite user : shareable ID + URL
    // email : new mail (gmail), parse URL

    // meet, skype, zoom

}
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()
        // Instantiate the RequestQueue.
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
//                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token
                Log.d(MainActivity.TAG, "firebase token:" + token)

//                val thisDevice = Device(firebaseToken = token)
                getSubscriberInfo(token)
            })

//            "https://api.nubhub.io/", callback, executor
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

}

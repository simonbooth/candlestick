package io.nubhub.candlestick

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyNotificationService :  FirebaseMessagingService() {
    private var broadcaster: LocalBroadcastManager? = null

    override fun onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this)
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(MainActivity.TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(MainActivity.TAG, "Message data payload: " + remoteMessage.data)
            val intent = Intent("CandlestickInboundMessage")
            for (datum in remoteMessage.data) {
                intent.putExtra(datum.key,datum.value)
            }
//            intent.putExtra("command", remoteMessage.data["command"])
//            intent.putExtra("url", remoteMessage.data["url"])
//            intent.putExtra("subject", remoteMessage.data["subject"])
//            intent.putExtra("sender", remoteMessage.data["sender"])
           broadcaster!!.sendBroadcast(intent)
            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                // scheduleJob()

            } else {
                // Handle message within 10 seconds
                //handleNow()
            }
        }


        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

    }


}

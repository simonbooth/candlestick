package io.nubhub.candlestick

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import com.zipow.videobox.confapp.ConfMgr
import us.zoom.sdk.InMeetingEventHandler
import us.zoom.sdk.MeetingActivity

class CustomMeetingActivity : MeetingActivity () {
//    private var broadcaster: LocalBroadcastManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        broadcaster = LocalBroadcastManager.getInstance(this)
        connectVoIP()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
//            Toast.makeText(this, "Green clicked.", Toast.LENGTH_LONG).show();
            Log.d(MainActivity.TAG,"green pressed")
            //todo - refactor into localbroadcast
            val intent = Intent("io.nubhub.candlestick.CANDLESTICK_BUTTONS")
            intent.putExtra(
                "button",
                "green"
            )
            sendBroadcast(intent)
//onClickEndButton()

            ConfMgr.getInstance().leaveConference()
            //handle click
            //joinMeeting(textView2.text.toString())

            true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val intent = Intent("io.nubhub.candlestick.CANDLESTICK_BUTTONS")
            intent.putExtra(
                "button",
                "blue"
            )
            sendBroadcast(intent)

//            Toast.makeText(this, "Blue clicked.", Toast.LENGTH_LONG).show();
            //handle click
            //joinMeeting(textView2.text.toString())
            //true
            false
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val intent = Intent("io.nubhub.candlestick.CANDLESTICK_BUTTONS")
            intent.putExtra(
                "button",
                "yellow"
            )
            sendBroadcast(intent)
//            Toast.makeText(this, "Yellow clicked.", Toast.LENGTH_LONG).show();
            //handle click
            //joinMeeting(textView2.text.toString())
            //true
            false
        } else super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
        private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }
}

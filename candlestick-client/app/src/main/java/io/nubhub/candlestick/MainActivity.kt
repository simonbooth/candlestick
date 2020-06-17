package io.nubhub.candlestick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil.api.load
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import us.zoom.sdk.*

public class MainActivity() : AppCompatActivity(),
    ZoomSDKInitializeListener,
    MeetingServiceListener, ZoomSDKAuthenticationListener {

    var callInProgressUrl: String = ""
    var callInProgressDisplayName: String = "Anonymous User"
    var zoomAppKey: String = ""
    var zoomAppSecret: String = ""
    var bgUrl: String = ""


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        val powerManager =
            getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "candlestick:MyWakelockTag")
        wakeLock.acquire()
        val decorView: View = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { hideSystemUI() }
        Intent(this, NubHubService::class.java).also { intent ->
            startService(intent)
        }
    }

    override fun onResume() {
        var intentFilter = IntentFilter()
        intentFilter.addAction("CandlestickInit")
        intentFilter.addAction("CandlestickInboundMessage")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            intentFilter
        )
        val filterGlobal = IntentFilter().apply {
            intentFilter.addAction("io.nubhub.candlestick.CANDLESTICK_BUTTONS")
        }
        registerReceiver(mMessageReceiver, filterGlobal)

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

    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)

    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mMessageReceiver)
    }

    fun initializeZoom() {
        val zoomSDK = ZoomSDK.getInstance()
        zoomSDK.initialize(
            this,
            zoomAppKey,
            zoomAppSecret,
            this as ZoomSDKInitializeListener
        )
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "intent-receive:" + intent.action)
            if (intent.action == "CandlestickInit") {
                if (intent.extras!!.getString("pairingCode") != "") {
                    tvStatus.text = "activating"
                    tvPairingCode.text = intent.extras!!.getString("pairingCode")
                    tvPairingCode.visibility = View.VISIBLE
                } else {
//                    initializeZoom()
//                    initializeAppState()
//                    if(intent.extras!!.getString("subscriberId")!=""){
//                        tvSubscriberId.text = intent.extras!!.getString("subscriberId")
//                    }
//                    textView2.text = "Ready" //todo populate with subscriber info
                }
            } else if (intent.action == "io.nubhub.candlestick.CANDLESTICK_BUTTONS") {
                val foo: CharSequence =
                    "" + intent.extras!!.getString("button").toString() + " clicked"
                Toast.makeText(applicationContext, foo, Toast.LENGTH_LONG).show()
                if (intent.extras!!.getString("button") == "green") {
                    val zoomSDK = ZoomSDK.getInstance()
                    val meetingService = zoomSDK.meetingService
                    meetingService.leaveCurrentMeeting(false)
                    tvStatus.text = "ready"
                }
            } else if (intent.action == "CandlestickInboundMessage") {
                val command = intent.extras!!.getString("command")
                if (command == "activated") {
                    tvPairingCode.text = ""
                    tvPairingCode.visibility = View.INVISIBLE
                    if (intent.extras!!.getString("zoomAppKey") != "") {
                        zoomAppKey = intent.extras!!.getString("zoomAppKey").toString()
                    }
                    if (intent.extras!!.getString("zoomAppKey") != "") {
                        zoomAppSecret = intent.extras!!.getString("zoomAppSecret").toString()
                    }
                    if (intent.extras!!.getString("groupUIData") != "") {
                        var groupUIData: String =
                            intent.extras!!.getString("groupUIData").toString()
                        if (groupUIData.length > 0) {
                            val parser = JsonParser()
                            val element: JsonElement = parser.parse(groupUIData)
                            if (element.isJsonObject) {
                                val groupUIObj = element.asJsonObject
//                            println(profileObj["FirstName"].asString)
                                if (groupUIObj["AppBackground"].asString.length > 0) {
                                    bgUrl = groupUIObj["AppBackground"].asString
                                }
                            }
                        }
                    }
                    if (intent.extras!!.getString("userData") != "") {
                        var userData: String =
                            intent.extras!!.getString("userData").toString()
                        if (userData.length > 0) {
                            val parser = JsonParser()
                            val element: JsonElement = parser.parse(userData)
                            if (element.isJsonObject) {
                                val userObj = element.asJsonObject
//                            println(profileObj["FirstName"].asString)
                                if (userObj["Profile"].isJsonObject) {
                                    tvSkype.text =
                                        userObj["Profile"].asJsonObject["Skype"].asString
                                    tvWhatsApp.text =
                                        userObj["Profile"].asJsonObject["WhatsApp"].asString
                                    tvZoomEmail.text =
                                        userObj["Profile"].asJsonObject["ZoomEmail"].asString
                                    callInProgressDisplayName =
                                        userObj["Profile"].asJsonObject["DisplayName"].asString
                                }

                            }
                        }
                    }
                    if (intent.extras!!.getString("activationData") != "") {
                        var activationData: String =
                            intent.extras!!.getString("activationData").toString()
                        if (activationData.length > 0) {
                            val parser = JsonParser()
                            val element: JsonElement = parser.parse(activationData)
                            if (element.isJsonObject) {
                                val activationObj = element.asJsonObject
//                            println(profileObj["FirstName"].asString)
                                if (activationObj["SubscriberId"].asString.length > 0) {

                                    initializeAppState()
                                }

                            }
                        }
                    }
//                    if(intent.extras!!.getString("subscriberId")!=""){
//                        tvSubscriberId.text = intent.extras!!.getString("subscriberId")
//                    }
//                    textView2.text = "Ready" //todo populate with subscriber info
                } else if (command == "text") {

                    showTextMessage(
                        intent.extras!!.getString("sender"),
                        intent.extras!!.getString("body"),
                        intent.extras!!.getString("subject")
                    )
                } else {
                    showInboundCall(
                        intent.extras!!.getString("sender"),
                        intent.extras!!.getString("url"),
                        intent.extras!!.getString("subject")
                    )
                }
            }
        }
    }

    private fun initializeAppState() {
        if (bgUrl.length > 0) {
            imageViewBackground.load(bgUrl)
        }
        initializeZoom()
    }

    fun showInboundCall(sender: String?, url: String?, subject: String?) {
        callInProgressUrl = url.toString()
        val callNotification = NotificationDialogFragment()
        val args = Bundle()
        args.putString("sender", sender)
        args.putString("subject", subject)
        args.putString("url", url)
        callNotification.setArguments(args)
        callNotification.show(supportFragmentManager, "call")
    }

    fun showTextMessage(sender: String?, body: String?, subject: String?) {
        val textNotification = NotificationDialogFragment()
        val args = Bundle()
        args.putString("sender", sender)
        args.putString("body", body)
        args.putString("subject", subject)
        textNotification.setArguments(args)
        textNotification.show(supportFragmentManager, "call")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            Toast.makeText(this, "Green clicked.", Toast.LENGTH_LONG).show();
            //handle click
            //joinMeeting(textView2.text.toString())

            true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Toast.makeText(this, "Blue clicked.", Toast.LENGTH_LONG).show();
            //handle click
            //joinMeeting(textView2.text.toString())
            true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Toast.makeText(this, "Yellow clicked.", Toast.LENGTH_LONG).show();
            //handle click
            //joinMeeting(textView2.text.toString())
            true
        } else super.onKeyDown(keyCode, event)
    }

    public fun onGoClick(view: View?) {
        // Step 1: Get meeting number from input field.
        joinMeeting(callInProgressUrl)

    }

    fun joinMeeting(url: String = "") {
        tvStatus.text = "joining"
        val prev: Fragment? = supportFragmentManager.findFragmentByTag("call")
        if (prev != null) {
            val df: DialogFragment = prev as DialogFragment
            df.dismiss()
        }
        // Step 1: Get meeting number from input field.
        val matchResult = Regex("j/(\\d+)(?:\\?pwd=([^&]*))?").find(url)
        val (meetingNo, password) = matchResult!!.destructured
        Log.d(TAG, "Launching Meeting:" + meetingNo + " " + password)

//        val meetingNo: String = parts.group//"75907493208"//"73624397564" //https://us04web.zoom.us/j/73624397564?pwd=Qnd1Z0RSQzV3U1hyWEc0QWxTMVRHUT09
//        //https://us04web.zoom.us/j/75907493208
//        val password: String = ""//""Qnd1Z0RSQzV3U1hyWEc0QWxTMVRHUT09"
        // Check if the meeting number is empty.
        // Check if the meeting number is empty.
        if (meetingNo.length == 0) {
//            Toast.makeText(
//                this,
//                "You need to enter a meeting number/ vanity id which you want to join.",
//                Toast.LENGTH_LONG
//            ).show()
            return
        }

        // Step 2: Get Zoom SDK instance.
        val zoomSDK = ZoomSDK.getInstance()
//    zoomSDK.initialize(this, Constants.APP_KEY,Constants.APP_SECRET,
//        this as ZoomSDKInitializeListener
//    )
        // Check if the zoom SDK is initialized
        if (!zoomSDK.isInitialized) {
            tvStatus.text = "error : Not initialized"
//            Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG)
//                .show()
            return
        }

//        zoomSDK.meetingSettingsHelper.isCustomizedMeetingUIEnabled = true;

        // Step 3: Get meeting service from zoom SDK instance.
        val meetingService = zoomSDK.meetingService
        meetingService.addListener(this)

        // Step 4: Configure meeting options.
        val opts = JoinMeetingOptions()

        // Some available options
        opts.no_driving_mode = true;
        opts.no_invite = true;
        opts.no_meeting_end_message = true;
        opts.no_titlebar = true;
        opts.no_bottom_toolbar = true;
        opts.no_dial_in_via_phone = true;
        opts.no_dial_out_to_phone = true;
        opts.no_disconnect_audio = true;
        opts.no_share = true;
        //		opts.invite_options = InviteOptions.INVITE_VIA_EMAIL + InviteOptions.INVITE_VIA_SMS;
        //		opts.no_audio = true;
        //		opts.no_video = true;
        opts.meeting_views_options = MeetingViewsOptions.NO_BUTTON_SHARE;
        opts.no_meeting_error_message = true;
        //		opts.participant_id = "participant id";


        val params = JoinMeetingParams()

        params.displayName = callInProgressDisplayName
        params.meetingNo = meetingNo

        if (password.length > 0) {
            params.password = password
        }
        // Step 6: Call meeting service to join meeting
        meetingService.joinMeetingWithParams(this, params, opts)
    }

    override fun onZoomSDKInitializeResult(
        errorCode: Int,
        internalErrorCode: Int
    ) { //int errorCode, int internalErrorCode
        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
            tvStatus.text = "error : Z" + errorCode //todo populate with subscriber info
        } else {
            tvStatus.text = "ready"
        }
    }

    override fun onZoomSDKLoginResult(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onZoomIdentityExpired() {
        TODO("Not yet implemented")
    }

    override fun onZoomSDKLogoutResult(p0: Long) {
        TODO("Not yet implemented")
    }

    override fun onZoomAuthIdentityExpired() {
        TODO("Not yet implemented")
    }

    public override fun onMeetingStatusChanged(
        meetingStatus: MeetingStatus?, errorCode: Int,
        internalErrorCode: Int
    ) {
        Log.i(
            TAG,
            "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode
                    + ", internalErrorCode=" + internalErrorCode
        );
        when (meetingStatus) {
            MeetingStatus.MEETING_STATUS_INMEETING -> tvStatus.text = "in call"
            MeetingStatus.MEETING_STATUS_IDLE -> tvStatus.text = "ready"
            MeetingStatus.MEETING_STATUS_FAILED -> tvStatus.text = "error : " + errorCode
            MeetingStatus.MEETING_STATUS_CONNECTING -> tvStatus.text = "connecting"
            MeetingStatus.MEETING_STATUS_WAITINGFORHOST -> tvStatus.text = "waiting"
            MeetingStatus.MEETING_STATUS_DISCONNECTING -> tvStatus.text = "connecting"
            MeetingStatus.MEETING_STATUS_RECONNECTING -> tvStatus.text = "connecting"
            MeetingStatus.MEETING_STATUS_IN_WAITING_ROOM -> tvStatus.text = "waiting"
            MeetingStatus.MEETING_STATUS_WEBINAR_PROMOTE -> tvStatus.text = "waiting"
            MeetingStatus.MEETING_STATUS_WEBINAR_DEPROMOTE -> tvStatus.text = "waiting"
            MeetingStatus.MEETING_STATUS_UNKNOWN -> tvStatus.text = "unknown"
            null -> tvStatus.text = "unknown"
        }
    }

    public companion object {
        const val TAG = "Candlestick"
    }


}

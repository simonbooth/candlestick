package io.nubhub.candlestick

import android.content.DialogInterface
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_notification_dialog.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_SENDER = "sender"
private const val ARG_SUBJECT = "subject"
private const val ARG_URL = "url"

/**
 * A simple [Fragment] subclass.
 * Use the [NotificationDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificationDialogFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var sender: String? = null
    private var subject: String? = null
    public var url: String? = null
    private var rtUri: Uri? = null
    private var ringer: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            val mArgs = arguments
            subject= mArgs?.getString("subject")
            sender= mArgs?.getString("sender")
            url= mArgs?.getString("url")
            rtUri = Uri.parse(mArgs?.getString("rtUrl"))
        }
    }

    override fun onResume() {
        super.onResume()
//        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
//        params.width = WindowManager.LayoutParams.MATCH_PARENT
//        params.height = WindowManager.LayoutParams.MATCH_PARENT
//        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        title.text="Incoming call"
        textViewSubject.text = subject
        textViewDescription.text = sender + " is calling you. Press Green to accept the call, any other button to reject."
//        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringer = RingtoneManager.getRingtone(activity, rtUri)
        ringer?.play()
//        this.ringer = RingtoneManager.getRingtone(applicationContext, notification)
///    this.ringer?.stop()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        hideSystemUI()
        dialog?.setOnKeyListener(keyEventListener)
        return inflater.inflate(R.layout.fragment_notification_dialog, container, false)

    }
    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private val keyEventListener = DialogInterface.OnKeyListener { dialog, keyCode, event ->
        Log.i(javaClass.name, "onKey() keyCode: $keyCode")
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            val activity: MainActivity? = activity as MainActivity?
            activity?.onGoClick(null)
            true
        }
        else {
            false
        }
    }


    override fun onDestroyView() {
        dialog?.setOnKeyListener(null)
        super.onDestroyView()
        ringer?.stop()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NotificationDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(sender: String, subject: String, url:String) =
            NotificationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SENDER, sender)
                    putString(ARG_SUBJECT, subject)
                    putString(ARG_URL, url)
                }
            }
    }
}

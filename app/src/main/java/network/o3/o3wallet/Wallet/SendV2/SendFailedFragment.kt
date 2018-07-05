package network.o3.o3wallet.Wallet.SendV2

import android.app.KeyguardManager
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class SendFailedFragment : Fragment() {

    private lateinit var mView: View

    fun updatePageForSecondFailure() {
        mView.find<TextView>(R.id.transactionFailedSubtitleOne).text = resources.getString(R.string.SEND_neo_network_issue)
        mView.find<TextView>(R.id.transactionFailedSubtitleTwo).text = resources.getString(R.string.SEND_try_again_later)
        mView.find<Button>(R.id.failedMainActionButton).text = resources.getString(R.string.SEND_contact_support)
        mView.find<Button>(R.id.failedMainActionButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            val data = Uri.parse("mailto:support@o3.network")
            intent.data = data
            ContextCompat.startActivity(context!!, intent, null)
        }
    }

    fun initiateSendResultListener() {
        val sendActivity = activity as SendV2Activity
        sendActivity.sendViewModel.getSendResult().observe(this, Observer { result ->
            sendActivity.sendingToast?.cancel()
            if (result!!) {
                sendActivity.sendingToast?.cancel()
                mView.findNavController().navigate(R.id.action_sendFailedFragment_to_sendSuccessFragment)
            } else {
                O3PlatformClient().getChainNetworks {
                    updatePageForSecondFailure()
                    sendActivity.sendingToast?.cancel()
                    if (it.first == null) {
                        return@getChainNetworks
                    } else {
                        PersistentStore.setNodeURL(it.first!!.neo.best)
                    }
                }
            }
            mView.find<Button>(R.id.failedMainActionButton).setOnClickListener { verifyPassCodeAndSend() }
        })
    }

    private fun verifyPassCodeAndSend() {
        val mKeyguardManager = activity!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(this.context,
                    resources.getString(R.string.ALERT_no_passcode_setup),
                    Toast.LENGTH_LONG).show()
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                activity!!.startActivityForResult(intent, 1)
            }
        }
    }

    fun initiateFirstFailure(){
        mView.find<Button>(R.id.failedCloseActionButton).setOnClickListener { activity!!.finish() }
        mView.find<Button>(R.id.failedMainActionButton).setOnClickListener { verifyPassCodeAndSend() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.send_failed_fragment, container, false)
        initiateFirstFailure()
        initiateSendResultListener()
        return mView
    }
}

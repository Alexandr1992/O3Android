package network.o3.o3wallet.Wallet.SendV2


import android.app.KeyguardManager
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.send_review_fragment.*
import network.o3.o3wallet.R
import network.o3.o3wallet.format
import network.o3.o3wallet.formattedFiatString
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.support.v4.toast
import org.w3c.dom.Text

class SendReviewFragment : Fragment() {
    private lateinit var mView: View


    fun initiateSelectedAssetDetails() {
        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset!!.symbol)
            Glide.with(this).load(imageURL).into(find(R.id.reviewAssetLogoImageView))
            find<TextView>(R.id.reviewAssetSymbolTextView).text = selectedAsset.symbol
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

    fun initiateSendButton() {
        val sendButton = mView.find<Button>(R.id.sendButton)
        sendButton.setOnClickListener { verifyPassCodeAndSend() }
    }

    fun initateSelectedBalanceDetails() {
        mView.find<TextView>(R.id.reviewAmountTextView).text = (activity as SendV2Activity)
                .sendViewModel.getSelectedSendAmount().removeTrailingZeros()

        (activity as SendV2Activity).sendViewModel.getRealTimePrice().observe(this, Observer { realTimePrice ->
            val fiatAmount = realTimePrice!!.price * (activity as SendV2Activity).sendViewModel.getSelectedSendAmount()
            mView.find<TextView>(R.id.reviewFiatAmountTextView).text = fiatAmount.formattedFiatString()
        })
    }

    fun initiateSelectedRecipientDetails() {
        val contact = (activity as SendV2Activity).sendViewModel.getSelectedContact().value
        if (contact != null) {
            mView.find<TextView>(R.id.reviewSelectedAddressTextView).text = contact.address
            mView.find<TextView>(R.id.reviewNicknameTextView).text = contact.nickname
        } else {
            val address = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!
            mView.find<TextView>(R.id.reviewSelectedAddressTextView).text = address
            (activity as SendV2Activity).sendViewModel.getVerifiedAddress(true, address).observe(this, Observer { verifiedAddress ->
                if (verifiedAddress != null) {
                    mView.find<TextView>(R.id.reviewNicknameTextView).text = verifiedAddress.displayName
                    mView.find<ImageView>(R.id.reviewVerifiedBadge).visibility = View.VISIBLE
                }
            })
        }
    }

    fun initiateSendResultListener() {
        (activity as SendV2Activity).sendViewModel.getSendResult().observe(this, Observer { result ->
            if (result!!) {
                mView.findNavController().navigate(R.id.action_sendReviewFragment_to_sendSuccessFragment)
            } else {
                toast("Send Failed")
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.send_review_fragment, container, false)
        initateSelectedBalanceDetails()
        initiateSelectedAssetDetails()
        initiateSelectedRecipientDetails()
        initiateSendButton()
        initiateSendResultListener()
        return mView
    }
}

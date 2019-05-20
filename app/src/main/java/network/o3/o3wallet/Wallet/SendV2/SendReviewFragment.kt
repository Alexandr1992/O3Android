package network.o3.o3wallet.Wallet.SendV2


import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.Ontology.OntologyClient
import network.o3.o3wallet.R
import network.o3.o3wallet.formattedFiatString
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find

class   SendReviewFragment : Fragment() {
    private lateinit var mView: View


    fun initiateSelectedAssetDetails() {
        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset!!.symbol)
            Glide.with(this).load(imageURL).into(find(R.id.reviewAssetLogoImageView))
            mView.find<TextView>(R.id.reviewAmountTextView).text = (activity as SendV2Activity)
                    .sendViewModel.getSelectedSendAmount().toDouble().removeTrailingZeros() + " " + selectedAsset.symbol
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
                .sendViewModel.getSelectedSendAmount().toDouble().removeTrailingZeros()

        (activity as SendV2Activity).sendViewModel.getRealTimePrice(false).observe(this, Observer { realTimePrice ->
               if (realTimePrice != null) {
                   val fiatAmount = realTimePrice.price * (activity as SendV2Activity).sendViewModel.getSelectedSendAmount().toDouble()
                   mView.find<TextView>(R.id.reviewFiatAmountTextView).text = fiatAmount.formattedFiatString()
               }
        })
    }

    fun initiateSelectedRecipientDetails() {
        val contact = (activity as SendV2Activity).sendViewModel.getSelectedContact().value
        if (contact != null) {
            mView.find<TextView>(R.id.reviewSelectedAddressTextView).text = contact.address
            mView.find<TextView>(R.id.reviewNicknameTextView).text = contact.nickname
        } else if ((activity as SendV2Activity).sendViewModel.nnsName != "") {
            mView.find<TextView>(R.id.reviewNicknameTextView).text = (activity as SendV2Activity).sendViewModel.nnsName
            mView.find<TextView>(R.id.reviewSelectedAddressTextView).text = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!
        } else {
            val address = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!
            mView.find<TextView>(R.id.reviewNicknameTextView).visibility = View.INVISIBLE
            mView.find<TextView>(R.id.reviewSelectedAddressTextView).text = address
            (activity as SendV2Activity).sendViewModel.getVerifiedAddress(true, address).observe(this, Observer { verifiedAddress ->
                if (verifiedAddress != null) {
                    mView.find<TextView>(R.id.reviewNicknameTextView).text = verifiedAddress.displayName
                    mView.find<ImageView>(R.id.reviewVerifiedBadge).visibility = View.VISIBLE
                }
            })
        }
    }

    fun createSendResultIntent(sendViewModel: SendViewModel, sendResult: String?): Intent {
        val intent = Intent()
        intent.putExtra("assetName", sendViewModel.getSelectedAsset().value!!.name)
        intent.putExtra("amount", sendViewModel.getSelectedSendAmount().toDouble())
        intent.putExtra("address", sendViewModel.getSelectedAddress().value!!)

        if (sendViewModel.txID != null && !sendViewModel.txID.isEmpty()) {
            intent.putExtra("txId", sendViewModel.txID)
        }

        if (sendResult != null) {
            intent.putExtra("sendResult", sendResult)
        }

        return intent
    }

    fun initiateSendResultListener() {
        val sendActivity = activity as SendV2Activity
        sendActivity.sendViewModel.getSendResult().observe(this, Observer { result ->
            sendActivity.sendingToast?.cancel()

            val intent = createSendResultIntent(sendActivity.sendViewModel, result)
            if (result != null) {
                sendActivity?.setResult(Activity.RESULT_OK, intent)
                sendActivity.sendViewModel.txID = result
                mView.findNavController().navigate(R.id.action_sendReviewFragment_to_sendSuccessFragment)
            } else {
                sendActivity?.setResult(Activity.RESULT_CANCELED, intent)
                mView.findNavController().navigate(R.id.action_sendReviewFragment_to_sendFailedFragment)
            }
        })
    }

    fun initiateFeeCalculator() {
        val networkLabel = mView.find<TextView>(R.id.totalFeeTextView)
        val sendActivity = activity as SendV2Activity
        val checkBox = mView.find<CheckBox>(R.id.sendPriorityCheckbox)

        checkBox.setOnClickListener {
            if (!checkBox.isChecked) {
                sendActivity.sendViewModel.setNeoNetworkFee(0.0)
            } else {
                sendActivity.sendViewModel.setNeoNetworkFee(0.0011)
            }
        }

        if (sendActivity.sendViewModel.isNeoAsset()) {
            sendActivity.sendViewModel.getNeoNetworkFee().observe(this, Observer { result ->
                networkLabel.text = result.toString() + " GAS"
            })
            sendActivity.sendViewModel.setNeoNetworkFee(0.0)
        } else {
            checkBox.visibility = View.GONE
            mView.find<TextView>(R.id.mempoolStatusTextView).visibility = View.GONE
            sendActivity.sendViewModel.getOntologyNetworkFee().observe(this, Observer { result ->
                networkLabel.text = (result!! / OntologyClient().DecimalDivisor).toString() + " ONG"
            })
        }
    }

    fun inititiateMemPoolChecker() {
        val sendActivity = activity as SendV2Activity
        sendActivity.sendViewModel.getMemPoolHeight().observe(this, Observer { result ->
            val mempoolStatusLabel = mView.find<TextView>(R.id.mempoolStatusTextView)
            if (result == null) {
                mempoolStatusLabel.text = resources.getString(R.string.SEND_mempool_height_unknown)
            } else {
                mempoolStatusLabel.text = String.format(resources.getString(R.string.SEND_mempool_height), result)
            }

        })
    }

    fun listenForSendinProgress() {
        (activity as SendV2Activity).sendViewModel.getIsSending().observe(this, Observer {
            mView.find<Button>(R.id.sendButton).isEnabled = it != true
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.send_review_fragment, container, false)
        initateSelectedBalanceDetails()
        initiateSelectedAssetDetails()
        initiateSelectedRecipientDetails()
        listenForSendinProgress()
        initiateSendButton()
        initiateSendResultListener()
        initiateFeeCalculator()
        inititiateMemPoolChecker()
        return mView
    }
}

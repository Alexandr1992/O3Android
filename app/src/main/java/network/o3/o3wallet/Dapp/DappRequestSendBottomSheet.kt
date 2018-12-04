package network.o3.o3wallet.Dapp

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.textColor
import org.opengraph.OpenGraph
import java.lang.Exception
import java.math.BigDecimal

class DappRequestSendBottomSheet : RoundedBottomSheetDialogFragment() {

    lateinit var mView: View
    lateinit var sendRequest: NeoDappProtocol.SendRequest
    lateinit var dappMessage: DappMessage

    lateinit var openGraphTitleTextView: TextView
    lateinit var openGraphLogoView: ImageView
    lateinit var fromWalletNameTextView: TextView
    lateinit var toWalletAddressTextView: TextView
    lateinit var memoTextView: TextView
    lateinit var totalTextView: TextView

    lateinit var sendButton: Button
    lateinit var cancelButton: Button

    lateinit var loadingAnimationView: LottieAnimationView
    lateinit var loadingTextView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.dapp_send_request_bottom_sheet, container, false)
        sendRequest = Gson().fromJson(arguments!!.getString("send_request")!!)
        bindViews()
        setFeeControls()
        loadOpenGraphDetails()
        return mView
    }

    fun bindViews() {
        openGraphTitleTextView = mView.find(R.id.openGraphTitleView)
        openGraphLogoView = mView.find(R.id.openGraphLogoView)
        fromWalletNameTextView = mView.find(R.id.fromWalletNameTextView)
        toWalletAddressTextView = mView.find(R.id.toWalletAddressTextView)
        memoTextView = mView.find(R.id.memoTextView)
        totalTextView = mView.find(R.id.totalTextView)
        sendButton = mView.find(R.id.acceptSendButton)
        cancelButton = mView.find(R.id.rejectSendButton)

        loadingAnimationView = mView.find(R.id.loadingAnimationView)
        loadingTextView = mView.find(R.id.loadingStateTextView)

        fromWalletNameTextView.text = (activity as DAppBrowserActivityV2).jsInterface.getDappExposedWalletName()
        toWalletAddressTextView.text = sendRequest.toAddress
        memoTextView.text = sendRequest.remark
        totalTextView.text = sendRequest.amount + sendRequest.asset

        cancelButton.onClick {
            dismiss()
        }

        sendButton.onClick {
            showSendingState()
            bg {
                val success = (activity as DAppBrowserActivityV2).jsInterface.handleSend(dappMessage!!)
                finishSending(success)
            }

        }
    }

    fun showSendingState() {
        onUiThread {
            sendButton.visibility = View.GONE
            cancelButton.visibility = View.GONE
            loadingAnimationView.visibility = View.VISIBLE
            loadingTextView.visibility = View.VISIBLE
            loadingTextView.text = resources.getString(R.string.DAPP_processing_transaction)
        }
    }

    fun finishSending(success: Boolean) {
        onUiThread {
            if (success) {
                loadingAnimationView.setAnimation(R.raw.claim_success)
                loadingAnimationView.playAnimation()
                loadingTextView.textColor = ContextCompat.getColor(context!!, R.color.colorGain)
                loadingTextView.text = resources.getString(R.string.DAPP_transaction_succeeded)
            } else {
                loadingAnimationView.setAnimation(R.raw.task_failed)
                loadingTextView.textColor = ContextCompat.getColor(context!!, R.color.colorLoss)
                loadingTextView.text = resources.getString(R.string.DAPP_transaction_failed)
            }
        }
    }

    fun loadOpenGraphDetails() {
        val url = arguments!!.getString("url")
        try {
            bg {
                val dapp = OpenGraph(url, true)
                val title = dapp.getContent("title")
                val image = dapp.getContent("image")

                onUiThread {
                    openGraphTitleTextView.text = title
                    Glide.with(mView).load(image).into(openGraphLogoView)
                }
            }

        } catch (e: Exception) {

        }
    }

    fun setFeeControls() {
        var checkBox = mView.find<CheckBox>(R.id.feeCheckbox)
        var fee = BigDecimal.ZERO
        try {
            fee = BigDecimal(sendRequest.fee)
        } catch (e : Exception) { }
        if (fee != BigDecimal.ZERO) {
            checkBox.visibility = View.GONE
            mView.find<TextView>(R.id.dappFeeTitleTextView).text =
                    fee.toDouble().removeTrailingZeros()
        } else {
            checkBox.onClick {
                if (checkBox.isSelected) {
                    sendRequest.fee = "0.0011"
                    dappMessage.data = sendRequest
                } else {
                    sendRequest.fee = ""
                    dappMessage.data = sendRequest
                }
            }
        }
    }

    companion object {
        fun newInstance(): DappRequestSendBottomSheet {
            return DappRequestSendBottomSheet()
        }
    }
}

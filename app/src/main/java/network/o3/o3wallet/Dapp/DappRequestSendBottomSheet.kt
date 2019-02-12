package network.o3.o3wallet.Dapp

import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.kaopiz.kprogresshud.KProgressHUD
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.textColor
import org.opengraph.OpenGraph
import java.lang.Exception
import java.math.BigDecimal
import java.net.URL

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
    lateinit var assetLogoImageView: ImageView

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
        assetLogoImageView = mView.find(R.id.assetLogoImageView)
        val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", sendRequest.asset.toUpperCase())
        Glide.with(context).load(imageURL).into(assetLogoImageView)

        sendButton = mView.find(R.id.acceptSendButton)
        cancelButton = mView.find(R.id.rejectSendButton)

        loadingAnimationView = mView.find(R.id.loadingAnimationView)
        loadingTextView = mView.find(R.id.loadingStateTextView)


        fromWalletNameTextView.text = (activity as DAppBrowserActivityV2).jsInterface.getDappExposedWalletName()
        toWalletAddressTextView.text = sendRequest.toAddress
        memoTextView.text = sendRequest.remark
        totalTextView.text = sendRequest.amount + " " + sendRequest.asset

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
            isCancelable = false
            sendButton.visibility = View.INVISIBLE
            cancelButton.visibility = View.INVISIBLE
            loadingAnimationView.visibility = View.VISIBLE
            loadingTextView.visibility = View.VISIBLE
            loadingTextView.text = resources.getString(R.string.DAPP_processing_transaction)
        }
    }

    fun finishSending(success: Boolean) {
        onUiThread {
            isCancelable = true
            if (success) {
                loadingAnimationView.setAnimation(R.raw.claim_success)
                loadingAnimationView.playAnimation()
                loadingTextView.textColor = ContextCompat.getColor(context!!, R.color.colorGain)
                loadingTextView.text = resources.getString(R.string.DAPP_transaction_succeeded)
            } else {
                loadingAnimationView.setAnimation(R.raw.task_failed)
                loadingAnimationView.playAnimation()
                loadingTextView.textColor = ContextCompat.getColor(context!!, R.color.colorLoss)
                loadingTextView.text = resources.getString(R.string.DAPP_transaction_failed)

            }
            Handler().postDelayed ({
                dismiss()
            }, 2800)
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
                    if(title == null) {
                        openGraphTitleTextView.text = url!!
                        openGraphLogoView.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unknown_app)
                    } else {
                        openGraphTitleTextView.text = title
                        if (URLUtil.isNetworkUrl(image)) {
                            Glide.with(mView).load(image).into(openGraphLogoView)
                        } else {
                            Glide.with(mView).load(URL(url!!).protocol + "://" +  URL(url!!).authority + image).into(openGraphLogoView)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            onUiThread {
                openGraphLogoView.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unknown_app)
                openGraphTitleTextView.text = url!!
            }
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

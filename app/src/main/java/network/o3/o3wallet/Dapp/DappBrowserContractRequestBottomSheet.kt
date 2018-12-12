package network.o3.o3wallet.Dapp

import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.opengraph.OpenGraph
import java.lang.Exception
import java.math.BigDecimal

class DappBrowserContractRequestBottomSheet: RoundedBottomSheetDialogFragment() {

    lateinit var mView: View
    lateinit var dappMessage: DappMessage
    lateinit var invokeRequest: NeoDappProtocol.InvokeRequest

    lateinit var contactRow: ConstraintLayout
    lateinit var acceptApproveButton: Button

    lateinit var openGraphTitleTextView: TextView
    lateinit var openGraphLogoView: ImageView

    lateinit var invokeButton: Button
    lateinit var cancelButton: Button

    fun bindViews() {
        openGraphTitleTextView = mView.find(R.id.openGraphTitleView)
        openGraphLogoView = mView.find(R.id.openGraphLogoView)

        invokeButton = mView.find(R.id.acceptApproveButton)
        cancelButton = mView.find(R.id.rejectInvokeButton)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.dapp_contract_request_bottom_sheet, container, false)
        contactRow = mView.find(R.id.contactRow)
        contactRow.onClick {
            val intent = Intent(activity, ContractInfoActivity::class.java)
            intent.putExtra("contract", "33ee36c712b37df8acfbda4a1beb165e100ed3e0")
            startActivity(intent)
        }

        bindViews()
        setFeeControls()
        loadOpenGraphDetails()
        invokeButton.onClick {
            val success = (activity as DAppBrowserActivityV2).jsInterface.handleInvoke(dappMessage!!)
            dismiss()
        }

        cancelButton.onClick {
            dismiss()
        }


        return mView
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
                        Glide.with(mView).load(image).into(openGraphLogoView)
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
            fee = BigDecimal(invokeRequest.fee)
        } catch (e : Exception) { }
        if (fee != BigDecimal.ZERO) {
            checkBox.visibility = View.GONE
            mView.find<TextView>(R.id.dappFeeTitleTextView).text =
                    fee.toDouble().removeTrailingZeros()
        } else {
            checkBox.onClick {
                if (checkBox.isSelected) {
                    invokeRequest.fee = "0.0011"
                    dappMessage.data = invokeRequest
                } else {
                    invokeRequest.fee = ""
                    dappMessage.data = invokeRequest
                }
            }
        }
    }

    companion object {
        fun newInstance(): DappBrowserContractRequestBottomSheet {
           return DappBrowserContractRequestBottomSheet()
        }
    }
}
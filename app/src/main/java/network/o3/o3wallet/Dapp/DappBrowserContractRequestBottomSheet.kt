package network.o3.o3wallet.Dapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.opengraph.OpenGraph
import java.math.BigDecimal
import java.net.URL

class DappBrowserContractRequestBottomSheet: RoundedBottomSheetDialogFragment() {

    lateinit var mView: View
    lateinit var dappMessage: DappMessage
    lateinit var invokeRequest: NeoDappProtocol.InvokeRequest

    lateinit var detailsRow: ConstraintLayout
    lateinit var acceptApproveButton: Button

    lateinit var openGraphTitleTextView: TextView
    lateinit var openGraphLogoView: ImageView

    lateinit var invokeButton: Button
    lateinit var cancelButton: Button

    lateinit var contractOperationTextView: TextView
    lateinit var withWalletTextView: TextView

    fun bindViews() {
        openGraphTitleTextView = mView.find(R.id.openGraphTitleView)
        openGraphLogoView = mView.find(R.id.openGraphLogoView)
        contractOperationTextView = mView.find(R.id.contractOperationTextView)
        withWalletTextView = mView.find(R.id.withWalletTextView)


        invokeButton = mView.find(R.id.acceptApproveButton)
        cancelButton = mView.find(R.id.rejectInvokeButton)
    }

    fun getContractDetails() {
        bg {
            NeoNodeRPC(PersistentStore.getNodeURL()).getContractState(invokeRequest.scriptHash) {
                if (it.first != null) {
                    val contract = it.first!!
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.dapp_contract_request_bottom_sheet, container, false)
        invokeRequest = Gson().fromJson(Gson().toJson(dappMessage.data))
        getContractDetails()
        detailsRow = mView.find(R.id.invokeTxDetailsRow)
        detailsRow.onClick {
            val intent = Intent(activity, ContractInfoActivity::class.java)
            intent.putExtra("contract", invokeRequest.scriptHash)
            startActivity(intent)
        }

        bindViews()
        setFeeControls()
        loadOpenGraphDetails()
        contractOperationTextView.text = resources.getString(R.string.DAPP_invoke_request, invokeRequest.operation)
        withWalletTextView.text = (activity as DAppBrowserActivityV2).jsInterface.getDappExposedWallet()!!.address

        invokeButton.onClick {
            val success = (activity as DAppBrowserActivityV2).jsInterface.handleInvoke(dappMessage!!)
            dismiss()
        }

        cancelButton.onClick {
            (activity as DAppBrowserActivityV2).jsInterface.rejectedInvoke(dappMessage!!)
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
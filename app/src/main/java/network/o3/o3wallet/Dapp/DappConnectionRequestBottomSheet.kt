package network.o3.o3wallet.Dapp


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.Image
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import neoutils.Neoutils
import network.o3.o3wallet.Account
import network.o3.o3wallet.MultiWallet.ManageMultiWallet.ManageWalletsBottomSheet
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.opengraph.OpenGraph
import java.lang.Exception


class DappConnectionRequestBottomSheet : RoundedBottomSheetDialogFragment() {
    lateinit var mView: View
    lateinit var logoView: ImageView
    lateinit var titleView: TextView

    lateinit var addressTextView: TextView
    lateinit var addressNameTextView: TextView

    lateinit var swapWalletContainer: ConstraintLayout

    lateinit var acceptConnectionButton: Button
    lateinit var rejectConnectionButton: Button

    var dappMessage: DappMessage? = null

    val needReloadExposedDappWallet = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            reloadDappWallet()
        }
    }

    fun registerReceivers() {
        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadExposedDappWallet,
                IntentFilter("update-exposed-dapp-wallet"))
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this.context!!).unregisterReceiver(needReloadExposedDappWallet)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mView = inflater.inflate(R.layout.dapp_connection_request_bottom_sheet, container, false)
        logoView = mView.find(R.id.openGraphLogoView)
        titleView = mView.find(R.id.openGraphTitleView)
        addressTextView = mView.find(R.id.walletAddressTextView)
        addressNameTextView = mView.find(R.id.walletNameTextView)
        swapWalletContainer = mView.find(R.id.swapWalletContainer)

        if (NEP6.getFromFileSystem().getWalletAccounts().count() > 1) {
            swapWalletContainer.onClick {
                val swapWalletSheet = DappWalletForSessionBottomSheet.newInstance()
                swapWalletSheet.show(activity!!.supportFragmentManager, swapWalletSheet.tag)
            }
        } else {
            mView.find<ImageView>(R.id.moreWalletsArrow).visibility = View.INVISIBLE
        }



        registerReceivers()
        loadOpenGraphDetails()
        setAccountDetails()
        setupConnectionResultButtons()
        return mView
    }

    fun setupConnectionResultButtons() {
        acceptConnectionButton = mView.find(R.id.acceptConnectionButton)
        rejectConnectionButton = mView.find(R.id.rejectConnectionButton)
        acceptConnectionButton.onClick {
            if (NEP6.getFromFileSystem().accounts.isEmpty()) {
                (activity as DAppBrowserActivityV2).jsInterface.setDappExposedWallet(Account.getWallet(), "My O3 Wallet")
            } else {
                (activity as DAppBrowserActivityV2).jsInterface.setDappExposedWallet(Account.getWallet(), NEP6.getFromFileSystem().getDefaultAccount().label)
            }
            (activity as DAppBrowserActivityV2).jsInterface.authorizedAccountCredentials(dappMessage!!)
            dismiss()
        }

        rejectConnectionButton.onClick {
            (activity as DAppBrowserActivityV2).jsInterface.rejectedAccountCredentials(dappMessage!!)
            dismiss()
        }
    }

    fun reloadDappWallet() {
        setAccountDetails()
    }

    fun setAccountDetails() {
        val wallet = (activity as DAppBrowserActivityV2).jsInterface.getDappExposedWallet() ?: Account.getWallet()
        var label = ""
        if ((activity as DAppBrowserActivityV2).jsInterface.getDappExposedWalletName() == "") {
            label = NEP6.getFromFileSystem().accounts.find { it.isDefault }?.label ?: "My O3 Wallet"
        }

        addressTextView.text = wallet.address
        addressNameTextView.text = label
    }

    fun loadOpenGraphDetails() {
        val url = arguments!!.getString("url")
        try {
            bg {
                val dapp = OpenGraph(url, true)
                val title = dapp.getContent("title")
                val image = dapp.getContent("image")

                onUiThread {
                    if (title == null) {
                        titleView.text = url!!
                        logoView.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unknown_app)
                    } else {
                        titleView.text = title
                        Glide.with(mView).load(image).into(logoView)
                    }
                }
            }

        } catch (e: Exception) {
            titleView.text = url!!
            logoView.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unknown_app)
        }
    }

    companion object {
        fun newInstance(): DappConnectionRequestBottomSheet {
            return DappConnectionRequestBottomSheet()
        }
    }
}

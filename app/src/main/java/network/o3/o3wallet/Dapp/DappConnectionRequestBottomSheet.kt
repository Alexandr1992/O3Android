package network.o3.o3wallet.Dapp


import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import network.o3.o3wallet.Account
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.opengraph.OpenGraph
import java.net.URL


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

    lateinit var dappViewModel: DAPPViewModel

    var didAccept = false


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


        dappViewModel = (activity as DappContainerActivity).dappViewModel
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
            (activity as DappContainerActivity).dappViewModel.handleWalletInfo(dappMessage!!, true)
            dismiss()
        }

        rejectConnectionButton.onClick {
            //in case they swapped but did not confirm reset to default
            dappViewModel.dappExposedWallet = Account.getWallet()
            dappViewModel.dappExposedWalletName = NEP6.getFromFileSystem().getDefaultAccount().label
            didAccept = false
            (activity as DappContainerActivity).dappViewModel.handleWalletInfo(dappMessage!!, false)
            dismiss()
        }
    }

    fun reloadDappWallet() {
        setAccountDetails()
    }

    fun setAccountDetails() {
        val wallet = if (dappViewModel.dappExposedWallet == null) {
            Account.getWallet()
        } else {
            dappViewModel.dappExposedWallet
        }

        val walletName = if (dappViewModel.dappExposedWallet == null) {
            NEP6.getFromFileSystem().getDefaultAccount().label
        } else {
            dappViewModel.dappExposedWalletName
        }

        addressTextView.text = wallet!!.address
        addressNameTextView.text = walletName
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
                        titleView.text = url!!
                        logoView.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unknown_app)
                    } else {
                        titleView.text = title
                        if (URLUtil.isNetworkUrl(image)) {
                            Glide.with(mView).load(image).into(logoView)
                        } else {
                            Glide.with(mView).load(URL(url!!).protocol + "://" +  URL(url!!).authority + image).into(logoView)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            titleView.text = url!!
            logoView.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unknown_app)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (didAccept == false) {
            dappViewModel.dappExposedWallet = Account.getWallet()
            dappViewModel.dappExposedWalletName = NEP6.getFromFileSystem().getDefaultAccount().label
        }
        super.onDismiss(dialog)
    }

    companion object {
        fun newInstance(): DappConnectionRequestBottomSheet {
            return DappConnectionRequestBottomSheet()
        }
    }
}

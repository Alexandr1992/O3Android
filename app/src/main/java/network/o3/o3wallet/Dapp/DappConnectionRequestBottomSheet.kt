package network.o3.o3wallet.Dapp


import android.content.DialogInterface
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import neoutils.Wallet
import network.o3.o3wallet.*
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.json.JSONObject
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
    lateinit var connectionViewModel: ConnectionViewModel

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
                swapWalletSheet.connectionViewModel = connectionViewModel
                swapWalletSheet.show(activity!!.supportFragmentManager, swapWalletSheet.tag)
            }
        } else {
            mView.find<ImageView>(R.id.moreWalletsArrow).visibility = View.INVISIBLE
        }


        dappViewModel = (activity as DappContainerActivity).dappViewModel
        connectionViewModel = ConnectionViewModel()
        loadOpenGraphDetails()
        setupConnectionResultButtons()
        listenForWalletChanges()

        connectionViewModel.setWalletDetails(dappViewModel.walletForSession, dappViewModel.walletForSessionName)
        return mView
    }



    fun setupConnectionResultButtons() {
        acceptConnectionButton = mView.find(R.id.acceptConnectionButton)
        rejectConnectionButton = mView.find(R.id.rejectConnectionButton)
        acceptConnectionButton.onClick {
            dappViewModel.walletForSession = connectionViewModel.getWalletDetails().value!!.first
            dappViewModel.walletForSessionName = connectionViewModel.getWalletDetails().value!!.second
            dappViewModel.handleWalletInfo(dappMessage!!, true)
            val attrs = mapOf("blockchain" to "NEO",
                    "net" to PersistentStore.getNetworkType(),
                    "url" to arguments!!.getString("url"),
                    "domain" to URL(arguments!!.getString("url")).authority)
            AnalyticsService.DAPI.logAccountConnected(JSONObject(attrs))
            dismiss()
        }

        rejectConnectionButton.onClick {
            (activity as DappContainerActivity).dappViewModel.handleWalletInfo(dappMessage!!, false)
            dismiss()
        }
    }

    fun listenForWalletChanges() {
        connectionViewModel.getWalletDetails().observe(this, Observer<Pair<Wallet,String>> { walletDetails ->
            addressTextView.text = walletDetails.first.address
            addressNameTextView.text = walletDetails.second
        })
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

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        (activity as DappContainerActivity).dappViewModel.handleWalletInfo(dappMessage!!, false)
    }


    class ConnectionViewModel(): ViewModel() {
        var walletDetails = MutableLiveData<Pair<Wallet, String>>()

        fun getWalletDetails(): LiveData<Pair<Wallet, String>> {
            return walletDetails
        }

        fun setWalletDetails(wallet: Wallet, walletName: String) {
            walletDetails.postValue(Pair(wallet, walletName))
        }
    }

    companion object {
        fun newInstance(): DappConnectionRequestBottomSheet {
            return DappConnectionRequestBottomSheet()
        }
    }
}

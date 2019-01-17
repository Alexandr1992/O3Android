package network.o3.o3wallet.Settings

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.annotation.SuppressLint
import android.content.*
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.commit451.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import com.commit451.modalbottomsheetdialogfragment.Option
import com.google.gson.Gson
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.Dapp.DAppBrowserActivityV2
import network.o3.o3wallet.Identity.NNSBottomSheet
import network.o3.o3wallet.Portfolio.PortfolioHeader
import network.o3.o3wallet.Wallet.toast
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.json.JSONStringer


class SettingsFragment : Fragment(), ModalBottomSheetDialogFragment.Listener {

    lateinit var headerView: View

    val needReloadAddressReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setAddressInfo()
        }
    }

    fun setAddressInfo() {
        val addressLabel = headerView.findViewById<TextView>(R.id.addressLabel)
        val qrImageView = headerView.findViewById<ImageView>(R.id.addressQRCodeImageView)
        val wallet = Account.getWallet()
        addressLabel?.text = wallet.address

        val bitmap = QRCode.from(wallet.address).withSize(1000, 1000).bitmap()
        qrImageView?.setImageBitmap(bitmap)

        headerView.find<ImageButton>(R.id.shareButton)?.setOnClickListener{
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, Account.getWallet().address)
            shareIntent.type = "text/plain"
            startActivity(shareIntent)
        }

        headerView.setOnClickListener{
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(resources.getString(R.string.WALLET_copied_address), Account.getWallet().address)
            clipboard.primaryClip = clip
            context?.toast(resources.getString(R.string.WALLET_copied_address))
        }

        val nnsTextView = headerView.findViewById<TextView>(R.id.myAddressNNSTextView)
        nnsTextView.text = ""
        O3PlatformClient().nnsReverseLookup(Account.getWallet().address) {
            onUiThread {
                if (it.first != null && it.first!!.isNotEmpty()) {
                    nnsTextView?.visibility = View.VISIBLE
                    val domainsList = it.first!!
                    if (domainsList.count() >= 2) {
                        nnsTextView?.text =
                                String.format(context!!.getString(R.string.SETTINGS_nns_names), domainsList[0].domain, (domainsList.count() - 1).toString())
                        nnsTextView?.onClick {
                            val nnsModal = NNSBottomSheet.newInstance()
                            val args = Bundle()
                            args.putString("domains", Gson().toJson(domainsList))
                            nnsModal.arguments = args
                            nnsModal.show(activity!!.supportFragmentManager, nnsModal.tag)
                        }
                    } else {
                        nnsTextView?.text = it.first!![0].domain
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.settings_fragment_menu, container, false)
        headerView = layoutInflater.inflate(R.layout.settings_header, null)

        val listView = view.findViewById<ListView>(R.id.settingsListView)
        listView.addHeaderView(headerView)

        val basicAdapter = SettingsAdapter(this.context!!, this)
        listView.adapter = basicAdapter

        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadAddressReciever,
                IntentFilter("need-update-watch-address-event"))

        setAddressInfo()
        return view
    }

    override fun onModalOptionSelected(tag: String?, option: Option) {
        if (option.id == R.id.buy_with_crypto) {
            val intent = Intent(this.context, DAppBrowserActivityV2::class.java)
            intent.putExtra("url", "https://o3.network/swap")
            startActivity(intent)
        } else {
            val intent = Intent(this.context, DAppBrowserActivityV2::class.java)
            intent.putExtra("url", "https://buy.o3.network/?c=NEO")
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this.context!!)
                .unregisterReceiver(needReloadAddressReciever)
        super.onDestroy()
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
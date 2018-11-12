package network.o3.o3wallet.Settings

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.google.gson.Gson
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.Account
import network.o3.o3wallet.Identity.NNSBottomSheet
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.Wallet.toast
import network.o3.o3wallet.setNoDoubleClickListener
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.json.JSONStringer


class SettingsFragment : Fragment() {
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            // Credentials entered successfully!
            if (resultCode == -1) {
                val privateKeyModal = PrivateKeyFragment.newInstance()
                val bundle = Bundle()
                bundle.putString("key", Account.getWallet().wif)
                privateKeyModal.arguments = bundle
                privateKeyModal.show((context as AppCompatActivity).supportFragmentManager, privateKeyModal.tag)
            } else {

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.settings_fragment_menu, container, false)
        val headerView = layoutInflater.inflate(R.layout.settings_header, null)

        val listView = view.findViewById<ListView>(R.id.settingsListView)
        listView.addHeaderView(headerView)

        val basicAdapter = SettingsAdapter(this.context!!, this)
        listView.adapter = basicAdapter

        val addressLabel = headerView.findViewById<TextView>(R.id.addressLabel)
        val qrImageView = headerView.findViewById<ImageView>(R.id.addressQRCodeImageView)

        val wallet = Account.getWallet()
        addressLabel.text = wallet.address

        val bitmap = QRCode.from(wallet.address).withSize(1000, 1000).bitmap()
        qrImageView.setImageBitmap(bitmap)

        headerView.find<ImageButton>(R.id.shareButton).setOnClickListener{
           val shareIntent = Intent()
           shareIntent.action = Intent.ACTION_SEND
           shareIntent.putExtra(Intent.EXTRA_STREAM, Account.getWallet().address)
           shareIntent.type = "text/plain"
           startActivity(shareIntent)
        }

        qrImageView.setOnClickListener{
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(resources.getString(R.string.WALLET_copied_address), Account.getWallet().address)
            clipboard.primaryClip = clip
            context?.toast(resources.getString(R.string.WALLET_copied_address))
        }


        O3PlatformClient().nnsReverseLookup(Account.getWallet().address) {
            onUiThread {
                if (it.first != null && it.first!!.isNotEmpty()) {
                    view.findViewById<TextView>(R.id.myAddressNNSTextView).visibility = View.VISIBLE
                    val domainsList = it.first!!
                    if (domainsList.count() >= 2) {
                        view.findViewById<TextView>(R.id.myAddressNNSTextView).text =
                                String.format(context!!.getString(R.string.SETTINGS_nns_names), domainsList[0].domain, (domainsList.count() - 1).toString())
                        view.findViewById<TextView>(R.id.myAddressNNSTextView).onClick {
                            val nnsModal = NNSBottomSheet.newInstance()
                            val args = Bundle()
                            args.putString("domains", Gson().toJson(domainsList))
                            nnsModal.arguments = args
                            nnsModal.show(activity!!.supportFragmentManager, nnsModal.tag)
                        }
                    } else {
                        view.findViewById<TextView>(R.id.myAddressNNSTextView).text = it.first!![0].domain
                    }
                }
            }
        }
        return view
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
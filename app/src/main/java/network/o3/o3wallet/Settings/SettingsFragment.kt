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
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.Account
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.Wallet.toast
import org.jetbrains.anko.find


class SettingsFragment : Fragment() {
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            // Credentials entered successfully!
            if (resultCode == -1) {
                val privateKeyModal = PrivateKeyFragment.newInstance()
                privateKeyModal.show((context as AppCompatActivity).supportFragmentManager, privateKeyModal.tag)
            } else {

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.settings_fragment_menu, container, false)
        val headerView = layoutInflater.inflate(R.layout.settings_header_row, null)
        headerView.findViewById<TextView>(R.id.headerTextView).text = resources.getString(R.string.SETTINGS_settings_title)

        val listView = view.findViewById<ListView>(R.id.settingsListView)
        listView.addHeaderView(headerView)

        val basicAdapter = SettingsAdapter(this.context!!, this)
        listView.adapter = basicAdapter

        val addressLabel = view.findViewById<TextView>(R.id.addressLabel)
        val qrImageView = view.findViewById<ImageView>(R.id.addressQRCodeImageView)

        val wallet = Account.getWallet()
        addressLabel.text = wallet.address

        val bitmap = QRCode.from(wallet.address).withSize(1000, 1000).bitmap()
        qrImageView.setImageBitmap(bitmap)

        view.find<ImageButton>(R.id.shareButton).setOnClickListener{
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, Account.getWallet().address)
            shareIntent.type = "text/plain"
            startActivity(shareIntent)
        }

        qrImageView.setOnClickListener{
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(resources.getString(R.string.WALLET_copied_address),Account.getWallet().address)
            clipboard.primaryClip = clip
            context?.toast(resources.getString(R.string.WALLET_copied_address))
        }

        return view
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
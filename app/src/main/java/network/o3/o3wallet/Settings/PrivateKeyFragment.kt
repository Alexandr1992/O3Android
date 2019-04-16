package network.o3.o3wallet.Settings


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.R
import network.o3.o3wallet.Wallet.toast
import org.jetbrains.anko.support.v4.onUiThread


class PrivateKeyFragment : BottomSheetDialogFragment() {

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.settings_fragment_private_key, null)
        dialog.setContentView(contentView)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val key= arguments!!.get("key") as String
        val view = inflater.inflate(R.layout.settings_fragment_private_key, container, false)
        view.findViewById<TextView>(R.id.privateKeyTextView).text = key
        val bitmap = QRCode.from(key).withSize(2000, 2000).bitmap()
        view.findViewById<ImageView>(R.id.qrView).setImageBitmap(bitmap)

        view.findViewById<TextView>(R.id.copyKeyToClipboardTextView).setOnClickListener {
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(resources.getString(R.string.SETTINGS_copied_key),key)
            clipboard.primaryClip = clip
            onUiThread {
                O3Wallet.appContext?.toast(resources.getString(R.string.SETTINGS_copied_key))
            }
        }

        return view
    }

    companion object {
        fun newInstance(): PrivateKeyFragment {
            return PrivateKeyFragment()
        }
    }
}

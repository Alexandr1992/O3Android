package network.o3.o3wallet.Onboarding.CreateKey

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.onboarding_private_key_card_fragment.*
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.Account
import network.o3.o3wallet.Onboarding.CreateKey.Backup.BackupOptionsFragment
import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.SettingsFragment
import org.jetbrains.anko.find


class PrivateKeyCardFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val wif = (activity as CreateNewWalletActivity).wif

        val view = inflater.inflate(R.layout.onboarding_private_key_card_fragment, container, false)
        val wifTextView = view.find<TextView>(R.id.wifTextView)
        wifTextView.text = wif

        val wifQRView = view.find<ImageView>(R.id.wifQRView)
        val bitmap = QRCode.from(wif).withSize(2000, 2000).bitmap()
        wifQRView.setImageBitmap(bitmap)

        val backupButton = view.find<Button>(R.id.backupButton).setOnClickListener {
            val backupFragment = BackupOptionsFragment.newInstance()
            backupFragment.show((context as AppCompatActivity).supportFragmentManager, "backup")
        }

        return view
    }

    companion object {
        fun newInstance(): PrivateKeyCardFragment {
           return PrivateKeyCardFragment()
        }
    }
}

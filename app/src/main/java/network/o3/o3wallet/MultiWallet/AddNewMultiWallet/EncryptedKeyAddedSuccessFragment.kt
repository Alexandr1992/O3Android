package network.o3.o3wallet.MultiWallet.AddNewMultiWallet

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

class EncryptedKeyAddedSuccessFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.multiwallet_watch_address_added, container, false)
        view.find<Button>(R.id.doneButton).onClick {
            activity?.finish()
        }

        view.find<TextView>(R.id.successMessage).text = resources.getString(R.string.MULTIWALLET_wallet_added)

        return view
    }
}
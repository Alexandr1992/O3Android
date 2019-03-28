package network.o3.o3wallet.MultiWallet.AddNewMultiWallet


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.amplitude.api.Amplitude
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.json.JSONObject

class WatchAddressAddedSuccess : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val attrs = mapOf(
                "type" to "watch_address",
                "method" to "import",
                "address_count" to NEP6.getFromFileSystem().accounts.size)
        Amplitude.getInstance().logEvent("ADD_WALLET", JSONObject(attrs))

        val view = inflater.inflate(R.layout.multiwallet_watch_address_added, container, false)
        view.find<Button>(R.id.doneButton).onClick {
            activity?.finish()
        }
        return view
    }
}

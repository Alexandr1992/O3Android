package network.o3.o3wallet.MultiWallet.AddNewMultiWallet


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import network.o3.o3wallet.AnalyticsService
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import org.json.JSONObject

class EnterMultiwalletWatchAddress : Fragment() {

    lateinit var mView: View
    lateinit var nameEditText: EditText
    lateinit var continueButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.multiwallet_enter_watch_address, container, false)
        nameEditText = mView.find(R.id.walletNameEditText)
        continueButton = mView.find(R.id.continueButton)

        initiateEditText()
        initiateContinueButton()
        return mView
    }

    fun initiateEditText() {
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) { continueButton.isEnabled = (p0?.length != 0) }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun initiateContinueButton() {
        val vm = (activity as AddNewMultiwalletRootActivity).viewModel
        continueButton.onClick {
            val newNep6 = NEP6.getFromFileSystem()
            if (newNep6.accounts.find { it.label == nameEditText.text.toString()} != null) {
                alert(resources.getString(R.string.MUTLWALLET_duplicate_name_error)) {
                    yesButton {}
                }.show()
                return@onClick
            }

            vm.nickname = nameEditText.text.toString()
            newNep6.addWatchAddress(vm.address, vm.nickname)
            newNep6.writeToFileSystem()
            val walledAddAttrs = mapOf(
                    "total_num_watch_addresses" to newNep6.getReadOnlyAccounts().count())
            AnalyticsService.Wallet.logWatchAddressAdded(JSONObject(walledAddAttrs))
            mView.findNavController().navigate(R.id.action_enterMultiwalletWatchAddress_to_watchAddressAddedSuccess)
        }
    }
}

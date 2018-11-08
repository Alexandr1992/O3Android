package network.o3.o3wallet.MultiWallet.AddNewMultiWallet


import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.multiwallet_enter_watch_address.*
import network.o3.o3wallet.NEP6

import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk15.coroutines.onClick

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
            vm.nickname = nameEditText.text.toString()
            val newNep6 = NEP6.getFromFileSystem()
            newNep6.addWatchAddress(vm.address, vm.nickname)
            newNep6.writeToFileSystem()
            mView.findNavController().navigate(R.id.action_enterMultiwalletWatchAddress_to_watchAddressAddedSuccess)
        }
    }
}

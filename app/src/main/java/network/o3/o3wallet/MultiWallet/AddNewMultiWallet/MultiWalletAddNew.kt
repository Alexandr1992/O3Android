package network.o3.o3wallet.MultiWallet.AddNewMultiWallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import network.o3.o3wallet.toHex
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import java.security.SecureRandom

class MultiWalletAddNew : Fragment() {
    lateinit var mView: View
    lateinit var walletEntryEditText: EditText
    lateinit var continueButton: Button


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.multiwallet_add_new, container, false)
        walletEntryEditText = mView.find(R.id.walletEntryEditText)
        continueButton = mView.find(R.id.continueButton)

        initiateQrScanner()
        initiateWalletEntryEditText()
        initateContinueButton()
        initiateGenerateNewButton()

        findNavController().addOnDestinationChangedListener  { controller, destination, args ->
            if (destination.id != R.id.multiWalletAddNew) {
                activity?.find<ImageButton>(R.id.rightNavButton)?.visibility = View.GONE
            }
        }

        return mView
    }

    fun validateAddress(): Boolean {
        val text = walletEntryEditText.text.toString()
        return Neoutils.validateNEOAddress(text)
    }

    fun validateEncryptedKey(): Boolean {
        val text = walletEntryEditText.text.toString()
        if (text.commonPrefixWith("6P") == "6P" && text.length == 58) {
            return true
        }
        return false
    }

    fun validateWif(): Boolean {
        val text = walletEntryEditText.text.toString()
        try {
            Neoutils.generateFromWIF(text)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun initiateWalletEntryEditText() {
        walletEntryEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) { continueButton.isEnabled = (p0?.length != 0) }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun initiateQrScanner() {
        val scanButton = activity?.find<ImageButton>(R.id.rightNavButton)
        scanButton?.setOnClickListener {
            val integrator = IntentIntegrator(activity)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }
    }

    fun initiateGenerateNewButton() {
        val generateButton = mView.find<Button>(R.id.generateNewWalletButton)
        val vm = (activity as AddNewMultiwalletRootActivity).viewModel
        generateButton.onClick {
            val random = SecureRandom()
            var bytes = ByteArray(32)
            random.nextBytes(bytes)
            val hex = bytes.toHex()
            val wallet = Neoutils.generateFromPrivateKey(hex)
            vm.address = wallet.address
            vm.wif = wallet.wif
            mView.findNavController().navigate(R.id.action_multiWalletAddNew_to_enterMultiwalletEncryptPrivateKey)
        }
    }

    fun initateContinueButton() {
        continueButton.isEnabled = false
        val vm = (activity as AddNewMultiwalletRootActivity).viewModel
        val scanButton = activity!!.find<ImageButton>(R.id.rightNavButton)
        continueButton.setOnClickListener {
            if (validateAddress()) {
                if (NEP6.getFromFileSystem().accounts.find { it.address == walletEntryEditText.text.toString() } != null) {
                    alert(resources.getString(R.string.MUTLIWALLET_duplicate_address_error)) {
                        yesButton {}
                    }.show()
                    return@setOnClickListener
                }

                vm.address = walletEntryEditText.text.toString()
                scanButton.visibility = View.GONE
                mView.findNavController().navigate(R.id.action_multiWalletAddNew_to_enterMultiwalletWatchAddress)
            } else if (validateEncryptedKey()) {
                if (NEP6.getFromFileSystem().accounts.find { it.key == walletEntryEditText.text.toString() } != null) {
                    alert(resources.getString(R.string.MUTLIWALLET_duplicate_key_error)) {
                        yesButton {}
                    }.show()
                    return@setOnClickListener
                }

                scanButton.visibility = View.GONE
                vm.encryptedKey = walletEntryEditText.text.toString()
                mView.findNavController().navigate(R.id.action_multiWalletAddNew_to_addMultiwalletVerifyNEP2)
            } else if (validateWif()) {
                if (NEP6.getFromFileSystem().accounts.find { it.address == Neoutils.generateFromWIF(walletEntryEditText.text.toString()).address } != null) {
                    alert(resources.getString(R.string.MUTLIWALLET_duplicate_key_error)) {
                        yesButton {}
                    }.show()
                    return@setOnClickListener
                }

                scanButton.visibility = View.GONE
                vm.wif = walletEntryEditText.text.toString()
                mView.findNavController().navigate(R.id.action_multiWalletAddNew_to_enterMultiwalletEncryptPrivateKey)
            } else {
                alert(resources.getString(R.string.MULTIWALLET_invalid_wallet_error)) {
                    yesButton {}
                }.show()
            }
        }
    }
}

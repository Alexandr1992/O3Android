package network.o3.o3wallet.MultiWallet.AddNewMultiWallet

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.navigation.findNavController
import com.amplitude.api.Amplitude
import neoutils.Neoutils
import network.o3.o3wallet.NEP6

import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import org.json.JSONObject
import java.lang.Exception

class EnterMultiwalletEncryptPrivateKey : Fragment() {

    lateinit var mView: View
    lateinit var nameField: EditText
    lateinit var passwordField: EditText
    lateinit var confirmField: EditText

    lateinit var passwordHideImageView: ImageView
    lateinit var confirmHideImageView: ImageView

    var passwordIsHidden = true
    var confirmPasswordIsHidden = true


    lateinit var continueButton: Button
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.multiwallet_encrypt_new_private_key, container, false)
        nameField = mView.find(R.id.walletNameField)
        passwordField = mView.find(R.id.enterPasswordField)
        confirmField = mView.find(R.id.confirmPasswordField)

        passwordHideImageView = mView.find(R.id.passwordHideImageView)
        confirmHideImageView = mView.find(R.id.confirmHideImageView)

        initiateContinueButton()
        initiatePasswordFields()
        return mView
    }

    fun validatePassword(): Boolean {
        if (passwordField.text.length < 8) {
            alert (resources.getString(R.string.MULTIWALLET_passwords_do_not_match)) {
                yesButton {  }
            }.show()
            return false
        } else if (passwordField.text.toString() != confirmField.text.toString()){
            alert (resources.getString(R.string.MULTIWALLET_password_to_short)) {
                yesButton {  }
            }.show()
            return false
        }
        (activity as AddNewMultiwalletRootActivity).viewModel.password = passwordField.text.toString()
        return true
    }

    fun initiateContinueButton() {
        continueButton = mView.find(R.id.continueButton)
        continueButton.isEnabled = false
        continueButton.setOnClickListener {
            val vm = (activity as AddNewMultiwalletRootActivity).viewModel
            if (validatePassword()) {
                try {
                    val account = Neoutils.neP2Encrypt(vm.wif, vm.password)
                    val nep6 = NEP6.getFromFileSystem()
                    if (nep6.accounts.find { it.label == nameField.text.toString()} != null) {
                        alert(resources.getString(R.string.MUTLWALLET_duplicate_name_error)) {
                            yesButton {}
                        }.show()
                        return@setOnClickListener
                    }

                    vm.encryptedKey = account.encryptedKey
                    vm.address = account.address
                    nep6.addEncryptedKey(vm.address, nameField.text.toString(), account.encryptedKey)
                    nep6.writeToFileSystem()
                    val walledAddAttrs = mapOf(
                            "total_num_wallets" to nep6.getWalletAccounts().count())
                    Amplitude.getInstance().logEvent("wallet_added", JSONObject(walledAddAttrs))
                    mView.findNavController().navigate(R.id.action_enterMultiwalletEncryptPrivateKey_to_keyEncryptionSuccess)
                } catch (e: Exception) {
                    //idk
                }
            }
        }
    }

    fun initiatePasswordFields() {
        passwordField.transformationMethod = PasswordTransformationMethod()
        passwordHideImageView.setOnClickListener {
            passwordIsHidden = !passwordIsHidden
            if (passwordIsHidden) {
                passwordField.transformationMethod = PasswordTransformationMethod()
                passwordHideImageView.alpha = 0.3f
            } else {
                passwordField.transformationMethod = null
                passwordHideImageView.alpha = 1.0f
            }
            passwordField.setSelection(passwordField.text?.length ?: 0)
        }

        confirmField.transformationMethod = PasswordTransformationMethod()
        confirmHideImageView.setOnClickListener {
            confirmPasswordIsHidden = !confirmPasswordIsHidden
            if (confirmPasswordIsHidden) {
                confirmField.transformationMethod = PasswordTransformationMethod()
                confirmHideImageView.alpha = 0.3f
            } else {
                confirmField.transformationMethod = null
                confirmHideImageView.alpha = 1.0f
            }
            confirmField.setSelection(confirmField.text?.length ?: 0)
        }

        passwordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (passwordField.text.toString() != "" && confirmField.text.toString() != "") {
                    continueButton.isEnabled = true
                } else {
                    continueButton.isEnabled = false
                }
            }
        })

        confirmField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (passwordField.text.toString() != "" && confirmField.text.toString() != "") {
                    continueButton.isEnabled = true
                } else {
                    continueButton.isEnabled = false
                }
            }
        })
    }
}

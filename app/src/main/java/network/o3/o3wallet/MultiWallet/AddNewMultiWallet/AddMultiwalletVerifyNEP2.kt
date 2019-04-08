package network.o3.o3wallet.MultiWallet.AddNewMultiWallet


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputLayout
import neoutils.Neoutils
import network.o3.o3wallet.AnalyticsService
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import org.json.JSONObject

class AddMultiwalletVerifyNEP2 : Fragment() {

    lateinit var mView: View
    lateinit var passwordField: EditText
    lateinit var passwordContainer: TextInputLayout
    lateinit var passwordHideImageView: ImageView
    lateinit var continueButton: Button
    lateinit var nameEditText: EditText
    lateinit var quickSwapSwitch: Switch

    var passwordIsHidden = true
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.multiwallet_verify_nep2, container, false)
        passwordField = mView.find(R.id.passwordEditText)
        passwordContainer = mView.find(R.id.passwordEditTextContainer)
        nameEditText = mView.find(R.id.walletNameField)
        passwordHideImageView = mView.find(R.id.passwordHideImageView)
        quickSwapSwitch = mView.find(R.id.quickSwapSwitch)

        quickSwapSwitch.isChecked = true
        continueButton = mView.find(R.id.continueButton)
        initiatePasswordFields()
        initiateContinueButton()
        return mView
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

        passwordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s!!.isBlank()) {
                    passwordContainer.error = null
                } else if (s!!.length < 8){
                    passwordContainer.error = resources.getString(R.string.ONBOARDING_password_length_minimum)
                } else {
                    passwordContainer.error = null
                }

                continueButton.isEnabled = (s?.length != 0 && nameEditText.text.toString() != "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) { continueButton.isEnabled = (p0?.length != 0 && passwordField.text.toString() != "") }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun initiateContinueButton() {
        continueButton.isEnabled = false
        continueButton.onClick {
            val vm = (activity as AddNewMultiwalletRootActivity).viewModel
            try {
                val newNep6 = NEP6.getFromFileSystem()
                if (newNep6.accounts.find { it.label == nameEditText.text.toString()} != null) {
                    alert(resources.getString(R.string.MUTLWALLET_duplicate_name_error)) {
                        yesButton {}
                    }.show()
                    return@onClick
                }

                val wif = Neoutils.neP2Decrypt(vm.encryptedKey, passwordField.text.toString())
                vm.address = Neoutils.generateFromWIF(wif).address
                vm.nickname = nameEditText.text.toString()
                newNep6.addEncryptedKey(vm.address, vm.nickname, vm.encryptedKey)
                newNep6.writeToFileSystem()
                if (quickSwapSwitch.isChecked) {
                    PersistentStore.setHasQuickSwapEnabled(true, vm.address, passwordField.text.toString())
                }

                val attrs = mapOf(
                        "type" to "import_key",
                        "method" to "import",
                        "address_count" to NEP6.getFromFileSystem().accounts.size)
                AnalyticsService.Wallet.logWalletAdded(JSONObject(attrs))
                mView.findNavController().navigate(R.id.action_addMultiwalletVerifyNEP2_to_encryptedKeyAddedSuccessFragment)
            } catch (e: Exception) {
                alert(resources.getString(R.string.MULTIWALLET_cannot_decrypt)) {
                    yesButton {  }
                }.show()
            }
        }
    }
}

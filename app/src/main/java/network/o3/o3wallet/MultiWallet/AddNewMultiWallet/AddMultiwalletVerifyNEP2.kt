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
import kotlinx.android.synthetic.main.multiwallet_add_new.*
import neoutils.Neoutils

import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import java.lang.Exception

class AddMultiwalletVerifyNEP2 : Fragment() {

    lateinit var mView: View
    lateinit var passwordField: EditText
    lateinit var passwordHideImageView: ImageView
    lateinit var continueButton: Button

    var passwordIsHidden = true
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.multiwallet_verify_nep2, container, false)
        passwordField = mView.find(R.id.passwordEditText)
        passwordHideImageView = mView.find(R.id.passwordHideImageView)

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
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (passwordField.text.toString() != "" ) {
                    continueButton.isEnabled = true
                } else {
                    continueButton.isEnabled = false
                }
            }
        })
    }

    fun initiateContinueButton() {
        continueButton.isEnabled = false
        continueButton.onClick {
            val vm = (activity as AddNewMultiwalletRootActivity).viewModel
            try {
                val wif = Neoutils.neP2Decrypt(vm.encryptedKey, passwordField.text.toString())
                vm.address = Neoutils.generateFromWIF(wif).address
                mView.findNavController().navigate(R.id.action_addMultiwalletVerifyNEP2_to_enterEncryptedKeyNameFragment)
            } catch (e: Exception) {
                alert(resources.getString(R.string.MULTIWALLET_cannot_decrypt)) {
                    yesButton {  }
                }.show()
            }
        }
    }
}

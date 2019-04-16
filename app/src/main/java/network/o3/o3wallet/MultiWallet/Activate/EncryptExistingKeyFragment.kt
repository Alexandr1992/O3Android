package network.o3.o3wallet.MultiWallet.Activate


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
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton

class EncryptExistingKeyFragment : Fragment() {

    lateinit var mView: View
    lateinit var enterPasswordEditText: EditText
    lateinit var confirmPasswordEditText: EditText
    lateinit var enterPasswordContainer: TextInputLayout
    lateinit var confirmPasswordContainer: TextInputLayout

    lateinit var showEnterImageView: ImageView
    lateinit var showConfirmPasswordImageView: ImageView
    lateinit var encryptButton: Button
    lateinit var quickSwap: Switch

    var passwordIsHidden = true
    var confirmPasswordIsHidden = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.multiwallet_encrypt_existing_key, container, false)
        enterPasswordEditText = mView.find(R.id.enterEncryptionPasswordEditText)
        confirmPasswordEditText = mView.find(R.id.confirmEncryptionPasswordEditText)
        enterPasswordContainer = mView.find(R.id.enterEncryptionPasswordEditTextContainer)
        confirmPasswordContainer = mView.find(R.id.confirmEncryptionPasswordEditTextContainer)

        showEnterImageView = mView.find(R.id.showEnterImageView)
        showConfirmPasswordImageView = mView.find(R.id.showConfirmImageView)

        encryptButton = mView.find(R.id.encryptExistingKeyButton)
        quickSwap = mView.find(R.id.quickSwapSwitch)
        quickSwap.isChecked = true

        initiatePasswordListeners()
        initiateEncryptionButton()
        return mView
    }

    fun validatePassword(): Boolean {
        if (enterPasswordEditText.text.length < 8) {
            alert (resources.getString(R.string.MULTIWALLET_password_to_short)) {
                yesButton {  }
            }.show()
            return false
        } else if (enterPasswordEditText.text.toString() != confirmPasswordEditText.text.toString()){
            alert (resources.getString(R.string.MULTIWALLET_passwords_do_not_match)) {
                yesButton {  }
            }.show()
            return false
        }
        return true
    }

    fun initiateEncryptionButton() {
        encryptButton.isEnabled = false
        encryptButton.setOnClickListener {
            if (validatePassword()) {
                (activity as MultiwalletActivateActivity).viewModel.encryptKey(enterPasswordEditText.text.toString(), quickSwap.isChecked)
                mView.findNavController().navigate(R.id.action_encryptExistingFragment_to_encryptionSuccessFragment)
            }
        }
    }

    fun initiatePasswordListeners() {
        enterPasswordEditText.transformationMethod = PasswordTransformationMethod()
        showEnterImageView.setOnClickListener {
            passwordIsHidden = !passwordIsHidden
            if (passwordIsHidden) {
                enterPasswordEditText.transformationMethod = PasswordTransformationMethod()
                showEnterImageView.alpha = 0.3f
            } else {
                enterPasswordEditText.transformationMethod = null
                showEnterImageView.alpha = 1.0f
            }
            enterPasswordEditText.setSelection(enterPasswordEditText.text?.length ?: 0)
        }

        confirmPasswordEditText.transformationMethod = PasswordTransformationMethod()
        showConfirmPasswordImageView.setOnClickListener {
            confirmPasswordIsHidden = !confirmPasswordIsHidden
            if (confirmPasswordIsHidden) {
                confirmPasswordEditText.transformationMethod = PasswordTransformationMethod()
                showConfirmPasswordImageView.alpha = 0.3f
            } else {
                confirmPasswordEditText.transformationMethod = null
                showConfirmPasswordImageView.alpha = 1.0f
            }
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text?.length ?: 0)
        }

        enterPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = enterPasswordEditText.text.toString()
                if (password.isBlank()) {
                    enterPasswordContainer.error = null
                } else if (password.length < 8){
                    enterPasswordContainer.error = resources.getString(R.string.ONBOARDING_password_length_minimum)
                } else {
                    enterPasswordContainer.error = null
                }

                if (enterPasswordEditText.text.toString() != "" && confirmPasswordEditText.text.toString() != "") {
                    encryptButton.isEnabled = true
                } else {
                    encryptButton.isEnabled = false
                }
            }
        })

        confirmPasswordEditText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = enterPasswordEditText.text.toString()
                val confirmPassword = confirmPasswordEditText.text.toString()
                if (password.length > 0 && password != confirmPassword) {
                    confirmPasswordContainer.error =
                            resources.getString(R.string.ONBOARDING_password_no_match)
                } else {
                    confirmPasswordContainer.error = null
                }
                if (enterPasswordEditText.text.toString() != "" && confirmPasswordEditText.text.toString() != "") {
                    encryptButton.isEnabled = true
                } else {
                    encryptButton.isEnabled = false
                }
            }
        })
    }
}

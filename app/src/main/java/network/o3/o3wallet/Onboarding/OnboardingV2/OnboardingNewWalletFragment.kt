package network.o3.o3wallet.Onboarding.OnboardingV2

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
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
import androidx.navigation.fragment.findNavController
import com.amplitude.api.Amplitude
import kotlinx.android.synthetic.main.multiwallet_unlock_watch_address.*
import neoutils.Neoutils
import network.o3.o3wallet.Account
import network.o3.o3wallet.MultiWallet.AddNewMultiWallet.AddNewMultiwalletRootActivity
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.Onboarding.SelectingBestNode
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.yesButton
import org.json.JSONObject
import java.lang.Exception


class OnboardingNewWalletFragment : Fragment() {
    lateinit var mView: View
    lateinit var continueButton: Button
    lateinit var passwordField: EditText
    lateinit var passwordHideImageView: ImageView
    lateinit var confirmPasswordField: EditText
    lateinit var confirmPasswordHideImageView: ImageView

    var passwordIsHidden = true
    var confirmPasswordIsHidden = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mView = inflater.inflate(R.layout.onboarding_new_wallet, container, false)
        continueButton = mView.find(R.id.continueButton)
        passwordField = mView.find(R.id.enterEncryptionPasswordEditText)
        confirmPasswordField = mView.find(R.id.confirmEncryptionPasswordEditText)
        passwordHideImageView = mView.find(R.id.showEnterImageView)
        confirmPasswordHideImageView = mView.find(R.id.showConfirmImageView)

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
        } else if (passwordField.text.toString() != confirmPasswordField.text.toString()){
            alert (resources.getString(R.string.MULTIWALLET_password_to_short)) {
                yesButton {  }
            }.show()
            return false
        }
        return true
    }

    fun initiateContinueButton() {
        continueButton = mView.find(R.id.continueButton)
        continueButton.isEnabled = false
        continueButton.setOnClickListener {
            if (validatePassword()) {
                try {
                    generateAndEncryptWallet()
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

        confirmPasswordField.transformationMethod = PasswordTransformationMethod()
        confirmPasswordHideImageView.setOnClickListener {
            confirmPasswordIsHidden = !confirmPasswordIsHidden
            if (confirmPasswordIsHidden) {
                confirmPasswordField.transformationMethod = PasswordTransformationMethod()
                confirmPasswordHideImageView.alpha = 0.3f
            } else {
                confirmPasswordField.transformationMethod = null
                confirmPasswordHideImageView.alpha = 1.0f
            }
            confirmPasswordField.setSelection(confirmPasswordField.text?.length ?: 0)
        }

        passwordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (passwordField.text.toString() != "" && confirmPasswordField.text.toString() != "") {
                    continueButton.isEnabled = true
                } else {
                    continueButton.isEnabled = false
                }
            }
        })

        confirmPasswordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (passwordField.text.toString() != "" && confirmPasswordField.text.toString() != "") {
                    continueButton.isEnabled = true
                } else {
                    continueButton.isEnabled = false
                }
            }
        })
    }

    fun generateNewWallet() {
        NEP6.removeFromDevice()
        var wallet = Neoutils.newWallet()
        PersistentStore.clearPersistentStore()
        val nep2 = Neoutils.neP2Encrypt(wallet.wif, passwordField.text.toString().trim())
        val nep6 = NEP6.getFromFileSystem()

        nep6.addEncryptedKey(nep2.address, "My O3 Wallet", nep2.encryptedKey)
        nep6.writeToFileSystem()
        nep6.makeNewDefault(nep2.address, passwordField.text.toString().trim())
        Account.deleteKeyFromDevice()



        val intent = Intent(activity, SelectingBestNode::class.java)
        startActivity(intent)
    }

    fun generateAndEncryptWallet() {
        val mKeyguardManager =  activity!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            toast(resources.getString(R.string.ALERT_no_passcode_setup))
            return
        } else {
            if (NEP6.getFromFileSystem().accounts.isNotEmpty()) {
                alert(resources.getString(R.string.ONBOARDING_remove_wallet)) {
                    yesButton { generateNewWallet() }
                    noButton {}
                }.show()
            } else {
                generateNewWallet()
            }
        }
    }
}
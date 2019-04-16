package network.o3.o3wallet.MultiWallet.ManageMultiWallet


import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton

class UnlockWatchAddressFragment : Fragment() {

    lateinit var mView: View
    lateinit var wifField: EditText
    lateinit var enterPasswordField: EditText
    lateinit var confirmPasswordField: EditText
    lateinit var continueButton: Button
    lateinit var passwordHideImageView: ImageView
    lateinit var confirmHideImageView: ImageView

    var address = ""

    var enterPasswordIsHidden = true
    var confirmPasswordIsHidden = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView =  inflater.inflate(R.layout.multiwallet_unlock_watch_address, container, false)
        wifField = mView.find(R.id.wipTextView)
        enterPasswordField = mView.find(R.id.enterPasswordField)
        confirmPasswordField = mView.find(R.id.confirmPasswordField)
        continueButton = mView.find(R.id.continueButton)
        passwordHideImageView = mView.find(R.id.passwordHideImageView)
        confirmHideImageView = mView.find(R.id.confirmHideImageView)
        continueButton.isEnabled = false

        address = (activity as MultiwalletManageWallet).viewModel.address

        initiateTextChangeListeners()
        initiatePasswordHideButtons()
        initiateContinueButton()
        initiateScanner()
        return mView
    }

    fun initiateScanner() {
        activity?.find<ImageButton>(R.id.rightNavButton)?.visibility = View.VISIBLE
        activity?.find<ImageButton>(R.id.rightNavButton)?.image =
                ContextCompat.getDrawable(context!!, R.drawable.ic_scan)

        activity?.find<ImageButton>(R.id.rightNavButton)?.setOnClickListener {
            val integrator = IntentIntegrator(activity)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }
    }

    fun initiateContinueButton() {
        continueButton.setOnClickListener {
            var keyText = wifField.text.toString()
            var keyIsEncrypted = false

            if (keyText.length == 58 && keyText.substring(0, 2) == "6P") {
                keyIsEncrypted = true
            } else try {
                val wallet = Neoutils.generateFromWIF(keyText)
                keyIsEncrypted = false
            } catch (e: Exception) {
                alert(resources.getString(R.string.MUTLIWALLET_invalid_key)) {
                    yesButton { }
                }.show()
                return@setOnClickListener
            }

            if (enterPasswordField.text.toString().count() < 8) {
                alert(resources.getString(R.string.MULTIWALLET_invalid_password_length)) {
                    yesButton { }
                }.show()
                return@setOnClickListener
            }

            if (enterPasswordField.text.toString() != confirmPasswordField.text.toString()) {
                alert(resources.getString(R.string.MULTIWALLET_passwords_do_not_match)) {
                    yesButton { }
                }.show()
                return@setOnClickListener
            }

            if (keyIsEncrypted) {
                try {
                    val decrypted = Neoutils.neP2Decrypt(keyText, enterPasswordField.text.toString())
                    NEP6.unlockWatchAddressInFileSystem(address, keyText)
                    (activity as MultiwalletManageWallet).viewModel.key = keyText
                    NEP6.unlockWatchAddressInFileSystem(address, keyText)
                    val intent = Intent("need-update-watch-address-event")
                    LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
                    mView.findNavController().navigate(R.id.action_unlockWatchAddressFragment_to_unlockKeySuccessFragment)
                } catch (e: Exception) {
                    alert(resources.getString(R.string.MULTIWALLET_cannot_decrypt)) {
                        yesButton { }
                    }.show()
                    return@setOnClickListener
                }
            } else try {
                val encryptedKey = Neoutils.neP2Encrypt(keyText, enterPasswordField.text.toString())
                (activity as MultiwalletManageWallet).viewModel.key = encryptedKey.encryptedKey
                NEP6.unlockWatchAddressInFileSystem(address, encryptedKey.encryptedKey)
                val intent = Intent("need-update-watch-address-event")
                LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
                mView.findNavController().navigate(R.id.action_unlockWatchAddressFragment_to_unlockKeySuccessFragment)
            } catch (e: Exception) {
                alert("Unknown error") {
                    yesButton { }
                }.show()
            }
        }
    }

    fun allFieldsCompleted(): Boolean {
        return enterPasswordField.text.toString() != "" &&
                confirmPasswordField.text.toString() != "" && wifField.text.toString() != ""
    }

    fun initiatePasswordHideButtons() {
        passwordHideImageView.setOnClickListener {
            enterPasswordIsHidden = !enterPasswordIsHidden
            if (enterPasswordIsHidden) {
                enterPasswordField.transformationMethod = PasswordTransformationMethod()
                passwordHideImageView.alpha = 0.3f
            } else {
                enterPasswordField.transformationMethod = null
                passwordHideImageView.alpha = 1.0f
            }
            enterPasswordField.setSelection(enterPasswordField.text?.length ?: 0)
        }

        confirmHideImageView.setOnClickListener {
            confirmPasswordIsHidden = !confirmPasswordIsHidden
            if (confirmPasswordIsHidden) {
                confirmPasswordField.transformationMethod = PasswordTransformationMethod()
                confirmHideImageView.alpha = 0.3f
            } else {
                confirmPasswordField.transformationMethod = null
                confirmHideImageView.alpha = 1.0f
            }
            confirmPasswordField.setSelection(confirmPasswordField.text?.length ?: 0)
        }
    }

    fun initiateTextChangeListeners() {
        wifField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                continueButton.isEnabled = allFieldsCompleted()
            }
        })

        enterPasswordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                continueButton.isEnabled = allFieldsCompleted()
            }
        })

        confirmPasswordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                continueButton.isEnabled = allFieldsCompleted()
            }
        })
    }
}

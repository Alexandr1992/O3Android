package network.o3.o3wallet.Onboarding.OnboardingV2


import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.Account
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onUiThread

class RestoreExistingWalletFragment : Fragment() {
    lateinit var mView: View
    lateinit var scanButton: ImageView

    lateinit var enterKeyContainer: TextInputLayout
    lateinit var enterKeyEditText: EditText

    lateinit var enterPasswordContainer: TextInputLayout
    lateinit var enterPasswordEditText: EditText
    lateinit var enterPasswordImageView: ImageView

    lateinit var confirmPasswordContainer: TextInputLayout
    lateinit var confirmPasswordEditText: EditText
    lateinit var confirmPasswordImageView: ImageView

    lateinit var continueButton: Button

    var passwordIsHidden = true
    var confirmPasswordIsHidden = true

    enum class KeyType {
        INVALID,
        WIF,
        ENCRYPTED
    }

    var keyType = KeyType.INVALID

    fun bindViews() {
        scanButton = mView.find(R.id.scanButton)
        enterKeyEditText = mView.find(R.id.enterKeyEditText)
        enterPasswordEditText = mView.find(R.id.enterEncryptionPasswordEditText)
        confirmPasswordEditText = mView.find(R.id.confirmEncryptionPasswordEditText)
        continueButton = mView.find(R.id.continueButton)
        enterPasswordImageView = mView.find(R.id.showEnterImageView)
        confirmPasswordImageView = mView.find(R.id.showConfirmImageView)

        enterPasswordContainer = mView.find(R.id.enterEncryptionPasswordEditTextContainer)
        confirmPasswordContainer = mView.find(R.id.confirmEncryptionPasswordEditTextContainer)
        enterKeyContainer = mView.find(R.id.enterKeyContainer)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.onboarding_restore_existing_wallet_fragment, container, false)

        bindViews()
        initiateScanner()
        initiateKeyListener()
        initiatePasswordListener()
        initiateConfirmListener()
        initiateContinueButton()
        initiatePasswordFields()
        return mView
    }

    fun checkEnableContinueButton() {
        if (keyType == KeyType.INVALID) {
            continueButton.isEnabled = false
        } else if (keyType == KeyType.WIF) {
            if (confirmPasswordEditText.text.isNotBlank() && enterPasswordEditText.text.isNotBlank()) {
                continueButton.isEnabled = true
            }
        } else if (keyType == KeyType.ENCRYPTED) {
            if (enterPasswordEditText.text.isNotBlank()) {
                continueButton.isEnabled = true
            }
        } else {
            continueButton.isEnabled = false
        }
    }

    fun initiateKeyListener() {
        enterKeyEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                onUiThread {
                    val key = enterKeyEditText.text.toString()
                    try {
                        val wallet = Neoutils.generateFromWIF(key)
                        enterPasswordContainer.visibility = View.VISIBLE
                        confirmPasswordContainer.visibility = View.VISIBLE
                        confirmPasswordImageView.visibility = View.VISIBLE
                        enterPasswordImageView.visibility = View.VISIBLE
                        setTransparencyConfirmPasswordEye()
                        setTransparencyPasswordEye()
                        enterKeyContainer.error = null
                        keyType = KeyType.WIF
                    } catch (e: Exception) {
                        if (key.length == 58 && key.substring(0, 2) == "6P") {
                            enterPasswordContainer.visibility = View.VISIBLE
                            enterPasswordImageView.visibility = View.VISIBLE
                            confirmPasswordContainer.visibility = View.GONE
                            confirmPasswordImageView.visibility = View.INVISIBLE
                            setTransparencyConfirmPasswordEye()
                            setTransparencyPasswordEye()
                            confirmPasswordEditText.text = SpannableStringBuilder("")
                            confirmPasswordContainer.error = null
                            enterKeyContainer.error = null
                            keyType = KeyType.ENCRYPTED
                        } else {
                            enterPasswordContainer.visibility = View.GONE
                            confirmPasswordContainer.visibility = View.GONE
                            confirmPasswordImageView.visibility = View.INVISIBLE
                            enterPasswordImageView.visibility = View.INVISIBLE
                            setTransparencyConfirmPasswordEye()
                            setTransparencyPasswordEye()
                            confirmPasswordEditText.text = SpannableStringBuilder("")
                            enterPasswordEditText.text = SpannableStringBuilder("")
                            confirmPasswordContainer.error = null
                            enterPasswordContainer.error = null
                            if (key == "") {
                                enterKeyContainer.error = null
                            } else {
                                enterKeyContainer.error = resources.getString(R.string.ONBOARDING_invalid_key)
                            }
                            keyType = KeyType.INVALID
                        }
                    }
                    checkEnableContinueButton()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }

    fun initiateConfirmListener() {
        confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                onUiThread {
                    val password = enterPasswordEditText.text.toString()
                    val confirmPassword = confirmPasswordEditText.text.toString()
                    if (password.length > 0 && password != confirmPassword) {
                        confirmPasswordContainer.error =
                                resources.getString(R.string.ONBOARDING_password_no_match)
                    } else {
                        confirmPasswordContainer.error = null
                    }
                    checkEnableContinueButton()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }

    fun initiatePasswordListener() {
        enterPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                onUiThread {
                    val password = enterPasswordEditText.text.toString()
                    if (password.isBlank()) {
                        enterPasswordContainer.error = null
                    } else if (password.length < 8 && keyType == KeyType.WIF){
                        enterPasswordContainer.error = resources.getString(R.string.ONBOARDING_password_length_minimum)
                    } else {
                        enterPasswordContainer.error = null
                    }
                    checkEnableContinueButton()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }


    fun validatePassword(): Boolean {
        val password = enterPasswordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        if (password.length < 8) {
            enterPasswordContainer.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
            return false
        } else if (password != confirmPassword && keyType == KeyType.WIF){
            confirmPasswordContainer.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
            return false
        }
        return true
    }

    fun initiateScanner() {
        scanButton.onClick {
            val integrator = IntentIntegrator(activity)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            integrator.setPrompt(resources.getString(R.string.ONBOARDING_scan_prompt))
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }
    }

    fun initiateContinueButton() {
        continueButton = mView.find(R.id.continueButton)
        continueButton.isEnabled = false
        continueButton.setOnClickListener {
            if (validatePassword()) {
                val key = enterKeyEditText.text.toString()
                val password = enterPasswordEditText.text.toString()
                if (keyType == KeyType.WIF) {
                    val nep2 = Neoutils.neP2Encrypt(key, password)
                    val nep6 = NEP6.getFromFileSystem()
                    nep6.addEncryptedKey(nep2.address, "My O3 Wallet", nep2.encryptedKey)
                    nep6.writeToFileSystem()
                    nep6.makeNewDefault(nep2.address, password)
                    Account.deleteKeyFromDevice()
                    PersistentStore.setHasDismissedBackup(true)
                    findNavController().navigate(R.id.action_restoreExistingWalletFragment_to_onboardingSuccessFragment)
                } else if (keyType == KeyType.ENCRYPTED) {
                    try {
                        val wif = Neoutils.neP2Decrypt(key, password)
                        val nep2 = Neoutils.neP2Encrypt(wif, password)
                        val nep6 = NEP6.getFromFileSystem()
                        nep6.addEncryptedKey(nep2.address, "My O3 Wallet", nep2.encryptedKey)
                        nep6.writeToFileSystem()
                        nep6.makeNewDefault(nep2.address, password)
                        Account.deleteKeyFromDevice()
                        PersistentStore.setHasDismissedBackup(true)
                        findNavController().navigate(R.id.action_restoreExistingWalletFragment_to_onboardingSuccessFragment)
                    } catch (e: Exception) {
                        alert {
                            resources.getString(R.string.MULTIWALLET_cannot_decrypt)
                        }.show()
                    }
                }
            }
        }
    }

    fun setTransparencyPasswordEye() {
        if (passwordIsHidden) {
            enterPasswordEditText.transformationMethod = PasswordTransformationMethod()
            enterPasswordImageView.alpha = 0.3f
        } else {
            enterPasswordEditText.transformationMethod = null
            enterPasswordImageView.alpha = 1.0f
        }
    }

    fun setTransparencyConfirmPasswordEye() {
        if (confirmPasswordIsHidden) {
            confirmPasswordEditText.transformationMethod = PasswordTransformationMethod()
            confirmPasswordImageView.alpha = 0.3f
        } else {
            confirmPasswordEditText.transformationMethod = null
            confirmPasswordImageView.alpha = 1.0f
        }
    }

    fun initiatePasswordFields() {
        enterPasswordEditText.transformationMethod = PasswordTransformationMethod()
        enterPasswordImageView.setOnClickListener {
            passwordIsHidden = !passwordIsHidden
            setTransparencyPasswordEye()
            enterPasswordEditText.setSelection(enterPasswordEditText.text?.length ?: 0)
        }

        confirmPasswordEditText.transformationMethod = PasswordTransformationMethod()
        confirmPasswordImageView.setOnClickListener {
            confirmPasswordIsHidden = !confirmPasswordIsHidden
            setTransparencyConfirmPasswordEye()
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text?.length ?: 0)
        }
    }
}

package network.o3.o3wallet.MultiWallet.ManageMultiWallet

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import neoutils.Neoutils
import network.o3.o3wallet.Account
import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.PrivateKeyFragment
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import java.lang.Exception


class DialogUnlockEncryptedKey : DialogFragment() {
    var encryptedKey: String = ""
    var decryptedKey: String = ""
    // Non loading Views
    lateinit var animationView: LottieAnimationView
    lateinit var nep2Title: TextView
    lateinit var nep2Subtitle: TextView
    lateinit var nep2PasswordField: EditText
    lateinit var submitButton: Button

    //Loading Views
    lateinit var decryptionProgress: ProgressBar
    lateinit var decryptionTitle: TextView

    var decryptionFailedCallback: (() -> Unit)? = null
    var decryptionSucceededCallback: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.dialog_nep2_fragment, container, false)
        animationView = view.find(R.id.nep2AnimationView)
        nep2Title = view.findViewById(R.id.nep2Title)
        nep2Subtitle = view.findViewById(R.id.nep2Subtitle)
        nep2PasswordField = view.findViewById(R.id.nep2PasswordField)
        submitButton = view.findViewById(R.id.decryptButton)

        decryptionProgress = view.findViewById(R.id.decryptionProgress)
        decryptionTitle = view.findViewById(R.id.decryptionProgressTitle)

        animationView.repeatCount = LottieDrawable.INFINITE
        animationView.playAnimation()

        nep2PasswordField = view.find<EditText>(R.id.nep2PasswordField)
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        submitButton.setOnClickListener {
            attemptDecryption()
        }

        nep2PasswordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                submitButton.isEnabled = (p0?.length != 0)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        return view
    }



    fun attemptDecryption() {
        onUiThread {
            showDecryptionProgress()
            Handler().postDelayed ({
            try {
                Log.w("EKEY: ", encryptedKey)
                decryptedKey = Neoutils.neP2Decrypt(encryptedKey, nep2PasswordField.text.trim().toString())
                if (Account.fromWIF(decryptedKey)) {
                    decryptionSuccessful(decryptedKey)
                } else {
                    decryptionFailed()
                }
            } catch (e: Exception) {
                decryptionFailed()
                }
            }, 3000)
        }
    }

    fun decryptionFailed() {
        showPasswordEntry()
        toast(R.string.ONBOARDING_decryption_failed)
        decryptionFailedCallback?.invoke()
    }

    fun decryptionSuccessful(decryptedKey: String) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(this.nep2PasswordField.getWindowToken(), 0)
        this.dismiss()
        decryptionSucceededCallback?.invoke()
    }

    fun showDecryptionProgress() {
        onUiThread {
            nep2Title.visibility = View.INVISIBLE
            nep2Subtitle.visibility = View.INVISIBLE
            nep2PasswordField.visibility = View.INVISIBLE
            submitButton.visibility = View.INVISIBLE
            animationView.visibility = View.INVISIBLE

            decryptionProgress.visibility = View.VISIBLE
            decryptionTitle.visibility = View.VISIBLE
        }
    }

    fun showPasswordEntry() {
        onUiThread {
            nep2Title.visibility = View.VISIBLE
            nep2Subtitle.visibility = View.VISIBLE
            nep2PasswordField.visibility = View.VISIBLE
            submitButton.visibility = View.VISIBLE
            animationView.visibility = View.VISIBLE

            decryptionProgress.visibility = View.INVISIBLE
            decryptionTitle.visibility = View.INVISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog_Light)
    }

    companion object {
        fun newInstance(): DialogUnlockEncryptedKey {
            return DialogUnlockEncryptedKey()
        }
    }
}
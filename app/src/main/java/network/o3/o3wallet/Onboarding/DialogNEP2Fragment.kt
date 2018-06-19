package network.o3.o3wallet.Onboarding


import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import org.jetbrains.anko.find
import android.content.Intent
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import neoutils.Neoutils
import network.o3.o3wallet.Account
import network.o3.o3wallet.R
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.toast
import java.lang.Exception


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class DialogNEP2Fragment() : DialogFragment() {
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
            Handler().postDelayed(Runnable {
                try {
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
        context?.toast(R.string.ONBOARDING_decryption_failed)
    }

    fun decryptionSuccessful(decryptedKey: String) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.getWindowToken(), 0)
        activity?.supportFragmentManager?.popBackStack()
        val intent = Intent(activity, SelectingBestNode::class.java)
        (activity as LoginActivity).startActivity(intent)
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


    companion object {
        fun newInstance(): DialogNEP2Fragment {
            return DialogNEP2Fragment()
        }
    }
}

package network.o3.o3wallet.MultiWallet

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert


class DialogInputEntryFragment : DialogFragment() {
    // Non loading Views
    lateinit var animationView: LottieAnimationView
    lateinit var inputEntryTitle: TextView
    lateinit var inputEntrySubtitle: TextView
    lateinit var inputField: EditText
    lateinit var submitButton: Button

    var submittedInput: ((String) -> Unit)? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.dialog_single_input, container, false)
        animationView = view.find(R.id.inputAnimationView)
        inputField = view.findViewById(R.id.inputField)
        submitButton = view.findViewById(R.id.submitButton)


        animationView.repeatCount = LottieDrawable.INFINITE
        animationView.playAnimation()


        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        submitButton.setOnClickListener {
            for (account in NEP6.getFromFileSystem().accounts) {
                if (account.label == inputField.text.toString()) {
                    alert(resources.getString(R.string.MULTIWALLET_cannot_create_duplicate)).show()
                    return@setOnClickListener
                }
            }
            this.submittedInput!!(inputField.text.toString())
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
            dismiss()
        }

        inputField.addTextChangedListener(object : TextWatcher {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog_Light)
    }

    companion object {
        fun newInstance(): DialogInputEntryFragment {
            return DialogInputEntryFragment()
        }
    }
}

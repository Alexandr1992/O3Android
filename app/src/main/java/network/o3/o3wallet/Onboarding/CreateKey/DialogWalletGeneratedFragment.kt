package network.o3.o3wallet.Onboarding.CreateKey

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.airbnb.lottie.LottieAnimationView
import network.o3.o3wallet.R
import org.jetbrains.anko.find


class DialogWalletGeneratedFragment() : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.dialog_animated_fragment, container, false)
        view.find<Button>(R.id.animatedDialogConfirmButton).setOnClickListener {
            dismiss()
        }

        view.find<LottieAnimationView>(R.id.animatedDialogAnimationView).playAnimation()



        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog)
    }

    companion object {
        fun newInstance(): DialogWalletGeneratedFragment {
            return DialogWalletGeneratedFragment()
        }
    }
}

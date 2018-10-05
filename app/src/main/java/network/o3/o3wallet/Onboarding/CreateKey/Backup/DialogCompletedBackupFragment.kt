package network.o3.o3wallet.Onboarding.CreateKey.Backup

import android.app.DialogFragment
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import network.o3.o3wallet.Account
import network.o3.o3wallet.R
import network.o3.o3wallet.Onboarding.SelectingBestNode
import org.jetbrains.anko.find


class DialogCompletedBackupFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val wif: String = arguments!!.get("wif") as String
        val title: String = arguments!!.get("title") as String
        val subtitle: String = arguments!!.get("subtitle") as String
        val buttonTitle: String = arguments!!.get("buttonTitle") as String

        val view = inflater.inflate(R.layout.dialog_animated_fragment, container, false)
        view.find<Button>(R.id.animatedDialogConfirmButton).setOnClickListener {
            Account.fromWIF(wif)
            dismiss()
            val intent = Intent(this.activity, SelectingBestNode::class.java)
            activity?.startActivity(intent)
        }
        view.find<TextView>(R.id.animatedDialogTitle).text = title
        view.find<TextView>(R.id.animatedDialogSubtitle).text = subtitle
        view.find<Button>(R.id.animatedDialogConfirmButton).text = buttonTitle
        view.find<LottieAnimationView>(R.id.animatedDialogAnimationView).playAnimation()

        return view
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(android.support.v4.app.DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog_Light)
    }

    companion object {
        fun newInstance(): DialogCompletedBackupFragment {
            return DialogCompletedBackupFragment()
        }
    }
}
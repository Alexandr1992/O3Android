package network.o3.o3wallet.Dapp


import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import org.jetbrains.anko.find
import org.jetbrains.anko.linkTextColor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.textColor


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class DappDisclaimerBottomSheet : RoundedBottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.dapp_disclaimer_bottom_sheet, container, false)
        mView.find<TextView>(R.id.warningDescriptionTextView).setMovementMethod(LinkMovementMethod.getInstance());
        mView.find<TextView>(R.id.warningDescriptionTextView).setText(Html.fromHtml(getResources().getString(R.string.DAPP_disclaimer_description)));
        mView.find<TextView>(R.id.warningDescriptionTextView).linkTextColor = ContextCompat.getColor(context!!, R.color.colorAccent)

        mView.find<CheckBox>(R.id.agreeCheckBox).textColor = ContextCompat.getColor(context!!, R.color.colorSubtitleGrey)
        mView.find<Button>(R.id.returnButton).textColor = ContextCompat.getColor(context!!, R.color.colorSubtitleGrey)
        mView.find<CheckBox>(R.id.agreeCheckBox).isChecked = true

        mView.find<Button>(R.id.returnButton).onClick {
            activity?.finish()
        }

        mView.find<Button>(R.id.agreeToWarning).onClick {
            if (mView.find<CheckBox>(R.id.agreeCheckBox).isChecked) {
                PersistentStore.setHasAgreedDappDisclaimer(true)
            }
            dismiss()
        }

        return mView
    }

    companion object {
        fun newInstance(): DappDisclaimerBottomSheet {
            return DappDisclaimerBottomSheet()
        }
    }


}

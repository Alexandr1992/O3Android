package network.o3.o3wallet


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
import org.jetbrains.anko.find
import org.jetbrains.anko.linkTextColor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.textColor


class AnalyticsDisclaimerBottomSheet : RoundedBottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.analytics_disclaimer_bottom_sheet_fragment, container, false)
        mView.find<TextView>(R.id.warningDescriptionTextView).setMovementMethod(LinkMovementMethod.getInstance());
        mView.find<TextView>(R.id.warningDescriptionTextView).setText(Html.fromHtml(getResources().getString(R.string.ANALYTICS_disclaimer_description)));
        mView.find<TextView>(R.id.warningDescriptionTextView).linkTextColor = ContextCompat.getColor(context!!, R.color.colorAccent)

        mView.find<CheckBox>(R.id.agreeCheckBox).textColor = ContextCompat.getColor(context!!, R.color.colorSubtitleGrey)
        mView.find<CheckBox>(R.id.agreeCheckBox).isChecked = true


        mView.find<Button>(R.id.agreeToWarning).onClick {
            if (mView.find<CheckBox>(R.id.agreeCheckBox).isChecked) {
                PersistentStore.setHasAgreedAnalyticsDisclaimer(true)
            }
            dismiss()
        }

        return mView
    }

    companion object {
        fun newInstance(): AnalyticsDisclaimerBottomSheet {
            return AnalyticsDisclaimerBottomSheet()
        }
    }


}

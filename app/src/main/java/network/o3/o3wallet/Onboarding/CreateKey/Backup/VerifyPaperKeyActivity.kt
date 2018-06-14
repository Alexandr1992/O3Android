package network.o3.o3wallet.Onboarding.CreateKey.Backup

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.onboarding_verify_paper_key_activity.*
import network.o3.o3wallet.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.yesButton

class VerifyPaperKeyActivity : AppCompatActivity() {
    lateinit var verifyButton: Button
    var wif = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_verify_paper_key_activity)
        intent.getStringExtra("wif")

        title = resources.getString(R.string.ONBOARDING_verify_private_key)

        verifyButton = find(R.id.verifyAndContinueButton)
        verifyButton.setOnClickListener { verifyTapped() }
    }

    fun verifyTapped() {
        if (editText.text.toString() == wif) {
            val paperDialog = DialogCompletedBackupFragment.newInstance()
            var args = Bundle()
            args.putString("wif", "")
            args.putString("title", resources.getString(R.string.ONBOARDING_paper_dialog_title))
            args.putString("subtitle", resources.getString(R.string.ONBOARDING_paper_dialog_subtitle))
            args.putString("buttonTitle", resources.getString(R.string.ONBOARDING_paper_dialog_button))
            paperDialog.arguments = args
            paperDialog.show(fragmentManager, paperDialog.tag)
        } else {
            alert (resources.getString(R.string.ONBOARDING_paper_key_no_match_description)) {
                yesButton { resources.getString(R.string.ALERT_OK_Confirm_Button) }
            }.show()
        }
    }
}

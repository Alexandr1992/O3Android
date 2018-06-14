package network.o3.o3wallet.Onboarding.CreateKey.Backup

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import network.o3.o3wallet.R

class VerifyPaperKeyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_verify_paper_key_activity)

        title = resources.getString(R.string.ONBOARDING_verify_private_key)
    }
}

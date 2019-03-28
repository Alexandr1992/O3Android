package network.o3.o3wallet.Onboarding.OnboardingV2

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.toast

class OnboardingRootActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_root_activity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            toast(resources.getString(R.string.ALERT_cancelled))
        } else if (result == null) {
            return
        } else {
            find<EditText>(R.id.enterKeyEditText).text = SpannableStringBuilder(result.contents)
        }
    }
}

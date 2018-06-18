package network.o3.o3wallet

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Button
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.onboarding_passcode_request_activity.*
import network.o3.o3wallet.Onboarding.LandingActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton

class PasscodeRequestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_passcode_request_activity)
        supportActionBar?.hide()

        find<Button>(R.id.logoutButton).setOnClickListener {
            logoutTapped()
        }

        find<Button>(R.id.signInButton).setOnClickListener {
            signinTapped()
        }
    }

    fun logoutTapped() {
        alert(O3Wallet.appContext!!.resources.getString(R.string.SETTINGS_logout_warning)) {
            yesButton {
                Account.deleteKeyFromDevice()
                finish()
            }
            noButton {

            }
        }.show()
    }

    fun signinTapped() {
        val mKeyguardManager =  getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.

            Toast.makeText(this,
                    R.string.ALERT_no_passcode_setup,
                    Toast.LENGTH_LONG).show()
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                val passcodeRequestIntent = Intent(this, PasscodeRequestActivity::class.java)
                startActivityForResult(intent, 1)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {

        } else {
            if (resultCode == -1) {
                Account.restoreWalletFromDevice()
                val intent = Intent(this, SelectingBestNode::class.java)
                startActivity(intent)
            }
        }
    }
}

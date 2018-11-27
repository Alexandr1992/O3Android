package network.o3.o3wallet.Onboarding
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import network.o3.o3wallet.*
import network.o3.o3wallet.Wallet.toast

class LoginActivity : AppCompatActivity() {
    private lateinit var wifTextfield: TextView
    var isFirstActivityLoad = true
    var mShowDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_activity_login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        wifTextfield = findViewById<TextView>(R.id.wipTextView)
        loginButton.setOnClickListener {
            login()
        }

        val scanButton = findViewById<Button>(R.id.loginScanButton)

        scanButton.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            integrator.setPrompt(resources.getString(R.string.ONBOARDING_scan_prompt))
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        wifTextfield.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                loginButton.isEnabled = (p0?.length != 0)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (Account.isEncryptedWalletPresent() && isFirstActivityLoad) {
            isFirstActivityLoad = false
            authenticateEncryptedWallet()
        }
    }

    fun authenticateEncryptedWallet() {
        val mKeyguardManager =  getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(this,
                    resources.getString(R.string.ALERT_no_passcode_setup),
                    Toast.LENGTH_LONG).show()
                return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                startActivityForResult(intent, 1)
            }
        }
    }

    /* Can insert these line for testing purposes if necessary
    * Account.fromWIF("L4Ns4Uh4WegsHxgDG49hohAYxuhj41hhxG6owjjTWg95GSrRRbLL") <- This is a testnet WIF
    * PersistentStore.addWatchAddress("ARHusFqxqX4vvkLMzjz2GgPbdeJuXyYrFb", "Test 1")
    * PersistentStore.addWatchAddress("AdrfqqSb9SkBucXu99ZGBtb6YAVu5bJzpu", "Test 2")
    */

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (mShowDialog) {
            mShowDialog = false
            enteredEncryptedKey(wifTextfield.text.trim().toString())
        }
    }

    fun login() {
        val keyText = wifTextfield.text.trim()
        if (keyText.count() == 0) {
            return
        }

        if (keyText.length == 58 && keyText.substring(0, 2) == "6P") {
            enteredEncryptedKey(wifTextfield.text.trim().toString())
            return
        }

        val valid = Account.fromWIF(wifTextfield.text.trim().toString())
        if (!valid) {
            baseContext.toast(resources.getString(R.string.ALERT_invalid_wif))
            return
        }

        val intent = Intent(this, SelectingBestNode::class.java)
        startActivity(intent)
    }

    fun enteredEncryptedKey(encryptedKey: String) {
        val neo2DialogFragment = DialogNEP2Fragment.newInstance()
        neo2DialogFragment.encryptedKey = encryptedKey
        neo2DialogFragment.showNow(this.supportFragmentManager, "backupkey")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
        } else if (result == null) {
            return
        } else {
            wifTextfield.text = result.contents
            if (result.contents.substring(0, 2) == "6P" &&  result.contents.length == 58) {
                mShowDialog = true
            } else {
                login()
            }
        }
    }
}
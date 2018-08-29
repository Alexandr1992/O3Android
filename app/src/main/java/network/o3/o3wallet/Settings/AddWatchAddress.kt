package network.o3.o3wallet.Settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.API.NEO.*
import network.o3.o3wallet.afterTextChanged
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.yesButton
import java.lang.Exception
import java.math.BigDecimal

class AddWatchAddress : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity_add_watch_address)

        val nickNameField = findViewById<EditText>(R.id.NickNameField)
        val addressField = findViewById<EditText>(R.id.AddressField)
        val saveButton = findViewById<Button>(R.id.AddButton)
        val scanAddressButton = findViewById<Button>(R.id.scanAddressButton)

        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        supportActionBar?.setCustomView(R.layout.actionbar_layout)
        find<TextView>(R.id.mytext).text = resources.getString(R.string.SETTINGS_watch_address)
        scanAddressButton.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            integrator.setPrompt(resources.getString(R.string.SETTINGS_scan_prompt_watch_address))
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }


        val pasteAddressButton = findViewById<Button>(R.id.pasteAddressButton)
        pasteAddressButton.setOnClickListener{
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null) {
                val item = clip.getItemAt(0)
                addressField.setText(item.text)
            }
        }
        addressField.afterTextChanged {
            saveButton.isEnabled = addressField.text.trim().toString().count() >0 && nickNameField.text.trim().toString().count() > 0
        }
        nickNameField.afterTextChanged {
            saveButton.isEnabled = addressField.text.trim().toString().count() >0 && nickNameField.text.trim().toString().count() > 0
        }
        saveButton.isEnabled = false
        saveButton.setOnClickListener {
            NeoNodeRPC(PersistentStore.getNodeURL()).validateAddress(addressField.text.trim().toString()) {
                if (it.second != null || it.first == false) {
                    runOnUiThread {
                        alert (resources.getString(R.string.ALERT_invalid_neo_address), resources.getString(R.string.ALERT_error)) {
                            yesButton {  }
                        }.show()
                    }
                } else {
                    PersistentStore.addWatchAddress(addressField.text.trim().toString(), nickNameField.text.trim().toString())
                    Answers().logCustom(CustomEvent("Watch Address Added")
                            .putCustomAttribute("Total Watch Addresses", PersistentStore.getWatchAddresses().count()))
                    finish()
                }
            }
        }
    }

    fun parseQRPayload(payload: String) {
        if (Neoutils.validateNEOAddress(payload)) {
            findViewById<EditText>(R.id.AddressField).setText(payload)
        } else {
            try {
                val uri = Neoutils.parseNEP9URI(payload)
                val toAddress = uri.to
                if (toAddress != "") {
                    findViewById<EditText>(R.id.AddressField).setText(toAddress)
                }
            } catch (e: Exception) {
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null ) {
            if (result.contents == null) {
                Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
            } else {
                parseQRPayload(result.contents)
            }
        }
    }
}

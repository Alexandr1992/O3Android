package network.o3.o3wallet.Settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.widget.Button
import android.widget.EditText
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import android.widget.Toast
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.afterTextChanged
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import java.lang.Exception

class AddContact : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity_add_contact)
        this.title = resources.getString(R.string.WALLET_address_book)
        val nickNameField = findViewById<EditText>(R.id.NickNameField)
        val addressField = findViewById<EditText>(R.id.AddressField)
        val saveButton = findViewById<Button>(R.id.AddButton)

        val scanAddressButton = findViewById<Button>(R.id.scanAddressButton)
        val pasteAddressButton = findViewById<Button>(R.id.pasteAddressButton)

        if (intent.extras != null) {
            val address = intent.getStringExtra("address")
            addressField.text = SpannableStringBuilder(address)
        }

        scanAddressButton.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            integrator.setPrompt(resources.getString(R.string.WALLET_scan))
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        pasteAddressButton.setOnClickListener{
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null) {
                val item = clip.getItemAt(0)
                addressField.setText(item.text)
            }
        }
        saveButton.isEnabled = false

        addressField.afterTextChanged {
            saveButton.isEnabled = addressField.text.trim().toString().count() >0 && nickNameField.text.trim().toString().count() > 0
        }
        nickNameField.afterTextChanged {
            saveButton.isEnabled = addressField.text.trim().toString().count() >0 && nickNameField.text.trim().toString().count() > 0
        }

        saveButton.setOnClickListener {


            NeoNodeRPC(PersistentStore.getNodeURL()).validateAddress(addressField.text.trim().toString()) {
                if (it.second != null || it.first == false) {
                    runOnUiThread {
                        alert (resources.getString(R.string.ALERT_invalid_neo_address), resources.getString(R.string.ALERT_error)) {
                            yesButton {  }
                        }.show()
                    }
                } else {
                    PersistentStore.addContact(addressField.text.trim().toString(), nickNameField.text.trim().toString())
                    Answers().logCustom(CustomEvent("Contact Added").
                            putCustomAttribute("Total Contacts", PersistentStore.getContacts().count()))
                    //RELOAD_DATA = 1
                    setResult(1)
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

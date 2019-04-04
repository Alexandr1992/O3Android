package network.o3.o3wallet.Settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.afterTextChanged
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.yesButton

class AddContact : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity_add_contact)
        val nickNameField = findViewById<EditText>(R.id.NickNameField)
        val addressField = findViewById<EditText>(R.id.AddressField)
        val saveButton = findViewById<Button>(R.id.AddButton)

        val scanAddressButton = findViewById<Button>(R.id.scanAddressButton)
        val pasteAddressButton = findViewById<Button>(R.id.pasteAddressButton)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.actionbar_layout)
        find<TextView>(R.id.mytext).text = resources.getString(R.string.WALLET_address_book)


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

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_Dark, true)
        } else {
            theme.applyStyle(R.style.AppTheme_White, true)
        }
        return theme
    }
}

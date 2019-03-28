package network.o3.o3wallet.Settings


import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import network.o3.o3wallet.Dapp.DAppBrowserActivityV2
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import java.net.URL

class AdvancedSettingsActivity : AppCompatActivity() {
    lateinit var mainnetCheckBox: CheckBox
    lateinit var testnetCheckbox: CheckBox
    lateinit var overrideCheckbox: CheckBox
    lateinit var useLegacyCheckBox: CheckBox
    lateinit var browserButton: Button
    lateinit var setCustomButton: Button
    lateinit var customNodeEditText: EditText


    fun bindViews() {
        mainnetCheckBox = find(R.id.checkBoxMainNet)
        testnetCheckbox = find(R.id.checkBoxTestNet)
        overrideCheckbox = find(R.id.rpcOverrideCheckbox)
        setCustomButton = find(R.id.connectButton)
        browserButton = find(R.id.browserButton)
        customNodeEditText = find(R.id.customEndpointEditText)
        useLegacyCheckBox = find(R.id.useLegacyCheckbox)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_advanced_activity)

        bindViews()
        initiateNetworkSelectors()
        initiateNetworkUpdateButton()
        initiateDevBrowser()
    }


    fun initiateNetworkSelectors() {
        mainnetCheckBox.isChecked = PersistentStore.getNetworkType() == "Main"
        testnetCheckbox.isChecked = PersistentStore.getNetworkType() == "Test"
        overrideCheckbox.isChecked = PersistentStore.getOverrideNodeURL() != ""
        if(overrideCheckbox.isChecked) {
            customNodeEditText.visibility = View.VISIBLE
            customNodeEditText.text = SpannableStringBuilder(PersistentStore.getOverrideNodeURL())
        } else {
            customNodeEditText.visibility = View.GONE
            customNodeEditText.text = SpannableStringBuilder("")
        }

        if (mainnetCheckBox.isChecked) {
            PersistentStore.setNetworkType("Main")
        } else if (testnetCheckbox.isChecked) {
            PersistentStore.setNetworkType("Test")
        }

        testnetCheckbox.setOnClickListener {
            testnetCheckbox.isChecked = true
            mainnetCheckBox.isChecked = false
        }

        mainnetCheckBox.setOnClickListener {
            testnetCheckbox.isChecked = false
            mainnetCheckBox.isChecked = true
        }

        overrideCheckbox.setOnClickListener {
            customNodeEditText.visibility = if(overrideCheckbox.isChecked) View.VISIBLE else View.GONE
        }
    }

    fun initiateNetworkUpdateButton() {

        var url: URL? = null
        setCustomButton.setOnClickListener {
            if (overrideCheckbox.isChecked) {
                try {
                    url = URL(customNodeEditText.text.toString())
                } catch (e: Exception) {
                    toast("Invalid url for the custom node")
                    return@setOnClickListener
                }
            }

            if (testnetCheckbox.isChecked) {
                PersistentStore.setNetworkType("Test")
            } else if (mainnetCheckBox.isChecked) {
                PersistentStore.setNetworkType("Main")
            }

            if (url != null && overrideCheckbox.isChecked) {
                PersistentStore.setOverrideNodeURL(customNodeEditText.text.toString())
            } else {
                PersistentStore.setOverrideNodeURL("")
            }

            val intent = Intent("need-update-watch-address-event")
            intent.putExtra("reset", true)
            LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
            toast("You are now connected to the " + PersistentStore.getNetworkType() + " Network")
        }
    }

    fun initiateDevBrowser() {
        browserButton.setOnClickListener {
            val url =  "https://www.ftwlotto.com/o3?provider=o3"
            val intent = Intent(this, DAppBrowserActivityV2::class.java)
            intent.putExtra("url", url)
            intent.putExtra("legacy", useLegacyCheckBox.isChecked)
            intent.putExtra("allowSearch", true)
            startActivity(intent)
        }
    }
}

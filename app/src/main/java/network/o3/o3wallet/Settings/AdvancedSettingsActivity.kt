package network.o3.o3wallet.Settings


import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.Dapp.DAppBrowserActivityV2
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast

class AdvancedSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_advanced_activity)
        val editText = findViewById<EditText>(R.id.customEndpointEditText)
        val setCustomButton = findViewById<Button>(R.id.connectButton)

        val browserButton = findViewById<Button>(R.id.browserButton)

        val mainnet = findViewById<CheckBox>(R.id.checkBoxMainNet)
        val testnet = findViewById<CheckBox>(R.id.checkBoxTestNet)
        val privatenet = findViewById<CheckBox>(R.id.checkBoxPrivateNet)

        privatenet.isChecked = true

        privatenet.setOnClickListener {
            testnet.isChecked = false
            mainnet.isChecked = false
            privatenet.isChecked = true
            editText.text = SpannableStringBuilder("https://privatenet.o3.network:30333")
        }

        testnet.setOnClickListener {
            testnet.isChecked = true
            mainnet.isChecked = false
            privatenet.isChecked = false
            editText.text = SpannableStringBuilder("http://seed2.neo.org:20332")
        }

        mainnet.setOnClickListener {
            testnet.isChecked = false
            mainnet.isChecked = true
            privatenet.isChecked = false
            editText.text = SpannableStringBuilder("http://seed2.o3node.org:10332")
        }

        setCustomButton.setOnClickListener {
            PersistentStore.setNodeURL(editText.text.toString())
            if (testnet.isChecked) {
                PersistentStore.setNetworkType("Test")
            } else if (mainnet.isChecked) {
                PersistentStore.setNetworkType("Main")
            } else {
                PersistentStore.setNetworkType("Private")
            }
            toast("Network Changed. Close the App Fully, and Restart to Reconnect")
        }

        browserButton.setOnClickListener {
            val url =  "https://s3-ap-northeast-1.amazonaws.com/network.o3.apps/testsend/index.html"
            val intent = Intent(this, DAppBrowserActivityV2::class.java)
            intent.putExtra("url", url)
            intent.putExtra("allowSearch", true)
            startActivity(intent)
        }
    }
}

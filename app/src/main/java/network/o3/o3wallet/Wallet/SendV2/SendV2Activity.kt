package network.o3.o3wallet.Wallet.SendV2

import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import neoutils.Neoutils.parseNEP9URI
import network.o3.o3wallet.R
import network.o3.o3wallet.Wallet.toastUntilCancel
import org.jetbrains.anko.find
import java.math.BigDecimal

class SendV2Activity : AppCompatActivity() {
    var sendViewModel: SendViewModel = SendViewModel()
    var sendingToast: Toast? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_v2_activity)
        if (intent.extras != null) {
            val uri = intent.getStringExtra("uri")
            if (uri != "") {
                parseQRPayload(uri)
                //give sometime to load eveything up
                Thread.sleep(2000)
            }
        }
        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        supportActionBar?.setCustomView(R.layout.actionbar_layout)
    }

    fun parseQRPayload(payload: String) {
        if (Neoutils.validateNEOAddress(payload)) {
            sendViewModel.setSelectedAddress(payload)
        } else {
            val uri = parseNEP9URI(payload)
            val toAddress = uri.to
            val amount = uri.amount
            val assetID = uri.asset
            if (toAddress != "") {
                sendViewModel.setSelectedAddress(toAddress)
            }
            if (amount != 0.0) {
                sendViewModel.setSelectedSendAmount(BigDecimal(amount))
            }
            if(assetID != "") {
                runOnUiThread {
                    sendViewModel.getOwnedAssets(false).observe ( this, Observer { ownedAssets ->
                        if (assetID == "neo" || assetID == "gas") {
                            val nativeAsset = ownedAssets?.find { it.symbol.toUpperCase() == assetID.toUpperCase() }
                            if (nativeAsset != null) {
                                sendViewModel.setSelectedAsset(nativeAsset)
                            }
                        } else {
                            val tokenAsset = ownedAssets?.find { it.id == assetID }
                            if (tokenAsset != null) {
                                sendViewModel.setSelectedAsset(tokenAsset)
                            }
                        }
                    })
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == -1) {
            runOnUiThread {
                sendingToast = baseContext.toastUntilCancel(resources.getString(R.string.SEND_sending_in_progress))
                sendingToast?.show()
            }
            sendViewModel.send()
            return
        } else if (requestCode == 1 && resultCode == 0) {
            Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
            return
        }

        if (result == null || result.contents == null) {
            return
        } else {
            parseQRPayload(result.contents)
            //give sometime to load eveything up
            Thread.sleep(2000)
            return
        }
    }
}

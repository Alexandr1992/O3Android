package network.o3.o3wallet.Wallet.SendV2

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.widget.EditText
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class SendV2Activity : AppCompatActivity() {
    var sendViewModel: SendViewModel = SendViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_v2_activity)
        title = getString(R.string.SEND_send)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
        } else if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
            } else {
                if (Neoutils.validateNEOAddress(result.contents)) {
                    find<EditText>(R.id.addressEntryEditText).text = SpannableStringBuilder(result.contents)
                }
            }
        }
    }
}

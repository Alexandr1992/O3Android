package network.o3.o3wallet.MultiWallet.AddNewMultiWallet

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.findOptional
import org.jetbrains.anko.image

class AddNewMultiwalletRootActivity : AppCompatActivity() {

    var viewModel = AddNewMultiwalletViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.multiwallet_add_new_activity)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.actionbar_layout)
        find<TextView>(R.id.mytext).text = resources.getString(R.string.MULTIWALLET_add_wallet)
        find<ImageButton>(R.id.rightNavButton).visibility = View.VISIBLE
        find<ImageButton>(R.id.rightNavButton).image = ContextCompat.getDrawable(this, R.drawable.ic_scan)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
        } else {
            find<EditText>(R.id.walletEntryEditText).text = SpannableStringBuilder(result.contents)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (findOptional<EditText>(R.id.walletEntryEditText) != null) {
            find<ImageButton>(R.id.rightNavButton).visibility = View.VISIBLE
        }
    }
}
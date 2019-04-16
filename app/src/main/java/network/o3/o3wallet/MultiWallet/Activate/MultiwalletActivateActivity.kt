package network.o3.o3wallet.MultiWallet.Activate

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.multiwallet_activate_activity.*
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class MultiwalletActivateActivity : AppCompatActivity() {

    var viewModel = ActivateMultiWalletViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.multiwallet_activate_activity)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.actionbar_layout)
        find<TextView>(R.id.mytext).text = resources.getString(R.string.MULTIWALLET_security_update)
        find<ImageButton>(R.id.rightNavButton).visibility = View.GONE
    }

    val currentFragment: Fragment?
        get() = add_multiwallet_nav_host.childFragmentManager.findFragmentById(R.id.add_multiwallet_nav_host)

    override fun onBackPressed() {
        if (currentFragment is EncryptExistingKeySuccessFragment) {
            if (NEP6.nep6HasActivated() == false) {
                val intent = Intent(this, MultiwalletActivateActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                val intent = Intent(this, MainTabbedActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        } else {
            super.onBackPressed()
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

package network.o3.o3wallet.Onboarding.CreateKey.Backup

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class Nep2BackupActivity : AppCompatActivity() {

    var enteredPassword = ""
    var wif = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_nep2_backup_activity)
        wif = intent.getStringExtra("wif")
        val pager = find<ViewPager>(R.id.passwordViewPager)
        pager.bringToFront()
        pager.adapter = PasswordAdapter(supportFragmentManager)

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
}

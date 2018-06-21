package network.o3.o3wallet

import android.app.Dialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.view.MenuItem
import network.o3.o3wallet.Feed.NewsFeedFragment
import network.o3.o3wallet.Portfolio.HomeFragment
import network.o3.o3wallet.Settings.SettingsFragment
import network.o3.o3wallet.Wallet.TabbedAccount
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import co.kyash.rkd.KeyboardDetector
import co.kyash.rkd.KeyboardStatus
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.tabbar_activity_main_tabbed.*
import network.o3.o3wallet.MarketPlace.MarketplaceTabbedFragment
import network.o3.o3wallet.Wallet.Send.SendActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton


class MainTabbedActivity : AppCompatActivity() {

    var activeTabID: Int? = 0
    var activeTabPosition: Int? = 0
    var fragments: Array<Fragment>? = arrayOf(HomeFragment.newInstance(),
            TabbedAccount.newInstance(), MarketplaceTabbedFragment.newInstance(), NewsFeedFragment.newInstance(),
            SettingsFragment.newInstance())

    override fun onBackPressed() {
        alert (resources.getString(R.string.TABBAR_logout_warning)) {
            yesButton { super.onBackPressed() }
            noButton {  }
        }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //QR-Scanned Activitry
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x0000c0de) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null && result.contents == null) {
                Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, SendActivity::class.java)
                intent.putExtra("address", "")
                intent.putExtra("payload", result.contents.trim())
                startActivity(intent)
            }
        }
    }

    fun setupKeyboardDetector() {
        KeyboardDetector(this).observe().subscribe({ status ->
            when(status) {
                KeyboardStatus.OPENED -> { find<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE}
                KeyboardStatus.CLOSED -> {find<BottomNavigationView>(R.id.bottom_navigation).visibility = View.VISIBLE}
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tabbar_activity_main_tabbed)


        val selectedFragment = fragments!!.get(0)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, selectedFragment, 0.toString())
        transaction.commit()

        setupKeyboardDetector()

        activeTabID = selectedFragment.id
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                var selectedFragment: Fragment? = null
                //avoid loading the data again when tap at the same tab
                if (activeTabID == item.itemId) {
                    return false
                }

                var tabName = ""
                when (item.itemId) {
                    R.id.action_item1 -> {
                        switchFragment(0)
                        activeTabID = item.itemId
                        activeTabPosition = 0
                        tabName = "Home"
                    }
                    R.id.action_item2 -> {
                        switchFragment(1)
                        activeTabID = item.itemId
                        activeTabPosition = 1
                        tabName = "Wallet"
                    }
                    R.id.action_item3 -> {
                        switchFragment(2)
                        activeTabID = item.itemId
                        activeTabPosition = 2
                        tabName = "Marketplace"
                    }
                    R.id.action_item4 -> {
                        switchFragment(3)
                        activeTabID = item.itemId
                        activeTabPosition = 3
                        tabName = "News"
                    }
                    R.id.action_item5 -> {
                        val settingsModal = fragments!!.get(4) as SettingsFragment
                        settingsModal.show(supportFragmentManager, settingsModal.tag)
                        tabName = "Settings"
                        Answers().logCustom(CustomEvent("Tab Tapped")
                                .putCustomAttribute("Tab Name", tabName))
                        return false
                    }
                }
                Answers().logCustom(CustomEvent("Tab Tapped")
                        .putCustomAttribute("Tab Name", tabName))
                return true
            }
        })
    }

    private fun switchFragment(index: Int) {

        val transaction = supportFragmentManager.beginTransaction()

        // if the fragment has not yet been added to the container, add it first
        if (supportFragmentManager.findFragmentByTag(index.toString()) == null) {
            transaction.add(R.id.frame_layout, fragments!!.get(index), index.toString())
        }

        transaction.hide(fragments!!.get(activeTabPosition!!))
        transaction.show(fragments!!.get(index))
        transaction.commit()
        if(index == 0) {
            (fragments!!.get(index) as HomeFragment).homeModel.loadAssetsFromModel(false)
        }
    }



}

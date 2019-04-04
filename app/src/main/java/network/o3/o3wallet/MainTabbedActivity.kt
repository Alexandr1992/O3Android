package network.o3.o3wallet

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import co.getchannel.channel.Channel
import co.getchannel.channel.callback.ChannelCallback
import co.kyash.rkd.KeyboardDetector
import co.kyash.rkd.KeyboardStatus
import com.amplitude.api.Amplitude
import com.crashlytics.android.Crashlytics
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.google.firebase.iid.FirebaseInstanceId
import com.google.zxing.integration.android.IntentIntegrator
import com.tapadoo.alerter.Alerter
import io.fabric.sdk.android.Fabric
import network.o3.o3wallet.Dapp.DappContainerActivity
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import org.json.JSONObject
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.Support

class MainTabbedActivity : AppCompatActivity() {

    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tabbar_activity_main_tabbed)
        setupKeyboardDetector()
        setupChannel()
        setupZendesk()

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }
        if (!BuildConfig.DEBUG) {
            Amplitude.getInstance().initialize(this, resources.getString(R.string.Amplitude_API_Key)).enableForegroundTracking(application)
            AnalyticsService.Navigation.logLoadedMainTab()
            if (PersistentStore.getHasLoggedFirstWallet() == false) {
                val type = if (PersistentStore.didGenerateFirstWallet()) {
                    "new_key"
                } else {
                    "import_key"
                }

                val method = if (PersistentStore.didGenerateFirstWallet()) {
                    "new"
                } else {
                    "import"
                }
                val attrs = mapOf(
                        "type" to type,
                        "method" to method,
                        "address_count" to NEP6.getFromFileSystem().accounts.size)
                AnalyticsService.Wallet.logWalletAdded(JSONObject(attrs))
                PersistentStore.setHasLoggedFirstWallet(true)
            }
            Fabric.with(this, Crashlytics())
        }


        // Else, need to wait for onRestoreInstanceState
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED

        val navGraphIds = listOf(R.navigation.portfolio, R.navigation.wallet, R.navigation.marketplace,
                R.navigation.news, R.navigation.settings)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.nav_host_container,
                intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    /**
     * Overriding popBackStack is necessary in this case if the app is started from the deep link.
     */
    override fun onBackPressed() {
        alert(resources.getString(R.string.TABBAR_logout_warning)) {
            yesButton { super.onBackPressed() }
            noButton { }
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
                if (URLUtil.isNetworkUrl(result.contents)) {
                    val intent = Intent(this, DappContainerActivity::class.java)
                    intent.putExtra("url", result.contents)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, SendV2Activity::class.java)
                    intent.putExtra("uri", result.contents)
                    startActivity(intent)
                }
            }
        } else if (requestCode == 101) {
            Log.d("hello", "hello")
        }
    }

    fun setupKeyboardDetector() {
        KeyboardDetector(this).observe().subscribe({ status ->
            when (status) {
                KeyboardStatus.OPENED -> {
                    find<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE
                }
                KeyboardStatus.CLOSED -> {
                    find<BottomNavigationView>(R.id.bottom_navigation).visibility = View.VISIBLE
                }
            }
        })
    }

    fun setupChannel() {

        Channel.setupApplicationContextWithApplicationKey(O3Wallet.appContext, "app_gUHDmimXT8oXRSpJvCxrz5DZvUisko_mliB61uda9iY", object: ChannelCallback {
            override fun onSuccess() {
                val refreshedToken = FirebaseInstanceId.getInstance().token
                Channel.saveDeviceToken(refreshedToken, object : ChannelCallback {
                    override fun onSuccess() {}

                    override fun onFail(message: String) {}
                })

                Channel.subscribeToTopic(Account.getWallet().address.toString(), object : ChannelCallback {
                    override fun onSuccess() {

                    }

                    override fun onFail(message: String) {

                    }
                })
            }

            override fun onFail(message: String) {

            }
        })
    }

    fun setupZendesk() {
        Zendesk.INSTANCE.init(this, resources.getString(R.string.Zendesk_url),
                resources.getString(R.string.Zendesk_API_key),
                resources.getString(R.string.Zendesk_client_key))
        val identity = AnonymousIdentity()
        Zendesk.INSTANCE.setIdentity(identity)
        Support.INSTANCE.init(Zendesk.INSTANCE)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), IntentFilter("Alert"))
        LocalBroadcastManager.getInstance(this).registerReceiver(needReloadThemeReciever, IntentFilter("need-reload-theme"))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver((mMessageReceiver))
        LocalBroadcastManager.getInstance(this).unregisterReceiver(needReloadThemeReciever)
    }

    fun getActivity(): Activity {
        return this
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_Dark_NoTopBar, true)
        } else {
            theme.applyStyle(R.style.AppTheme_White_NoTopBar, true)
        }
        return theme
    }

    private val needReloadThemeReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            getActivity().finish()
            getActivity().startActivity(getActivity().intent)
        }
    }

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Alerter.isShowing) {
                return
            }
            Alerter.create(getActivity())
                    .setTitle(intent.extras!!.getString("alert_title"))
                    .setText(intent.extras!!.getString("alert_message"))
                    .setBackgroundColorRes(R.color.colorPrimaryTranslucent)
                    .setIcon(R.drawable.ic_notifciation_luna)
                    .setTextAppearance(R.style.NotificationText)
                    .setTitleAppearance(R.style.NotificationTitle)
                    .setIconColorFilter(0)
                    .enableSwipeToDismiss()
                    .setDuration(3000)
                    .show()
        }
    }
}

package network.o3.o3wallet.Dapp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.zxing.integration.android.IntentIntegrator
import com.tapadoo.alerter.Alerter
import network.o3.o3wallet.AnalyticsService
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.json.JSONObject
import java.net.URL


class DappContainerActivity : AppCompatActivity() {
    lateinit var dappViewModel: DAPPViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dapp_container_activity)

        //check if deeplink or coming locally from the app
        val url = if(intent.data?.getQueryParameter("url") != null) {
            intent.data?.getQueryParameter("url")!!
        } else {
            intent.getStringExtra("url")
        }



        dappViewModel = DAPPViewModel(url)
        dappViewModel.allowSearch = intent.getBooleanExtra("allowSearch", false)
        dappViewModel.legacy = intent.getBooleanExtra("legacy", false)
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_NoTopBar_Dark, true)
        } else {
            theme.applyStyle(R.style.AppTheme_NoTopBar_White, true)
        }
        return theme
    }

    override fun onBackPressed() {
        val webView = find<WebView>(R.id.dapp_browser_webview)
        if (webView.canGoBack()) {
            var currIndex = webView.copyBackForwardList().currentIndex
            if (webView.copyBackForwardList().getItemAtIndex(currIndex).url == webView.copyBackForwardList().getItemAtIndex(currIndex - 1).url) {
                currIndex -= 1
                webView.goBack()
            }
            val url = webView.copyBackForwardList().getItemAtIndex(currIndex).url
            if (url != null) {
                setStylingForURLText(url)
            }
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    fun setStylingForURLText(url: String) {
        if (!url.startsWith("http") && !url.startsWith("https")) {
            return
        }

        val dip = 24f
        val r = resources
        val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.displayMetrics
        )

        val searchBar = find<EditText>(R.id.dappSearch)
        val searchImage = find<ImageView>(R.id.httpsLock)
        searchBar.text = SpannableStringBuilder(url)
        if (searchBar.text.toString().startsWith("https://")) {
            searchBar.text.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorGain)),
                    0, "https://".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE )
            searchBar.setPadding(px.toInt() + 16, 0, 16, 0)
        } else {
            searchBar.setPadding(16, 0, 16, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), IntentFilter("Alert"))
        val json = mapOf("dappVersion" to "V1", "url" to dappViewModel.url, "domain" to URL(dappViewModel.url).authority)
        AnalyticsService.DAPI.logDappOpened(JSONObject(json))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver((mMessageReceiver))

    }

    override fun onPause() {
        super.onPause()
        val json = mapOf("dappVersion" to "V1", "url" to dappViewModel.url, "domain" to URL(dappViewModel.url).authority)
        AnalyticsService.DAPI.logDappClosed(JSONObject(json))
    }


    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Alerter.isShowing) {
                return
            }

            Alerter.create(this@DappContainerActivity)
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

    //for managing file uploading

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        //uploading a file
        //if (requestCode == dappViewModel.FILECHOOSER_RESULTCODE) {
            if (null == dappViewModel.mUploadMessage) {
                return
            }
            var toParse = if (data == null || resultCode !== Activity.RESULT_OK)
                null
            else
                data.data
            if (toParse == null && resultCode != Activity.RESULT_CANCELED) {
                toParse = dappViewModel.photoURI
            }

            if (toParse == null) {
                dappViewModel.mUploadMessage!!.onReceiveValue(arrayOf())
            } else {
                dappViewModel.mUploadMessage!!.onReceiveValue(arrayOf(toParse!!))
            }
            dappViewModel.mUploadMessage = null
            return
       // }

        //Removed auth for switching, all auth is done in top level off app now isntead of at dapp level
        /*
        //Changing Wallets
        if (result != null && result.contents == null) {
            return
        } else {
            if (resultCode == -1) {
                Log.d("hello", "hello")
               if (dappViewModel.legacyInterface == null) {
                   /*if (NEP6.getFromFileSystem().accounts.isEmpty()) {
                      dappViewModel.dapiInterface.setWalletForSession(Account.getWallet(), "My O3 Wallet")
                  } else {
                      dappViewModel.dapiInterface.setWalletForSession(walletToExpose,
                              walletToExposeName)
                  }
                  dappViewModel.dapiInterface.authorizedAccountCredentials(pendingDappMessage!!)
                  pendingDappMessage = null*/
                } else {
                    dappViewModel.legacyInterface?.finishConnectionToO3()
                }
            } else {
                return
            }
        }*/
    }




    /*var walletToExpose: Wallet = Account.getWallet()
    var walletToExposeName: String = NEP6.getFromFileSystem().getDefaultAccount().label

    fun verifyPassCodeAndSign(message: DappMessage? = null) {
        val mKeyguardManager = webView.context!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (message != null) {
                pendingDappMessage = message
            }
            if (intent != null) {
                (webView.context as Activity).startActivityForResult(intent, 1)
            }
        }
    }*/
}

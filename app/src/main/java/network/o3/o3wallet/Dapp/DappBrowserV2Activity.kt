package network.o3.o3wallet.Dapp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.*
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.amplitude.api.Amplitude
import com.google.zxing.integration.android.IntentIntegrator
import com.tapadoo.alerter.Alerter
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.Account
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.NativeTrade.NativeTradeRootActivity
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.json.JSONObject
import java.io.InputStream
import java.net.URL

class DAppBrowserActivityV2 : AppCompatActivity() {

    lateinit var dappBrowserView: View
    lateinit var webView: WebView
    lateinit var jsInterface: DappBrowserJSInterfaceV2

    var previousWasRedirect = false

    val whitelistedAuthorities = arrayOf("neoscan.io", "beta.switcheo.exchange", "switcheo.exchange",
            "neonewstoday.com", "public.o3.network", "explorer.ont.io")
    val doNotShowAuthorities = arrayOf("analytics.o3.network")
    var lastClickTime: Long = 0

    var userHasAuthorizedWallet = false

    data class ResourceObject(val url: String, val mimeType: String, val resourceID: Int, val encoding: String)

    val localResources = arrayOf(
            ResourceObject("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css", "text/css", R.raw.bootstrap, "UTF-8"),
            ResourceObject("https://d3js.org/d3.v4.min.js", "text/javascript", R.raw.d3, "UTF-8"),
            ResourceObject("https://cdn.jsdelivr.net/npm/vue", "text/javascript", R.raw.vue, "UTF-8"),
            ResourceObject("https://unpkg.com/axios/dist/axios.min.js", "text/javascript", R.raw.axios, "UTF-8"),
            ResourceObject("https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.22.2/moment.min.js", "text/javascript", R.raw.moment, "UTF-8"),
            ResourceObject("https://cdn.o3.network/assets/fa/js/fontawesome-all.min.js", "text/javascript", R.raw.font_awesome, "UTF-8"),
            ResourceObject("https://cdnjs.cloudflare.com/ajax/libs/bodymovin/4.13.0/bodymovin.min.js", "text/javascript", R.raw.bodymovin, "UTF-8")
    )


    fun authorizeWalletInfo(message: DappMessage) {
        runOnUiThread {


        val bottomSheet = DappConnectionRequestBottomSheet()
        bottomSheet.dappMessage = message
        val bundle = Bundle()
        bundle.putString("url", webView.url)
        bottomSheet.arguments = bundle
        bottomSheet.show(this.supportFragmentManager, bottomSheet.tag)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dapp_browser_activity)

        dappBrowserView = findViewById<View>(R.id.dapp_browser_root_layout)
        webView = dappBrowserView.findViewById(R.id.dapp_browser_webview)
        val searchBar = dappBrowserView.find<SearchView>(R.id.dappSearch)

        val webLoader = find<LottieAnimationView>(R.id.webLoader)
        val url = intent.getStringExtra("url")
        val currentUrlRoute = URL(url)
        setVerifiedHeaderUrl(url)

        dappBrowserView.find<ImageButton>(R.id.webBrowserBackButton).setOnClickListener {
            if (webView.canGoBack()) {
                val currIndex = webView.copyBackForwardList().currentIndex
                setVerifiedHeaderUrl(webView.copyBackForwardList().getItemAtIndex(currIndex - 1).url)
                webView.goBack()
            } else {
                //onBackPressed()
            }
        }

        val showSearchBar = intent.getBooleanExtra("allowSearch", false)
        if (showSearchBar) {
            searchBar.visibility = View.VISIBLE
            dappBrowserView.find<ImageButton>(R.id.refreshButton).visibility = View.VISIBLE
            dappBrowserView.find<ImageButton>(R.id.refreshButton).setOnClickListener {
                webView.reload()
            }


            searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    webLoader.visibility = View.VISIBLE
                    webView.loadUrl(query)
                    return true
                }
            })
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                webLoader.visibility = View.VISIBLE

                val urlToLoad = request.url.toString()
                //we are in our own app, open a new browser

                if (!urlToLoad.startsWith("http") && !urlToLoad.startsWith("https")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToLoad))
                    val activityToUse = intent.resolveActivity(packageManager)
                    if (activityToUse == null) {
                        webLoader.visibility = View.INVISIBLE
                        return false
                    } else {
                        startActivity(intent)
                        return true
                    }
                }

                setVerifiedHeaderUrl(urlToLoad)
                if (previousWasRedirect) {
                    return false
                }
                previousWasRedirect = (doNotShowAuthorities.contains(request.url.authority))

                view.loadUrl(urlToLoad)
                return false // then it is not handled by default action
            }


            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val localResource = localResources.find { it.url == request!!.url.toString() }
                if (localResource != null) {
                    val inputStream: InputStream = resources.openRawResource(localResource.resourceID)
                    val statusCode = 200
                    val reasonPhase = "OK"
                    val responseHeaders = mutableMapOf<String, String>()
                    responseHeaders.put("Access-Control-Allow-Origin", "*")
                    return WebResourceResponse(localResource.mimeType, localResource.encoding, statusCode, reasonPhase, responseHeaders, inputStream)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                webLoader.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.visibility = View.VISIBLE
            }
        }


        webView.visibility = View.INVISIBLE
        webView.loadUrl(url)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        jsInterface = DappBrowserJSInterfaceV2(this, webView, Account.getWallet(),
                NEP6.getFromFileSystem().accounts.find { it.isDefault }?.label ?: "My O3 Wallet")
        webView.addJavascriptInterface(jsInterface, "_o3dapi")
        WebView.setWebContentsDebuggingEnabled(true)
    }


    fun setVerifiedHeaderUrl(url: String) {
        if (!url.startsWith("http") && !url.startsWith("https")) {
            return
        }

        val toLoadUrl = URL(url)
        val browserTitleTextView = dappBrowserView.find<TextView>(R.id.browserTitleTextView)
        val verifiedImageView = dappBrowserView.find<ImageView>(R.id.verifiedURLImageView)
        if (doNotShowAuthorities.contains(toLoadUrl.authority)) {
            verifiedImageView.visibility = View.INVISIBLE
            browserTitleTextView.visibility = View.INVISIBLE
            return
        }

        if (whitelistedAuthorities.contains(toLoadUrl.authority)) {
            verifiedImageView.visibility = View.VISIBLE
        } else {
            verifiedImageView.visibility = View.INVISIBLE
        }
        browserTitleTextView.visibility = View.VISIBLE
        browserTitleTextView.text = toLoadUrl.authority.toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            return
        } else {
            if (resultCode == -1) {
                //jsInterface.finishConnectionToO3()
            } else {
                return
            }
        }
    }

    override fun onBackPressed() {
        //if (jsInterface.userAuthenticatedApp) {
         //   alert(resources.getString(R.string.DAPP_logout_warning)) {
           //     yesButton { super.onBackPressed() }
             //   noButton { }
         //   }.show()
       // } else {
            if (webView.canGoBack()) {
                var currIndex = webView.copyBackForwardList().currentIndex
                if (webView.copyBackForwardList().getItemAtIndex(currIndex).url == webView.copyBackForwardList().getItemAtIndex(currIndex - 1).url) {
                    currIndex -= 1
                    webView.goBack()
                }
                val url = webView.copyBackForwardList().getItemAtIndex(currIndex).url
                if (url != null) {
                    setVerifiedHeaderUrl(url)
                }
                webView.goBack()
            } else {
                super.onBackPressed()
            }
    //    }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), IntentFilter("Alert"))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver((mMessageReceiver))

    }

    fun getActivity(): Activity {
        return this
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

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_NoTopBar_Dark, true)
        } else {
            theme.applyStyle(R.style.AppTheme_NoTopBar_White, true)
        }
        return theme
    }
}
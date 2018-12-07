package network.o3.o3wallet.Dapp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.MenuAdapter
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.tapadoo.alerter.Alerter
import neoutils.Neoutils
import network.o3.o3wallet.*
import network.o3.o3wallet.Settings.AddContact
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk15.coroutines.onClick
import java.io.InputStream
import java.net.URL
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.CustomPowerMenu
import com.skydoves.powermenu.OnMenuItemClickListener
import kotlinx.android.synthetic.main.dialog_single_input.view.*
import org.w3c.dom.Text


class DAppBrowserActivityV2 : AppCompatActivity() {

    lateinit var dappBrowserView: View
    lateinit var webView: WebView
    lateinit var jsInterface: DappBrowserJSInterfaceV2
    lateinit var searchBar: EditText
    lateinit var progressBar: ProgressBar

    var legacyInterface: DappBrowserJSInterface? = null
    var previousWasRedirect = false

    val whitelistedAuthorities = arrayOf("neoscan.io", "beta.switcheo.exchange", "switcheo.exchange",
            "neonewstoday.com", "public.o3.network", "explorer.ont.io")
    val doNotShowAuthorities = arrayOf("analytics.o3.network")

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dapp_browser_activity)

        dappBrowserView = findViewById<View>(R.id.dapp_browser_root_layout)
        webView = dappBrowserView.findViewById(R.id.dapp_browser_webview)
        searchBar = dappBrowserView.find<EditText>(R.id.dappSearch)
        progressBar = dappBrowserView.find(R.id.dappViewProgressBar)
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val url = intent.getStringExtra("url")
        setStylingForURLText(url)
        setupTopBar(intent.getBooleanExtra("allowSearch", false))
        setupWebClients()

        webView.visibility = View.INVISIBLE
        webView.loadUrl(url)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        WebView.setWebContentsDebuggingEnabled(true)

        if (URL(url).authority == "switcheo.exchange" || URL(url).authority == "legacy.switcheo.exchange") {
            legacyInterface = DappBrowserJSInterface(this, webView)
            webView.addJavascriptInterface(legacyInterface, "O3AndroidInterface")
        } else {
            jsInterface = DappBrowserJSInterfaceV2(this, webView, null, "")
            webView.addJavascriptInterface(jsInterface, "_o3dapi")
        }
    }




    fun setMoreActions() {
        val moreButton = dappBrowserView.find<ImageView>(R.id.moreButton)
        moreButton.onClick {
            val customPowerMenu = CustomPowerMenu.Builder(this@DAppBrowserActivityV2, DappPopupMenuAdapter())
                    .setWidth(700)
                    .addItem(DappPopupMenuItem(
                            resources.getString(R.string.DAPP_refresh),
                            ContextCompat.getDrawable(this@DAppBrowserActivityV2, R.drawable.ic_refresh))
                    )

                    .addItem(DappPopupMenuItem(
                            resources.getString(R.string.DAPP_disconnect),
                            ContextCompat.getDrawable(this@DAppBrowserActivityV2, R.drawable.ic_home))
                    )

                    .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
                    .setMenuRadius(10f)
                    .setMenuShadow(10f)
                    .build()

            val onIconMenuItemClickListener = OnMenuItemClickListener<DappPopupMenuItem> { position, item ->
                if (position == 0) {
                    webView.reload()
                } else if (position == 1) {
                    this@DAppBrowserActivityV2.finish()
                }
                customPowerMenu.dismiss()
            }
            customPowerMenu.setOnMenuItemClickListener(onIconMenuItemClickListener)
            customPowerMenu.showAsDropDown(moreButton)
        }
    }

    fun setUnlockState() {
        runOnUiThread {
            var walletStatusView = dappBrowserView.find<ImageView>(R.id.walletStatusImageView)
            walletStatusView.image = resources.getDrawable(R.drawable.ic_dapp_wallet_active)

            walletStatusView.onClick {
                val customPowerMenu = CustomPowerMenu.Builder(this@DAppBrowserActivityV2, DappPopupMenuAdapter())
                        .setWidth(700)
                        .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
                        .setMenuRadius(10f)
                        .setMenuShadow(10f)
                        .build()

                var headerView = layoutInflater.inflate(R.layout.dapp_popup_header, null, false)
                headerView.find<TextView>(R.id.walletAddressTitle).text = this@DAppBrowserActivityV2.jsInterface.getDappExposedWallet()!!.address
                headerView.find<TextView>(R.id.walletNameTitle).text = this@DAppBrowserActivityV2.jsInterface.getDappExposedWalletName()
                headerView.find<Button>(R.id.swapButton).onClick {
                    val swapWalletSheet = DappWalletForSessionBottomSheet.newInstance()
                    swapWalletSheet.show(this@DAppBrowserActivityV2!!.supportFragmentManager, swapWalletSheet.tag)
                    customPowerMenu.dismiss()
                }
                customPowerMenu.setHeaderView(headerView)
                customPowerMenu.showAsDropDown(walletStatusView)
            }
        }
    }

    fun setupTopBar(allowSearch: Boolean) {
        val allowSearch = intent.getBooleanExtra("allowSearch", false)
        searchBar.isFocusable = false
        if (allowSearch) {
            searchBar.setOnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_GO) {
                    webView.loadUrl(textView.text.toString())
                }
                true
            }
        } else {
            searchBar.isEnabled = true
        }
        setMoreActions()
    }

    fun setupWebClients() {
        webView.webChromeClient = object: WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val urlToLoad = request.url.toString()
                //we are in our own app, open a new browser

                if (!urlToLoad.startsWith("http") && !urlToLoad.startsWith("https")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToLoad))
                    val activityToUse = intent.resolveActivity(packageManager)
                    if (activityToUse == null) {
                        //webLoader.visibility = View.INVISIBLE
                        return false
                    } else {
                        startActivity(intent)
                        return true
                    }
                }

                setStylingForURLText(urlToLoad)
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

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
                progressBar.progress = 0
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
        }
    }

    fun setStylingForURLText(url: String) {
        if (!url.startsWith("http") && !url.startsWith("https")) {
            return
        }

        searchBar.text = SpannableStringBuilder(url)
        if (searchBar.text.toString().startsWith("https://")) {
            searchBar.text.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorGain)),
                    0, "https://".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE )
            searchBar.setPadding(85, 0, 16, 0)
        } else {
            searchBar.setPadding(16, 0, 16, 0)
        }
    }

    fun authorizeWalletInfo(message: DappMessage) {
        runOnUiThread{
            val bottomSheet = DappConnectionRequestBottomSheet()
            bottomSheet.dappMessage = message
            val bundle = Bundle()
            bundle.putString("url", webView.url)
            bottomSheet.arguments = bundle
            bottomSheet.show(this.supportFragmentManager, bottomSheet.tag)
        }
    }

    fun authorizeSend(message: DappMessage) {
        runOnUiThread {
            val bottomSheet = DappRequestSendBottomSheet()
            bottomSheet.dappMessage = message
            val bundle = Bundle()
            bundle.putString("send_request", Gson().toJson(Gson().fromJson<NeoDappProtocol.SendRequest>(Gson().toJson(message.data))))
            bundle.putString("url", webView.url)
            bottomSheet.arguments = bundle
            bottomSheet.show(this.supportFragmentManager, bottomSheet.tag)
        }
    }

    fun authorizeInvoke(message: DappMessage) {
        runOnUiThread {
            val bottomSheet = DappBrowserContractRequestBottomSheet.newInstance()
            bottomSheet.dappMessage = message
            val bundle = Bundle()
            bundle.putString("url", webView.url)
            bottomSheet.arguments = bundle
            bottomSheet.show(this.supportFragmentManager, bottomSheet.tag)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            return
        } else {
            if (resultCode == -1) {
                legacyInterface?.finishConnectionToO3()
            } else {
                return
            }
        }
    }

    override fun onBackPressed() {
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
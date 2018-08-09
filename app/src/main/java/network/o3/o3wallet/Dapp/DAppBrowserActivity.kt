package network.o3.o3wallet.Dapp

import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.view.View
import android.webkit.*
import network.o3.o3wallet.R
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.google.zxing.integration.android.IntentIntegrator
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import java.net.URL


class DAppBrowserActivity : AppCompatActivity() {

    lateinit var dappBrowserView: View
    lateinit var webView: WebView
    lateinit var jsInterface: DappBrowserJSInterface

    var previousWasRedirect = false

    val whitelistedAuthorities = arrayOf("neoscan.io", "beta.switcheo.exchange", "switcheo.exchange", "neonewstoday.com", "public.o3.network")
    val doNotShowAuthorities = arrayOf("analytics.o3.network")

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
                onBackPressed()
            }
        }

        val showSearchBar = intent.getBooleanExtra("allowSearch", false)
        if (showSearchBar) {
            searchBar.visibility = View.VISIBLE
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


        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                webLoader.visibility = View.VISIBLE

                val currentAuthority = currentUrlRoute.authority
                //we are in our own app, open a new browser
                if (currentAuthority == "public.o3.network") {
                    val i = Intent(view.context, DAppBrowserActivity::class.java)
                    i.putExtra("url", request.url.toString())
                    view.context.startActivity(i)
                    return false
                }

                setVerifiedHeaderUrl(request.url.toString())
                if (previousWasRedirect) {
                    return false
                }
                previousWasRedirect = (doNotShowAuthorities.contains(request.url.authority))

                view.loadUrl(request.url.toString())
                return false // then it is not handled by default action
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                webLoader.visibility = View.GONE
            }
        }

        webView.loadUrl(url)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        jsInterface = DappBrowserJSInterface(this, webView)
        webView!!.addJavascriptInterface(jsInterface, "O3AndroidInterface")
        WebView.setWebContentsDebuggingEnabled(true)
    }

    fun setVerifiedHeaderUrl(url: String) {
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
                jsInterface.finishConnectionToO3()
            } else {
                return
            }
        }
    }

    override fun onBackPressed() {
        if (jsInterface.userAuthenticatedApp) {
            alert(resources.getString(R.string.DAPP_logout_warning)) {
                yesButton { super.onBackPressed() }
                noButton { }
            }.show()
        } else {
            if (webView.canGoBack()) {
                val currIndex = webView.copyBackForwardList().currentIndex
                setVerifiedHeaderUrl(webView.copyBackForwardList().getItemAtIndex(currIndex - 1).url)
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        }
    }
}

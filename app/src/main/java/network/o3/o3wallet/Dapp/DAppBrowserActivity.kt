package network.o3.o3wallet.Dapp

import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import network.o3.o3wallet.R
import android.webkit.WebView
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.SearchView
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonObject
import com.google.zxing.integration.android.IntentIntegrator
import network.o3.o3wallet.Account
import network.o3.o3wallet.Onboarding.SelectingBestNode
import org.jetbrains.anko.find


class DAppBrowserActivity : AppCompatActivity() {

    lateinit var view: View
    lateinit var webView: WebView
    lateinit var jsInterface: DappBrowserJSInterface


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dapp_browser_activity)

        view = findViewById<View>(R.id.dapp_browser_root_layout)
        webView = view.findViewById(R.id.dapp_browser_webview)
        val searchBar = view.find<SearchView>(R.id.dappSearch)

        val webLoader = find<ProgressBar>(R.id.webLoader)
        val url = intent.getStringExtra("url")
        val showSearchBar = intent.getBooleanExtra("allowSearch", false)
        if (showSearchBar) {
            searchBar.visibility = View.VISIBLE
            searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    webLoader.visibility = View.VISIBLE
                    if (query.startsWith("https://www.")) {
                        webView.loadUrl(query)
                    } else {
                        webView.loadUrl("https://www." + query)
                    }

                    return true
                }
            })
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // dmayo your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                view.loadUrl(url)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            jsInterface.clearPendingTransaction()
        } else {
            if (resultCode == -1) {
                jsInterface.executePendingTransaction()
            } else {
                jsInterface.clearPendingTransaction()
            }
        }
    }
}

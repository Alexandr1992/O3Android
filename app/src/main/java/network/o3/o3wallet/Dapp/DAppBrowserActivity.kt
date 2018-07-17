package network.o3.o3wallet.Dapp

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
import org.jetbrains.anko.find


class DAppBrowserActivity : AppCompatActivity() {

    lateinit var view: View
    lateinit var webView: WebView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dapp_browser_activity)

        view = findViewById<View>(R.id.dapp_browser_root_layout)
        webView = view.findViewById(R.id.dapp_browser_webview)
        val webLoader = find<ProgressBar>(R.id.webLoader)
        val url = intent.getStringExtra("url")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // do your handling codes here, which url is the requested url
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
        webView!!.addJavascriptInterface(DappBrowserJSInterface(this, webView),"O3AndroidInterface")
        WebView.setWebContentsDebuggingEnabled(true)
    }
}

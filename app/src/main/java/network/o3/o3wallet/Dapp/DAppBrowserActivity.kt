package network.o3.o3wallet.Dapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import network.o3.o3wallet.R
import android.webkit.WebView
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebViewClient



class DAppBrowserActivity : AppCompatActivity() {

    lateinit var view: View
    lateinit var webView: WebView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dapp_browser_activity)

        view = findViewById<View>(R.id.dapp_browser_root_layout)
        webView = view.findViewById(R.id.dapp_browser_webview)
        webView.loadUrl("https://s3-ap-northeast-1.amazonaws.com/network.o3.cdn/____dapp/example/index.html")
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webView!!.addJavascriptInterface(DappBrowserJSInterface(this, webView),"O3AndroidInterface")
        WebView.setWebContentsDebuggingEnabled(true)
    }
}

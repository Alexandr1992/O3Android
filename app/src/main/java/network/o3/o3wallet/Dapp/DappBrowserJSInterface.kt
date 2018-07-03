package network.o3.o3wallet.Dapp

import android.webkit.JavascriptInterface
import android.widget.Toast
import android.content.Context
import android.webkit.WebView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.github.salomonbrys.kotson.*
import android.os.Handler
import network.o3.o3wallet.Account
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.toHex

data class O3Message(var command: String, var data: JsonObject)

class DappBrowserJSInterface(private val context: Context, private val webView: WebView) {

    @JavascriptInterface
    fun messageHandler(jsonString: String) {
        val gson = Gson()
        val message = gson.fromJson<O3Message>(jsonString)
        Toast.makeText(context, message!!.command, Toast.LENGTH_LONG).show()
        if (message.command == "init") {
            callback(message.command, jsonObject())
        } else if (message.command == "requestToConnect") {

        }

        if (message.command == "getPlatform") {
            val platform = jsonObject(
                    "platform" to "android",
                    "version" to O3Wallet.version
            )
            callback(message.command, platform)
        }

        if (message.command == "getAccounts") {
            val address = Account.getWallet()!!.address
            val publicKey = Account.getWallet()!!.publicKey.toHex()
            val neoAccount = jsonObject("address" to address,
            "publicKey" to publicKey)
            val blockchains = jsonObject("neo" to neoAccount)
            val dic = jsonObject( "accounts" to jsonArray(blockchains))
            callback(message.command, dic)
        }
    }

    private fun callback(command: String, data: JsonObject) {
        val mainHandler = Handler(O3Wallet.appContext!!.mainLooper);
        val myRunnable = Runnable() {
                val message = jsonObject(
                        "command" to command,
                        "data" to data
                )
                val script = "o3.callback(" + message.toString() + ")"
                webView.evaluateJavascript(script) { value ->
                    System.out.println(value)
                }
        }
        mainHandler.post(myRunnable)
    }

    @JavascriptInterface
    fun getAndroidVersion(): Int {
        return android.os.Build.VERSION.SDK_INT
    }

}
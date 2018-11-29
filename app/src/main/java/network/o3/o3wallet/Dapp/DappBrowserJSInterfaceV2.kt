package network.o3.o3wallet.Dapp
/*
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Handler
import android.webkit.WebView
import com.github.salomonbrys.kotson.isNotEmpty
import com.github.salomonbrys.kotson.put
import com.github.salomonbrys.kotson.set
import com.github.salomonbrys.kotson.toJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import network.o3.o3wallet.Account
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.toHex

class DappBrowserJSInterfaceV2(private val context: Context, private val webView: WebView) {


    fun processMessage(message: DappMessage) {
        listOf("getProvider", "getNetworks", "getAccount",
                "getBalance", "getStorage", "invokeRead", "invoke", "send")
        when (message.command.toLowerCase()) {
            "getprovider" -> handleGetProvider(message)
            "getnetworks" -> handleGetNetworks(message)
            "getaccount" -> handleGetAccount(message)
            "getbalance" -> handleGetBalance(message)
            "getstorage" -> handleGetStorage(message)
            "invokeread" -> handleInvokeRead(message)
            "invoke" -> handleInvoke(message)
            "send"-> handleSend(message)
            else -> return
        }
    }

    fun handleGetProvider(message: DappMessage) {
        val response = NeoDappProtocol.GetProviderResponse(name = "o3", version = "v1",
                website = "https://o3.network", compatibility = listOf("NEP-dapi"))
        callback(message, response)
    }

    fun handleGetNetworks(message: DappMessage) {
        val response: GetNetworkResponse = listOf("MainNet", "TestNet", "PrivateNet")
        callback(message, response)
    }

    fun handleGetAccount(message: DappMessage) {
        val response = NeoDappProtocol.GetAccountResponse(address = Account.getWallet().address!!,
                publicKey = Account.getWallet().publicKey.toHex())
        callback(message, response)
    }

    fun handleGetBalance(message: DappMessage) {}
    fun handleGetStorage(message: DappMessage) {}
    fun handleInvokeRead(message: DappMessage) {}
    fun handleInvoke(message: DappMessage) {}
    fun handleSend(message: DappMessage) {}

    fun callback(message: DappMessage, data: Any) {
        val mainHandler = Handler(O3Wallet.appContext!!.mainLooper)

        val json = Gson().toJson(message).toJson().asJsonObject
        json["data"] = Gson().toJson(data)

        val myRunnable = Runnable {
            val script = "_o3dapi.receiveMessage(" + json.toString() + ")"
            webView.evaluateJavascript(script) { value ->
                System.out.println(value)
            }
        }
        mainHandler.post(myRunnable)
    }

    private fun verifyPassCodeAndSign() {
        val mKeyguardManager = webView.context!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                (webView.context as Activity).startActivityForResult(intent, 1)
            }
        }
    }
}*/
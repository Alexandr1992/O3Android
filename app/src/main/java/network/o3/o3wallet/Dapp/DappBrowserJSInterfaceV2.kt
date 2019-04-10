package network.o3.o3wallet.Dapp

import android.webkit.JavascriptInterface
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.AnalyticsService
import network.o3.o3wallet.PersistentStore
import org.json.JSONObject

class DappBrowserJSInterfaceV2(private val vm: DAPPViewModel) {
    enum class EVENT {
        READY,
        ACCOUNT_CHANGED,
        CONNECTED,
        DISCONNECTED,
        NETWORK_CHANGED
    }

    @JavascriptInterface
    fun messageHandler(jsonString: String) {
        val gson = Gson()
        val message = gson.fromJson<DappMessage>(jsonString)
        listOf("getProvider", "getNetworks", "getAccount",
                "getBalance", "getStorage", "invokeRead", "invoke", "send")
        val attrs = mapOf("url" to vm.url,
                "blockchain" to "NEO",
                "net" to PersistentStore.getNetworkType(),
                "method" to message.command)
        AnalyticsService.DAPI.logDapiMethodCall(JSONObject(attrs))
        when (message.command.toLowerCase()) {
            "getprovider" -> vm.handleGetProvider(message)
            "disconnect" -> vm.handleDisconnect(message)
            "getnetworks" -> vm.handleGetNetworks(message)
            "getbalance" -> vm.handleGetBalance(message)
            "getstorage" -> vm.handleGetStorage(message)
            "invokeread" -> vm.handleInvokeRead(message)

            "getaccount" -> vm.requestAuthorizeWalletInfo(message)
            "invoke" -> vm.requestAuthorizeInvoke(message)
            "send"-> vm.requestAuthorizeSend(message)
            else -> return
        }
    }
}
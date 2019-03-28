package network.o3.o3wallet.Dapp

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.amplitude.api.Amplitude
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import neoutils.Neoutils
import neoutils.Wallet
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import org.json.JSONObject
import java.util.*

data class O3Message(var command: String, var data: JsonElement)
class DappBrowserJSInterface(private val context: Context, private val webView: WebView) {
    var userAuthenticatedApp = false
    var sessionId: String? = null
    var pendingTransaction: JsonObject? = null

    var wallet: Wallet = Account.getWallet()

    fun currentAccount(): JsonObject {
        val json = JsonObject()
        json.put("address" to wallet.address)
        json.put("publicKey" to wallet.publicKey.toHex())
        return json
    }

    // commands don't require authentication
    fun handleInit(message: O3Message) {
        callback(message.command, JsonObject(), null, false)
    }

    fun finishConnectionToO3() {
        sessionId = UUID.randomUUID().toString()
        userAuthenticatedApp = true
        //Amplitude.getInstance().logEvent("Connected_To_Switcheo")
        callback("requestToConnect", currentAccount(), null, true)
    }

    fun handleRequestToConnect(message: O3Message) {
        val appName = message.data.asString
        val authenticateMessage = webView.context.resources.getString(R.string.DAPP_connection_request, appName)
        context.alert(authenticateMessage) {
            yesButton {
                verifyPassCodeAndSign()
            }
            noButton {
                callback(message.command, JsonObject(), "User rejected connection request", false)
            }
        }.show()
    }

    fun handleVerifySession(message: O3Message) {
        val session = message.data.asString
        if (session != sessionId) {
            callback(message.command, JsonObject(), "Invalid Session", false)
        } else {
            callback(message.command, currentAccount(), null, true)
        }
    }

    // commands require authentication
    fun handlePlatformRequest(message: O3Message) {
        val json = JsonObject()
        json.put("platform" to "android")
        json.put("version" to O3Wallet.version)
        callback(message.command, json, null, false)
    }

    fun handleConnectedNodeRequest(message: O3Message) {
        val json = JsonObject()
        json.put("network" to PersistentStore.getNetworkType())
        json.put("node" to PersistentStore.getNodeURL())
        callback(message.command, json, null, true)
    }

    fun handleAccountsRequest(message: O3Message) {
        val blockchains = jsonObject("neo" to currentAccount())
        val dic = jsonObject( "accounts" to jsonArray(blockchains))
        callback(message.command, dic, null, true)
    }

    fun handleBalancesRequest(message: O3Message) {
        O3PlatformClient().getTransferableAssets(wallet.address) {
            if(it.second != null) {
                callback(message.command, JsonObject(), it.second!!.localizedMessage, true)
            } else {
                val balancesJson = JsonObject()
                val assetsArrayList = arrayListOf<JsonObject>()
                for (asset in it.first!!.assets) {
                    val jsonObject = jsonObject(
                            "name" to asset.name,
                            "symbol" to asset.symbol,
                            "decimals" to asset.decimals,
                            "value" to asset.value.toPlainString(),
                            "id" to asset.id
                            )
                    assetsArrayList.add(jsonObject)
                }
                for (token in it.first!!.tokens) {
                    val jsonObject = jsonObject(
                            "name" to token.name,
                            "symbol" to token.symbol,
                            "decimals" to token.decimals,
                            "value" to token.value.toPlainString(),
                            "id" to token.id
                    )
                    assetsArrayList.add(jsonObject)
                }
                val balances = JsonObject()
                balances.put("balances" to assetsArrayList.toJsonArray())
                balances.put("account" to currentAccount())
                callback(message.command, balances, null, true)
            }
        }
    }

    fun handleAppIsAvailableRequest(message: O3Message) {
        val json = JsonObject()
        json.put("isAppAvailable" to true)
        callback(message.command, json, null, true)
    }

    fun handleDeviceInfoRequest(message: O3Message) {
        val json = JsonObject()
        json.put("device" to Build.MANUFACTURER + " " + Build.MODEL)
        callback(message.command, json, null, true)
    }

    fun parseAndAnalyze(unsignedJson: String) {
        unsignedJson.removeSuffix("0000")
        val index = unsignedJson.indexOf("7b")
        if (index != -1 && index % 2 == 0) {
            val unsignedJsonSubstring = unsignedJson.substring(index)
            try {
                Amplitude.getInstance().logEvent("Switcheo_Signed_JSON", JSONObject(String(unsignedJsonSubstring.hexStringToByteArray())))
            } catch (e: Exception) {

            }

        }
    }

    fun handleRequestToSign(message: O3Message) {
        val unsignedTx = message.data.asString
        // a raw json request coming from Switcheo
        if (unsignedTx.endsWith("0000")) {
            parseAndAnalyze(unsignedTx)
        } else {
            Amplitude.getInstance().logEvent("Switcheo_Signed_Raw_TX")
        }
        if (unsignedTx.length < 2) {
            callback(message.command, JsonObject(), "invalid unsigned raw transaction", true)
            return
        }


        val unsignedHex = unsignedTx.hexStringToByteArray()
        val authenticateMessage = webView.context.resources.getString(R.string.DAPP_transaction_signing_request)
        context.alert(authenticateMessage) {
            yesButton {
                try {
                    val signed = Neoutils.sign(unsignedHex, wallet.privateKey.toHex())
                    val signedTxJson = jsonObject (
                            "signatureData" to signed.toHex(),
                            "account" to currentAccount()
                    )
                    callback("requestToSign", signedTxJson, null, true)
                } catch (e: Exception) {
                    callback("requestToSign", JsonObject(), e.localizedMessage, true)
                }
            }
            noButton {
                callback(message.command, JsonObject(), "User rejected transaction signature request", false)
            }
        }.show()
    }

    @JavascriptInterface
    fun messageHandler(jsonString: String) {
        val gson = Gson()
        val message = gson.fromJson<O3Message>(jsonString)
        //Toast.makeText(context, message!!.command, Toast.LENGTH_LONG).show()
        val availableCommands = arrayOf("init", "requestToConnect", "getPlatform", "getAccounts",
                "getBalances", "isAppAvailable", "requestToSign", "getDeviceInfo", "verifySession", "getLastConnectedNode")
        if (!availableCommands.contains(message.command)) {
            callback(message.command, JsonObject(), "Unsupported command", false)
            return
        }

        //No login necessary
        when(message.command) {
            "init" -> {
                handleInit(message)
                return
            }
            "requestToConnect" -> {
                handleRequestToConnect(message)
                return
            }
            "verifySession" -> {
                handleVerifySession(message)
                return
            }
        }

        if (!userAuthenticatedApp) {
            callback(message.command, JsonObject(), "User Has Not Authenticated this request", false)
            return
        }

        //login necessary
        when(message.command) {
            "getPlatform" -> handlePlatformRequest(message)
            "getLastConnectedNode" -> handleConnectedNodeRequest(message)
            "getAccounts" -> handleAccountsRequest(message)
            "getBalances" -> handleBalancesRequest(message)
            "isAppAvailable" -> handleAppIsAvailableRequest(message)
            "getDeviceInfo" -> handleDeviceInfoRequest(message)
            "requestToSign" -> handleRequestToSign(message)
        }
    }

    fun callback(command: String, data: JsonObject, errorMessage: String?, withSession: Boolean) {
        val mainHandler = Handler(O3Wallet.appContext!!.mainLooper)
        val jsonObject = JsonObject()
        jsonObject.put("command" to command)

        if (data.isNotEmpty()) {
            jsonObject.put("data" to data)
        }

        val errorJson = JsonObject()
        if (errorMessage != null) {
            errorJson.put("message" to errorMessage)
            jsonObject.put("error" to errorJson)
        }

        if (withSession) {
            jsonObject.put("sessionID" to sessionId)
        }

        val myRunnable = Runnable {
            val script = "o3.callback(" + jsonObject.toString() + ")"
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
}
package network.o3.o3wallet.Dapp

import android.webkit.JavascriptInterface
import android.widget.Toast
import android.content.Context
import android.os.Build
import android.webkit.WebView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.github.salomonbrys.kotson.*
import android.os.Handler
import com.google.gson.JsonElement
import neoutils.Neoutils
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import java.lang.Exception
import java.util.*

data class O3Message(var command: String, var data: JsonElement)
class DappBrowserJSInterface(private val context: Context, private val webView: WebView) {
    var userAuthenticatedApp = false
    var sessionId: String? = null

    fun currentAccount(): JsonObject {
        val json = JsonObject()
        json.put("address" to Account.getWallet()!!.address)
        json.put("publicKey" to Account.getWallet()!!.publicKey.toHex())
        return json
    }

    // commands don't require authentication
    fun handleInit(message: O3Message) {
        callback(message.command, JsonObject(), null, false)
    }

    fun handleRequestToConnect(message: O3Message) {
        val appName = message.data.toString()
        val authenticateMessage = webView.context.resources.getString(R.string.DAPP_connection_request, appName)
        context.alert(authenticateMessage) {
            yesButton {
                sessionId = UUID.randomUUID().toString()
                userAuthenticatedApp = true
                callback(message.command, currentAccount(), null, true)
            }
            noButton {
                callback(message.command, JsonObject(), "User rejected connection request", false)
            }
        }.show()
    }

    fun handleVerifySession(message: O3Message) {
        val session = message.data.toString()
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

    fun handleAccountsRequest(message: O3Message) {
        val blockchains = jsonObject("neo" to currentAccount())
        val dic = jsonObject( "accounts" to jsonArray(blockchains))
        callback(message.command, dic, null, true)
    }

    fun handleBalancesRequest(message: O3Message) {
        O3PlatformClient().getTransferableAssets(Account.getWallet()!!.address) {
            if(it.second != null) {
                callback(message.command, JsonObject(), it.second!!.localizedMessage, true)
            } else {
                val balancesJson = JsonObject()
                val assetsArrayList = arrayListOf<TransferableAsset>()
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

    fun handleRequestToSign(message: O3Message) {
        val unsignedTx = message.data.toString()
        if (unsignedTx.length < 2) {
            callback("requestToSign", JsonObject(), "invalid unsigned raw transaction", true)
        }

        val unsignedHex = unsignedTx.hexStringToByteArray()
        try {
            val signed = Neoutils.sign(unsignedHex, Account.getWallet()!!.privateKey.toHex())
        } catch (e: Exception) {
            callback("requestToSign", JsonObject(), e.localizedMessage, true)
        }


    }

    @JavascriptInterface
    fun messageHandler(jsonString: String) {
        val gson = Gson()
        val message = gson.fromJson<O3Message>(jsonString)
        //Toast.makeText(context, message!!.command, Toast.LENGTH_LONG).show()
        val availableCommands = arrayOf("init", "requestToConnect", "getPlatform", "getAccounts",
                "getBalances", "isAppAvailable", "requestToSign", "getDeviceInfo", "verifySession")
        if (!availableCommands.contains(message.command)) {
            //Unsupported Commands
        }

        //No login necessary
        when(message.command) {
            "init" -> handleInit(message)
            "requestToConnect" -> handleRequestToConnect(message)
            "verifySession" -> handleVerifySession(message)
        }

        if (!userAuthenticatedApp) {
            //handle user not being authenticated
            return
        }

        //login necessary
        when(message.command) {
            "getPlatform" -> handlePlatformRequest(message)
            "getAccounts" -> handleAccountsRequest(message)
            "getBalances" -> handleBalancesRequest(message)
            "isAppAvailable" -> handleAppIsAvailableRequest(message)
            "getDeviceInfo" -> handleDeviceInfoRequest(message)
            "requestToSign" -> handleRequestToSign(message)
        }
    }

    private fun callback(command: String, data: JsonObject, errorMessage: String?, withSession: Boolean) {
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
}
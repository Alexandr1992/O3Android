package network.o3.o3wallet.Dapp

import android.app.Activity
import android.app.KeyguardManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.github.salomonbrys.kotson.*
import android.os.Handler
import android.support.v4.content.ContextCompat.getSystemService
import com.google.gson.JsonElement
import neoutils.Neoutils
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Onboarding.PasscodeRequestActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import java.lang.Exception
import java.util.*

data class O3Message(var command: String, var data: JsonElement)
class DappBrowserJSInterface(private val context: Context, private val webView: WebView) {
    var userAuthenticatedApp = false
    var sessionId: String? = null
    var pendingTransaction: JsonObject? = null

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
        val appName = message.data.asString
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

    fun executePendingTransaction() {
        callback("requestToSign", pendingTransaction!!, null, true)
        pendingTransaction = null
    }

    fun clearPendingTransaction() {
        pendingTransaction = null
    }

    fun handleRequestToSign(message: O3Message) {
        val unsignedTx = message.data.asString
        if (unsignedTx.length < 2) {
            callback(message.command, JsonObject(), "invalid unsigned raw transaction", true)
        }

        val unsignedHex = unsignedTx.hexStringToByteArray()
        val authenticateMessage = webView.context.resources.getString(R.string.DAPP_transaction_signing_request)
        context.alert(authenticateMessage) {
            yesButton {
                try {
                    val signed = Neoutils.sign(unsignedHex, Account.getWallet()!!.privateKey.toHex())
                    val signedTxJson = jsonObject (
                            "signatureData" to signed.toHex(),
                            "account" to currentAccount()
                    )
                    pendingTransaction = signedTxJson
                    verifyPassCodeAndSign()

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
                "getBalances", "isAppAvailable", "requestToSign", "getDeviceInfo", "verifySession")
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
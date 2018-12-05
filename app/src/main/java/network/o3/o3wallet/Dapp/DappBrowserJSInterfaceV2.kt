package network.o3.o3wallet.Dapp

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import neoutils.Wallet
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.NEO.TransactionAttribute
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.toHex
import java.lang.Exception
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

class DappBrowserJSInterfaceV2(private val context: Context, private val webView: WebView,
                               private var dappExposedWallet: Wallet, private var dappExposedWalletName: String) {

    @JavascriptInterface
    fun messageHandler(jsonString: String) {
        val gson = Gson()
        val message = gson.fromJson<DappMessage>(jsonString)
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
            "send"-> authorizeSend(message)
            else -> return
        }
    }

    fun getDappExposedWallet(): Wallet {
        return dappExposedWallet
    }

    fun getDappExposedWalletName(): String {
        return dappExposedWalletName
    }

    fun setDappExposedWallet(wallet: Wallet, name: String) {
        dappExposedWallet = wallet
        dappExposedWalletName = name
        val intent = Intent("update-exposed-dapp-wallet")
        LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
    }

    fun handleGetProvider(message: DappMessage) {
        val response = NeoDappProtocol.GetProviderResponse(name = "o3", version = "v1",
                website = "https://o3.network", compatibility = listOf("NEP-dapi"))
        callback(message, response)
    }

    fun handleGetNetworks(message: DappMessage) {
        val response: NeoDappProtocol.GetNetworkResponse
        if (PersistentStore.getNetworkType() == "Test") {
            response = NeoDappProtocol.GetNetworkResponse(listOf("TestNet"))
        } else if (PersistentStore.getNetworkType() == "Main"){
            response = NeoDappProtocol.GetNetworkResponse(listOf("MainNet"))
        } else {
            response = NeoDappProtocol.GetNetworkResponse(listOf("PrivateNet"))
        }

        callback(message, response)
    }

    fun authorizedAccountCredentials(message: DappMessage) {
        val response = NeoDappProtocol.GetAccountResponse(address = dappExposedWallet.address,
                publicKey = dappExposedWallet.publicKey.toHex())
        callback(message, response)
    }

    fun rejectedAccountCredentials(message: DappMessage) {
        val response = NeoDappProtocol.GetAccountResponse("", "")
        callback(message, response)
    }


    fun handleGetAccount(message: DappMessage) {
        (webView.context as DAppBrowserActivityV2).authorizeWalletInfo(message)
    }

    fun authorizeSend(message: DappMessage) {
        (webView.context as DAppBrowserActivityV2).authorizeSend(message)
    }

    fun handleGetBalance(message: DappMessage) {
        val inputs =  Gson().fromJson<NeoDappProtocol.GetBalanceRequest>(Gson().toJson(message.data)).params
        val latch = CountDownLatch(inputs.count() * 2)

        val jsonResponse = jsonObject()
        val jsonUTXOS = jsonObject()
        for (input in inputs) {
            O3PlatformClient().getUTXOS(input.address) {
                if (it.second == null) {
                    jsonUTXOS.add(input.address, Gson().toJsonTree(it.first?.data))
                    latch.countDown()
                } else {
                    latch.countDown()
                }
            }


            O3PlatformClient().getTransferableAssets(input.address) {
                var balances = mutableListOf<NeoDappProtocol.GetBalanceResponseElement>()
                if (it.second == null) {
                    for (asset in it.first?.assets ?: arrayListOf()) {
                        if (input.assets.find {it.toLowerCase() ==  asset.symbol.toLowerCase() } != null) {
                            balances.add(NeoDappProtocol.GetBalanceResponseElement(
                                    amount = asset.value.toPlainString(), scriptHash = asset.id,
                                    symbol = asset.symbol, unspent = listOf()))
                        }
                    }

                    for(asset in it.first?.tokens ?: arrayListOf()) {
                        if (input.assets.find {it.toLowerCase() ==  asset.symbol.toLowerCase() } != null) {
                            balances.add(NeoDappProtocol.GetBalanceResponseElement(
                                    amount = asset.value.toPlainString(), scriptHash = asset.id,
                                    symbol = asset.symbol, unspent = listOf()))
                        }
                    }

                    jsonResponse.add(input.address, Gson().toJsonTree(balances))
                    latch.countDown()
                } else {
                    latch.countDown()
                }
            }
        }

        latch.await()
        for (input in inputs) {
            val gasUTXOS = mutableListOf<JsonObject>()
            val neoUTXOS = mutableListOf<JsonObject>()
            for (utxo in jsonUTXOS[input.address].asJsonArray ) {
                if (utxo["asset"].asString == "0x602c79718b16e442de58778e148d0b1084e3b2dffd5de6b7b16cee7969282de7") {
                    gasUTXOS.add(utxo.asJsonObject)
                } else {
                    neoUTXOS.add(utxo.asJsonObject)
                }
            }

            for (asset in jsonResponse[input.address].asJsonArray) {
                if (asset.asJsonObject["symbol"].asString.toLowerCase() == "neo") {
                    asset.asJsonObject["unspent"] = neoUTXOS.toJsonArray()
                } else {
                    asset.asJsonObject["unspent"] = gasUTXOS.toJsonArray()
                }
            }
        }
        callback(message, jsonResponse)
    }

    fun sendNativeNeoAsset(message: DappMessage, sendRequest: NeoDappProtocol.SendRequest): Boolean {
        var success = false
        var toSendAsset: NeoNodeRPC.Asset? = null
        toSendAsset = if (sendRequest.asset.toLowerCase() == "neo") {
            NeoNodeRPC.Asset.NEO
        } else {
            NeoNodeRPC.Asset.GAS
        }

        val attributes = arrayOf(TransactionAttribute().dapiRemarkAttribute(sendRequest.remark))

        val recipientAddress = sendRequest.toAddress
        val amount = sendRequest.amount
        val node = PersistentStore.getNodeURL()
        val latch = CountDownLatch(1)

        var fee = BigDecimal.ZERO
        try {
            fee = BigDecimal(sendRequest.fee)
        } catch (e : Exception) {
            success = false
        }

        NeoNodeRPC(node).sendNativeAssetTransaction(dappExposedWallet, toSendAsset, BigDecimal(amount),
                recipientAddress, attributes, fee) {
            if (it.first != null) {
                success = true
            }

            callback(message, NeoDappProtocol.SendResponse(txid = it.first?: "", nodeUrl = node))
                latch.countDown()
        }
        latch.await()
        return success
    }

    fun sendTokenNeoAsset(message: DappMessage, sendRequest: NeoDappProtocol.SendRequest): Boolean {
        sendRequest.toAddress = dappExposedWallet.address
        var success = false

        var fee = BigDecimal.ZERO
        try {
            fee = BigDecimal(sendRequest.fee)
        } catch (e : Exception) { }

        val attributes = arrayOf(TransactionAttribute().dapiRemarkAttribute(sendRequest.remark))

        val latch = CountDownLatch(1)
        O3PlatformClient().getTransferableAssets(dappExposedWallet.address) {
            var token = it.first?.assets?.find { it.symbol.toLowerCase() == sendRequest.asset.toLowerCase() }
            if (fee == BigDecimal.ZERO) {
                NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(dappExposedWallet, null, token!!.id,
                        dappExposedWallet.address, sendRequest.toAddress,
                        BigDecimal(sendRequest.amount), token?.decimals ?: 8, BigDecimal.ZERO, attributes) {
                    if (it.first != null) {
                        success = true
                    }

                    callback(message, NeoDappProtocol.SendResponse(txid = it.first?: "", nodeUrl = PersistentStore.getNodeURL()))
                    latch.countDown()
                }
            } else {
                O3PlatformClient().getUTXOS(dappExposedWallet.address) {
                    var assets = it.first
                    var error = it.second
                    if (error != null) {
                        latch.countDown()
                    } else {
                        NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(dappExposedWallet, assets, sendRequest.asset, dappExposedWallet.address,
                                sendRequest.toAddress, BigDecimal(sendRequest.amount),
                                token?.decimals ?: 8, fee, attributes) {
                            if (it.first != null) {
                                success = true
                            }
                            callback(message, NeoDappProtocol.SendResponse(txid = it.first?: "", nodeUrl = PersistentStore.getNodeURL()))
                            latch.countDown()
                        }
                    }
                }
            }
        }
        latch.await()
        return success
    }

    fun handleSend(message: DappMessage): Boolean {
        val sendRequest = Gson().fromJson<NeoDappProtocol.SendRequest>(Gson().toJson(message.data))
        if (sendRequest.asset == "NEO" || sendRequest.asset == "GAS") {
            return sendNativeNeoAsset(message, sendRequest)
        } else {
            return sendTokenNeoAsset(message, sendRequest)
        }
    }

    fun handleGetStorage(message: DappMessage) {
        val latch = CountDownLatch(1)
        var storageRequest = Gson().fromJson<NeoDappProtocol.GetStorageRequest>(Gson().toJson(message.data))
        NeoNodeRPC(PersistentStore.getNodeURL()).getStorage(storageRequest.scriptHash, storageRequest.key) {
            callback(message, jsonObject("result" to it.first))
            latch.countDown()
        }
        latch.await()

    }
    fun handleInvokeRead(message: DappMessage) {}
    fun handleInvoke(message: DappMessage) {}

    fun callback(message: DappMessage, data: Any) {
        val mainHandler = Handler(O3Wallet.appContext!!.mainLooper)
        val json = Gson().typedToJsonTree(message).asJsonObject
        json["data"] = Gson().typedToJsonTree(data)

        val myRunnable = Runnable {
            val script = "_o3dapi.receiveMessage(" + json.toString() + ")"
            webView.evaluateJavascript(script) { value ->
                Log.d("javascript", value)
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
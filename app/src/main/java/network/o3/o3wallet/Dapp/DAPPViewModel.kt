package network.o3.o3wallet.Dapp

import android.net.Uri
import android.webkit.ValueCallback
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import neoutils.Wallet
import network.o3.o3wallet.*
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.NEO.TransactionAttribute
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import org.json.JSONObject
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

class DAPPViewModel(url: String): ViewModel() {
    //browser settings
    var url = url
    var allowSearch = false
    var legacy = false


    //dapp connections
    lateinit var dapiInterface: DappBrowserJSInterfaceV2
    var legacyInterface: DappBrowserJSInterface? = null

    //dapp active wallets
    var walletForSession: Wallet = Account.getWallet()
    var walletForSessionName: String = NEP6.getFromFileSystem().getDefaultAccount().label

    var connectionRequest: MutableLiveData<DappMessage>? = null

    //requests that require user auth and an additional bottom sheet to display
    var walletInfoRequest: MutableLiveData<DappMessage> = MutableLiveData()
    var sendRequest: MutableLiveData<DappMessage> = MutableLiveData()
    var invokeRequest: MutableLiveData<DappMessage> = MutableLiveData()
    var publicKeyRequest: MutableLiveData<DappMessage> = MutableLiveData()

    var lockStatus: MutableLiveData<Boolean> = MutableLiveData()

    var jsResponse: MutableLiveData<String> = MutableLiveData()

    //for uploading files
    var mUploadMessage: ValueCallback<Array<Uri>>? = null
    val FILECHOOSER_RESULTCODE = 101
    var photoURI: Uri? = null


    //maybe consolidate these into one request auth message


    fun getDappResponse(): LiveData<String> {
        return jsResponse
    }

    fun setWalletToExpose(wallet: Wallet, name: String) {
        walletForSession =  wallet
        walletForSessionName = name
        fireEvent(DappBrowserJSInterfaceV2.EVENT.ACCOUNT_CHANGED)
    }

    fun getLockStatus(): LiveData<Boolean> {
        return lockStatus
    }

    fun setLockStatus(isLocked: Boolean) {
        if (isLocked == true) {
            fireEvent(DappBrowserJSInterfaceV2.EVENT.DISCONNECTED)
        }
        lockStatus.postValue(isLocked)
    }


    // Authorization requests delegate the UI to show the relevant
    // bottom sheets in order to fullfill the request
    // The response will then filter through js response like all other
    // non auth methods
    fun getWalletInfo(): LiveData<DappMessage> {
        return walletInfoRequest
    }

    fun getInvokeRequest(): LiveData<DappMessage> {
        return invokeRequest
    }

    fun getSendRequest(): LiveData<DappMessage> {
        return sendRequest
    }

    fun requestAuthorizeWalletInfo(message: DappMessage){
        walletInfoRequest.postValue(message)
    }

    fun requestAuthorizeInvoke(message: DappMessage) {
        invokeRequest.postValue(message)
    }

    fun requestAuthorizeSend(message: DappMessage) {
        sendRequest.postValue(message)
    }

    fun setDappResponse(message: DappMessage, data: Any) {
        val json = Gson().typedToJsonTree(message).asJsonObject
        if (Gson().typedToJsonTree(data).isJsonObject && Gson().typedToJsonTree(data).asJsonObject.has("error")) {
            json["error"] = Gson().typedToJsonTree(data)["error"]
        } else {
            json["data"] = Gson().typedToJsonTree(data)
        }

        jsResponse.postValue(json.toString())
    }

    //handlers for non-authenticated messages
    fun handleGetProvider(message: DappMessage) {
        val theme = if (PersistentStore.getTheme() == "Light") "Light Mode" else "Dark Mode"
        val currency = PersistentStore.getCurrency()

        val response = NeoDappProtocol.GetProviderResponse(name = "o3-Android", version = "v2",
                website = "https://o3.network", compatibility = listOf("NEP-dapi"),
                extra = jsonObject("theme" to theme, "currency" to currency))
        setDappResponse(message, response)
    }

    fun handleGetPublicKey(message: DappMessage) {
        setDappResponse(message, jsonObject("address" to walletForSession.address, "publicKey" to walletForSession.publicKey.toHex()))
    }

    fun handleGetNetworks(message: DappMessage) {
        val response: NeoDappProtocol.GetNetworkResponse
        if (PersistentStore.getNetworkType() == "Test") {
            response = NeoDappProtocol.GetNetworkResponse(listOf("TestNet"), "TestNet")
        } else if (PersistentStore.getNetworkType() == "Main"){
            response = NeoDappProtocol.GetNetworkResponse(listOf("MainNet"), "MainNet")
        } else {
            response = NeoDappProtocol.GetNetworkResponse(listOf(), "")
        }
        setDappResponse(message, response)
    }

    fun handleGetBalance(message: DappMessage) {
        val inputs =  Gson().fromJson<NeoDappProtocol.GetBalanceRequest>(Gson().toJson(message.data)).params
        val latch = CountDownLatch(inputs.count() * 2)

        val jsonResponse = jsonObject()
        val jsonUTXOS = jsonObject()
        for (input in inputs) {

            if (input.fetchUTXO == true) {
                O3PlatformClient().getUTXOS(input.address) {
                    if (it.second == null) {
                        jsonUTXOS.add(input.address, Gson().toJsonTree(it.first?.data))
                        latch.countDown()
                    } else {
                        latch.countDown()
                    }
                }
            } else {
                latch.countDown()
            }

            O3PlatformClient().getTransferableAssets(input.address) {
                var balances = mutableListOf<NeoDappProtocol.GetBalanceResponseElement>()
                if (it.second == null) {
                    for (asset in it.first?.assets ?: arrayListOf()) {
                        if (input.assets == null) {
                            balances.add(NeoDappProtocol.GetBalanceResponseElement(
                                    amount = asset.value.toPlainString(), scriptHash = asset.id,
                                    symbol = asset.symbol, unspent = null))
                        } else if (input.assets.find {it.toLowerCase() == asset.symbol.toLowerCase() } != null) {
                            balances.add(NeoDappProtocol.GetBalanceResponseElement(
                                    amount = asset.value.toPlainString(), scriptHash = asset.id,
                                    symbol = asset.symbol, unspent = null))
                        }
                    }

                    for(asset in it.first?.tokens ?: arrayListOf()) {
                        if (input.assets.find {it.toLowerCase() ==  asset.symbol.toLowerCase() } != null) {
                            balances.add(NeoDappProtocol.GetBalanceResponseElement(
                                    amount = asset.value.toPlainString(), scriptHash = asset.id,
                                    symbol = asset.symbol, unspent = null))
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
            if (input.fetchUTXO == true) {
                val gasUTXOS = mutableListOf<JsonObject>()
                val neoUTXOS = mutableListOf<JsonObject>()
                for (utxo in jsonUTXOS[input.address].asJsonArray) {
                    if (utxo["asset"].asString == "0x602c79718b16e442de58778e148d0b1084e3b2dffd5de6b7b16cee7969282de7") {
                        gasUTXOS.add(utxo.asJsonObject)
                    } else {
                        neoUTXOS.add(utxo.asJsonObject)
                    }
                }

                if (jsonResponse[input.address] != null) {
                    for (asset in jsonResponse[input.address].asJsonArray) {
                        if (asset.asJsonObject["symbol"].asString.toLowerCase() == "neo") {
                            asset.asJsonObject["unspent"] = neoUTXOS.toJsonArray()
                        } else {
                            asset.asJsonObject["unspent"] = gasUTXOS.toJsonArray()
                        }
                    }
                }
            }
        }
        setDappResponse(message, jsonResponse)
    }

    fun handleGetStorage(message: DappMessage) {
        val latch = CountDownLatch(1)
        var storageRequest = Gson().fromJson<NeoDappProtocol.GetStorageRequest>(Gson().toJson(message.data))
        NeoNodeRPC(PersistentStore.getNodeURL()).getStorage(storageRequest.scriptHash, storageRequest.key) {
            if (it.second != null) {
                setDappResponse(message, jsonObject("result" to "RPC_ERROR"))
            } else {
                setDappResponse(message, jsonObject("result" to it.first))
            }

            latch.countDown()
        }
        latch.await()
    }

    fun handleInvokeRead(message: DappMessage) {
        var invokeReadRequest = Gson().fromJson<NeoDappProtocol.InvokeReadRequest>(Gson().toJson(message.data))
        var network = invokeReadRequest.network ?: "MainNet"
        if (network == "") {
            network = "MainNet"
        }
        if (!network.contains(PersistentStore.getNetworkType())) {
            setDappResponse(message, jsonObject("error" to "CONNECTION_REFUSED"))
            return
        }
        val latch = CountDownLatch(1)
        NeoNodeRPC(PersistentStore.getNodeURL()).readOnlyInvoke(invokeReadRequest.scriptHash,
                invokeReadRequest.operation, invokeReadRequest.args) {
            if (it.second == null) {
                setDappResponse(message, jsonObject("result" to it.first))
            } else {
                setDappResponse(message, jsonObject("error" to "RPC_ERROR"))
            }
            latch.countDown()
        }
        latch.await()
    }

    fun handleInvoke(message: DappMessage, authorized: Boolean): Boolean {
        if (authorized) {
            var invokeRequest = Gson().fromJson<NeoDappProtocol.InvokeRequest>(Gson().toJson(message.data))
            var success = false
            var fee = BigDecimal.ZERO
            try {
                fee = BigDecimal(invokeRequest.fee)
            } catch (e : Exception) {
            }

            val latch = CountDownLatch(1)
            O3PlatformClient().getUTXOS(walletForSession!!.address) {
                var assets = it.first
                var error = it.second
                if (error != null) {
                    latch.countDown()
                } else {
                    NeoNodeRPC(PersistentStore.getNodeURL()).genericWriteInvoke(walletForSession!!,
                            assets, invokeRequest.scriptHash, invokeRequest.operation, invokeRequest.args ?: listOf(),
                            fee, arrayOf(), invokeRequest.attachedAssets) {
                        if (it.second == null) {
                            success = true
                            val obj = jsonObject("txid" to it.first!!.toLowerCase(),
                                    "nodeUrl" to PersistentStore.getNodeURL())
                            setDappResponse(message, obj)
                        } else {
                            setDappResponse(message, jsonObject("error" to "RPC_ERROR"))
                        }
                        val attrs = mapOf("url" to url,
                                "blockchain" to "NEO",
                                "net" to PersistentStore.getNetworkType(),
                                "method" to "invoke",
                                "success" to success)
                        AnalyticsService.DAPI.logDapiTxAccepted(JSONObject(attrs))
                        latch.countDown()
                    }
                }
            }

            latch.await()
            return success
        }
        return false
    }

    fun sendNativeNeoAsset(message: DappMessage, sendRequest: NeoDappProtocol.SendRequest): Boolean {
        var success = false
        var toSendAsset: NeoNodeRPC.Asset? = null
        toSendAsset = if (sendRequest.asset.toLowerCase() == "neo") {
            NeoNodeRPC.Asset.NEO
        } else {
            NeoNodeRPC.Asset.GAS
        }
        val attributes = if (sendRequest.remark != null) {
            arrayOf(TransactionAttribute().dapiRemarkAttribute(sendRequest.remark))
        } else {
            arrayOf()
        }

        val recipientAddress = sendRequest.toAddress
        val amount = sendRequest.amount
        val node = PersistentStore.getNodeURL()
        val latch = CountDownLatch(1)

        var fee = BigDecimal.ZERO
        try {
            fee = BigDecimal(sendRequest.fee)
        } catch (e : Exception) {
        }

        NeoNodeRPC(node).sendNativeAssetTransaction(walletForSession!!, toSendAsset, BigDecimal(amount),
                recipientAddress, attributes, fee) {
            if (it.first != null) {
                success = true
                setDappResponse(message, NeoDappProtocol.SendResponse(txid = it.first!!.toLowerCase(), nodeUrl = node))
            } else {
                setDappResponse(message, jsonObject("error" to "RPC_ERROR"))
            }
            val attrs = mapOf("url" to url,
                    "blockchain" to "NEO",
                    "net" to PersistentStore.getNetworkType(),
                    "method" to "send",
                    "success" to success)
            AnalyticsService.DAPI.logDapiTxAccepted(JSONObject(attrs))

            latch.countDown()

        }
        latch.await()
        return success
    }

    fun sendTokenNeoAsset(message: DappMessage, sendRequest: NeoDappProtocol.SendRequest): Boolean {
        sendRequest.toAddress = walletForSession!!.address
        var success = false

        var fee = BigDecimal.ZERO
        try {
            fee = BigDecimal(sendRequest.fee)
        } catch (e : Exception) { }

        val attributes = if (sendRequest.remark != null) {
            arrayOf(TransactionAttribute().dapiRemarkAttribute(sendRequest.remark))
        } else {
            arrayOf()
        }

        val latch = CountDownLatch(1)
        O3PlatformClient().getTransferableAssets(walletForSession!!.address) {
            var token = it.first?.assets?.find { it.symbol.toLowerCase() == sendRequest.asset.toLowerCase() }
            if (fee == BigDecimal.ZERO) {
                NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(walletForSession!!, null, token!!.id,
                        walletForSession!!.address, sendRequest.toAddress,
                        BigDecimal(sendRequest.amount), token?.decimals ?: 8, BigDecimal.ZERO, attributes) {
                    if (it.first != null) {
                        success = true
                        setDappResponse(message, NeoDappProtocol.SendResponse(txid = it.first!!.toLowerCase(), nodeUrl = PersistentStore.getNodeURL()))
                    } else {
                        setDappResponse(message, jsonObject("error" to "RPC_ERROR"))
                    }
                    val attrs = mapOf("url" to url,
                            "blockchain" to "NEO",
                            "net" to PersistentStore.getNetworkType(),
                            "method" to "send",
                            "success" to success)
                    AnalyticsService.DAPI.logDapiTxAccepted(JSONObject(attrs))
                    latch.countDown()
                }
            } else {
                O3PlatformClient().getUTXOS(walletForSession!!.address) {
                    var assets = it.first
                    var error = it.second
                    if (error != null) {
                        latch.countDown()
                    } else {
                        NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(walletForSession!!, assets, sendRequest.asset, walletForSession!!.address,
                                sendRequest.toAddress, BigDecimal(sendRequest.amount),
                                token?.decimals ?: 8, fee, attributes) {
                            if (it.first != null) {
                                success = true
                                setDappResponse(message, NeoDappProtocol.SendResponse(txid = it.first!!.toLowerCase(), nodeUrl = PersistentStore.getNodeURL()))
                            } else {
                                setDappResponse(message, jsonObject("error" to "RPC_ERROR"))
                            }
                            val attrs = mapOf("url" to url,
                                    "blockchain" to "NEO",
                                    "net" to PersistentStore.getNetworkType(),
                                    "method" to "send",
                                    "success" to success)
                            AnalyticsService.DAPI.logDapiTxAccepted(JSONObject(attrs))
                            latch.countDown()
                        }
                    }
                }
            }
        }
        latch.await()
        return success
    }

    fun handleSend(message: DappMessage, authorized: Boolean): Boolean {
        if (authorized) {
            val sendRequest = Gson().fromJson<NeoDappProtocol.SendRequest>(Gson().toJson(message.data))
            if (sendRequest.asset.toUpperCase()
                    == "NEO" || sendRequest.asset.toUpperCase() == "GAS") {
                return sendNativeNeoAsset(message, sendRequest)
            } else {
                return sendTokenNeoAsset(message, sendRequest)
            }
        }
        return false
    }

    fun handleWalletInfo(message: DappMessage, authorized: Boolean) {
        if (authorized) {
            val response = NeoDappProtocol.GetAccountResponse(address = walletForSession!!.address,
                    label = walletForSessionName)
            setDappResponse(message, response)
            setLockStatus(false)
        } else {
            setDappResponse(message, jsonObject("error" to "CONNECTION_REFUSED"))
        }
    }

    fun handleDisconnect(message: DappMessage) {
        fireEvent(DappBrowserJSInterfaceV2.EVENT.DISCONNECTED)
        setDappResponse(message, jsonObject())
    }


    private fun fireReady() {
        val response = jsonObject("command" to "event",
                "eventName" to "READY",
                "data" to jsonObject(),
                "blockchain" to "NEO",
                "platform" to "o3-dapi",
                "version" to "1")
        jsResponse.postValue(response.toString())
    }

    private fun fireConnected() {
        val response = jsonObject("command" to "event",
                "eventName" to "CONNECTED",
                "data" to jsonObject(),
                "blockchain" to "NEO",
                "platform" to "o3-dapi",
                "version" to "1"
        )
       jsResponse.postValue(response.toString())

    }

    private fun fireAccountChanged() {
        val response = jsonObject("command" to "event",
                "eventName" to "ACCOUNT_CHANGED",
                "data" to jsonObject("address" to walletForSession!!.address, "label" to walletForSessionName),
                "blockchain" to "NEO",
                "platform" to "o3-dapi",
                "version" to "1"
        )
        jsResponse.postValue(response.toString())
    }

    private fun fireDisconnected() {
        walletForSession = Account.getWallet()
        walletForSessionName = NEP6.getFromFileSystem().getDefaultAccount().label
        val response = jsonObject("command" to "event",
                "eventName" to "DISCONNECTED",
                "data" to jsonObject(),
                "blockchain" to "NEO",
                "platform" to "o3-dapi",
                "version" to "1"
        )
        jsResponse.postValue(response.toString())
    }

    private fun fireNetworkChanged() {
        //we do not support network changing at this time
    }

    fun fireEvent(event: DappBrowserJSInterfaceV2.EVENT) {
        when(event) {
            DappBrowserJSInterfaceV2.EVENT.READY -> fireReady()
            DappBrowserJSInterfaceV2.EVENT.CONNECTED -> fireConnected()
            DappBrowserJSInterfaceV2.EVENT.ACCOUNT_CHANGED -> fireAccountChanged()
            DappBrowserJSInterfaceV2.EVENT.DISCONNECTED -> fireDisconnected()
            DappBrowserJSInterfaceV2.EVENT.NETWORK_CHANGED -> fireNetworkChanged()
        }
    }
}

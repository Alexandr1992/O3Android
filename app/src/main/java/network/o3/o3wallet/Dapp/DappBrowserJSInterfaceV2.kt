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

   /* fun setWalletForSession(wallet: Wallet, name: String) {
        val shouldFireAccountChanged = vm.walletForSession != null
        vm.walletForSession = wallet
        vm.walletForSessionName = name

        if (shouldFireAccountChanged) {
            fireAccountChangedEvent()
        }
        val intent = Intent("update-exposed-dapp-wallet")
        LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
    }*/


    /*fun handleDisconnect(message: DappMessage) {
        vm.walletForSession = null
        vm.walletForSessionName = ""
        fireDisconnect()
        val intent = Intent("update-exposed-dapp-wallet")
        LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
        (webView.context as DAppBrowserActivityV2).walletToExpose = Account.getWallet()
        (webView.context as DAppBrowserActivityV2).walletToExposeName =
                NEP6.getFromFileSystem().getDefaultAccount().label
        callback(message,  jsonObject())
    }*/

    /*fun manualDisconnect() {
        walletForSession = null
        walletForSessionName = ""
        fireDisconnect()
        val intent = Intent("update-exposed-dapp-wallet")
        LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
        (webView.context as DAppBrowserActivityV2).walletToExpose = Account.getWallet()
        (webView.context as DAppBrowserActivityV2).walletToExposeName =
                NEP6.getFromFileSystem().getDefaultAccount().label
    }*/


    /*fun authorizedAccountCredentials(message: DappMessage) {
        val response = NeoDappProtocol.GetAccountResponse(address = vm.walletForSession!!.address,
                label = vm.walletForSessionName)
        callback(message, response)
        //(webView.context as DAppBrowserActivityV2).setUnlockState()
    }

    fun rejectedAccountCredentials(message: DappMessage) {
        callback(message, jsonObject("error" to "CONNECTION_REFUSED"))
    }

    fun rejectedInvoke(message: DappMessage) {
        callback(message, jsonObject("error" to "CANCELED"))
    }*/


    /*fun handleGetAccount(message: DappMessage) {
        if (vm.walletForSession != null) {
            authorizedAccountCredentials(message)
        } else {
            //(webView.context as DAppBrowserActivityV2).authorizeWalletInfo(message)
            vm.authorizeWalletInfo(message)
        }
    }*/

    /*fun authorizeSend(message: DappMessage) {
        val sendRequest = Gson().fromJson<NeoDappProtocol.SendRequest>(Gson().toJson(message.data))
        var network = sendRequest.network ?: "MainNet"
        if (network == "") {
            network = "MainNet"
        }
        if (!network.contains(PersistentStore.getNetworkType())) {
            callback(message, jsonObject("error" to "CONNECTION_REFUSED"))
        } else if (sendRequest.fromAddress != vm.walletForSession?.address ?: "") {
            callback(message, jsonObject("error" to "CONNECTION_REFUSED"))
        } else {
            vm.authorizeSend(message)
            //(webView.context as DAppBrowserActivityV2).authorizeSend(message)
        }
    }*/

    /*fun authorizeInvoke(message: DappMessage) {
        val sendRequest = Gson().fromJson<NeoDappProtocol.InvokeRequest>(Gson().toJson(message.data))
        var network = sendRequest.network ?: "MainNet"
        if (network == "") {
            network = "MainNet"
        }
        if (!network.contains(PersistentStore.getNetworkType())) {
            callback(message, jsonObject("error" to "CONNECTION_REFUSED"))
        } else {
            vm.requestAuthorizeInvoke(message)
            //(webView.context as DAppBrowserActivityV2).authorizeInvoke(message)
        }
    }*/

    /*fun handleGetBalance(message: DappMessage) {
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
        callback(message, jsonResponse)
    }*/

    /*fun sendNativeNeoAsset(message: DappMessage, sendRequest: NeoDappProtocol.SendRequest): Boolean {
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
                callback(message, NeoDappProtocol.SendResponse(txid = it.first!!.toLowerCase(), nodeUrl = node))
            } else {
                callback(message, jsonObject("error" to "RPC_ERROR"))
            }
            val attrs = mapOf("url" to url,
                    "blockchain" to "NEO",
                    "net" to PersistentStore.getNetworkType(),
                    "method" to "send",
                    "success" to success)
            Amplitude.getInstance().logEvent("dAPI_tx_accepted", JSONObject(attrs))

            latch.countDown()

        }
        latch.await()
        return success
    }*/

    /*fun sendTokenNeoAsset(message: DappMessage, sendRequest: NeoDappProtocol.SendRequest): Boolean {
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
                        callback(message, NeoDappProtocol.SendResponse(txid = it.first!!.toLowerCase(), nodeUrl = PersistentStore.getNodeURL()))
                    } else {
                        callback(message, jsonObject("error" to "RPC_ERROR"))
                    }
                    val attrs = mapOf("url" to url,
                            "blockchain" to "NEO",
                            "net" to PersistentStore.getNetworkType(),
                            "method" to "send",
                            "success" to success)
                    Amplitude.getInstance().logEvent("dAPI_tx_accepted", JSONObject(attrs))
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
                                callback(message, NeoDappProtocol.SendResponse(txid = it.first!!.toLowerCase(), nodeUrl = PersistentStore.getNodeURL()))
                            } else {
                                callback(message, jsonObject("error" to "RPC_ERROR"))
                            }
                            val attrs = mapOf("url" to url,
                                    "blockchain" to "NEO",
                                    "net" to PersistentStore.getNetworkType(),
                                    "method" to "send",
                                    "success" to success)
                            Amplitude.getInstance().logEvent("dAPI_tx_accepted", JSONObject(attrs))
                            latch.countDown()
                        }
                    }
                }
            }
        }
        latch.await()
        return success
    }*/

    /*fun handleSend(message: DappMessage): Boolean {
        val sendRequest = Gson().fromJson<NeoDappProtocol.SendRequest>(Gson().toJson(message.data))
        if (sendRequest.asset.toUpperCase()
                == "NEO" || sendRequest.asset.toUpperCase() == "GAS") {
            return sendNativeNeoAsset(message, sendRequest)
        } else {
            return sendTokenNeoAsset(message, sendRequest)
        }
    }*/

    /*fun handleGetStorage(message: DappMessage) {
        val latch = CountDownLatch(1)
        var storageRequest = Gson().fromJson<NeoDappProtocol.GetStorageRequest>(Gson().toJson(message.data))
        NeoNodeRPC(PersistentStore.getNodeURL()).getStorage(storageRequest.scriptHash, storageRequest.key) {
            if (it.second != null) {
                callback(message, jsonObject("result" to "RPC_ERROR"))
            } else {
                callback(message, jsonObject("result" to it.first))
            }

            latch.countDown()
        }
        latch.await()
    }*/

    /*fun handleInvokeRead(message: DappMessage) {
        var invokeReadRequest = Gson().fromJson<NeoDappProtocol.InvokeReadRequest>(Gson().toJson(message.data))
        var network = invokeReadRequest.network ?: "MainNet"
        if (network == "") {
            network = "MainNet"
        }
        if (!network.contains(PersistentStore.getNetworkType())) {
            callback(message, jsonObject("error" to "CONNECTION_REFUSED"))
            return
        }
        val latch = CountDownLatch(1)
        NeoNodeRPC(PersistentStore.getNodeURL()).readOnlyInvoke(invokeReadRequest.scriptHash,
                invokeReadRequest.operation, invokeReadRequest.args) {
            if (it.second == null) {
                callback(message, jsonObject("result" to it.first))
            } else {
                callback(message, jsonObject("result" to "RPC_ERROR"))
            }
            latch.countDown()
        }
        latch.await()
    }*/

    /*fun handleInvoke(message: DappMessage): Boolean {
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
                        callback(message, obj)
                    } else {
                        callback(message, jsonObject("result" to "RPC_ERROR"))
                    }
                    val attrs = mapOf("url" to url,
                            "blockchain" to "NEO",
                            "net" to PersistentStore.getNetworkType(),
                            "method" to "invoke",
                            "success" to success)
                    Amplitude.getInstance().logEvent("dAPI_tx_accepted", JSONObject(attrs))
                    latch.countDown()
                }
            }
        }

        latch.await()
        return success
    }*/

    /*fun fireAccountChangedEvent() {
        val mainHandler = Handler(O3Wallet.appContext!!.mainLooper)
        val fireAccountChanged = jsonObject("command" to "event",
                "eventName" to "ACCOUNT_CHANGED",
                "data" to jsonObject("address" to walletForSession!!.address, "label" to walletForSessionName),
                "blockchain" to "NEO",
                "platform" to "o3-dapi",
                "version" to "1"
        )

        val myRunnable = Runnable {
            val script = "_o3dapi.receiveMessage(" + fireAccountChanged.toString() + ")"
            webView.evaluateJavascript(script) { value ->
                Log.d("javascript", value)
            }
        }
        mainHandler.post(myRunnable)
    }*/

    /*fun fireDisconnect() {
        val mainHandler = Handler(O3Wallet.appContext!!.mainLooper)
        val fireDisconnectResponse = jsonObject("command" to "event",
                "eventName" to "DISCONNECTED",
                "data" to jsonObject(),
                "blockchain" to "NEO",
                "platform" to "o3-dapi",
                "version" to "1"
        )

        val myRunnable = Runnable {
            val script = "_o3dapi.receiveMessage(" + fireDisconnectResponse.toString() + ")"
            webView.evaluateJavascript(script) { value ->
                Log.d("javascript", value)
            }
        }
        mainHandler.post(myRunnable)
        (webView.context as DAppBrowserActivityV2).find<ImageView>(R.id.walletStatusImageView).image =
                ContextCompat.getDrawable(context, R.drawable.ic_walletitem)
    }*/

    /*fun fireReady() {
        val mainHandler = Handler(O3Wallet.appContext!!.mainLooper)
        val fireDisconnectResponse = jsonObject("command" to "event",
                "eventName" to "READY",
                "data" to jsonObject(),
                "blockchain" to "NEO",
                "platform" to "o3-dapi",
                "version" to "1"
        )

        val myRunnable = Runnable {
            val script = "_o3dapi.receiveMessage(" + fireDisconnectResponse.toString() + ")"
            webView.evaluateJavascript(script) { value ->
                Log.d("javascript", value)
            }
        }
        mainHandler.post(myRunnable)
    }*/

    /*fun callback(message: DappMessage, data: Any) {
        val json = Gson().typedToJsonTree(message).asJsonObject
        if (Gson().typedToJsonTree(data).isJsonObject && Gson().typedToJsonTree(data).asJsonObject.has("error")) {
            json["error"] = Gson().typedToJsonTree(data)["error"]
        } else {
            json["data"] = Gson().typedToJsonTree(data)
        }
        vm.setDappResponse(json.toString())
    }*/
}
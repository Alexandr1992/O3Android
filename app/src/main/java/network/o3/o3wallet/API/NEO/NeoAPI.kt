
package network.o3.o3wallet.API.NEO


import android.util.Log
import com.github.kittinunf.fuel.httpPost
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import neoutils.Neoutils
import neoutils.Neoutils.sign
import neoutils.Neoutils.validateNEOAddress
import neoutils.RawTransaction
import network.o3.o3wallet.API.O3Platform.*
import neoutils.Wallet
import network.o3.o3wallet.*
import org.jetbrains.anko.db.DoubleParser
import org.jetbrains.anko.defaultSharedPreferences
import unsigned.toUByte
import java.lang.Exception
import java.math.BigDecimal
import java.nio.*


class NeoNodeRPC {
    var nodeURL = PersistentStore.getNodeURL()

    //var nodeURL = "http://seed3.neo.org:20332" //TESTNET
    enum class Asset {
        NEO,
        GAS;

        fun assetID(): String {
            if (this == GAS) {
                return "602c79718b16e442de58778e148d0b1084e3b2dffd5de6b7b16cee7969282de7"
            } else if (this == NEO) {
                return "c56f33fc6ecfcd0c225c4ab356fee59390af8560be0e930faebe74a6daff7c9b"
            }
            return ""
        }
    }

    constructor(url: String = "http://seed3.neo.org:10332") {
        this.nodeURL = url
    }

    enum class RPC {
        GETBLOCKCOUNT,
        GETCONNECTIONCOUNT,
        VALIDATEADDRESS,
        GETACCOUNTSTATE,
        SENDRAWTRANSACTION,
        INVOKEFUNCTION;

        fun methodName(): String {
            return this.name.toLowerCase()
        }
    }

    fun getBlockCount(completion: (Pair<Int?, Error?>) -> (Unit)) {
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to RPC.GETBLOCKCOUNT.methodName(),
                "params" to jsonArray(),
                "id" to 1
        )

        var request = nodeURL.httpPost().body(dataJson.toString())
        println(RPC.GETBLOCKCOUNT.methodName())
        request.headers["Content-Type"] = "application/json"
        request.responseString { _, _, result ->
            print(result.component1())

            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val nodeResponse = gson.fromJson<NodeResponsePrimitive>(data!!)
                val blockCount = gson.fromJson<Int>(nodeResponse.result)
                completion(Pair<Int?, Error?>(blockCount, null))
            } else {
                completion(Pair<Int?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getConnectionCount(completion: (Pair<Int?, Error?>) -> Unit) {
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to RPC.GETCONNECTIONCOUNT.methodName(),
                "params" to jsonArray(),
                "id" to 1
        )

        var request = nodeURL.httpPost().body(dataJson.toString())
        request.headers["Content-Type"] = "application/json"
        request.responseString { _, _, result ->

            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val nodeResponse = gson.fromJson<NodeResponsePrimitive>(data!!)
                val blockCount = gson.fromJson<Int>(nodeResponse.result)
                completion(Pair<Int?, Error?>(blockCount, null))
            } else {
                completion(Pair<Int?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getAccountState(address: String, completion: (Pair<AccountState?, Error?>) -> Unit) {
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to RPC.GETACCOUNTSTATE.methodName(),
                "params" to jsonArray(address),
                "id" to 1
        )

        var request = nodeURL.httpPost().body(dataJson.toString())
        request.headers["Content-Type"] = "application/json"
        request.responseString { _, _, result ->

            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val nodeResponse = gson.fromJson<NodeResponse>(data!!)
                val block = gson.fromJson<AccountState>(nodeResponse.result)
                completion(Pair<AccountState?, Error?>(block, null))
            } else {
                Log.d("ERROR", error.localizedMessage)
                completion(Pair<AccountState?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun validateAddress(address: String, completion: (Pair<Boolean?, Error?>) -> Unit) {
        val valid = validateNEOAddress(address)
        completion(kotlin.Pair<kotlin.Boolean?, Error?>(valid, null))
    }

    private fun sendRawTransaction(data: ByteArray, completion: (Pair<Boolean?, Error?>) -> Unit) {
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to RPC.SENDRAWTRANSACTION.methodName(),
                "params" to jsonArray(data.toHex()),
                "id" to 3
        )

        var request = nodeURL.httpPost().body(dataJson.toString()).timeout(600000)
        request.headers["Content-Type"] = "application/json"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                try {
                    val nodeResponse = gson.fromJson<SendRawTransactionResponse>(data!!)
                    completion(Pair<Boolean?, Error?>(nodeResponse.result, null))
                } catch (error: Error) {
                    completion(kotlin.Pair<kotlin.Boolean?, Error?>(null, Error(error.localizedMessage)))
                }
            } else {
                completion(Pair<Boolean?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun claimGAS(wallet: Wallet, storedClaims: ClaimData? = null, completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        if (storedClaims == null) {
            O3PlatformClient().getClaimableGAS(wallet.address) {
                val claims = it.first
                var error = it.second
                if (error != null) {
                    completion(Pair<Boolean?, Error?>(false, error))
                } else {
                    val payload = generateClaimTransactionPayload(wallet, claims!!)
                    sendRawTransaction(payload) {
                        var success = it.first
                        var error = it.second
                        completion(Pair<Boolean?, Error?>(success, error))
                    }
                }
            }
        } else {
            val payload = generateClaimTransactionPayload(wallet, storedClaims!!)
            sendRawTransaction(payload) {
                var success = it.first
                var error = it.second
                completion(Pair<Boolean?, Error?>(success, error))
            }
        }
    }

    fun sendNativeAssetTransaction(wallet: Wallet, asset: Asset, amount: Double, toAddress: String, attributes: Array<TransactionAttritbute>?, completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        O3PlatformClient().getUTXOS(wallet.address) {
            var assets = it.first
            var error = it.second
            if (error != null) {
                completion(Pair<Boolean?, Error?>(false, error))
            } else {
                val payload = generateSendTransactionPayload(wallet, asset, amount, toAddress, assets!!, attributes)
                sendRawTransaction(payload) {
                    var success = it.first
                    var error = it.second
                    completion(Pair<Boolean?, Error?>(success, error))
                }
            }
        }
    }

    private fun to8BytesArray(value: Int): ByteArray {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun to8BytesArray(value: Long): ByteArray {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
    }

    data class SendAssetReturn(val totalAmount: Double?, val payload: ByteArray?, val error: Error?)
    data class TransactionAttritbute(val messaeg: String?)


    private fun getSortedUnspents(asset: Asset, utxos: Array<UTXO>): List<UTXO> {
        if (asset == Asset.NEO) {
            val unsorted = utxos.filter { it.asset.contains(Asset.NEO.assetID()) }
            return unsorted.sortedBy { it.value.toDouble() }
        } else {
            val unsorted = utxos.filter { it.asset.contains(Asset.GAS.assetID()) }
            return unsorted.sortedBy { it.value.toDouble() }
        }
    }

    private fun getInputsNecessaryToSendAsset(asset: Asset, amount: Double, utxos: UTXOS): SendAssetReturn {
        var sortedUnspents  = getSortedUnspents(asset, utxos.data)
        var neededForTransaction: MutableList<UTXO> = arrayListOf()
        if (sortedUnspents.sumByDouble { it.value.toDouble() } <  amount) {
            return SendAssetReturn(null, null, Error("insufficient balance"))
        }

        var runningAmount = 0.0
        var index = 0
        var count: Int = 0
        //Assume we always have enough balance to do this, prevent the check for bal
        while (runningAmount < amount) {
            neededForTransaction.add(sortedUnspents[index])
            runningAmount += sortedUnspents[index].value.toDouble()
            index += 1
            count += 1
        }
        var inputData: ByteArray = byteArrayOf(count.toByte())
        for (t: UTXO in neededForTransaction) {
            val data = hexStringToByteArray(t.txid.removePrefix("0x"))
            val reversedBytes = data.reversedArray()
            inputData = inputData + reversedBytes + ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(t.index.toShort()).array()
        }
        return SendAssetReturn(runningAmount, inputData, null)
    }

    private fun packRawTransactionBytes(payloadPrefix: ByteArray, wallet: Wallet, asset: Asset, inputData: ByteArray,
                                        runningAmount: Double, toSendAmount: Double, toAddress: String,
                                        attributes: Array<TransactionAttritbute>?): ByteArray {
        var inputDataBytes = inputData
        val needsTwoOutputTransactions = runningAmount != toSendAmount

        var numberOfAttributes: Byte = 0x00.toByte()
        var attributesPayload: ByteArray = ByteArray(0)
        //TODO add attribute
//        if attributes != nil {
//            for (attribute in attributes!!) {
//            if attribute.data != nil {
//                attributesPayload = attributesPayload + attribute.data!
//                numberOfAttributes = numberOfAttributes + 1
//            }
//        }
//        }

        var payload: ByteArray = payloadPrefix + numberOfAttributes
        payload = payload + attributesPayload + inputDataBytes
        if (needsTwoOutputTransactions) {
            //Transaction To Reciever
            payload = payload + byteArrayOf(0x02.toByte()) + asset.assetID().hexStringToByteArray().reversedArray()
            val amountToSendInMemory: Long = (toSendAmount * 100000000).toLong()
            payload += to8BytesArray(amountToSendInMemory)
            //reciever addressHash
            payload += toAddress.hashFromAddress().hexStringToByteArray()
            //Transaction To Sender
            payload += asset.assetID().hexStringToByteArray().reversedArray()
            val amountToGetBackInMemory = (runningAmount * 100000000).toLong() - (toSendAmount * 100000000).toLong()
            payload += to8BytesArray(amountToGetBackInMemory)
            payload += wallet.hashedSignature

        } else {
            payload = payload + byteArrayOf(0x01.toByte()) + asset.assetID().hexStringToByteArray().reversedArray()
            val amountToSendInMemory = (toSendAmount * 100000000).toLong()
            payload += to8BytesArray(amountToSendInMemory)
            payload += toAddress.hashFromAddress().hexStringToByteArray()
        }
        return payload
    }

    private fun generateSendTransactionPayload(wallet: Wallet, asset: Asset, amount: Double, toAddress: String, utxos: UTXOS, attributes: Array<TransactionAttritbute>?): ByteArray {
        var error: Error?
        val inputData = getInputsNecessaryToSendAsset(asset, amount, utxos)
        val payloadPrefix = byteArrayOf(0x80.toUByte(), 0x00.toByte())
        val rawTransaction = packRawTransactionBytes(payloadPrefix, wallet,
                asset, inputData.payload!!, inputData.totalAmount!!,
                amount, toAddress, attributes)
        val privateKeyHex = wallet.privateKey.toHex()
        val signatureData = sign(rawTransaction, privateKeyHex)
        val finalPayload = concatenatePayloadData(wallet, rawTransaction, signatureData)
        Log.d("PAYLAOD:", finalPayload.toHex())
        return finalPayload
    }

    /*
    private fun genereateInvokeInputData(wallet: Wallet, assets: UTXOS): ByteArray {
       /* var payload: ByteArray = byteArrayOf(0xd1.toUByte()) //Invoke Transaction Type
        payload += byteArrayOf(0x00.toUByte()) // Version
        // TODO: Im making this a one input with absolute minimum gas cost since I only care about NEP-5 Transfer
        // TODO: In the future this needs to be expanded to allow for more generic smart contracts
        val inputCount = 0x01.toUByte()
        payload += byteArrayOf(inputCount)
        for (unspent in assets.GAS.unspent)*/
    }*/


    private fun generateInvokeTransactionPayload(wallet: Wallet, utxos: UTXOS, script: String, contractAddress: String): ByteArray {
        val inputData = getInputsNecessaryToSendAsset(NeoNodeRPC.Asset.GAS, 0.00000001, utxos)
        val payloadPrefix = byteArrayOf(0xd1.toUByte(), 0x00.toUByte()) + script.hexStringToByteArray()
        var rawTransaction = packRawTransactionBytes(payloadPrefix, wallet, Asset.GAS,
                inputData.payload!!, inputData.totalAmount!!, 0.00000001,
                Account.getWallet()?.address!!, null)

        val privateKeyHex = wallet.privateKey.toHex()
        val signature = sign(rawTransaction, privateKeyHex)
        var finalPayload = concatenatePayloadData(wallet, rawTransaction, signature)
        finalPayload = finalPayload + contractAddress.hexStringToByteArray()
        return finalPayload

    }

    private fun concatenatePayloadData(wallet: Wallet, txData: ByteArray, signatureData: ByteArray): ByteArray {
        var payload = txData + byteArrayOf(0x01.toByte())           // signature number
        payload += byteArrayOf(0x41.toByte())                                 // signature struct length
        payload += byteArrayOf(0x40.toByte())                                 // signature data length
        payload += signatureData                                              // signature
        payload += byteArrayOf(0x23.toByte())                                 // contract data length
        payload = payload + byteArrayOf(0x21.toByte()) + wallet.publicKey + byteArrayOf(0xac.toByte()) // NeoSigned publicKey
        return payload
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun generateClaimInputData(wallet: Wallet, claims: ClaimData): ByteArray {
        var payload: ByteArray = byteArrayOf(0x02.toByte()) // Claim Transaction Type
        payload += byteArrayOf(0x00.toByte()) // Version
        val claimsCount = claims.data.claims.count().toByte()
        payload += byteArrayOf(claimsCount)
        for (claim: UTXO in claims.data.claims) {
            payload += hexStringToByteArray(claim.txid.removePrefix("0x")).reversedArray()
            payload += ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(claim.index.toShort()).array()
        }
        payload += byteArrayOf(0x00.toByte()) // Attributes
        payload += byteArrayOf(0x00.toByte()) // Inputs
        payload += byteArrayOf(0x01.toByte()) // Output Count
        payload += hexStringToByteArray(NeoNodeRPC.Asset.GAS.assetID()).reversedArray()

        val claimIntermediate = BigDecimal(claims.data.gas)
        val claimLong = claimIntermediate.multiply(BigDecimal(100000000)).toLong()
        payload += ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(claimLong).array()
        payload += wallet.hashedSignature
        Log.d("Claim Payload", payload.toHex())
        return payload
    }

    private fun generateClaimTransactionPayload(wallet: Wallet, claims: ClaimData): ByteArray {
        val rawClaim = generateClaimInputData(wallet, claims)
        val privateKeyHex = wallet.privateKey.toHex()
        val signature = sign(rawClaim, privateKeyHex)
        val finalPayload = concatenatePayloadData(wallet, rawClaim, signature)
        return finalPayload
    }

    fun invokeFunction(params: JsonArray, completion: (Pair<InvokeFunctionResponse?, Error?>) -> Unit) {
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to RPC.INVOKEFUNCTION.methodName(),
                "params" to params,
                "id" to 3
        )

        var request = nodeURL.httpPost().body(dataJson.toString()).timeout(600000)
        request.headers["Content-Type"] = "application/json"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                try {
                    val nodeResponse = gson.fromJson<NodeResponse>(data!!)
                    val invokeResponse = gson.fromJson<InvokeFunctionResponse>(nodeResponse.result)

                    completion(Pair<InvokeFunctionResponse?, Error?>(invokeResponse, null))
                } catch (error: Error) {
                    completion(Pair<InvokeFunctionResponse?, Error?>(null, Error(error.localizedMessage)))
                }
            } else {
                completion(Pair<InvokeFunctionResponse?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getTokenBalanceOf(tokenHash: String, address: String, completion: (Pair<Long?, Error?>) -> Unit) {

        var params: ArrayList<Any> = arrayListOf<Any>()
        params.add(tokenHash)
        params.add("balanceOf")
        var invokeFunctionParams: ArrayList<Any> = arrayListOf()
        //var stack = Stack(type = "Hash160",value = address.hash160().toString())
        var stack = JsonObject()
        stack.set("type", "Hash160")
        stack.set("value", address.hash160().toString())
        invokeFunctionParams.add(stack)
        params.add(jsonArray(invokeFunctionParams))

        invokeFunction(params.toJsonArray()) {
            if (it.second != null) {
                completion(Pair<Long?, Error?>(null, it.second))
            } else if (it.first!!.stack.count() > 0) {
                val stack = it.first!!.stack[0]
                var amount: Long = 0
                if (stack.value.isNotEmpty()) {
                    amount = stack.value.littleEndianHexStringToInt64()
                }
                completion(Pair<Long?, Error?>(amount, null))
            } else {
                completion(Pair<Long?, Error?>(0, null))
            }
        }
    }

    fun getWhiteListStatus(contractHash: String, address: String, completion: (Pair<Boolean?, Error?>) -> Unit) {

        var params: ArrayList<Any> = arrayListOf<Any>()
        params.add(contractHash)
        params.add("kycStatus")
        var invokeFunctionParams: ArrayList<Any> = arrayListOf()
        //var stack = Stack(type = "Hash160",value = address.hash160().toString())
        var stack = JsonObject()
        stack.set("type", "Hash160")
        stack.set("value", address.hash160().toString())
        invokeFunctionParams.add(stack)
        params.add(jsonArray(invokeFunctionParams))

        invokeFunction(params.toJsonArray()) {
            if (it.second != null) {
                completion(Pair<Boolean?, Error?>(null, it.second))
            } else if (it.first!!.stack.count() > 0) {
                val whiteListed = it.first!!.stack[0].value
                if (whiteListed == "01") {
                    completion(Pair<Boolean?, Error?>(true, null))
                }
                completion(Pair<Boolean?, Error?>(false, null))
            } else {
                completion(Pair<Boolean?, Error?>(false, null))
            }
        }
    }

    fun buildNEP5TransferScript(scriptHash: String, fromAddress: String, toAddress: String, amount: Double): ByteArray {
        val amountToSendInMemory: Long = (amount * 100000000).toLong()
        val fromAddressHash = fromAddress.hashFromAddress()
        val toAddressHash = toAddress.hashFromAddress()
        val scriptBuilder = ScriptBuilder()
        scriptBuilder.pushContractInvoke(scriptHash, operation = "transfer",
                args = arrayOf(amountToSendInMemory, toAddressHash, fromAddressHash)
        )
        var script = scriptBuilder.getScriptHexString()
        return byteArrayOf((script.length / 2).toUByte()) + script.hexStringToByteArray()
    }

    // Args: scriptHash -> Contract Address of NEP-5 Token to Transfer
    // fromAddress -> Address of Sender
    // toAddress -> Address of Recipient
    // transfer amount *
    fun sendNEP5Token(wallet: Wallet, tokenContractHash: String, fromAddress: String, toAddress: String, amount: Double,
                          completion: (Pair<Boolean?, Error?>) -> Unit) {
        O3PlatformClient().getUTXOS(wallet.address) {
            var assets = it.first
            var error = it.second
            if (error != null) {
                completion(Pair<Boolean?, Error?>(false, error))
                return@getUTXOS
            } else {
                val scriptBytes = buildNEP5TransferScript(tokenContractHash, fromAddress, toAddress, amount)
                val scriptBytesString = scriptBytes.toHex()
                val finalPayload = generateInvokeTransactionPayload(wallet, assets!!, scriptBytes.toHex(), tokenContractHash)
                val finalPayloadString = finalPayload.toHex()
                sendRawTransaction(finalPayload) {
                    var success = it.first
                    var error = it.second
                    completion(Pair<Boolean?, Error?>(success, error))
                }
            }
        }
    }

    fun participateTokenSales(scriptHash: String, assetID: String, amount: Double, remark: String, networkFee: Double,  completion: (Pair<String?, Error?>) -> Unit){
        var utxoEndpoint = "main"

        if (PersistentStore.getNetworkType() == "Test") {
            utxoEndpoint = "test"
        } else if(PersistentStore.getNetworkType() == "Private") {
            utxoEndpoint = "private"
        }
        var finalPayload: RawTransaction? = null
        try {
            finalPayload = Neoutils.mintTokensRawTransactionMobile(utxoEndpoint, scriptHash, Account.getWallet()?.wif, assetID, amount, remark, networkFee)
        } catch (e: Exception) {
            completion(Pair(null, Error(e.localizedMessage)))
            return
        }
        if (finalPayload == null) {
            completion(Pair(null, null))
            return
        }
        Log.d("MINT TRANSACTION ID: ", finalPayload.txid )
        sendRawTransaction(finalPayload.data) {
            var success = it.first
            var error = it.second
            if (success == false) {
                error = Error("Transaction Failed")
            }
            completion(Pair (finalPayload.txid, error))
        }
    }
}

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
import java.util.*


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
        GETRAWMEMPOOL,
        SENDRAWTRANSACTION,
        INVOKEFUNCTION;

        fun methodName(): String {
            return this.name.toLowerCase(Locale.US)
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

    fun getMemPoolHeight(completion: (Pair<Int?, Error?>) -> Unit) {
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to RPC.GETRAWMEMPOOL.methodName(),
                "params" to jsonArray(),
                "id" to 1
        )
        var request = nodeURL.httpPost().body(dataJson.toString())
        request.headers["Content-Type"] = "application/json"

        request.responseString { _, response, result ->
            Log.d("response", response.toString())
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val nodeResponse = gson.fromJson<NodeResponse>(data!!)
                val mempool = gson.fromJson<Array<String>>(nodeResponse.result)
                completion(Pair<Int?, Error?>(mempool.count(), null))
            } else {
                completion(Pair<Int?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun validateAddress(address: String, completion: (Pair<Boolean?, Error?>) -> Unit) {
        val valid = validateNEOAddress(address)
        completion(kotlin.Pair<kotlin.Boolean?, Error?>(valid, null))
    }

    private fun sendRawTransaction(finalPayload: ByteArray, unsignedPayload: ByteArray, completion: (Pair<String?, Error?>) -> Unit) {
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to RPC.SENDRAWTRANSACTION.methodName(),
                "params" to jsonArray(finalPayload.toHex()),
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
                    if (nodeResponse.result == false) {
                        completion(Pair<String?, Error?>(null, Error("Transaction failed")))
                    } else {
                        completion(Pair<String?, Error?>(unsignedPayload.toHex().transactionToID(), null))
                    }
                } catch (error: Error) {
                    completion(kotlin.Pair<String?, Error?>(null, Error(error.localizedMessage)))
                }
            } else {
                completion(Pair<String?, Error?>(null, Error(error.localizedMessage)))
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
                    sendRawTransaction(payload, payload) {
                        var txid = it.first
                        var error = it.second
                        if (txid == null) {
                            completion(Pair<Boolean?, Error?>(false, Error("Transaction failed")))
                        } else {
                            completion(Pair<Boolean?, Error?>(true, null))
                        }
                    }
                }
            }
        } else {
            val payload = generateClaimTransactionPayload(wallet, storedClaims!!)
            sendRawTransaction(payload, payload) {
                var txid = it.first
                var error = it.second
                if (txid == null) {
                    completion(Pair<Boolean?, Error?>(false, Error("Transaction failed")))
                } else {
                    completion(Pair<Boolean?, Error?>(true, null))
                }
            }
        }
    }

    fun sendNativeAssetTransaction(wallet: Wallet, asset: Asset, amount: BigDecimal, toAddress: String,
                                   attributes: Array<TransactionAttribute>?, fee: BigDecimal = BigDecimal.ZERO, completion: (Pair<String?, Error?>) -> (Unit)) {
        O3PlatformClient().getUTXOS(wallet.address) {
            var assets = it.first
            var error = it.second
            if (error != null) {
                completion(Pair<String?, Error?>(null, error))
            } else {
                val txData = generateSendTransactionPayload(wallet, asset, amount, toAddress, assets!!, attributes, fee)
                sendRawTransaction(txData.first, txData.second) {
                    var txid = it.first
                    var error = it.second
                    if (txid == null) {
                        completion(Pair<String?, Error?>(null, Error("Transaction Failed")))
                    }  else {
                        completion(Pair<String?, Error?>(txid, error))
                    }
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

    data class SendAssetReturn(val totalAmount: BigDecimal?,
                               val inputCount: Int,
                               val inputPayload: ByteArray?,
                               val fee: BigDecimal,
                               val error: Error?)

    private fun getSortedUnspents(asset: Asset, utxos: Array<UTXO>): List<UTXO> {
        if (asset == Asset.NEO) {
            val unsorted = utxos.filter { it.asset.contains(Asset.NEO.assetID()) }
            return unsorted.sortedBy { it.value.toDouble() }
        } else {
            val unsorted = utxos.filter { it.asset.contains(Asset.GAS.assetID()) }
            return unsorted.sortedBy { it.value.toDouble() }
        }
    }

    private fun getInputsNecessaryToSendGAS(amount: BigDecimal, utxos: UTXOS?, fee: BigDecimal = BigDecimal.ZERO): SendAssetReturn {
        if (utxos == null) {
            return SendAssetReturn(BigDecimal.ZERO, 0, byteArrayOf(), BigDecimal.ZERO, null)
        }

        var sortedUnspents  = getSortedUnspents(Asset.GAS, utxos.data)
        var neededForTransaction: MutableList<UTXO> = arrayListOf()
        var decimalSum = BigDecimal(0)
        for (utxo in sortedUnspents) {
            decimalSum += (utxo.value.toSafeDecimal())
        }
        if (decimalSum.toSafeMemory(8) < (amount + fee).toSafeMemory(8)) {
            return SendAssetReturn(null, 0, null, fee, Error("insufficient balance"))
        }

        var runningAmount = BigDecimal(0.0)
        var index = 0
        var count: Int = 0
        //Assume we always have enough balance to do this, prevent the check for bal
        while (runningAmount.toSafeMemory(8) < (amount + fee).toSafeMemory(8)) {
            neededForTransaction.add(sortedUnspents[index])
            runningAmount += (sortedUnspents[index].value.toSafeDecimal())
            index += 1
            count += 1
        }
        var inputData: ByteArray = byteArrayOf()
        for (t: UTXO in neededForTransaction) {
            val data = hexStringToByteArray(t.txid.removePrefix("0x"))
            val reversedBytes = data.reversedArray()
            inputData = inputData + reversedBytes + ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(t.index.toShort()).array()
        }
        return SendAssetReturn(runningAmount, count,  inputData, fee, null)
    }

    private fun getInputsNecessaryToSendNEO(amount: BigDecimal, utxos: UTXOS?): SendAssetReturn {
        if (utxos == null) {
            return SendAssetReturn(BigDecimal.ZERO, 0, byteArrayOf(), BigDecimal.ZERO, null)
        }

        var sortedUnspents  = getSortedUnspents(Asset.NEO, utxos.data)
        var neededForTransaction: MutableList<UTXO> = arrayListOf()
        var decimalSum = BigDecimal(0)
        for (utxo in sortedUnspents) {
            decimalSum += (utxo.value.toSafeDecimal())
        }
        if (decimalSum < amount) {
            return SendAssetReturn(null, 0, null, BigDecimal.ZERO, Error("insufficient balance"))
        }

        var runningAmount = BigDecimal(0.0)
        var index = 0
        var count: Int = 0
        //Assume we always have enough balance to do this, prevent the check for bal
        while (runningAmount < amount) {
            neededForTransaction.add(sortedUnspents[index])
            runningAmount += (sortedUnspents[index].value.toSafeDecimal())
            index += 1
            count += 1
        }
        var inputData: ByteArray = byteArrayOf()
        for (t: UTXO in neededForTransaction) {
            val data = hexStringToByteArray(t.txid.removePrefix("0x"))
            val reversedBytes = data.reversedArray()
            inputData = inputData + reversedBytes + ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(t.index.toShort()).array()
        }
        return SendAssetReturn(runningAmount, count, inputData, BigDecimal.ZERO, null)
    }

    fun getAttributesPayload(attributes: Array<TransactionAttribute>?): ByteArray {
        var numberOfAttributes: Int = 0
        var attributesPayload: ByteArray = ByteArray(0)
        if (attributes != null) {
            for (attribute in attributes!!) {
                if (attribute.data != null) {
                    attributesPayload += attribute.data!!
                    numberOfAttributes += 1
                }
            }
        }

        var payload: ByteArray = byteArrayOf(numberOfAttributes.toUByte())
        payload = payload + attributesPayload
        return payload
    }


    private fun getOutputDataPayload(wallet: Wallet, asset: Asset,
                                        runningAmount: BigDecimal, toSendAmount: BigDecimal, toAddressHash: String,
                                        fee: BigDecimal): Pair<ByteArray, Int> {
        val needsTwoOutputTransactions = runningAmount.toSafeMemory(8) != (toSendAmount + fee).toSafeMemory(8)

        var outputCount: Int = 0
        var payload = byteArrayOf()
        //assetless send
        if(runningAmount == BigDecimal.ZERO && fee == BigDecimal.ZERO) {
            payload += byteArrayOf()
            return Pair(payload, 0)
        }

        if (needsTwoOutputTransactions) {
            //Transaction To Reciever
            outputCount = 2
            payload = payload + asset.assetID().hexStringToByteArray().reversedArray()
            val amountToSendInMemory: Long = toSendAmount.toSafeMemory(8)
            payload += to8BytesArray(amountToSendInMemory)
            //reciever addressHash
            payload += toAddressHash.hexStringToByteArray()
            //Transaction To Sender
            payload += asset.assetID().hexStringToByteArray().reversedArray()
            val amountToGetBackInMemory = runningAmount.toSafeMemory(8) - toSendAmount.toSafeMemory(8) - fee.toSafeMemory(8)
            payload += to8BytesArray(amountToGetBackInMemory)
            payload += wallet.hashedSignature

        } else {
            outputCount = 1
            payload = payload + asset.assetID().hexStringToByteArray().reversedArray()
            val amountToSendInMemory = toSendAmount.toSafeMemory(8)
            payload += to8BytesArray(amountToSendInMemory)
            payload += toAddressHash.hexStringToByteArray()
        }

        return Pair(payload, outputCount)
    }


    private fun generateSendTransactionPayload(wallet: Wallet, asset: Asset, amount: BigDecimal, toAddress: String, utxos: UTXOS,
                                               attributes: Array<TransactionAttribute>?, fee: BigDecimal = BigDecimal.ZERO): Pair<ByteArray, ByteArray> {
        val mainInput: SendAssetReturn
        var optionalFeeInput: SendAssetReturn? = null
        val payloadPrefix = byteArrayOf(0x80.toUByte(), 0x00.toByte())

        if (asset == Asset.GAS) {
            mainInput = getInputsNecessaryToSendGAS(amount, utxos, fee)
        } else {
            mainInput = getInputsNecessaryToSendNEO(amount, utxos)
                if (fee > BigDecimal.ZERO) {
                    optionalFeeInput = getInputsNecessaryToSendGAS(BigDecimal(0.00000001), utxos, fee)
            }
        }

        var mainOutputData =
                getOutputDataPayload(wallet,
                asset, mainInput.totalAmount!!,
                amount, toAddress.hashFromAddress(), mainInput.fee)

        var optionalFeeOutputData: Pair<ByteArray, Int>? = null
        if (optionalFeeInput != null) {
            optionalFeeOutputData = getOutputDataPayload(wallet,
                    Asset.GAS, optionalFeeInput.totalAmount!!,
                    BigDecimal(0.00000001), wallet.address.hashFromAddress(), fee)
       }

        val totalInputCount = mainInput.inputCount + (optionalFeeInput?.inputCount ?: 0)
        val finalInputPayload = mainInput.inputPayload!! + (optionalFeeInput?.inputPayload ?: byteArrayOf())

        val totalOutputCount = mainOutputData.second + (optionalFeeOutputData?.second ?: 0)
        val finalOutputPayload = mainOutputData.first + (optionalFeeOutputData?.first ?: byteArrayOf())

        val rawTransaction = payloadPrefix +
                getAttributesPayload(attributes) +
                totalInputCount.toByte() + finalInputPayload +
                totalOutputCount.toByte() + finalOutputPayload

        val privateKeyHex = wallet.privateKey.toHex()
        val signatureData = sign(rawTransaction, privateKeyHex)
        val finalPayload = concatenatePayloadData(wallet, rawTransaction, signatureData)
        return Pair(finalPayload, rawTransaction)
    }

    private fun generateInvokeTransactionPayload(wallet: Wallet, utxos: UTXOS?, script: String,
                                                 contractHash: String,
                                                 attributes: Array<TransactionAttribute>? = null, fee: BigDecimal = BigDecimal.ZERO): Pair<ByteArray, ByteArray> {
        val payloadPrefix = byteArrayOf(0xd1.toUByte(), 0x00.toUByte()) + script.hexStringToByteArray()

        val gasSendAmount = if(fee > BigDecimal.ZERO){
            BigDecimal(0.00000001)
        } else {
            BigDecimal.ZERO
        }

        val mainInput = getInputsNecessaryToSendGAS(gasSendAmount, utxos, fee)
        //since nep5 transfers are fee, the destination address is just sending to yourself
        var mainOutputData = getOutputDataPayload(wallet,
                        Asset.GAS, mainInput.totalAmount!!,
                gasSendAmount, Account.getWallet()?.address!!.hashFromAddress(), mainInput.fee)



        val rawTransaction = payloadPrefix +
                getAttributesPayload(attributes) +
                mainInput.inputCount.toByte() + mainInput.inputPayload!! +
                mainOutputData.second.toByte() + mainOutputData.first
        Log.d("payload: ", rawTransaction.toHex())

        val privateKeyHex = wallet.privateKey.toHex()
        val signature = sign(rawTransaction, privateKeyHex)
        var finalPayload = concatenatePayloadData(wallet, rawTransaction, signature)
        finalPayload = finalPayload + contractHash.hexStringToByteArray()
        return Pair(finalPayload, rawTransaction)

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

    fun buildNEP5TransferScript(scriptHash: String, fromAddress: String, toAddress: String, amount: BigDecimal, decimals: Int): ByteArray {
        val amountToSendInMemory: Long = amount.toSafeMemory(decimals)
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
    fun sendNEP5Token(wallet: Wallet, utxos: UTXOS?, tokenContractHash: String, fromAddress: String, toAddress: String, amount: BigDecimal, decimals: Int, fee: BigDecimal,
                      completion: (Pair<String?, Error?>) -> Unit) {
        val attributes = arrayOf<TransactionAttribute>(
            TransactionAttribute().scriptAttribute(fromAddress.hashFromAddress()),
            TransactionAttribute().remarkAttribute(String.format("O3X%s", Date().time.toString())),
            TransactionAttribute().hexDescriptionAttribute(tokenContractHash))

        val scriptBytes = buildNEP5TransferScript(tokenContractHash, fromAddress, toAddress, amount, decimals)
        val finalPayload = generateInvokeTransactionPayload(wallet, utxos, scriptBytes.toHex(), tokenContractHash, attributes, fee)
        sendRawTransaction(finalPayload.first, finalPayload.second) {
            var txid = it.first
            var error = it.second
            if (txid == null) {
                completion(Pair<String?, Error?>(null, Error("Transaction Failed")))
            }  else {
                completion(Pair<String?, Error?>(txid, error))
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
        sendRawTransaction(finalPayload.data, finalPayload.data) {
            var txid = it.first
            var error = it.second
            if (txid == null) {
                error = Error("Transaction Failed")
            }
            completion(Pair (finalPayload.txid, error))
        }
    }
}
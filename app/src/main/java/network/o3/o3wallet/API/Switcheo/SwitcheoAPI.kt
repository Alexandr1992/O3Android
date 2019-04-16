package network.o3.o3wallet.API.Switcheo

import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import neoutils.Neoutils
import network.o3.o3wallet.*
import org.jetbrains.anko.coroutines.experimental.bg
import org.json.JSONObject
import java.math.BigDecimal
import java.util.*
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.count
import kotlin.collections.find
import kotlin.collections.listOf
import kotlin.collections.mutableListOf
import kotlin.collections.reversedArray
import kotlin.collections.set

class SwitcheoAPI {
    val baseTestUrl = "https://test-api.switcheo.network/v2/"
    val baseMainUrl = "https://api.switcheo.network/v2/"

    val baseURL = if (PersistentStore.getNetworkType() == "Main") {
        baseMainUrl
    } else {
        baseTestUrl
    }

    val testNetContract = "aff38ca20ef96a7b86618d86f1bd3879db78c8bb"
    val mainNetContract = "a32bcf5d7082f740a4007b16e812cf66a457c3d4"
    val defaultContract = if (PersistentStore.getNetworkType() == "Main") {
        mainNetContract
    } else {
        testNetContract
    }

    enum class Route {
        TICKERS,
        OFFERS,
        TRADES,
        EXCHANGE,
        DEPOSITS,
        WITHDRAWALS,
        CANCELLATIONS,
        ORDERS,
        ADDRESS,
        AMOUNT,
        BALANCES,
        PAIRS,
        TIMESTAMP,
        CONTRACTS;

        fun routeName(): String {
            return this.name.toLowerCase(Locale.US)
        }
    }



    //region Transaction Serialization
    fun toEvenHexString(value: Int): String {
        var string = value.toString(16)
        if (string.length % 2 != 0) {
            string = "0" + string
        }
        return string
    }

    fun serializeAttributes(attributes: JsonArray): String {
        var attributeCount = 0
        var attributeHex = ""
        for (attribute in attributes) {
            //Only support 32 remarks from switcheo
            if (attribute["usage"].asInt != 32) {
                continue
            }
            attributeCount += 1
            val attributeUsage = toEvenHexString(attribute["usage"].asInt)
            val attributeValue = attribute["data"].asString
            attributeHex += attributeUsage + attributeValue
        }
        val attributeCountString = toEvenHexString(attributeCount)
        return attributeCountString + attributeHex
    }

    fun serializeInputHex(inputs: JsonArray): String {
        val inputCountHex = toEvenHexString(inputs.count())

        var inputHex = ""
        for (input in inputs) {
            val prevHashHex = input["prevHash"].asString.hexStringToByteArray().reversedArray().toHex()
            var prevIndexHex = toEvenHexString(input["prevIndex"].asInt)
            if (prevIndexHex.count() != 4) {
                prevIndexHex = prevIndexHex.padStart(4, '0')
            }

            inputHex = inputHex + prevHashHex + prevIndexHex.hexStringToByteArray().reversedArray().toHex()
        }
        return inputCountHex + inputHex
    }

    fun serializeOutputHex(outputs: JsonArray): String {
        var outputCountHex = toEvenHexString(outputs.count())
        var outputHex = ""
        for (output in outputs) {

            outputHex = outputHex +
                    output["assetId"].asString.hexStringToByteArray().reversedArray().toHex() +
                    to8BytesArray(BigDecimal(output["value"].asDouble).toSafeMemory(8)).toHex() +
                    output["scriptHash"].asString.hexStringToByteArray().reversedArray().toHex()
        }
        return outputCountHex + outputHex
    }

    fun serializeTransactionFromJson(txJson: JsonObject): ByteArray {
        var transactionHex = ""
        var txTypeHex = toEvenHexString(txJson["type"].asInt)
        var versionHex = toEvenHexString(txJson["version"].asInt)

        val script = txJson["script"].asString
        var scriptLengthHex = toEvenHexString(script.length / 2)

        val gas = to8BytesArray(txJson["gas"].asLong).toHex()
        val attributeHex = serializeAttributes(txJson["attributes"].asJsonArray)
        val inputHex = serializeInputHex(txJson["inputs"].asJsonArray)
        val outputHex = serializeOutputHex(txJson["outputs"].asJsonArray)

        transactionHex = (txTypeHex + versionHex + scriptLengthHex + script + gas + attributeHex + inputHex + outputHex).toLowerCase()

        return transactionHex.hexStringToByteArray()
    }

    fun getSignatureForJsonPayload(jsonPayload: JsonObject): String {
        val jsonPayloadBytes = jsonPayload.toString().toByteArray()
        var byteCountHex = toEvenHexString(jsonPayloadBytes.count())
        val finalHex = "010001f0" + byteCountHex + jsonPayloadBytes.toHex().toLowerCase() + "0000"
        Log.d("SWITCHEO FINAL HEX:", finalHex)
        val signedHex = Neoutils.sign(finalHex.hexStringToByteArray(), Account.getWallet().privateKey.toHex())
        Log.d("SIGNATURE: ", signedHex.toHex().toLowerCase())
        return signedHex.toHex().toLowerCase()
    }
    // endregion

    // region Exchange Information
    fun getPairs(bases: Array<String> = arrayOf(), completion: (Pair<List<String>?, Error?>) -> Unit) {
        val url = baseURL + Route.EXCHANGE.routeName() + "/pairs"
        val parameters = listOf("bases" to bases)
        url.httpGet(parameters).responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                val pairs = Gson().fromJson<List<String>>(data!!)
                completion(Pair<List<String>?, Error?>(pairs, null))
            } else {
                completion(Pair<List<String>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getContracts(completion: (Pair<JsonObject?, Error?>) -> Unit) {
        val url = baseURL + Route.EXCHANGE.routeName() + "/contracts"
        url.httpGet().responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                val contracts = Gson().fromJson<JsonObject>(data!!)
                completion(Pair<JsonObject?, Error?>(contracts, null))
            } else {
                completion(Pair<JsonObject?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getTokens(completion: (Pair<JsonObject?, Error?>) -> Unit) {
        val url = baseURL + Route.EXCHANGE.routeName() + "/tokens"
        url.httpGet().responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                val tokens = Gson().fromJson<JsonObject>(data!!)
                completion(Pair<JsonObject?, Error?>(tokens, null))
            } else {
                completion(Pair<JsonObject?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
    //endregion

    //region Tickers
    fun getCandleSticks(pair: String, start_time: Long, end_time: Long, interval: Long, completion: (Pair<Array<SwitcheoCandlestick>?, Error?>) -> Unit) {
        val url = baseURL + Route.TICKERS.routeName() + "/candlesticks"
        val request = url.httpGet(listOf("pair" to pair, "interval" to interval, "start_time" to start_time, "end_time" to end_time))
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val candleSticks = Gson().fromJson<Array<SwitcheoCandlestick>>(data!!)
                completion(Pair<Array<SwitcheoCandlestick>?, Error?>(candleSticks, null))
            } else {
                completion(Pair<Array<SwitcheoCandlestick>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getDailyTickers(completion: (Pair<Array<Ticker>?, Error?>) -> (Unit)) {
        val url = baseURL + Route.TICKERS.routeName() + "/last_24_hours"
        var request = url.httpGet()
        request.headers["X-Referral-Source"] = "o3-api"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val tickers = Gson().fromJson<Array<Ticker>>(data!!)
                completion(Pair<Array<Ticker>?, Error?>(tickers, null))
            } else {
                completion(Pair<Array<Ticker>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
    //endregion

    //region Offers
    fun getOffersForPair(pair: String, blockchain: String = "neo", contract_hash: String = defaultContract, completion: (Pair<Array<Offer>?, Error?>) -> (Unit)) {
        val url = baseURL + Route.OFFERS.routeName()
        var request = url.httpGet(
                listOf("blockchain" to blockchain, "pair" to pair, "contract_hash" to contract_hash))
        request.headers["X-Referral-Source"] = "o3-api"
        request.responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                val tickers = Gson().fromJson<Array<Offer>>(data!!)
                completion(Pair<Array<Offer>?, Error?>(tickers, null))
            } else {
                completion(Pair<Array<Offer>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
    //endregion

    //region Trades
    fun getTrades(pair: String, limit: Long, contract_hash: String, blockchain: String = "neo", completion: (Pair<List<SwitcheoTrade>?, Error?>) -> Unit) {
        val url = baseURL + Route.TRADES.routeName()
        val parameters = listOf("pair" to pair, "limit" to limit, "contract_hash" to contract_hash, "blockchain" to blockchain)
        url.httpGet(parameters).responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                val trades = Gson().fromJson<List<SwitcheoTrade>>(data!!)
                completion(Pair<List<SwitcheoTrade>?, Error?>(trades, null))
            } else {
                completion(Pair<List<SwitcheoTrade>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
    //endregion

    //region Deposits
    fun submitDeposit(asset_id: String, amount: String,
                      contract_hash: String, blockchain: String = "neo",
                      completion: (Pair<SwitcheoDepositTransaction?, Error?>) -> (Unit)) {
        val url = baseURL + Route.DEPOSITS.routeName()
        bg {
            val (data, error) = (baseURL + "/" + Route.TIMESTAMP.routeName()).httpGet().responseString().third
            val timeStamp = Gson().fromJson<JsonObject>(data!!)["timestamp"].asLong

            val jsonPayload = jsonObject(
                    "amount" to amount,
                    "asset_id" to asset_id,
                    "blockchain" to blockchain,
                    "contract_hash" to contract_hash,
                    "timestamp" to timeStamp)

            val signedHex = getSignatureForJsonPayload(jsonPayload)
            jsonPayload.addProperty("signature", signedHex)
            jsonPayload.addProperty("address", Account.getWallet().hashedSignature.reversedArray().toHex().toLowerCase())

            val request = url.httpPost().body(jsonPayload.toString())
            request.headers["Content-Type"] = "application/json"
            request.headers["X-Referral-Source"] = "o3-api"
            request.responseString { req, response, result ->
                Log.d("SWITCHEO:", response.toString())
                val (data, error) = result
                if (error == null) {
                    val tx = Gson().fromJson<SwitcheoDepositTransaction>(data!!)
                    completion(Pair<SwitcheoDepositTransaction?, Error?>(tx, null))
                } else {
                    completion(Pair<SwitcheoDepositTransaction?, Error?>(null, Error(error.localizedMessage)))
                }
            }
        }
    }

    fun executeDeposit(depositId: String, transactionJson: JsonObject, completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        Log.d("Transaction:", transactionJson.toString())
        val jsonPayloadBytes = serializeTransactionFromJson(transactionJson)
        Log.d("EXECUTION BYTES:", jsonPayloadBytes.toHex())
        val signature = Neoutils.sign(jsonPayloadBytes, Account.getWallet().privateKey.toHex()).toHex().toLowerCase()
        val url = baseURL + Route.DEPOSITS.routeName() + "/" + depositId + "/broadcast"

        val jsonPayload = jsonObject("signature" to signature)
        val request = url.httpPost().body(jsonPayload.toString())
        request.headers["Content-Type"] = "application/json"
        request.headers["X-Referral-Source"] = "o3-api"
        request.responseString { req, response, result ->
            Log.d("SWITCHEO:", response.toString())
            val (data, error) = result
            if (error == null) {
                completion(Pair<Boolean?, Error?>(true, null))
            } else {
                completion(Pair<Boolean?, Error?>(false, null))
            }
        }
    }

    fun singleStepDeposit(asset_id: String, amount: String,
                          contract_hash: String = defaultContract, blockchain: String = "neo",
                          completion: (Pair<Boolean?, Error?>) -> (Unit)) {

        submitDeposit(asset_id, amount, contract_hash, blockchain) {
            if (it.second != null) {
                completion(Pair<Boolean?, Error?>(false, it.second))
            } else {
                executeDeposit(it.first!!.id, it.first!!.transaction!!) {
                    completion(Pair<Boolean?, Error?>(it.first, it.second))
                }
            }
        }
    }
    //endregion

    //region Withdrawals
    fun submitWithdrawal(asset_id: String, amount: String,
                         contract_hash: String, blockchain: String = "neo",
                         completion: (Pair<String?, Error?>) -> (Unit)) {
        val url = baseURL + Route.WITHDRAWALS.routeName()
        bg {
            val (data, error) = (baseURL + "/" + Route.TIMESTAMP.routeName()).httpGet().responseString().third
            val timeStamp = Gson().fromJson<JsonObject>(data!!)["timestamp"].asLong

            val jsonPayload = jsonObject(
                    "amount" to amount,
                    "asset_id" to asset_id,
                    "blockchain" to blockchain,
                    "contract_hash" to contract_hash,
                    "timestamp" to timeStamp)

            Log.d("Withdrawal: ", jsonPayload.toString())
            val signedHex = getSignatureForJsonPayload(jsonPayload)
            jsonPayload.addProperty("signature", signedHex)
            jsonPayload.addProperty("address", Account.getWallet().hashedSignature.reversedArray().toHex().toLowerCase())

            val request = url.httpPost().body(jsonPayload.toString())
            request.headers["Content-Type"] = "application/json"
            request.headers["X-Referral-Source"] = "o3-api"
            request.responseString { req, response, result ->
                val (data, error) = result
                if (error == null) {
                    val id = Gson().fromJson<JsonObject>(data!!)["id"].asString
                    completion(Pair<String?, Error?>(id, null))
                } else {
                    completion(Pair<String?, Error?>(null, Error(error.localizedMessage)))
                }
            }
        }
    }

    fun executeWithdrawal(withdrawalId: String, completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        bg {
            val (data, error) = (baseURL + "/" + Route.TIMESTAMP.routeName()).httpGet().responseString().third
            val timeStamp = Gson().fromJson<JsonObject>(data!!)["timestamp"].asLong
            val jsonPayload = jsonObject("id" to withdrawalId,
                    "timestamp" to timeStamp)

            val signedHex = getSignatureForJsonPayload(jsonPayload)
            jsonPayload.addProperty("signature", signedHex)

            val url = baseURL + Route.WITHDRAWALS.routeName() + "/" + withdrawalId + "/broadcast"
            val request = url.httpPost().body(jsonPayload.toString())
            request.headers["Content-Type"] = "application/json"
            request.headers["X-Referral-Source"] = "o3-api"
            request.responseString { req, response, result ->
                val (data, error) = result
                if (error == null) {
                    completion(Pair<Boolean?, Error?>(true, null))
                } else {
                    completion(Pair<Boolean?, Error?>(false, Error(error.localizedMessage)))
                }
            }
        }
    }

    fun singleStepWithdrawal(asset_id: String, amount: String,
                             contract_hash: String = defaultContract, blockchain: String = "neo",
                             completion: (Pair<Boolean?, Error?>) -> (Unit)) {

        submitWithdrawal(asset_id, amount, contract_hash, blockchain) {
            if (it.second != null) {
                completion(Pair<Boolean?, Error?>(false, it.second))
            } else {
                executeWithdrawal(it.first!!) {
                    completion(Pair<Boolean?, Error?>(it.first, it.second))
                }
            }
        }
    }
    //endregion

    //region Orders
    fun calculatePercentFilled(order: SwitcheoOrders): Pair<Double, Double> {
        var fillSum = 0.0
        var errorMargin = 0.0
        for (make in order.makes) {
            for (trade in make.trades ?: listOf()) {
                errorMargin += 1
                fillSum += trade["filled_amount"].asDouble / order.want_amount.toDouble()
            }
        }

        for (fill in order.fills) {
            if (order.side.toLowerCase() == "buy") {
                errorMargin +=1
                fillSum  += (fill.want_amount.toDoubleOrNull() ?: 0.0) / order.want_amount.toDouble()
            } else {
                errorMargin +=1
                fillSum  += (fill.fill_amount.toDoubleOrNull() ?: 0.0) / order.offer_amount.toDouble()
            }
        }
        val percentFilled = fillSum * 100
        return Pair(percentFilled, errorMargin)
    }

    fun getPendingOrders(contract_hash: String = defaultContract, blockchain: String = "neo", completion: (Pair<List<SwitcheoOrders>?, Error?>) -> (Unit)) {
        val parameters = listOf("blockchain" to blockchain,
                "contract_hash" to contract_hash,
                "address" to Account.getWallet().hashedSignature.reversedArray().toHex().toLowerCase())

        val url = baseURL + Route.ORDERS.routeName()
        val request = url.httpGet(parameters)
        request.headers["Content-Type"] = "application/json"
        request.headers["X-Referral-Source"] = "o3-api"

        request.responseString { req, response, result ->
            val (data, error) = result
            var pendingOrders: MutableList<SwitcheoOrders> = mutableListOf()
            if (error == null) {
                val orders = Gson().fromJson<List<SwitcheoOrders>>(data!!)
                for (order in orders) {
                    if (order.status == "processed") {
                        if (order.makes.find { it.status == "cancelled" } == null) {
                            val percentFilledAndError = calculatePercentFilled(order)
                            if (100.0 - percentFilledAndError.first >= 0.000001 * percentFilledAndError.second) {
                                pendingOrders.add(order)
                            }
                        }
                    }
                }

                completion(Pair<List<SwitcheoOrders>?, Error?>(pendingOrders, null))
            } else {
                completion(Pair<List<SwitcheoOrders>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun submitOrder(pair: String, side: String, price: String, want_amount: String, orderType: String, use_native_tokens: Boolean = false,
                    contract_hash: String = defaultContract, blockchain: String = "neo", completion: (Pair<SwitcheoOrders?, Error?>) -> (Unit)) {
        bg {
            val (data, error) = (baseURL + "/" + Route.TIMESTAMP.routeName()).httpGet().responseString().third
            val timeStamp = Gson().fromJson<JsonObject>(data!!)["timestamp"].asLong
            val jsonPayload = jsonObject("blockchain" to blockchain,
                    "contract_hash" to contract_hash,
                    "order_type" to orderType,
                    "pair" to pair,
                    "price" to price,
                    "side" to side,
                    "timestamp" to timeStamp,
                    "use_native_tokens" to use_native_tokens,
                    "want_amount" to want_amount)
            Log.d("JSON PAYLOD: ",jsonPayload.toString())
            val signedHex = getSignatureForJsonPayload(jsonPayload)
            jsonPayload.addProperty("signature", signedHex)
            jsonPayload.addProperty("address", Account.getWallet().hashedSignature.reversedArray().toHex().toLowerCase())

            val url = baseURL + Route.ORDERS.routeName()
            val request = url.httpPost().body(jsonPayload.toString())
            request.headers["Content-Type"] = "application/json"
            request.headers["X-Referral-Source"] = "o3-api"
            request.responseString { req, response, result ->
                val (data, error) = result
                if (error == null) {
                    val orderRequest = Gson().fromJson<SwitcheoOrders>(data!!)
                    completion(Pair<SwitcheoOrders?, Error?>(orderRequest, null))
                } else {
                    var error = JSONObject(String(response.data)).get("error") as String
                    completion(Pair<SwitcheoOrders?, Error?>(null, Error(error)))
                }
            }
        }
    }

    fun executeOrder(orderRequest: SwitcheoOrders, completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        val makesObject = jsonObject()
        for (makeTx in orderRequest.makes) {
            val makeId = makeTx.id
            val jsonPayloadBytes = serializeTransactionFromJson(Gson().toJsonTree(makeTx.txn!!).asJsonObject)
            val signature = Neoutils.sign(jsonPayloadBytes, Account.getWallet().privateKey.toHex()).toHex().toLowerCase()
            makesObject.addProperty(makeId, signature)
        }

        val fillsObject = jsonObject()
        for (fillTx in orderRequest.fills) {
            val fillId = fillTx.id
            val jsonPayloadBytes = serializeTransactionFromJson(Gson().toJsonTree(fillTx.txn!!).asJsonObject)
            val signature = Neoutils.sign(jsonPayloadBytes, Account.getWallet().privateKey.toHex()).toHex().toLowerCase()
            fillsObject.addProperty(fillId, signature)
        }

        val executionsObject = jsonObject("signatures" to
                jsonObject("makes" to makesObject,
                        "fills" to fillsObject))

        val url = baseURL + Route.ORDERS.routeName() + "/" + orderRequest.id + "/broadcast"
        val request = url.httpPost().body(executionsObject.toString())
        request.headers["Content-Type"] = "application/json"
        request.headers["X-Referral-Source"] = "o3-api"
        request.responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                completion(Pair<Boolean?, Error?>(true, null))
            } else {
                completion(Pair<Boolean?, Error?>(false, Error(error.localizedMessage)))
            }
        }
    }

    fun singleStepOrder(pair: String, side: String, price: String, want_amount: String, orderType: String, use_native_tokens: Boolean = false,
                        contract_hash: String = defaultContract, blockchain: String = "neo", completion: (Pair<SwitcheoOrders?, Error?>) -> (Unit)) {

        submitOrder(pair, side, price, want_amount, orderType, use_native_tokens, contract_hash, blockchain) {
            if (it.second != null) {
                completion(Pair<SwitcheoOrders?, Error?>(null, it.second))
            } else {
                val order = it.first!!
                executeOrder(order) {
                    if (it.first == true) {
                        completion(Pair<SwitcheoOrders?, Error?>(order, it.second))
                    } else {
                        completion(Pair<SwitcheoOrders?, Error?>(null, it.second))
                    }
                }
            }
        }
    }

    fun submitCancelOrder(orderId: String, completion: (Pair<SwitcheoCancellationTransaction?, Error?>) -> Unit) {
        bg {
            val (data, error) = (baseURL + "/" + Route.TIMESTAMP.routeName()).httpGet().responseString().third
            val timeStamp = Gson().fromJson<JsonObject>(data!!)["timestamp"].asLong
            val jsonPayload = jsonObject("order_id" to orderId,
                    "timestamp" to timeStamp)

            val signedHex = getSignatureForJsonPayload(jsonPayload)
            jsonPayload.addProperty("signature", signedHex)
            jsonPayload.addProperty("address", Account.getWallet().hashedSignature.reversedArray().toHex().toLowerCase())

            val url = baseURL + Route.CANCELLATIONS.routeName()
            val request = url.httpPost().body(jsonPayload.toString())
            request.headers["Content-Type"] = "application/json"
            request.headers["X-Referral-Source"] = "o3-api"
            request.responseString { req, response, result ->
                val (data, error) = result
                val cancelRequest = Gson().fromJson<SwitcheoCancellationTransaction>(data!!)
                if (error == null) {
                    completion(Pair<SwitcheoCancellationTransaction?, Error?>(cancelRequest, null))
                } else {
                    completion(Pair<SwitcheoCancellationTransaction?, Error?>(cancelRequest, Error(error.localizedMessage)))
                }
            }
        }
    }

    fun executeCancelOrder(orderId: String, transactionJson: JsonObject, completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        val jsonPayloadBytes = serializeTransactionFromJson(transactionJson)
        val signature = Neoutils.sign(jsonPayloadBytes, Account.getWallet().privateKey.toHex()).toHex().toLowerCase()
        val url = baseURL + Route.CANCELLATIONS.routeName() + "/" + orderId + "/broadcast"

        val jsonPayload = jsonObject("signature" to signature)
        val request = url.httpPost().body(jsonPayload.toString())
        request.headers["Content-Type"] = "application/json"
        request.headers["X-Referral-Source"] = "o3-api"
        request.responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                completion(Pair<Boolean?, Error?>(true, null))
            } else {
                completion(Pair<Boolean?, Error?>(false, Error(error.localizedMessage)))
            }
        }
    }

    fun singleStepCancel(orderId: String,
                         completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        submitCancelOrder(orderId) {
            if (it.second != null) {
                completion(Pair<Boolean?, Error?>(false, it.second))
            } else {
                executeCancelOrder(it.first!!.id, it.first!!.transaction!!) {
                    completion(Pair<Boolean?, Error?>(it.first, it.second))
                }
            }
        }
    }
    //endregion

    //region Balances
    fun getBalances(addresses: Array<String> = arrayOf(Account.getWallet().hashedSignature.reversedArray().toHex().toLowerCase()), contract_hashes: Array<String> = arrayOf(defaultContract), completion: (Pair<ContractBalance?, Error?>) -> (Unit)) {
        val url = baseURL + Route.BALANCES.routeName()
        val parameters = listOf("addresses" to addresses, "contract_hashes" to contract_hashes)
        url.httpGet(parameters).responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                val contractBalance = Gson().fromJson<ContractBalance>(data!!)
                completion(Pair<ContractBalance?, Error?>(contractBalance, null))
            } else {
                completion(Pair<ContractBalance?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
    //endregion
}

fun SwitcheoOrders.calculatePercentFilled(): Pair<Double, Double> {
    var fillSum = 0.0
    var errorMargin = 0.0
    for (make in this.makes) {
        for (trade in make.trades ?: listOf()) {
            errorMargin += 1
            fillSum += trade["filled_amount"].asDouble / this.want_amount.toDouble()
        }
    }

    for (fill in this.fills) {
        errorMargin +=1
        fillSum  += (fill.want_amount.toDoubleOrNull() ?: 0.0) / this.want_amount.toDouble()
    }
    val percentFilled = fillSum * 100
    return Pair(percentFilled, errorMargin)
}




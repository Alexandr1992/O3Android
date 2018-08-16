package network.o3.o3wallet.API.Switcheo

import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import neoutils.Neoutils
import neoutils.Neoutils.generateFromWIF
import neoutils.Wallet
import network.o3.o3wallet.*
import org.jetbrains.anko.coroutines.experimental.bg
import java.util.*

class SwitcheoAPI {
    val baseTestUrl = "https://test-api.switcheo.network/v2/"
    val baseMainUrl = "https://api.switcheo.network/v2/"

   val baseURL = if (PersistentStore.getNetworkType() == "Main") {
        baseMainUrl
    }  else {
        baseTestUrl
    }

    enum class Route {
        TICKERS,
        OFFERS,
        TRADES,
        DEPOSITS,
        WITHDRAWALS,
        ORDERS,
        ADDRESS,
        AMOUNT,
        BALANCES,
        PAIRS,
        CONTRACTS;

        fun routeName(): String {
            return this.name.toLowerCase(Locale.US)
        }
    }

    fun getDailyTickers(completion: (Pair<Array<Ticker>?, Error?>) -> (Unit)) {
        val url = baseURL + Route.TICKERS.routeName() + "/last_24_hours"
        var request = url.httpGet()
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

    fun getOffersForPair(pair: String, blockchain: String = "neo", completion: (Pair<Array<Offer>?, Error?>) -> (Unit)) {
        val url = baseURL + Route.OFFERS.routeName()
        var request = url.httpGet(
                listOf("blockchain" to blockchain, "pair" to pair ))

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

    fun submitDeposit(asset_id: String, amount: String,
                      contract_hash: String, blockchain: String = "neo",
                      completion: (Pair<SwitcheoTransaction?, Error?>) -> (Unit)) {
        val url = baseURL + Route.DEPOSITS.routeName()
        val timeStamp = System.currentTimeMillis()

        val jsonPayload = jsonObject(
                "amount" to amount,
                "asset_id" to asset_id,
                "blockchain" to blockchain,
                "contract_hash" to contract_hash,
                "timestamp" to timeStamp)

        val jsonPayloadBytes = jsonPayload.toString().toByteArray()
        var byteCountHex = jsonPayloadBytes.count().toString(16)
        if (byteCountHex.length % 2 != 0) {
            byteCountHex = "0" + byteCountHex
        }
        val finalHex = "010001f0" + byteCountHex + jsonPayloadBytes.toHex().toLowerCase() + "0000"
        Log.d("SWITCHEO:", finalHex)
        val signedHex = Neoutils.sign(finalHex.hexStringToByteArray(), Account.getWallet()!!.privateKey.toHex())

        val jsonSignedPayload = jsonObject(
                "amount" to amount,
                "asset_id" to asset_id,
                "blockchain" to blockchain,
                "contract_hash" to contract_hash,
                "timestamp" to timeStamp,
                "address" to Account.getWallet()!!.address.hashFromAddress().toLowerCase(),
                "signature" to signedHex.toHex().toLowerCase())

        val request = url.httpPost().body(jsonSignedPayload.toString())
        request.headers["Content-Type"] = "application/json"
        request.responseString { req, response, result ->
            Log.d("SWITCHEO:", response.responseMessage)
            val (data, error) = result
            if (error == null) {
                val tx = Gson().fromJson<SwitcheoTransaction>(data!!)
                completion(Pair<SwitcheoTransaction?, Error?>(tx, null))
            } else {
                completion(Pair<SwitcheoTransaction?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun executeDeposit(depositId: String, transactionJson: String, completion: (Pair<Boolean?, Error?>) -> (Unit)) {
        val jsonPayloadBytes = transactionJson.toByteArray()
        val signature = Neoutils.sign(jsonPayloadBytes, Account.getWallet()!!.privateKey.toHex()).toHex().toLowerCase()
        val url = baseURL + Route.DEPOSITS.routeName() + "/" + depositId + "/broadcast"

        val request = url.httpPost().body(jsonObject("signature" to signature).toString())
        request.headers["Content-Type"] = "application/json"
        request.responseString { req, response, result ->
            val (data, error) = result
            if (error == null) {
                completion(Pair<Boolean?, Error?>(true, null))
            } else {
                completion(Pair<Boolean?, Error?>(false, null))
            }
        }
    }

    fun singleStepDeposit(asset_id: String, amount: String,
        contract_hash: String, blockchain: String = "neo",
        completion: (Pair<Boolean?, Error?>) -> (Unit)) {

        submitDeposit(asset_id, amount, contract_hash, blockchain) {
            if (it.second != null) {
                completion(Pair<Boolean?, Error?>(false, it.second))
            } else {
                executeDeposit(it.first!!.id, it.first!!.transaction.toString()) {
                    completion(Pair<Boolean?, Error?>(it.first, it.second))
                }
            }
        }
    }
}
package network.o3.o3wallet.API.O3Platform

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.PersistentStore
import java.util.*

/**
 * Created by drei on 11/24/17.
 */

class O3PlatformClient {
    val baseAPIURL = "https://platform.o3.network/api/v1/neo/"
    enum class Route {
        CLAIMABLEGAS,
        BALANCES,
        UTXO;

        fun routeName(): String {
            return this.name.toLowerCase(Locale.US)
        }
    }

    fun networkQueryString(): String {
        if (PersistentStore.getNetworkType() == "Main") {
            return ""
        } else if (PersistentStore.getNetworkType() == "Test") {
            return "?network=test"
        } else {
            return "?network=private"
        }
    }


    fun getClaimableGAS(address: String, completion: (Pair<ClaimData?, Error?>) -> Unit) {
        val url = baseAPIURL + address + "/" + Route.CLAIMABLEGAS.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] =  "O3Android"
        request.responseString { _, _, result ->
            val (data, error) = result

            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val claims = Gson().fromJson<ClaimData>(platformResponse.result)
                completion(Pair<ClaimData?, Error?>(claims, null))
            } else {
                completion(Pair<ClaimData?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getClaimableGasBlocking(address: String) : ClaimData? {
        val url = baseAPIURL + address + "/" + Route.CLAIMABLEGAS.routeName() + networkQueryString()
        var request = url.httpGet()
        request.timeoutInMillisecond = 5000
        request.headers["User-Agent"] =  ""
        val (_, _, result) = request.responseString()
        val (data, error) = result
        if (error == null) {
            val gson = Gson()
            val platformResponse = gson.fromJson<PlatformResponse>(data!!)
            val claims = Gson().fromJson<ClaimData>(platformResponse.result)
            return claims
        }
        return null
    }

    fun getUTXOS(address: String, completion: (Pair<UTXOS?, Error?>) -> Unit) {
        val url = baseAPIURL + address + "/" + Route.UTXO.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] =  "O3Android"
        request.timeout(600000).responseString { _, _, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val assets = Gson().fromJson<UTXOS>(platformResponse.result)
                completion(Pair<UTXOS?, Error?>(assets, null))
            } else {
                completion(Pair<UTXOS?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getTransferableAssets(address: String, completion: (Pair<TransferableAssets?, Error?>) -> Unit) {
        val url = baseAPIURL + address + "/" + Route.BALANCES.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = ""
        request.timeout(600000).responseString { _, _, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val balanceData = Gson().fromJson<TransferableBalanceData>(platformResponse.result)
                completion(Pair<TransferableAssets?, Error?>(TransferableAssets(balanceData.data), null))
            } else {
                completion(Pair<TransferableAssets?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
}

package network.o3.o3wallet.API.O3

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import neoutils.Neoutils
import network.o3.o3wallet.API.NEO.AccountAsset
import network.o3.o3wallet.API.NEO.NEP5Token
import network.o3.o3wallet.API.NEO.NEP5Tokens
import network.o3.o3wallet.API.O3Platform.PlatformResponse
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.PersistentStore
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

/**
 * Created by drei on 11/24/17.
 */

class O3API {
    val baseURL = "http://api.o3.network/v1/"
    enum class Route {
        PRICE,
        HISTORICAL,
        FEED;

        fun routeName(): String {
            return this.name.toLowerCase(Locale.US)
        }
    }

    fun getPriceHistory(symbol: String, interval: String, completion: (Pair<PriceHistory?, Error?>) -> (Unit)) {
        val url = baseURL + Route.PRICE.routeName() + "/" + symbol + String.format("?i=%s", interval)
        var request = url.httpGet()
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val o3Response = gson.fromJson<O3Response>(data!!)
                println(o3Response.result["data"])
                val history = gson.fromJson<PriceHistory>(o3Response.result["data"])
                completion(Pair<PriceHistory?, Error?>(history, null))
            } else {
                completion(Pair<PriceHistory?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getPortfolio(assets: ArrayList<TransferableAsset>, interval: String, completion: (Pair<Portfolio?, Error?>) -> Unit) {
        var queryString = String.format(Locale.US, "?i=%s", interval)
        if(assets.isEmpty()) {
            queryString = queryString + String.format(Locale.US, "&%s=%.8f", "NEO", 0.0)
            queryString = queryString + String.format(Locale.US, "&%s=%.8f", "GAS", 0.0)
        }

        for (asset in assets) {
            queryString = queryString + String.format(Locale.US, "&%s=%.8f", asset.symbol, asset.value)
        }
        queryString += String.format("&currency=%s", PersistentStore.getCurrency())

        val url = baseURL + Route.HISTORICAL.routeName() + queryString
        var request = url.httpGet()
        request.responseString { request, response, result ->
           // print (request)
           // print (response)
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val o3Response = gson.fromJson<O3Response>(data!!)
                println(o3Response.result["data"])
                val history = gson.fromJson<Portfolio>(o3Response.result["data"])
                completion(Pair<Portfolio?, Error?>(history, null))
            } else {
                completion(Pair<Portfolio?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getNewsFeed(completion: (Pair<FeedData?, Error?>) -> Unit) {
        val url = "https://staging-api.o3.network/v1/feed/"/*baseURL + Route.FEED.routeName()*/
        url.httpGet().responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val o3Response = gson.fromJson<O3Response>(data!!)
                val feed = gson.fromJson<FeedData>(o3Response.result["data"])
                completion(Pair(feed, null))
            } else {
                completion(Pair(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getFeatures(completion: (Pair<Array<Feature>?, Error?>) -> Unit) {
        var url = "https://platform.o3.network/api/v1/neo/news/featured"
        if (PersistentStore.getNetworkType() == "Test") {
            url = "https://platform.o3.network/api/v1/neo/news/featured?network=test"
        } else if (PersistentStore.getNetworkType() == "Private") {
            url = "https://platform.o3.network/api/v1/neo/news/featured?network=private"
        }
        url.httpGet().responseString {request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val featureFeedData = gson.fromJson<FeatureFeedData>(platformResponse.result)
                completion(Pair(featureFeedData.data.features, null))
            } else {
                completion(Pair(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getTokenSales(address: String, completion: (Pair<TokenSales?, Error?>) -> Unit) {
        var url = "https://platform.o3.network/api/v1/neo/" + address + "/tokensales"
        if (PersistentStore.getNetworkType() == "Test") {
            url = "https://platform.o3.network/api/v1/neo/" + address + "/tokensales?network=test"
        } else if (PersistentStore.getNetworkType() == "Private") {
            url = "https://platform.o3.network/api/v1/neo/" + address + "tokensales?network=private"
        }

        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.headers["Version"] = O3Wallet.version ?: ""
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val tokenSales = gson.fromJson<TokenSales>(data!!)
                completion(Pair(tokenSales, null))
            } else {
                completion(Pair(null, Error(error.localizedMessage)))
            }
        }
    }
}
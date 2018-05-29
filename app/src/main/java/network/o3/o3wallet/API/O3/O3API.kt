package network.o3.o3wallet.API.O3

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.API.NEO.AccountAsset
import network.o3.o3wallet.API.NEO.NEP5Token
import network.o3.o3wallet.API.NEO.NEP5Tokens
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
            return this.name.toLowerCase()
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
        var queryString = String.format("?i=%s", interval)
        for (asset in assets) {
            queryString = queryString + String.format(Locale.US, "&%s=%.8f", asset.symbol, asset.value)
        }

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

    fun getAvailableNEP5Tokens(completion: (Pair<Array<NEP5Token>?, Error?>) -> Unit) {
        var url = "https://o3.network/settings/nep5.json"
        val isPrivateNet =  O3Wallet.appContext!!.defaultSharedPreferences.getBoolean("USING_PRIVATE_NET", false)
        if (PersistentStore.getNetworkType() == "Test") {
            url = "https://s3-ap-northeast-1.amazonaws.com/network.o3.cdn/data/nep5.test.json"
        } else if (PersistentStore.getNetworkType() == "Private") {
            url = "https://s3-ap-northeast-1.amazonaws.com/network.o3.cdn/data/nep5.private.json"
        }
        url.httpGet().responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val tokens = gson.fromJson<NEP5Tokens>(data!!)
                completion(Pair(tokens.nep5tokens, null))
            } else {
                completion(Pair(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getFeatures(completion: (Pair<Array<Feature>?, Error?>) -> Unit) {
        val url = "https://cdn.o3.network/data/featured.json"
        url.httpGet().responseString {request, response, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val featureFeed = gson.fromJson<FeatureFeed>(data!!)
                completion(Pair(featureFeed.features, null))
            } else {
                completion(Pair(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getTokenSales(completion: (Pair<TokenSales?, Error?>) -> Unit) {
        var url = "https://cdn.o3.network/data/tokensales.json"
        if (PersistentStore.getNetworkType() == "Test") {
            url = "https://s3-ap-northeast-1.amazonaws.com/network.o3.cdn/data/___tokensale.json"
        } else if (PersistentStore.getNetworkType() == "Private") {
            url = "https://s3-ap-northeast-1.amazonaws.com/network.o3.cdn/data/___tokensale.json"
        }

        url.httpGet().responseString { request, response, result ->
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
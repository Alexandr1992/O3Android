package network.o3.o3wallet.API.O3Platform

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.O3Wallet
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
        NEP5,
        INBOX,
        VERIFICATION,
        PRICING,
        NODES,
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
        request.headers["User-Agent"] = "O3Android"
        request.headers["Version"] = O3Wallet.version ?: ""
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
        request.headers["User-Agent"] = "O3Android"
        request.headers["Version"] = O3Wallet.version ?: ""
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
        request.headers["User-Agent"] = "O3Android"
        request.headers["Version"] = O3Wallet.version ?: ""
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
        request.headers["User-Agent"] = "O3Android"
        request.headers["Version"] = O3Wallet.version ?: ""
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

    fun getTokenListings(completion: (Pair<TokenListings?, Error?>) -> Unit) {
        val url = baseAPIURL + "/" + Route.NEP5.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.headers["Version"] = O3Wallet.version ?: ""
        request.timeout(600000).responseString { _, _, result ->
            val(data, error)  = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val nep5Data = Gson().fromJson<TokenListingsData>(platformResponse.result)
                completion(Pair<TokenListings?, Error?>(nep5Data.data, null))
            } else {
                completion(Pair<TokenListings?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getInbox(address: String, completion: (Pair<List<O3InboxItem>?, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/" + Route.INBOX.routeName() + "/" + address + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.timeout(600000).responseString {_, _, result ->
            val(data, error) = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val inboxData = Gson().fromJson<O3Inbox>(platformResponse.result)
                val items: List<O3InboxItem> = inboxData.data
                completion(Pair<List<O3InboxItem>?, Error?>(items, null))
            } else {
                completion(Pair<List<O3InboxItem>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getVerifiedAddress(address: String, completion: (Pair<VerifiedAddress?, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/" + Route.VERIFICATION.routeName() + "/"  + address + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.timeout(600000).responseString {_, _, result ->
            val(data, error) = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val verifiedAddressData = Gson().fromJson<VerifiedAddressData>(platformResponse.result)
                val verifiedAddress: VerifiedAddress = verifiedAddressData.data
                completion(Pair<VerifiedAddress?, Error?>(verifiedAddress, null))
            } else {
                completion(Pair<VerifiedAddress?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getRealTimePrice(token: String, currency: String, completion: (Pair<O3RealTimePrice?, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/" + Route.PRICING.routeName() + "/"  + token + "/" + currency + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.timeout(600000).responseString {_, _, result ->
            val(data, error) = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val realTimePricingData = Gson().fromJson<O3RealTimePriceData>(platformResponse.result)
                val realTimePricing: O3RealTimePrice = realTimePricingData.data
                completion(Pair<O3RealTimePrice?, Error?>(realTimePricing, null))
            } else {
                completion(Pair<O3RealTimePrice?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getChainNetworks(completion: (Pair<ChainNetwork?, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/" + Route.NODES.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.timeout(600000).responseString {_, _, result ->
            val(data, error) = result
            if (error == null) {
                val platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val chainNetworkData = Gson().fromJson<ChainNetworkData>(platformResponse.result)
                val chainNetwork = chainNetworkData.data
                completion(Pair<ChainNetwork?, Error?>(chainNetwork, null))
            } else {
                completion(Pair<ChainNetwork?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
}

package network.o3.o3wallet.API.O3Platform

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.API.O3.TokenSaleLogSigned
import network.o3.o3wallet.Account
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.PersistentStore
import java.text.SimpleDateFormat
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
        UNBOUNDONG,
        HISTORY,
        TRADING,
        ORDERS,
        DAPPS,
        NNS,
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
            return "?network=test"
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

    fun getMarketPlace(completion: (Pair<TokenListings?, Error?>) -> Unit) {
        val url = "https://api.o3.network/v1/marketplace" + networkQueryString()
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

    fun getNep5(completion: (Pair<NEP5Tokens?, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/neo/" + Route.NEP5.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.headers["Version"] = O3Wallet.version ?: ""
        request.timeout(600000).responseString { _, _, result ->
            val(data, error)  = result
            if (error == null) {
                val gson = Gson()
                val platformResponse = gson.fromJson<PlatformResponse>(data!!)
                val nep5Data = Gson().fromJson<NEP5TokensData>(platformResponse.result)
                completion(Pair<NEP5Tokens?, Error?>(nep5Data.data, null))
            } else {
                completion(Pair<NEP5Tokens?, Error?>(null, Error(error.localizedMessage)))
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
        val url = "https://platform.o3.network/api/v1/" + Route.PRICING.routeName() + "/"  + token.toLowerCase() + "/" + currency.toLowerCase() + networkQueryString()
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

    fun getOntologyCalculatedGas(address: String, completion: (Pair<OntologyClaimableGas?, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/ont/" + address + "/" + Route.UNBOUNDONG.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.timeout(600000).responseString { _, _, result ->
            val(data, error) = result
            if (error == null) {
                val (data, error) = result
                if (error == null) {
                    val platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                    val calculatedGasData = Gson().fromJson<OntologyClaimableGasData>(platformResponse.result)
                    val calculatedGas = calculatedGasData.data
                    completion(Pair<OntologyClaimableGas?, Error?>(calculatedGas, null))
                } else {
                    completion(Pair<OntologyClaimableGas?, Error>(null, Error(error.localizedMessage)))
                }
            }
        }
    }

    fun postTokenSaleLog(address: String, companyID: String, tokenSaleLog: TokenSaleLogSigned, completion: (Pair<Boolean, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/neo/" + address + "/tokensales/" + companyID
        var request = url.httpPost()
        request.headers["User-Agent"] = "O3Android"
        request.body(Gson().toJson(tokenSaleLog)).timeout(600000).responseString {request, response, result ->
            val(data, error) = result
            if (error == null) {
                val (data, error) =result
                if (error == null) {
                    val platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                    if (platformResponse.code == 200) {
                        completion(Pair<Boolean, Error?>(true, null))
                    } else {
                        completion(Pair<Boolean, Error?>(false, Error("Something went wrong when posting transaction log")))
                    }
                }
            }
        }
    }

    fun getTransactionHistory(page: Int, completion: (Pair<TransactionHistory?, Error?>) -> (Unit)) {
        val url = "https://platform.o3.network/api/v1/"  + Route.HISTORY.routeName() + "/" + Account.getWallet().address + "?p=" + page.toString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                val platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val history = Gson().fromJson<TransactionHistory>(platformResponse.result.asJsonObject["data"])
                completion(Pair<TransactionHistory?, Error?>(history, null))
            } else {
                completion(Pair<TransactionHistory?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun getDapps(completion: (Pair<List<Dapp>?, Error?>) -> (Unit)) {
        val url = "https://platform.o3.network/api/v1/" + Route.DAPPS.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                var platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val dapps = Gson().fromJson<List<Dapp>>(platformResponse.result.asJsonObject["data"])
                completion(Pair<List<Dapp>?, Error?>(dapps, null))
            } else {
                completion(Pair<List<Dapp>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }


    fun getTradingAccounts(completion: (Pair<TradingAccount?, Error?>) -> (Unit)) {
        val url = "https://platform.o3.network/api/v1/" + Route.TRADING.routeName() + "/" + Account.getWallet().address + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                var platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val tradingAccount = Gson().fromJson<TradingAccount>(platformResponse.result.asJsonObject["data"])
                completion(Pair<TradingAccount?, Error?>(tradingAccount, null))
            } else {
                completion(Pair<TradingAccount?, Error?>(null, Error(error.localizedMessage)))
            }

        }
    }

    fun getOrders(completion: (Pair<O3Orders?, Error?>) -> (Unit) ) {
        val url = "https://platform.o3.network/api/v1/" + Route.TRADING.routeName() + "/" +
                Account.getWallet().address + "/" + Route.ORDERS.routeName() + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                var platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val o3Orders = Gson().fromJson<O3Orders>(platformResponse.result.asJsonObject["data"])
                completion(Pair<O3Orders?, Error?>(o3Orders, null))
            } else {
                completion(Pair<O3Orders?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
    fun calculatePercentFilled(order: O3SwitcheoOrders): Pair<Double, Double> {
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

    //SWITCHEO API currently has an error margin of 0.000001 percent on each make and trade
    //we have to account for this to make sure a "filled" order is not mistreeated as
    //still open
    fun getPendingOrders(completion: (Pair<List<O3SwitcheoOrders>?, Error?>) -> (Unit)) {
        getOrders {
            if (it.second == null) {
                val orders = it.first!!.switcheo
                var pendingOrders: MutableList<O3SwitcheoOrders> = mutableListOf()
                //open not filled
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
                //closed orders
                var closedOrders: MutableList<O3SwitcheoOrders> = mutableListOf()
                for (order in orders) {
                    if (order.status == "processed") {
                        if (order.makes.find { it.status == "cancelled" } != null) {
                            if (calculatePercentFilled(order).first > 0.0) {
                                closedOrders.add(order)
                            }
                        }
                    }
                }
                //fully filled
                for (order in orders) {
                    if (order.status == "processed") {
                        if (order.makes.find { it.status == "cancelled" } == null) {
                            val percentFilledAndError = calculatePercentFilled(order)
                            if (100.0 - percentFilledAndError.first < 0.000001 * percentFilledAndError.second) {
                                closedOrders.add(order)
                            }
                        }
                    }
                }
                val df1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                pendingOrders.sortBy { df1.parse(it.created_at).time}
                pendingOrders.reverse()
                closedOrders.sortBy { df1.parse(it.created_at).time }
                closedOrders.reverse()
                pendingOrders.addAll(closedOrders)

                completion(Pair<List<O3SwitcheoOrders>?, Error?>(pendingOrders, null))
            } else {
                completion(Pair<List<O3SwitcheoOrders>?, Error?>(null, Error(it.second!!.localizedMessage)))
            }
        }
    }

    //nns address must end in .neo
    fun resolveNNS(nnsQuery: String, completion: (Pair<String?, Error?>) -> Unit) {
        val url =  "https://platform.o3.network/api/v1/neo/" + Route.NNS.routeName() + "/" + nnsQuery + networkQueryString()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                var platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val nns = Gson().fromJson<ResolvedNNS>(platformResponse.result.asJsonObject["data"])
                completion(Pair<String?, Error?>(nns.address, null))
            } else {
                completion(Pair<String?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    fun nnsReverseLookup(address: String, completion: (Pair<List<ReverseLookupNNS>?, Error?>) -> Unit) {
        val url = "https://platform.o3.network/api/v1/neo/" + Route.NNS.routeName() +  "/" + address + "/domains"
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                var platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val domains = Gson().fromJson<List<ReverseLookupNNS>>(platformResponse.result.asJsonObject["data"])
                completion(Pair<List<ReverseLookupNNS>?, Error?>(domains, null))
            } else {
                completion(Pair<List<ReverseLookupNNS>?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }

    //default value will be two in case of error
    data class SwitcheoPair(val name: String, val precision: Int)
    fun getPrecisionForPair(baseAsset: String, otherAsset: String, completion: (Int) -> Unit) {
        val url = "https://platform.o3.network/api/v1/trading/switcheo/pairs"
        val pair = otherAsset.toUpperCase() + "_" + baseAsset.toUpperCase()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                var platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val pairs = Gson().fromJson<List<SwitcheoPair>>(platformResponse.result.asJsonObject["data"])
                val correctPair = pairs.find{ it.name == pair}
                completion(correctPair?.precision ?: 2)
            } else {
                completion(2)
            }
        }
    }

    data class SwitcheoToken(val id: String, val name: String, val symbol: String, val decimals: Int, val precision: Int)
    fun getPrecisionForToken(tokenSymbol: String, completion: (Int) -> Unit) {
        val url = "https://platform.o3.network/api/v1/trading/switcheo/pairs"
        val token = tokenSymbol.toUpperCase()
        var request = url.httpGet()
        request.headers["User-Agent"] = "O3Android"
        request.responseString { request, response, result ->
            val (data, error) = result
            if (error == null) {
                var platformResponse = Gson().fromJson<PlatformResponse>(data!!)
                val pairs = Gson().fromJson<List<SwitcheoToken>>(platformResponse.result.asJsonObject["data"])
                val correctPair = pairs.find{ it.symbol == token}
                completion(correctPair?.precision ?: 2)
            } else {
                completion(2)
            }
        }
    }
}

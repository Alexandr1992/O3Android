package network.o3.o3wallet.API.O3Platform

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import network.o3.o3wallet.API.NEO.Transaction
import org.json.JSONObject
import java.math.BigDecimal

/**
 * Created by drei on 11/24/17.
 */


data class PlatformResponse(val code: Int, val result: JsonElement)


data class TransactionHistory(val totalPage: Int, val pageIndex: Int, val history: Array<TransactionHistoryEntry>)

data class TransactionHistoryEntry(val blockchain: String, val txid: String, val time: Long,
                                   val blockHeight: Long, val asset: TokenListing,
                                   val amount: String, val to: String,
                                   val from: String)


data class TokenListingsData(val data: TokenListings)

data class TokenListings(val assets: Array<TokenListing>,
                         val nep5: Array<TokenListing>)

data class NEP5TokensData(val data: NEP5Tokens)

data class NEP5Tokens(val nep5tokens: Array<TokenListing>)

data class TokenListing(val logoURL: String,
                        val logoSVG: String,
                        val url: String,
                        val tokenHash: String,
                        val name: String,
                        val symbol: String,
                        val decimal: Int,
                        val totalSupply: Int)

data class ClaimData(val data: Claimable)

data class Claimable(val gas: String, val claims: Array<UTXO>)

data class UTXOS(val data: Array<UTXO>)

data class UTXO(val asset: String, val index: Int, val txid: String, val value: String,  val createdAtBlock: Int)

data class TransferableBalanceData(val data: TransferableBalances)
data class TransferableBalances(val version: String, val address: String, val scriptHash: String, val assets: Array<TransferableBalance>,
                                val nep5Tokens: Array<TransferableBalance>,
                                val ontology: Array<TransferableBalance>)

data class TransferableBalance(val id: String, val name: String, val value: String, val symbol: String, val decimals: Int)

data class O3Inbox(val data: List<O3InboxItem>)
data class O3InboxItem(val title: String, val subtitle: String, val description: String,
                       val actionTitle: String, val actionURL: String, val readmoreTitle: String, val readmoreURL: String, val iconURL: String)

data class VerifiedAddressData(val data: VerifiedAddress)
data class VerifiedAddress(val address: String, val publicKey: String, val displayName: String)

data class O3RealTimePriceData(val data: O3RealTimePrice)
data class O3RealTimePrice(val symbol: String, val currency: String, val price: Double, val lastUpdate: Long)

data class ChainNetworkData(val data: ChainNetwork)
data class ChainNetwork(val neo: NetworkStatus, val ontology: NetworkStatus)
data class NetworkStatus(val blockcount: Int, val best: String, val nodes: List<String>)

data class OntologyClaimableGasData(val data: OntologyClaimableGas)
data class OntologyClaimableGas(val ong: String, val calculated: Boolean)

class TransferableAssets(private val balances: TransferableBalances) {
    var version: String
    var address: String
    var scriptHash: String
    var assets: ArrayList<TransferableAsset> = arrayListOf()
    var tokens: ArrayList<TransferableAsset> = arrayListOf()
    var ontology: ArrayList<TransferableAsset> = arrayListOf()

    override fun equals(other: Any?): Boolean {
        other as TransferableAssets
        for (asset in assets) {
            if(other.assets.contains(asset) == false) {
                return false
            }
        }
        return true
    }

    init {
        version = balances.version
        address = balances.address
        scriptHash = balances.scriptHash
        for (asset in balances.assets) {
            assets.add(TransferableAsset(asset))
        }
        for (ontAsset in balances.ontology) {
            assets.add(TransferableAsset(ontAsset))
        }
        for (token in balances.nep5Tokens) {
            assets.add(TransferableAsset(token))
        }
    }
}

class TransferableAsset(val asset: TransferableBalance) {
    var id: String
    var name: String
    var value: BigDecimal
    var symbol: String
    var decimals: Int

    override fun equals(other: Any?): Boolean {
        other as TransferableAsset
        if (other.id == this.id && other.value == this.value) {
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    init {
        id = asset.id
        name = asset.name
        decimals = asset.decimals
        symbol = asset.symbol
        value = BigDecimal(asset.value)
        if (asset.symbol.toUpperCase() != "GAS") {
            value = value.divide(BigDecimal(Math.pow(10.0, decimals.toDouble())), decimals, BigDecimal.ROUND_HALF_UP)
        }
        print(value)

    }

    fun deepCopy(): TransferableAsset {
        var copyValue: BigDecimal
        copyValue = value.multiply(BigDecimal(Math.pow(10.0, decimals.toDouble())))
        if (asset.symbol.toUpperCase() == "GAS") {
            copyValue = copyValue.divide(BigDecimal(Math.pow(10.0, decimals.toDouble())), decimals, BigDecimal.ROUND_HALF_UP)
        }
        val balance = TransferableBalance(asset.id, asset.name, copyValue.toPlainString(), asset.symbol, asset.decimals)
        return TransferableAsset(balance)
    }
}



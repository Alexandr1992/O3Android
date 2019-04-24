package network.o3.o3wallet.API.O3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.text.NumberFormat
import java.util.*

/**
 * Created by drei on 11/24/17.
 */

data class O3Response(var code: Int, var result: JsonObject)


data class PriceData (val currency: String,
                      val average: Double,
                      val averageUSD: Double,
                      val averageBTC: Double,
                      val time: String)

data class PriceHistory(val symbol: String,
                        val currency: String,
                        val data: Array<PriceData>)

data class Portfolio(val price: Map<String, PriceData>,
                          val firstPrice: Map<String, PriceData>,
                          val data: Array<PriceData>)

data class PortfolioValue(val total: String, val currency: String)

data class FeedData(val features: JsonArray, val items: Array<FeedItem>)

data class NewsImage(val title: String, val url: String)

data class FeedItem(val title: String,
                    val description: String,
                    val link: String,
                    val published: String,
                    val source: String,
                    val images: Array<NewsImage>)

data class FeatureFeedData(val data: FeatureFeed)
data class FeatureFeed(val features: Array<Feature>)

data class Feature(val category: String,
                   val title: String,
                   val subtitle: String,
                   val imageURL: String,
                   val createdAt: Int,
                   val index: Int,
                   val actionTitle: String,
                   val actionURL: String)

data class TokenSales(val subscribeURL: String, val live: Array<TokenSale>)

data class TokenSale(val name: String,
                     val symbol: String,
                     val shortDescription: String,
                     val scriptHash: String,
                     val webURL: String,
                     val imageURL: String,
                     val squareLogoURL: String,
                     val startTime: Long,
                     val endTime: Long,
                     val acceptingAssets: Array<AcceptingAsset>,
                     val info: Array<InfoRow>,
                     val footer: Array<FooterRow>,
                     val companyID: String,
                     val address: String,
                     val kycStatus: KycStatus)

data class KycStatus(val address: String,
                     val verified: Boolean)

data class AcceptingAsset(val asset: String,
                          val basicRate: Double,
                          val min: Double,
                          val max: Double,
                          val price: RealTimePricing)

data class RealTimePricing(val symbol: String,
                           val currency: String,
                           val price: Double,
                           val lastUpdate: Int)

data class TokenSaleLogSigned(val data: TokenSaleLog, val signature: String, val publicKey: String)


class TokenSaleLog(amount: String, asset: AcceptingAsset, txid: String) {
    var amount: String
    var asset: AcceptingAssetStringified
    var txid: String
    init {
        this.amount = amount
        this.txid = txid
        this.asset = AcceptingAssetStringified(asset)

    }

    class AcceptingAssetStringified(asset: AcceptingAsset) {
        var asset: String
        var basicRate: String
        var max: String
        var min: String
        var price: RealTimePricingStringified
        init {
            var formatter = NumberFormat.getNumberInstance(Locale.US)
            formatter.maximumFractionDigits = 8
            formatter.isGroupingUsed = false
            this.asset = asset.asset
            this.basicRate = formatter.format(asset.basicRate)
            this.min = formatter.format(asset.min)
            this.max = formatter.format(asset.max)
            this.price = RealTimePricingStringified(asset.price)
        }

        class RealTimePricingStringified(price: RealTimePricing) {
            var symbol: String
            var currency: String
            var price: String
            var lastUpdate: String
            init {
                var formatter = NumberFormat.getNumberInstance(Locale.US)
                formatter.maximumFractionDigits = 8
                formatter.isGroupingUsed = false
                this.symbol = price.symbol
                this.currency = price.currency
                this.price = formatter.format(price.price)
                this.lastUpdate = formatter.format(price.lastUpdate)
            }
        }
    }
}

data class InfoRow(val label: String,
                   val value: String)

data class FooterRow(val label: String,
                     val value: String,
                     val link: String)

data class NotificationSubscribeRequestUnsigned(val timestamp: String, val service: String, val topic: String )
data class NotificationSubscribeRequestSigned(val data: NotificationSubscribeRequestUnsigned, val signature: String)

data class MessagesUnsignedRequest(val timestamp: String)
data class MessagesSignedRequest(val data: MessagesUnsignedRequest, val signature: String)
data class Message(val id: String, val title: String, val timestamp: String, val channel: MessageChannel, val action: MessageAction)
data class MessageChannel(val service: String, val topic: String)
data class MessageAction(val type: String, val title: String, val url: String)

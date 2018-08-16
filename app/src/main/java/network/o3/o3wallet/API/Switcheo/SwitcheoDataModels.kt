package network.o3.o3wallet.API.Switcheo

data class Ticker(val pair: String, val open: String, val close: String,
                  val high: String, val low: String, val volume: String,
                  val quote_volume: String)

data class Offer(val id: String, val offer_asset: String,
                 val want_asset: String, val available_amount: Long,
                 val offer_amount: Long, val want_amount: Long)
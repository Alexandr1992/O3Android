package network.o3.o3wallet.API.Switcheo

import com.google.gson.JsonObject

data class Ticker(val pair: String, val open: String, val close: String,
                  val high: String, val low: String, val volume: String,
                  val quote_volume: String)

data class Offer(val id: String, val offer_asset: String,
                 val want_asset: String, val available_amount: Long,
                 val offer_amount: Long, val want_amount: Long)

data class SwitcheoDepositTransaction(val id: String, val transaction: JsonObject? = null)

data class SwitcheoOrders(
        val id: String,
        val blockchain: String,
        val contract_hash: String,
        val address: String,
        val side: String,
        val offer_asset_id: String,
        val want_asset_id: String,
        val offer_amount: String,
        val want_amount: String,
        val transfer_amount: String,
        val priority_gas_amount: String,
        val use_native_token: Boolean,
        val native_fee_transfer_amount: Int,
        val deposit_txn: Any?,
        val created_at: String,
        val status: String,
        val fills: List<Fill>,
        val makes: List<Make>
) {

    data class Fill(
            val id: String,
            val offer_hash: String,
            val offer_asset_id: String,
            val want_asset_id: String,
            val fill_amount: String,
            val want_amount: String,
            val filled_amount: String,
            val fee_asset_id: String,
            val fee_amount: String,
            val price: String,
            val txn: Any?,
            val status: String,
            val created_at: String,
            val transaction_hash: String
    )

    data class Make(
            val id: String,
            val offer_hash: String,
            val available_amount: String,
            val offer_asset_id: String,
            val offer_amount: String,
            val want_asset_id: String,
            val want_amount: String,
            val filled_amount: String,
            val txn: Any?,
            val cancel_txn: Any?,
            val price: String,
            val status: String,
            val created_at: String,
            val transaction_hash: String,
            val trades: List<JsonObject>?
    )
}

data class SwitcheoCancellationTransaction(val id: String, val transaction: JsonObject? = null)

data class ContractBalance(val confirmed: JsonObject, val confirming: JsonObject, val locked: JsonObject)

data class SwitcheoTrade(val id: String, val fill_amount: Long,
                         val take_amount: Long, val event_time: String, val is_buy: Boolean)

data class SwitcheoCandlestick(val time: String, val open: String, val close: String, val high: String,
                               val low: String, val volume: String, val quote_volume: String)
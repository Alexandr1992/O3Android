package network.o3.o3wallet

import com.amplitude.api.Amplitude
import org.json.JSONObject

class AnalyticsService {

    class DAPI {
        companion object {
            fun logDapiMethodCall(json: JSONObject) {
                Amplitude.getInstance().logEvent("dAPI_method_call", json)
            }
            fun logDapiTxAccepted(json: JSONObject) {
                Amplitude.getInstance().logEvent("dAPI_tx_accepted", json)
            }
            fun logDappOpened(json: JSONObject) {
                Amplitude.getInstance().logEvent("dAPI_open", json)
            }
            fun logDappClosed(json: JSONObject) {
                Amplitude.getInstance().logEvent("dAPI_closed", json)
            }
            fun logAccountConnected(json: JSONObject) {
                Amplitude.getInstance().logEvent("dAPI_account_connected", json)
            }
        }
    }

    class Trading {
        companion object {
            fun logNativeOrderPlaced(json: JSONObject) {
                Amplitude.getInstance().logEvent("Native_Order_Placed", json)
            }
            fun logBuyInitiated(json: JSONObject) {
                Amplitude.getInstance().logEvent("Buy_Initiated", json)
            }
            fun logSellInitiated(json: JSONObject) {
                Amplitude.getInstance().logEvent("Sell_Initiated", json)
            }
            fun logWithdrawInitiated(json: JSONObject) {
                Amplitude.getInstance().logEvent("Withdraw_Initiated", json)
            }
            fun logDepositInitiated(json: JSONObject) {
                Amplitude.getInstance().logEvent("Deposit_Initiated", json)
            }
            fun logDeposit(json: JSONObject) {
                Amplitude.getInstance().logEvent("Deposit", json)
            }
            fun logWithdraw(json: JSONObject) {
                Amplitude.getInstance().logEvent("Withdraw", json)
            }
            fun logTradingBalanceError() {
                Amplitude.getInstance().logEvent("Not_enough_trading_balance_error")
            }
            fun logOrderCancelled(json: JSONObject) {
                Amplitude.getInstance().logEvent("Order Cancelled", json)

            }

        }
    }

    class Wallet {
        companion object {
            fun logGasClaim() {
                val attrs = mapOf(
                        "type" to "GAS",
                        "is_ledger" to false)
                Amplitude.getInstance().logEvent("CLAIM", JSONObject(attrs))
            }
            fun logOngClaim() {
                val attrs = mapOf(
                        "type" to "ONG",
                        "is_ledger" to false)
                Amplitude.getInstance().logEvent("CLAIM", JSONObject(attrs))
            }
            fun logSend(json: JSONObject) {
                Amplitude.getInstance().logEvent("Asset Send", json)
            }
            fun logWalletAdded(json: JSONObject) {
                Amplitude.getInstance().logEvent("wallet_added", json)
            }
            fun logWalletUnlocked() {
                Amplitude.getInstance().logEvent("wallet_unlocked")
            }
            fun logWatchAddressAdded(json: JSONObject){
                Amplitude.getInstance().logEvent("watch_address_added", json)
            }
        }
    }

    class SwitcheoDAPP {
        companion object {
            fun logSignedJSON(json: JSONObject) {
                Amplitude.getInstance().logEvent("Switcheo_Signed_JSON", json)
            }
            fun logSignedTX() {
                Amplitude.getInstance().logEvent("Switcheo_Signed_Raw_TX")
            }
        }
    }

    class Navigation {
        companion object {
            fun logLoadedMainTab() {
                Amplitude.getInstance().logEvent("Loaded_Main_Tab")
            }
            fun logTokenDetailsSelected(json: JSONObject) {
                Amplitude.getInstance().logEvent("Token_Details_Selected", json)
            }
        }
    }
}
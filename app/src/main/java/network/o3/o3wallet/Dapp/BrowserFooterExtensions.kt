package network.o3.o3wallet.Dapp

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.widget.Button
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.AnalyticsService
import network.o3.o3wallet.NativeTrade.NativeTradeRootActivity
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.runOnUiThread
import org.json.JSONObject

fun DAPPBrowser.initiateTradeFooter(uri: Uri) {
    if (uri.authority == "public.o3.network") {
        SwitcheoAPI().getTokens {
            runOnUiThread {
                val asset = uri.lastPathSegment!!
                //TODO: Since there is no existing NEO to GAS market
                if (it.first?.get(asset.toUpperCase()) != null &&
                        it.first?.get(asset.toUpperCase())!!.asJsonObject.get("trading_active").asBoolean == true) {
                    mView.find<View>(R.id.dappFooter).visibility = View.VISIBLE
                    mView.find<Button>(R.id.buyButton).onClick {
                        if (SystemClock.elapsedRealtime() - lastClickTime < 3000) {
                            return@onClick
                        }
                        val buyAttrs = mapOf(
                                "asset" to asset,
                                "source" to "token_details")
                        AnalyticsService.Trading.logBuyInitiated(JSONObject(buyAttrs))
                        val intent = Intent(mView.context, NativeTradeRootActivity::class.java)
                        intent.putExtra("asset", asset)
                        intent.putExtra("is_buy", true)
                        startActivity(intent)
                        lastClickTime = SystemClock.elapsedRealtime()

                    }
                    mView.find<Button>(R.id.sellButton).onClick {
                        if (SystemClock.elapsedRealtime() - lastClickTime < 3000) {
                            return@onClick
                        }
                        val sellAttrs = mapOf(
                                "asset" to asset,
                                "source" to "token_details")
                        AnalyticsService.Trading.logSellInitiated(JSONObject(sellAttrs))
                        val intent = Intent(mView.context, NativeTradeRootActivity::class.java)
                        intent.putExtra("asset", asset)
                        intent.putExtra("is_buy", false)
                        startActivity(intent)
                        lastClickTime = SystemClock.elapsedRealtime()
                    }
                } else {
                    mView.find<View>(R.id.dappFooter).visibility = View.GONE
                }
            }

        }

    } else {
        mView.find<View>(R.id.dappFooter).visibility = View.GONE
    }
}
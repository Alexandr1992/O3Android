package network.o3.o3wallet.NativeTrade


import android.annotation.SuppressLint
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.API.O3Platform.TradingAccount
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment

class NativeTradeBaseAssetBottomSheet : RoundedBottomSheetDialogFragment() {

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.native_trade_base_asset_selection_fragment, null)
        dialog.setContentView(contentView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.native_trade_base_asset_selection_fragment, container, false)
        val listView = view.findViewById<ListView>(R.id.baseAssetListView)

        val tradingAccount = Gson().fromJson<TradingAccount>(arguments!!.getString("trading_account"))
        val orderasset = arguments!!.getString("order_asset")
        val isBuyOrder = arguments!!.getBoolean("is_buy_order")
        var usdPair = Pair("SDUSD", 0.0)
        var neoPair = Pair("NEO", 0.0)
        val confirmedUSD = tradingAccount.switcheo.confirmed.find { it.symbol.toLowerCase() == "sdusd" }
        val confirmedNeo = tradingAccount.switcheo.confirmed.find { it.symbol.toLowerCase() == "neo" }
        val basePairs = arrayListOf<Pair<String, Double>>()
        if (confirmedUSD != null && orderasset.toUpperCase() != "SDUSD" && orderasset.toUpperCase() == "NEO") {
            basePairs.add(Pair("SDUSD", confirmedUSD.value.toDouble()))
        } else if (!isBuyOrder && orderasset.toUpperCase() == "NEO") {
            basePairs.add(usdPair)
        }

        if (confirmedNeo != null && orderasset.toUpperCase() != "NEO") {
            basePairs.add(Pair("NEO", confirmedNeo.value.toDouble()))
        } else if(!isBuyOrder) {
            basePairs.add(neoPair)
        }

        listView.adapter = NativeTradeBaseAssetSelectionAdapter(this.context!!, this, basePairs.toTypedArray(), isBuyOrder)

        return view
    }

    companion object {
        fun newInstance(): NativeTradeBaseAssetBottomSheet {
            return NativeTradeBaseAssetBottomSheet()
        }
    }
}

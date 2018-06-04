package network.o3.o3wallet.Portfolio

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.yesButton
import java.text.NumberFormat

/**
 * Created by drei on 12/15/17.
 */

class AssetListAdapter(context: Context, fragment: HomeFragment): BaseAdapter() {
    data class TableCellData(var assetName: String, var assetAmount: Double,
                             var assetPrice: Double, var totalValue: Double, var percentChange: Double)

    private val mContext: Context
    private val mfragment: HomeFragment
    var assets = ArrayList<TransferableAsset>()
    var portfolio: Portfolio? = null
    var referenceCurrency: CurrencyType = CurrencyType.FIAT
    init {
        mContext = context
        mfragment = fragment
    }

    override fun getItem(position: Int): TableCellData {
        var assetData = TableCellData("", 0.0, 0.0, 0.0, 0.0)
        assetData.assetName = assets.get(position).symbol
        assetData.assetAmount = assets.get(position).value.toDouble()

        if (portfolio != null) {
            if (referenceCurrency == CurrencyType.FIAT) {
                val latestPrice = portfolio!!.price[assets.get(position).symbol]?.averageUSD ?: 0.0
                val firstPrice = portfolio!!.firstPrice[assets.get(position).symbol]?.averageUSD ?: 0.0
                assetData.assetPrice = latestPrice
                assetData.percentChange = (latestPrice - firstPrice) / firstPrice * 100
                assetData.totalValue = latestPrice * assetData.assetAmount
            } else {
                val latestPrice = portfolio!!.price[assets.get(position).symbol]?.averageBTC ?: 0.0
                val firstPrice = portfolio!!.firstPrice[assets.get(position).symbol]?.averageBTC ?: 0.0
                assetData.assetPrice = latestPrice
                assetData.percentChange = (latestPrice - firstPrice) / firstPrice * 100
                assetData.totalValue = latestPrice * assetData.assetAmount
            }
        }

        return assetData
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return assets.count()
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(R.layout.portfolio_asset_card, viewGroup, false)
        val asset = getItem(position)
        val assetNameView = view.findViewById<TextView>(R.id.assetNameTextView)
        val assetPriceView = view.findViewById<TextView>(R.id.assetPriceTextView)
        val assetAmountView = view.findViewById<TextView>(R.id.assetAmountTextView)
        val assetTotalValueView = view.findViewById<TextView>(R.id.totalValueTextView)
        val assetPercentChangeView = view.findViewById<TextView>(R.id.percentChangeTextView)

        assetNameView.text = asset.assetName
        assetPriceView.text = asset.assetPrice.formattedCurrencyString(referenceCurrency)
        assetTotalValueView.text = asset.totalValue.formattedCurrencyString(referenceCurrency)
        assetPercentChangeView.text = asset.percentChange.formattedPercentString()

        if (asset.assetPrice == 0.0 && portfolio?.price?.get(assets.get(position).symbol)?.averageUSD != null) {
            assetPercentChangeView.visibility = View.GONE
            assetPriceView.visibility = View.GONE
            view.find<TextView>(R.id.pricingNotAvailableTextView).visibility = View.VISIBLE
            view.setOnClickListener {
                mfragment.activity?.alert (
                    mContext.getString(R.string.PORTFOLIO_pricing_not_available_description)) {
                    yesButton { mContext.getString(R.string.ALERT_OK_Confirm_Button) }
                }?.show()
            }
        } else {
            assetPercentChangeView.visibility = View.VISIBLE
            assetPriceView.visibility = View.VISIBLE
            view.find<TextView>(R.id.pricingNotAvailableTextView).visibility = View.GONE
            view.setOnClickListener {
                val intent = Intent(mfragment.activity, AssetGraph::class.java)
                intent.putExtra("SYMBOL", asset.assetName.capitalize())
                mfragment.activity?.startActivity(intent)
            }
        }

        if (asset.percentChange < 0) {
            assetPercentChangeView.setTextColor(ContextCompat.getColor(mContext, R.color.colorLoss))
        } else {
            assetPercentChangeView.setTextColor(ContextCompat.getColor(mContext, R.color.colorGain))
        }

        var formatter = NumberFormat.getNumberInstance()
        formatter.maximumFractionDigits = assets[position].decimals
        assetAmountView.text = formatter.format(asset.assetAmount)

        return view
    }
}
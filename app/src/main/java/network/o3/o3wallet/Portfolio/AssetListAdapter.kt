package network.o3.o3wallet.Portfolio

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.R.*
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import java.text.NumberFormat

/**
 * Created by drei on 12/15/17.
 */

class AssetListAdapter(context: Context, fragment: HomeFragment): BaseAdapter() {
    data class TableCellData(var assetName: String, var assetSymbol: String, var assetAmount: Double,
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
        var assetData = TableCellData("", "",  0.0, 0.0, 0.0, 0.0)
        assetData.assetName = assets.get(position).symbol
        assetData.assetAmount = assets.get(position).value.toDouble()
        assetData.assetSymbol = assets.get(position).symbol
        //TODO: HARDCODED FOR ONTOLOGY, FIND IMPROVED WAY TO DO THIS SOON
        if (assets.get(position).id.contains("000000000000000")) {
            assetData.assetName = assets.get(position).symbol + " (M)"
        }

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
        if (PersistentStore.shouldShowSwitcheoOnPortfolio()) {
            return assets.count() + 1
        }
        return assets.count()
    }

    fun getNotificationView(viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(layout.portfolio_notification_row, viewGroup, false)
        view.find<ImageButton>(id.dismissNotificationButton).setOnClickListener {
            mfragment.alert(mfragment.resources.getString(string.PORTFOLIO_are_you_sure_switcheo)) {
                yesButton {
                    PersistentStore.setShouldShowSwitcheoOnPortfolio(false)
                    notifyDataSetChanged()
                }
                noButton {

                }
            }.show()

        }

        view.find<Button>(id.tradeNowPortfolioButton).setOnClickListener {
            val url = "http://analytics.o3.network/redirect/?url=https://switcheo.exchange/?ref=o3"
            val intent = Intent(mContext, DAppBrowserActivity::class.java)
            intent.putExtra("url", url)
            intent.putExtra("allowSearch", false)
            mContext.startActivity(intent)
        }
        return view
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        var assetPosition = position
        if (position == 0 && PersistentStore.shouldShowSwitcheoOnPortfolio()) {
            return getNotificationView(viewGroup)
        }

        if (PersistentStore.shouldShowSwitcheoOnPortfolio()) {
            assetPosition = position - 1
        }


        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(layout.portfolio_asset_card, viewGroup, false)
        val asset = getItem(assetPosition)
        val assetNameView = view.findViewById<TextView>(id.assetNameTextView)
        val assetPriceView = view.findViewById<TextView>(id.assetPriceTextView)
        val assetAmountView = view.findViewById<TextView>(id.assetAmountTextView)
        val assetTotalValueView = view.findViewById<TextView>(id.totalValueTextView)
        val assetPercentChangeView = view.findViewById<TextView>(id.percentChangeTextView)
        val logoView = view.find<ImageView>(id.portfolioAssetLogoView)

        assetNameView.text = asset.assetName
        assetPriceView.text = asset.assetPrice.formattedCurrencyString(referenceCurrency)
        assetTotalValueView.text = asset.totalValue.formattedCurrencyString(referenceCurrency)
        assetPercentChangeView.text = asset.percentChange.formattedPercentString()
        val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", asset.assetSymbol)
        Glide.with(mContext).load(imageURL).into(logoView)

        assetPercentChangeView.visibility = View.VISIBLE
        assetPriceView.visibility = View.VISIBLE
        assetTotalValueView.visibility = View.VISIBLE
        view.find<TextView>(id.pricingNotAvailableTextView).visibility = View.GONE
        view.setOnClickListener {
            val detailURL = "https://public.o3.network/neo/assets/" + asset.assetSymbol + "?address=" + Account.getWallet()!!.address
            val intent = Intent(mfragment.activity, DAppBrowserActivity::class.java)
            intent.putExtra("url", detailURL)
            mfragment.activity?.startActivity(intent)
        }


        if (asset.percentChange < 0) {
            assetPercentChangeView.setTextColor(ContextCompat.getColor(mContext, color.colorLoss))
        } else {
            assetPercentChangeView.setTextColor(ContextCompat.getColor(mContext, color.colorGain))
        }

        if (portfolio ==  null) {
            assetPriceView.visibility = View.INVISIBLE
            assetPercentChangeView.visibility = View.INVISIBLE
            assetTotalValueView.visibility = View.INVISIBLE
        } else {
            assetPriceView.visibility = View.VISIBLE
            assetPercentChangeView.visibility = View.VISIBLE
            assetTotalValueView.visibility = View.VISIBLE
        }

        var formatter = NumberFormat.getNumberInstance()
        formatter.maximumFractionDigits = assets[assetPosition].decimals
        assetAmountView.text = formatter.format(asset.assetAmount)

        return view
    }
}
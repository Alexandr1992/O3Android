package network.o3.o3wallet.Portfolio

import android.content.Context
import android.content.Intent
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.robinhood.spark.SparkView
import com.robinhood.spark.animation.MorphSparkAnimator
import kotlinx.android.synthetic.main.settings_activity_add_contact.view.*
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.R.*
import network.o3.o3wallet.Settings.WatchAddressFragment
import network.o3.o3wallet.Wallet.MyAddressFragment
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import java.text.NumberFormat

/**
 * Created by drei on 12/15/17.
 */

class AssetListAdapter(context: Context, fragment: HomeFragment): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
    companion object {
        val ASSETROW = 0
        val NOTIFICATIONROW = 1
    }

    override fun getItemViewType(position: Int): Int {
        if (PersistentStore.shouldShowSwitcheoOnPortfolio() && position == 0) {
            return NOTIFICATIONROW
        } else {
            return ASSETROW
        }
    }

    override fun getItemCount(): Int {
        if (PersistentStore.shouldShowSwitcheoOnPortfolio()) {
            return assets.count() + 1
        }
        return assets.count()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        if (viewType == NOTIFICATIONROW) {
            val view = layoutInflater.inflate(layout.portfolio_notification_row, viewGroup, false)
            return NotificationViewHolder(view)
        } else {
            val view = layoutInflater.inflate(layout.portfolio_asset_card, viewGroup, false)
            return PortfolioAssetViewHolder(view)
        }
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        var assetPosition = position
        if (PersistentStore.shouldShowSwitcheoOnPortfolio() && position == 0) {
            (vh as NotificationViewHolder).bindNotification(this)
            return
        }

        if (PersistentStore.shouldShowSwitcheoOnPortfolio()){
            assetPosition = position - 1
        } else {
            assetPosition = position
        }
        (vh as PortfolioAssetViewHolder).bindPortfolioAsset(assets[assetPosition], portfolio, referenceCurrency)
    }

    class PortfolioAssetViewHolder(v: View): RecyclerView.ViewHolder(v) {
        data class TableCellData(var assetName: String, var assetSymbol: String, var assetAmount: Double,
                                 var assetPrice: Double, var totalValue: Double, var percentChange: Double)

        val view = v
        val assetNameView = view.findViewById<TextView>(id.assetNameTextView)
        val assetPriceView = view.findViewById<TextView>(id.assetPriceTextView)
        val assetAmountView = view.findViewById<TextView>(id.assetAmountTextView)
        val assetTotalValueView = view.findViewById<TextView>(id.totalValueTextView)
        val assetPercentChangeView = view.findViewById<TextView>(id.percentChangeTextView)
        val logoView = view.find<ImageView>(id.portfolioAssetLogoView)

        private var asset: TransferableAsset? = null

        fun getItem(asset: TransferableAsset, portfolio: Portfolio?, referenceCurrency: CurrencyType): TableCellData {
            var assetData = TableCellData("", "", 0.0, 0.0, 0.0, 0.0)
            assetData.assetName = asset.symbol
            assetData.assetAmount = asset.value.toDouble()
            assetData.assetSymbol = asset.symbol
            //TODO: HARDCODED FOR ONTOLOGY, FIND IMPROVED WAY TO DO THIS SOON
            if (asset.id.contains("000000000000000")) {
                assetData.assetName = asset.symbol + " (M)"
            }

            if (portfolio != null) {
                if (referenceCurrency == CurrencyType.FIAT) {
                    val latestPrice = portfolio!!.price[asset.symbol]?.averageUSD ?: 0.0
                    val firstPrice = portfolio!!.firstPrice[asset.symbol]?.averageUSD ?: 0.0
                    assetData.assetPrice = latestPrice
                    assetData.percentChange = (latestPrice - firstPrice) / firstPrice * 100
                    assetData.totalValue = latestPrice * assetData.assetAmount
                } else {
                    val latestPrice = portfolio!!.price[asset.symbol]?.averageBTC ?: 0.0
                    val firstPrice = portfolio!!.firstPrice[asset.symbol]?.averageBTC ?: 0.0
                    assetData.assetPrice = latestPrice
                    assetData.percentChange = (latestPrice - firstPrice) / firstPrice * 100
                    assetData.totalValue = latestPrice * assetData.assetAmount
                }
            }

            return assetData
        }


        fun bindPortfolioAsset(asset: TransferableAsset, portfolio: Portfolio?, referenceCurrency: CurrencyType) {

            val tableCellData = getItem(asset, portfolio, referenceCurrency)
            assetNameView.text = tableCellData.assetName
            assetPriceView.text = tableCellData.assetPrice.formattedCurrencyString(referenceCurrency)
            assetTotalValueView.text = tableCellData.totalValue.formattedCurrencyString(referenceCurrency)
            assetPercentChangeView.text = tableCellData.percentChange.formattedPercentString()
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", tableCellData.assetSymbol)
            Glide.with(view.context).load(imageURL).into(logoView)

            assetPercentChangeView.visibility = View.VISIBLE
            assetPriceView.visibility = View.VISIBLE
            assetTotalValueView.visibility = View.VISIBLE
            view.find<TextView>(id.pricingNotAvailableTextView).visibility = View.GONE

            view.setOnClickListener {
                var detailURL = "https://public.o3.network/neo/assets/" + tableCellData.assetSymbol + "?address=" + Account.getWallet()!!.address
                if (asset.id.contains("00000000000")) {
                    detailURL = "https://public.o3.network/ont/assets/" + tableCellData.assetSymbol + "?address=" + Account.getWallet()!!.address
                }

                val intent = Intent(view.context, DAppBrowserActivity::class.java)
                intent.putExtra("url", detailURL)
                view.context.startActivity(intent)
            }


            if (tableCellData.percentChange == 0.0) {
                assetPercentChangeView.setTextColor(view.context.getColorFromAttr(R.attr.defaultSubtitleTextColor))
            } else if (tableCellData.percentChange < 0) {
                assetPercentChangeView.setTextColor(ContextCompat.getColor(view.context, color.colorLoss))
            } else {
                assetPercentChangeView.setTextColor(ContextCompat.getColor(view.context, color.colorGain))
            }

            var formatter = NumberFormat.getNumberInstance()
            formatter.maximumFractionDigits = asset.decimals
            assetAmountView.text = formatter.format(tableCellData.assetAmount)
        }
    }

    class NotificationViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val view = v

        fun bindNotification(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
            view.find<ImageButton>(id.dismissNotificationButton).setOnClickListener {
                view.context.alert(view.context.resources.getString(string.PORTFOLIO_are_you_sure_switcheo)) {
                    yesButton {
                        PersistentStore.setShouldShowSwitcheoOnPortfolio(false)
                           adapter.notifyDataSetChanged()
                    }
                    noButton {

                    }
                }.show()
            }

            view.find<Button>(id.tradeNowPortfolioButton).setOnClickListener {
                val url = "http://analytics.o3.network/redirect/?url=https://switcheo.exchange/?ref=o3"
                val intent = Intent(view.context, DAppBrowserActivity::class.java)
                intent.putExtra("url", url)
                intent.putExtra("allowSearch", false)
                view.context.startActivity(intent)
            }
        }
    }
}
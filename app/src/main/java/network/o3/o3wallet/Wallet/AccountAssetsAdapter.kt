package network.o3.o3wallet.Wallet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.*
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3.PriceData
import network.o3.o3wallet.API.O3Platform.O3InboxItem
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find
import java.math.BigDecimal
import java.security.AccessControlContext
import java.text.NumberFormat
import kotlin.math.min

/**
 * Created by apisit on 12/20/17.
 */
class AccountAssetsAdapter(mFragment: AccountFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var arrayOfAccountAssets = arrayListOf<TransferableAsset>()
    private var arrayOfTradingAccountAssets = listOf<TransferableAsset>()
    private var tradingAccountPriceData: PriceData? = null
    private var inboxList = listOf<O3InboxItem>()
    private val mFragment = mFragment
    private var isInitialLoad = true
    private var isExpanded = false

    companion object {
        val ASSETROW = 0
        val INBOXROW = 1
        val ACCOUNTROW = 2
    }

    @Synchronized
    fun setInboxList(list: List<O3InboxItem>) {
        inboxList = list
        if (inboxList.count() == list.count()) {
            return
        }
        notifyItemRangeChanged(0, inboxList.count() + arrayOfAccountAssets.count())
    }

    @Synchronized
    fun setAssetsArray(assets: ArrayList<TransferableAsset>) {
        arrayOfAccountAssets = assets
        notifyItemRangeChanged(inboxList.count(), arrayOfAccountAssets.count())
    }

    fun setTradingAccountAssets(assets: List<TransferableAsset>) {
        arrayOfTradingAccountAssets = assets
        notifyItemChanged(itemCount - 1)
    }

    fun setTradingAccountPriceData(priceData: PriceData) {
        tradingAccountPriceData = priceData
        notifyItemChanged(itemCount - 1)
    }

    override fun getItemCount(): Int {
        return arrayOfAccountAssets.count() + inboxList.count() + 1
    }

    override fun getItemViewType(position: Int): Int {
        if(position == itemCount - 1) {
            return ACCOUNTROW
        }

        if (inboxList.isEmpty()) {
            return ASSETROW
        } else if (position < inboxList.size) {
            return INBOXROW
        } else {
            return ASSETROW
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position == itemCount - 1) {
            (holder as AccountHolder).bindAccount(arrayOfTradingAccountAssets, tradingAccountPriceData)
            holder.itemView.isActivated = isExpanded
            if (isExpanded) {
                holder.itemView.find<View>(R.id.accountAssetsRecyclerView).visibility = View.VISIBLE
            } else {
                holder.itemView.find<View>(R.id.accountAssetsRecyclerView).visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                isExpanded = true
                TransitionManager.beginDelayedTransition(mFragment.assetListView)
                notifyItemChanged(position)
            }
            return
        }

        if (inboxList.isEmpty()) {
            (holder as AssetHolder).bindAsset(arrayOfAccountAssets[position])
        } else if (position < inboxList.size) {
            (holder as InboxHolder).bindInboxItem(inboxList[position])
        } else {
            (holder as AssetHolder).bindAsset(arrayOfAccountAssets[position - inboxList.size])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        if (viewType == ASSETROW) {
            val view = layoutInflater.inflate(R.layout.wallet_account_asset_row, parent, false)
            return AssetHolder(view)
        } else if (viewType == INBOXROW) {
            val view = layoutInflater.inflate(R.layout.wallet_inbox_layout, parent, false)
            return InboxHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.wallet_trading_account_card, parent, false)
            return AccountHolder(view, arrayOfTradingAccountAssets, tradingAccountPriceData)
        }
    }

    class InboxHolder(v: View): RecyclerView.ViewHolder(v) {
        private var view: View = v
        private var inboxItem: O3InboxItem? = null

        fun bindInboxItem(inboxItem: O3InboxItem) {
            this.inboxItem = inboxItem
            view.find<CardView>(R.id.tokenSwapCard).visibility = View.VISIBLE
            view.find<TextView>(R.id.tokenSwapTitleView).text = inboxItem.title
            view.find<TextView>(R.id.tokenSwapDescriptionView).text = inboxItem.description
            view.find<TextView>(R.id.tokenSwapSubtitleLabel).text = inboxItem.subtitle
            view.find<Button>(R.id.tokenSwapLearnmoreButton).text = inboxItem.readmoreTitle
            view.find<Button>(R.id.tokenSwapActionButton).text = inboxItem.actionTitle

            if (view.find<TextView>(R.id.tokenSwapSubtitleLabel).text.isBlank()) {
                view.find<TextView>(R.id.tokenSwapSubtitleLabel).visibility = View.GONE
            } else {
                view.find<TextView>(R.id.tokenSwapSubtitleLabel).visibility = View.VISIBLE
            }

            if (view.find<TextView>(R.id.tokenSwapDescriptionView).text.isBlank()) {
                view.find<TextView>(R.id.tokenSwapDescriptionView).visibility = View.GONE
            } else {
                view.find<TextView>(R.id.tokenSwapDescriptionView).visibility = View.VISIBLE
            }
            Glide.with(view.context).load(inboxItem.iconURL).into(view.find(R.id.tokenSwapLogoImageView))

            val nep9 = inboxItem.actionURL
            view.find<Button>(R.id.tokenSwapActionButton).setOnClickListener {
                val intent = Intent(view.context, SendV2Activity::class.java)
                intent.putExtra("uri", nep9)
                view.context.startActivity(intent)
            }

            val learnURL = inboxItem.readmoreURL
            view.find<Button>(R.id.tokenSwapLearnmoreButton).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setData(Uri.parse(learnURL))
                view.context.startActivity(intent)
            }
        }
    }

    class AssetHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private var asset: TransferableAsset? = null

        init {
            v.setOnClickListener(this)
        }

        val assetNameTextView = view.find<TextView>(R.id.assetName)
        val assetAmountTextView = view.find<TextView>(R.id.assetAmount)
        val logoImageView = view.find<ImageView>(R.id.coinLogoImageView)

        override fun onClick(p0: View?) {
            var detailURL = "https://public.o3.network/neo/assets/" + asset!!.symbol + "?address=" +
                    Account.getWallet().address + "&theme=" + PersistentStore.getTheme()
            if (asset!!.id.contains("00000000000")) {
                detailURL = "https://public.o3.network/ont/assets/" + asset!!.symbol + "?address=" +
                        Account.getWallet().address + "&theme=" + PersistentStore.getTheme()
            }
            val intent = Intent(view.context, DAppBrowserActivity::class.java)
            intent.putExtra("url", detailURL)
            view.context.startActivity(intent)
        }


        fun bindAsset(asset: TransferableAsset) {
            this.asset = asset
            if (asset.id.contains(NeoNodeRPC.Asset.NEO.assetID())) {
                assetNameTextView.text = NeoNodeRPC.Asset.NEO.name
                assetAmountTextView.text = "%d".format(asset.value.toInt())
                val imageURL = "https://cdn.o3.network/img/neo/NEO.png"
                Glide.with(view.context).load(imageURL).into(logoImageView)
            } else if (asset.id.contains(NeoNodeRPC.Asset.GAS.assetID())) {
                assetNameTextView.text = NeoNodeRPC.Asset.GAS.name
                assetAmountTextView.text = "%.8f".format(asset.value)
                val imageURL = "https://cdn.o3.network/img/neo/GAS.png"
                Glide.with(view.context).load(imageURL).into(logoImageView)
            } else {
                assetNameTextView.text = asset.symbol
                var formatter = NumberFormat.getNumberInstance()
                formatter.maximumFractionDigits = asset.decimals
                assetAmountTextView.text = formatter.format(asset.value)
                val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", asset.symbol.toUpperCase())
                Glide.with(view.context).load(imageURL).into(logoImageView)
                if (asset.id.contains("000000000000")) {
                    assetNameTextView.text = asset.symbol + " (M)"
                }
            }
        }
    }

    class AccountHolder(v: View, assets: List<TransferableAsset>, priceData: PriceData?) : RecyclerView.ViewHolder(v){
        private var mView: View = v
        private var mAssets = assets
        private var mPriceData = priceData


        fun fillLogos() {
            val logoImageOne = mView.find<ImageView>(R.id.assetLogo1)
            val logoImageTwo = mView.find<ImageView>(R.id.assetLogo2)
            val logoImageThree = mView.find<ImageView>(R.id.assetLogo3)
            val logoImageFour = mView.find<ImageView>(R.id.assetLogo4)
            val logos = arrayOf(logoImageOne, logoImageTwo, logoImageThree, logoImageFour)
            for (logo in logos) {
                logo.visibility = View.GONE
            }

            val endIndex = min(4, mAssets.size)
            if (endIndex == 0) {
                return
            }
            for (x in 0 until endIndex) {
                logos[x].visibility = View.VISIBLE
                val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", mAssets[x].symbol)
                Glide.with(mView.context).load(imageURL).into(logos[x])
            }

            val additionalAssetsTextView = mView.find<TextView>(R.id.additionalAssetsTextView)
            if (endIndex > 4) {
                additionalAssetsTextView.visibility = View.VISIBLE
                additionalAssetsTextView.text =
                        String.format(mView.context.getString(R.string.WALLET_additonal_asset), mAssets.size - 4)
            } else {
                additionalAssetsTextView.visibility = View.GONE
            }
        }

        fun fillPrice() {
            if (mPriceData == null) {
                mView.find<TextView>(R.id.accountBalanceTextView).text = ""
            } else {
                mView.find<TextView>(R.id.accountBalanceTextView).text = mPriceData!!.average.formattedFiatString()
            }

        }


        fun bindAccount(assets: List<TransferableAsset>, priceData: PriceData?) {
            mAssets = assets
            mPriceData = priceData
            mView.findViewById<TextView>(R.id.accountTitleTextView).text = mView.context.getString(R.string.WALLET_trading_account)
            fillLogos()
            fillPrice()
            if (mView.find<RecyclerView>(R.id.accountAssetsRecyclerView).adapter == null) {
                mView.find<RecyclerView>(R.id.accountAssetsRecyclerView).adapter = SingleAccountAdapter(mAssets)
            } else {
                (mView.find<RecyclerView>(R.id.accountAssetsRecyclerView).adapter as SingleAccountAdapter).setAssets(assets)
            }

            (mView.find<RecyclerView>(R.id.accountAssetsRecyclerView).adapter as SingleAccountAdapter).setAssets(assets)
        }


        class SingleAccountAdapter(val assets: List<TransferableAsset>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private var mAssets = assets

            fun setAssets(assets: List<TransferableAsset>) {
                mAssets = assets
                notifyDataSetChanged()
            }

            override fun getItemCount(): Int {
                 return mAssets.size
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder as AssetHolder).bindAsset(mAssets[position])
            }

            override fun onCreateViewHolder(parent: ViewGroup, p1: Int): AssetHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.wallet_account_asset_row, parent, false)
                return AssetHolder(view)
            }

            class AssetHolder(v: View) : RecyclerView.ViewHolder(v) {
                private var mView = v

                val assetNameTextView = mView.find<TextView>(R.id.assetName)
                val assetAmountTextView = mView.find<TextView>(R.id.assetAmount)
                val logoImageView = mView.find<ImageView>(R.id.coinLogoImageView)


                fun bindAsset(asset: TransferableAsset) {
                    if (asset.id.contains(NeoNodeRPC.Asset.NEO.assetID())) {
                        assetNameTextView.text = NeoNodeRPC.Asset.NEO.name
                        assetAmountTextView.text =  asset.value.toDouble().removeTrailingZeros()
                        val imageURL = "https://cdn.o3.network/img/neo/NEO.png"
                        Glide.with(mView.context).load(imageURL).into(logoImageView)
                    } else if (asset.id.contains(NeoNodeRPC.Asset.GAS.assetID())) {
                        assetNameTextView.text = NeoNodeRPC.Asset.GAS.name
                        assetAmountTextView.text = asset.value.toDouble().removeTrailingZeros()
                        val imageURL = "https://cdn.o3.network/img/neo/GAS.png"
                        Glide.with(mView.context).load(imageURL).into(logoImageView)
                    } else {
                        assetNameTextView.text = asset.symbol
                        var formatter = NumberFormat.getNumberInstance()
                        formatter.maximumFractionDigits = asset.decimals
                        assetAmountTextView.text = asset.value.toDouble().removeTrailingZeros()
                        val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", asset.symbol.toUpperCase())
                        Glide.with(mView.context).load(imageURL).into(logoImageView)
                        if (asset.id.contains("000000000000")) {
                            assetNameTextView.text = asset.symbol + " (M)"
                        }
                    }
                }
            }
        }
    }
}
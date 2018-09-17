package network.o3.o3wallet.Wallet

import android.content.Intent
import android.net.Uri
import android.support.v7.widget.CardView
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
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
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.NativeTrade.DepositWithdrawal.DepositWithdrawalActivity
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import org.jetbrains.anko.find
import java.text.NumberFormat
import kotlin.math.min

/**
 * Created by apisit on 12/20/17.
 */
class AccountAssetsAdapter(mFragment: AccountFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var arrayOfAccountAssets = arrayListOf<TransferableAsset>()
    private var arrayOfTradingAccountAssets = listOf<TransferableAsset>()
    private var walletAccountPriceData: PriceData? = null
    private var tradingAccountPriceData: PriceData? = null
    private var inboxList = listOf<O3InboxItem>()
    private val mFragment = mFragment
    private var isInitialLoad = true
    private var currExpandedPosition = -1

    companion object {
        val INBOXROW = 1
        val ACCOUNTROW = 2
    }

    @Synchronized
    fun setInboxList(list: List<O3InboxItem>) {
        inboxList = list
        if (inboxList.count() == list.count()) {
            return
        }
        notifyItemRangeChanged(0, inboxList.count())
    }

    @Synchronized
    fun setAssetsArray(assets: ArrayList<TransferableAsset>) {
        arrayOfAccountAssets = assets
        notifyItemChanged(inboxList.count())
    }

    fun setTradingAccountAssets(assets: List<TransferableAsset>) {
        arrayOfTradingAccountAssets = assets
        notifyItemChanged(itemCount - 1)
    }

    fun setTradingAccountPriceData(priceData: PriceData) {
        tradingAccountPriceData = priceData
        notifyItemChanged(itemCount - 1)
    }

    fun setWalletAccountPriceData(priceData: PriceData) {
        walletAccountPriceData = priceData
        notifyItemChanged(itemCount - 2)
    }

    override fun getItemCount(): Int {
        return inboxList.count() + 2
    }

    override fun getItemViewType(position: Int): Int {
        if(position == itemCount - 1) {
            return ACCOUNTROW
        }

        if (inboxList.isEmpty()) {
            return ACCOUNTROW
        } else if (position < inboxList.size) {
            return INBOXROW
        } else {
            return ACCOUNTROW
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (inboxList.isNotEmpty() && position < inboxList.size) {
            (holder as InboxHolder).bindInboxItem(inboxList[position])
            return
        }


        if(position == itemCount - 1) {
            (holder as AccountHolder).bindAccount(arrayOfTradingAccountAssets,
                    tradingAccountPriceData, mFragment.getString(R.string.WALLET_trading_account), false)
            if (currExpandedPosition == position) {
                holder.itemView.find<View>(R.id.accountAssetsRecyclerView).visibility = View.VISIBLE
            } else {
                holder.itemView.find<View>(R.id.accountAssetsRecyclerView).visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                if (currExpandedPosition == position) {
                    currExpandedPosition = -1
                    notifyItemChanged(position)
                } else if (currExpandedPosition != -1) {
                    val prevExpandedPosition = currExpandedPosition
                    currExpandedPosition = position
                    notifyItemChanged(position)
                    notifyItemChanged(prevExpandedPosition)
                } else {
                    currExpandedPosition = position
                    notifyItemChanged(position)
                }
            }
        } else {
            (holder as AccountHolder).bindAccount(arrayOfAccountAssets, walletAccountPriceData, mFragment.getString(R.string.WALLET_my_o3_wallet), true)
            if (currExpandedPosition == position) {
                holder.itemView.find<View>(R.id.accountAssetsRecyclerView).visibility = View.VISIBLE
            } else {
                holder.itemView.find<View>(R.id.accountAssetsRecyclerView).visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                if (currExpandedPosition == position) {
                    currExpandedPosition = -1
                    notifyItemChanged(position)
                } else if (currExpandedPosition != -1) {
                        val prevExpandedPosition = currExpandedPosition
                        currExpandedPosition = position
                        notifyItemChanged(position)
                        notifyItemChanged(prevExpandedPosition)
                } else {
                    currExpandedPosition = position
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        if (viewType == INBOXROW) {
            val view = layoutInflater.inflate(R.layout.wallet_inbox_layout, parent, false)
            return InboxHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.wallet_trading_account_card, parent, false)
            return AccountHolder(mFragment, view, arrayOfTradingAccountAssets, tradingAccountPriceData)
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

    class AccountHolder(fragment: AccountFragment, v: View, assets: List<TransferableAsset>, priceData: PriceData?) : RecyclerView.ViewHolder(v){
        private var mView: View = v
        private var mFragment = fragment
        private var mAssets = assets
        private var mPriceData = priceData


        fun fillLogos() {
            val logoImageOne = mView.find<ImageView>(R.id.assetLogo1)
            val logoImageTwo = mView.find<ImageView>(R.id.assetLogo2)
            val logoImageThree = mView.find<ImageView>(R.id.baseAssetLogoImageView)
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
            if (mAssets.size > 4) {
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

        fun initiateToolbarButtons(isWallet: Boolean) {
            val leftToolbarButton = mView.find<Button>(R.id.leftToolbarButton)
            val rightToolbarButton = mView.find<Button>(R.id.rightToolbarButton)
            if (isWallet) {
                leftToolbarButton.setOnClickListener { mFragment.showMyAddress() }
                leftToolbarButton.text = mFragment.getString(R.string.WALLET_Request)
                leftToolbarButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_request, 0, 0, 0)

                rightToolbarButton.setOnClickListener { mFragment.sendButtonTapped("") }
                rightToolbarButton.text = mFragment.getString(R.string.WALLET_send_action_label)
                rightToolbarButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_send, 0, 0, 0)
            } else {
                leftToolbarButton.setOnClickListener {
                    val intent = Intent(mView.context, DepositWithdrawalActivity::class.java)
                    intent.putExtra("isDeposit", false)
                    mView.context.startActivity(intent)
                }
                leftToolbarButton.text = mFragment.getString(R.string.WALLET_Withdraw)
                leftToolbarButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_withdraw, 0, 0, 0)

                rightToolbarButton.setOnClickListener {
                    val intent = Intent(mView.context, DepositWithdrawalActivity::class.java)
                    intent.putExtra("isDeposit", true)
                    mView.context.startActivity(intent)
                }
                rightToolbarButton.text = mFragment.getString(R.string.WALLET_Deposit)
                rightToolbarButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_deposit, 0, 0, 0)
            }
        }


        fun bindAccount(assets: List<TransferableAsset>, priceData: PriceData?, accountName: String, isWallet: Boolean) {
            mAssets = assets
            mPriceData = priceData
            mView.findViewById<TextView>(R.id.accountTitleTextView).text = accountName
            fillLogos()
            fillPrice()
            initiateToolbarButtons(isWallet)


            val recyclerView = mView.find<RecyclerView>(R.id.accountAssetsRecyclerView)
            if (recyclerView.itemDecorationCount == 0) {
                val itemDecorator = DividerItemDecoration(mView.context!!, DividerItemDecoration.VERTICAL)
                itemDecorator.setDrawable(mView.context.getDrawable(R.drawable.vertical_divider)!!)
                recyclerView.addItemDecoration(itemDecorator)
            }

            if (recyclerView.adapter == null) {
                recyclerView.adapter = SingleAccountAdapter(mAssets)
            } else {
                (recyclerView.adapter as SingleAccountAdapter).setAssets(assets)
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
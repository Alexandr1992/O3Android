package network.o3.o3wallet.Wallet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3Platform.O3InboxItem
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Account
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.R
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find
import java.security.AccessControlContext
import java.text.NumberFormat

/**
 * Created by apisit on 12/20/17.
 */
class AccountAssetsAdapter(mFragment: AccountFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var arrayOfAccountAssets = arrayListOf<TransferableAsset>()
    private var inboxList = listOf<O3InboxItem>()
    private val mFragment = mFragment
    private var isInitialLoad = true

    companion object {
        val ASSETROW = 0
        val INBOXROW = 1
    }


    fun setInboxList(list: List<O3InboxItem>) {
        inboxList = list
        notifyItemRangeChanged(0, inboxList.count() + arrayOfAccountAssets.count())
    }

    fun setAssetsArray(assets: ArrayList<TransferableAsset>) {
        arrayOfAccountAssets = assets
        notifyItemRangeChanged(inboxList.count(), arrayOfAccountAssets.count())
    }

    override fun getItemCount(): Int {
        return arrayOfAccountAssets.count() + inboxList.count()
    }

    override fun getItemViewType(position: Int): Int {
        if (inboxList.isEmpty()) {
            return ASSETROW
        } else if (position < inboxList.size) {
            return INBOXROW
        } else {
            return ASSETROW
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
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
        } else {
            val view = layoutInflater.inflate(R.layout.wallet_inbox_layout, parent, false)
            return InboxHolder(view)
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
            var detailURL = "https://public.o3.network/neo/assets/" + asset!!.symbol + "?address=" + Account.getWallet()!!.address
            if (asset!!.id.contains("00000000000")) {
                detailURL = "https://public.o3.network/ont/assets/" + asset!!.symbol + "?address=" + Account.getWallet()!!.address
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
}
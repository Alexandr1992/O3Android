package network.o3.o3wallet.Wallet

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.NEO.AccountAsset
import network.o3.o3wallet.API.NEO.AssetType
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.TransferableAssets
import network.o3.o3wallet.Account
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.runOnUiThread
import java.text.NumberFormat

/**
 * Created by apisit on 12/20/17.
 */
class AccountAssetsAdapter(fragment: AccountFragment, context: Context, address: String, assets: ArrayList<TransferableAsset>) : BaseAdapter() {

    private var arrayOfAccountAssets = assets
    private var address = address
    private var mContext = context
    private val inflator: LayoutInflater
    private val mFragment = fragment


    init {
        this.inflator = LayoutInflater.from(context)
    }

    override fun getCount(): Int {
        return arrayOfAccountAssets.count()
    }

    override fun getItem(p0: Int): TransferableAsset {
        return arrayOfAccountAssets[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    fun configureRow(position: Int, vh: AccountAssetRow) {
        val asset = arrayOfAccountAssets[position]
        if (asset.id.contains(NeoNodeRPC.Asset.NEO.assetID())) {
            vh.assetNameTextView.text = NeoNodeRPC.Asset.NEO.name
            vh.assetAmountTextView.text = "%d".format(asset.value.toInt())
            val imageURL = "https://cdn.o3.network/img/neo/NEO.png"
            Glide.with(mContext).load(imageURL).into(vh.logoImageView)
        } else if (asset.id.contains(NeoNodeRPC.Asset.GAS.assetID())) {
            vh.assetNameTextView.text = NeoNodeRPC.Asset.GAS.name
            vh.assetAmountTextView.text = "%.8f".format(asset.value)
            val imageURL = "https://cdn.o3.network/img/neo/GAS.png"
            Glide.with(mContext).load(imageURL).into(vh.logoImageView)
        } else {
            vh.assetNameTextView.text = asset.symbol
            var formatter = NumberFormat.getNumberInstance()
            formatter.maximumFractionDigits = asset.decimals
            vh.assetAmountTextView.text = formatter.format(asset.value)
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", asset.symbol.toUpperCase())
            Glide.with(mContext).load(imageURL).into(vh.logoImageView)
            if (asset.id.contains("000000000000")) {
                vh.assetNameTextView.text = asset.symbol + " (M)"
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View?
        val vh: AccountAssetRow
        if (convertView == null || convertView.tag !is AccountAssetRow) {
            view = this.inflator.inflate(R.layout.wallet_account_asset_row, parent, false)
            vh = AccountAssetRow(view)
            view.tag = vh
        } else {
            view = convertView
            vh = view.tag as AccountAssetRow
        }

        configureRow(position,vh)

        val asset = arrayOfAccountAssets[position]

        view?.setOnClickListener {
            val detailURL = "https://public.o3.network/neo/assets/" + asset.symbol + "?address=" + Account.getWallet()!!.address
            val intent = Intent(view.context, DAppBrowserActivity::class.java)
            intent.putExtra("url", detailURL)
            view.context.startActivity(intent)
        }

        return view!!

    }

    fun updateAdapter(assets: TransferableAssets) {
        this.arrayOfAccountAssets = assets.assets
        notifyDataSetChanged()
    }
}

class AccountAssetRow(row: View) {
    val assetNameTextView: TextView
    val assetAmountTextView: TextView
    val logoImageView: ImageView

    init {
        this.assetNameTextView = row.findViewById(R.id.assetName)
        this.assetAmountTextView = row.findViewById(R.id.assetAmount)
        this.logoImageView = row.find(R.id.coinLogoImageView)
    }
}
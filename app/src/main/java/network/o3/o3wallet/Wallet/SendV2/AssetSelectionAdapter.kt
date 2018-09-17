package network.o3.o3wallet.Wallet.SendV2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.NativeTrade.DepositWithdrawal.DepositWithdrawalActivity
import network.o3.o3wallet.R
import java.text.NumberFormat

/**
 * Created by drei on 1/18/18.
 */

class AssetSelectorAdapter(context: Context, fragment: AssetSelectionBottomSheet,
                           assets: ArrayList<TransferableAsset>): BaseAdapter() {
    private val mContext: Context
    private val mFragment: AssetSelectionBottomSheet
    private val mAssets: ArrayList<TransferableAsset>
    private val inflator: LayoutInflater

    init {
        mContext = context
        mFragment = fragment
        mAssets = assets
        inflator = LayoutInflater.from(context)
    }

    override fun getCount(): Int {
        return mAssets.count()  + 2
    }

    override fun getItem(position: Int): TransferableAsset? {
        return when(position) {
            0 ->  null
                1,2 -> mAssets[position - 1]
            3 -> null
            else -> mAssets[position - 2]
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = getItem(position)
        when (position) {
            0 -> {
                val view = inflator.inflate(R.layout.wallet_send_fragment_asset_row_header, parent, false)
                view.findViewById<TextView>(R.id.headerTextView).text = mContext.resources.getString(R.string.SEND_native_assets_header)
                return view
            }
            3 -> {
                val view = inflator.inflate(R.layout.wallet_send_fragment_asset_row_header, parent, false)
                view.findViewById<TextView>(R.id.headerTextView).text = mContext.resources.getString(R.string.SEND_token_assets_header)
                return view
            }
            1, 2 -> {
                val view = inflator.inflate(R.layout.wallet_send_fragment_native_asset_row, parent, false)
                val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", item!!.symbol)
                Glide.with(mContext).load(imageURL).into(view.findViewById(R.id.logoImageView))
                view.findViewById<TextView>(R.id.nativeAssetName).text = item!!.name
                var formatter = NumberFormat.getNumberInstance()
                formatter.maximumFractionDigits = item.decimals
                view.findViewById<TextView>(R.id.assetAmountTextView).text = formatter.format(item.value)
                setListenerForRow(view, item)
                return view
            } else -> {
                val view = inflator.inflate(R.layout.wallet_send_fragment_token_row, parent, false)
                view.findViewById<TextView>(R.id.assetShortNameTextView).text = item!!.symbol
                val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", item!!.symbol)
                Glide.with(mContext).load(imageURL).into(view.findViewById(R.id.logoImageView))
                var formatter = NumberFormat.getNumberInstance()
                formatter.maximumFractionDigits = item.decimals
                view.findViewById<TextView>(R.id.assetAmountTextView).text = formatter.format(item.value)
                setListenerForRow(view, item)
                return view
            }
        }
    }

     private fun setListenerForRow(view: View, asset: TransferableAsset) {
        view.setOnClickListener {
            if (mContext is SendV2Activity) {
                mContext.sendViewModel.setSelectedAsset(asset)
            } else {
                (mContext as DepositWithdrawalActivity).viewModel.setSelectedAsset(asset)
            }

            mFragment.dismiss()
        }
    }
}
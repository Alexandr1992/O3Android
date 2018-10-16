package network.o3.o3wallet.NativeTrade

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class NativeTradeBaseAssetSelectionAdapter(context: Context,
                                           fragment: NativeTradeBaseAssetBottomSheet,
                                           assets: Array<Pair<String, Double?>>,
                                           isBuyOrder: Boolean): BaseAdapter() {
    private val mContext: Context
    private val mFragment: NativeTradeBaseAssetBottomSheet
    private val mAssets: Array<Pair<String, Double?>>
    private val mIsBuyOrder: Boolean
    private val inflator: LayoutInflater

    init {
        mContext = context
        mFragment = fragment
        mAssets = assets
        inflator = LayoutInflater.from(context)
        mIsBuyOrder = isBuyOrder
    }

    override fun getCount(): Int {
        return mAssets.count()
    }

    override fun getItem(position: Int): Pair<String, Double?> {
        return mAssets[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = getItem(position)
        val view = inflator.inflate(R.layout.native_trade_base_asset_row, null)
        view.setOnClickListener {
            (mFragment.activity as NativeTradeRootActivity).viewModel.setSelectedBaseAssetValue(item.first)
            (mFragment.activity as NativeTradeRootActivity).viewModel.setSelectedBaseAssetBalance(item.second!! / 100000000)
            (mFragment.activity as NativeTradeRootActivity).viewModel.setSelectedBaseAssetImageUrl(
                    String.format("https://cdn.o3.network/img/neo/%s.png", item.first))
            mFragment.dismiss()
        }
        view.find<TextView>(R.id.baseAssetName).text = item.first
        if (!mIsBuyOrder) {
            view.find<TextView>(R.id.baseAssetBalance).visibility = View.INVISIBLE
        } else {
            view.find<TextView>(R.id.baseAssetBalance).visibility = View.VISIBLE
        }
        view.find<TextView>(R.id.baseAssetBalance).text = (item.second!! / 100000000).toString()
        Glide.with(view.context).load(String.format("https://cdn.o3.network/img/neo/%s.png", item.first)).
                into(view.find(R.id.baseAssetlogoImageView))


        return view
    }
}
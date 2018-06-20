package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import network.o3.o3wallet.API.O3.*
import network.o3.o3wallet.R

/**
 * Created by drei on 4/17/18.
 */

class TokenSaleInfoAdapter(context: Context): BaseAdapter() {
    var mContext: Context
    private var tokenSale: TokenSale? = null

    init {
        mContext = context
    }

    fun setData(tokenSale: TokenSale) {
        this.tokenSale = tokenSale
        this.notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): InfoRow {
        return tokenSale?.info?.get(position)!!
    }

    override fun getCount(): Int {
        return tokenSale?.info?.count() ?: 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(R.layout.tokensale_info_row, parent, false)
        val infoRowData = getItem(position)
        val infoLabelTextView = view.findViewById<TextView>(R.id.infoLabelTextView)
        val infoDescriptionTextView = view.findViewById<TextView>(R.id.infoDescriptionTextView)

        infoLabelTextView.text = infoRowData.label
        infoDescriptionTextView.text = infoRowData.value
        return view
    }
}
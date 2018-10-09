package network.o3.o3wallet.Identity

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import network.o3.o3wallet.API.O3Platform.ReverseLookupNNS
import network.o3.o3wallet.Account
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk15.coroutines.onClick

class  NNSAdapter(context: Context, reverseLookups: List<ReverseLookupNNS>): BaseAdapter() {

    private val mContext: Context
    private val mReverseLookups: List<ReverseLookupNNS>

    init {
        mContext = context
        mReverseLookups = reverseLookups
    }

    override fun getItem(position: Int): ReverseLookupNNS {
        return mReverseLookups[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return mReverseLookups.count() + 1
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        if (position == 0) {
            val view = layoutInflater.inflate(R.layout.settings_header_row, null)
            view.find<TextView>(R.id.headerTextView).text = view.context.resources.getString(R.string.SETTINGS_registered_domains)
            return view
        } else {
            val view = layoutInflater.inflate(R.layout.settings_row_layout, null)
            view.find<ImageView>(R.id.settingsIcon).visibility = View.GONE
            view.find<TextView>(R.id.titleTextView).text = getItem(position - 1).domain
            view.onClick {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, String.format(view.context.resources.
                        getString(R.string.SETTINGS_nns_share,getItem(position - 1).domain, Account.getWallet().address)))
                shareIntent.type = "text/plain"
                view.context.startActivity(shareIntent)
            }
            return view
        }
    }
}

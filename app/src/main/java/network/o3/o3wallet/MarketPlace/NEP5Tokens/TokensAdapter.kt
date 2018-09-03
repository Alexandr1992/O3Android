package network.o3.o3wallet.MarketPlace.NEP5Tokens

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3Platform.TokenListing
import network.o3.o3wallet.Account
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class TokensAdapter(private var tokens: ArrayList<TokenListing>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val HEADER = 0
    private val ITEM = 1


    fun setData(tokens: ArrayList<TokenListing>) {
        this.tokens = tokens
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return tokens.count() + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return HEADER
        }
        return ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
       if (holder is FeatureHolder) {
           holder.bindFeature(tokens[position - 1])
       } else if (holder is HeaderHolder) {
           holder.bindHeader()
       }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        if (viewType == HEADER) {
            val view = layoutInflater.inflate(R.layout.marketplace_header_layout, parent, false)
            return HeaderHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.martketplace_token_grid_cell, parent, false)
            return FeatureHolder(view)
        }
    }

    class HeaderHolder(v: View): RecyclerView.ViewHolder(v) {
        private var view: View = v

        fun bindHeader() {
            view.find<Button>(R.id.tradeNowButton).setOnClickListener {
                val url = "http://analytics.o3.network/redirect/?url=https://switcheo.exchange/?ref=o3"
                val intent = Intent(view.context, DAppBrowserActivity::class.java)
                intent.putExtra("url", url)
                intent.putExtra("allowSearch", false)
                view.context.startActivity(intent)
            }
        }
    }

    class FeatureHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private var token: TokenListing? = null

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val detailURL = token?.url!! + "?address=" + Account.getWallet().address + "&theme=" + PersistentStore.getTheme().toLowerCase()
            val intent = Intent(v.context, DAppBrowserActivity::class.java)
            intent.putExtra("url", detailURL)
            v.context.startActivity(intent)
        }

        companion object {
            private val FEATURE_KEY = "FEATURE"
        }

        fun bindFeature(token: TokenListing?) {
            this.token = token
            view.find<TextView>(R.id.tokenSymbolTextView).text = token?.symbol ?: ""

            val imageView = view.findViewById<ImageView>(R.id.tokenLogoImageView)
            Glide.with(view.context).load(token?.logoURL).into(imageView)
        }
    }
}
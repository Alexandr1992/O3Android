package network.o3.o3wallet.MarketPlace.NEP5Tokens

import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3Platform.TokenListing
import network.o3.o3wallet.Account
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.Portfolio.AssetGraph
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class TokensAdapter(private var tokens: ArrayList<TokenListing>):
        RecyclerView.Adapter<TokensAdapter.FeatureHolder>() {

    init {
        var tokens = tokens
    }

    fun setData(tokens: ArrayList<TokenListing>) {
        this.tokens = tokens
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return tokens.count()
    }

    override fun onBindViewHolder(holder: FeatureHolder, position: Int) {
        holder.bindFeature(tokens[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.martketplace_token_grid_cell, parent, false)
        return FeatureHolder(view)
    }

    class FeatureHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private var token: TokenListing? = null

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val detailURL = token?.url!! + "?address=" + Account.getWallet()!!.address
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
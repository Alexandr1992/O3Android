package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import network.o3.o3wallet.API.O3.TokenSale
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.R
import org.jetbrains.anko.find


/**
 * Created by drei on 4/17/18.
 */

class TokenSalesAdapter(private var tokensales: ArrayList<TokenSale>, private var subscribeURL: String): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val TOKEN_SALE_VIEW = 0
    val FOOTER_VIEW = 1

    fun setData(tokenSales: ArrayList<TokenSale>, subscribeURL: String) {
        this.tokensales = tokenSales
        this.subscribeURL = subscribeURL
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return tokensales.count() + 1
    }

    override fun getItemViewType(position: Int): Int {
        if(position < tokensales.count()) {
            return TOKEN_SALE_VIEW
        }
        return FOOTER_VIEW
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position < tokensales.count()) {
            (holder as? TokenSaleViewHolder)?.bindTokenSale(tokensales[position])
        } else {
            (holder as? FooterViewHolder)?.bindFooter(subscribeURL)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        if (viewType == TOKEN_SALE_VIEW) {
            val view = layoutInflater.inflate(R.layout.tokensales_listing_row, parent, false)
            return TokenSaleViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.tokensales_listing_footer, parent, false)
            return FooterViewHolder(view)
        }
    }

    class TokenSaleViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private var tokenSale: TokenSale? = null

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            if (tokenSale != null) {
                val tokenSaleInfoIntent = Intent(view.context, TokenSaleRootActivity::class.java)
                tokenSaleInfoIntent.putExtra("TOKENSALE_JSON", Gson().toJson(tokenSale!!))
                view.context.startActivity(tokenSaleInfoIntent)
            }
        }

        companion object {
            private val TOKENSALE_KEY = "TOKENSALE"
        }

        fun bindTokenSale(tokenSale: TokenSale?) {
            this.tokenSale = tokenSale!!

            val daysLeftTextView = view.find<TextView>(R.id.tokenSaleDaysLeftTextView)
            val coinNameTextView = view.find<TextView>(R.id.tokenSaleCoinNameTextView)
            val coinDescriptionTextView = view.find<TextView>(R.id.tokenSaleDescriptionTextView)
            val logoImageView = view.find<ImageView>(R.id.tokenSaleSquareImageView)

            daysLeftTextView.text = String.format(view.context.resources.getString(R.string.TOKENSALE_Days_Remaining), ((tokenSale.endTime - (System.currentTimeMillis()/1000)) / 3600 / 24).toString())
            coinNameTextView.text = tokenSale.name
            coinDescriptionTextView.text = tokenSale.shortDescription
            Glide.with(view.context).load(tokenSale.squareLogoURL).into(logoImageView)
        }
    }

    class FooterViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private var view: View = v

        companion object {
            private val FOOTER_KEY = "FOOTER"
        }

        fun bindFooter(subscribeURL: String) {
            val subscribe = view.findViewById<Button>(R.id.subscribeNewsletterButton)
            subscribe.setOnClickListener {
                val browserIntent = Intent(view.context, DAppBrowserActivity::class.java)
                browserIntent.putExtra("url", subscribeURL)
                view.context.startActivity(browserIntent)
            }
        }
    }
}

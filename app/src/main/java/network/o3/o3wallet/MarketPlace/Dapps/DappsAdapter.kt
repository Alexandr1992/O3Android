package network.o3.o3wallet.MarketPlace.Dapps

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3Platform.Dapp
import network.o3.o3wallet.Dapp.DAppBrowserActivityV2
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class DappsAdapter(private val dapps: List<Dapp>): RecyclerView.Adapter<DappsAdapter.DappHolder>() {

    override fun getItemCount(): Int {
        return dapps.count()
    }

    override fun onBindViewHolder(holder: DappHolder, position: Int) {
        holder.bindDapp(dapps[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.marketplace_dapp_card, parent, false)
        return DappHolder(view)
    }

    class DappHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v

        fun bindDapp(dapp: Dapp) {
            val imageView = view.findViewById<ImageView>(R.id.dappSquareImage)
            view.find<TextView>(R.id.dappNameTextView).text = dapp.name
            view.find<TextView>(R.id.dappDescriptionTextView).text = dapp.description
            Glide.with(view.context).load(dapp.iconURL).into(imageView)
            view.setOnClickListener {
                val browserIntent = Intent(view.context, DAppBrowserActivityV2::class.java)
                browserIntent.putExtra("url", dapp.url)
                view.context.startActivity(browserIntent)
            }
        }

        companion object {
            private val Dapp_key = "DAPP"
        }
    }
}
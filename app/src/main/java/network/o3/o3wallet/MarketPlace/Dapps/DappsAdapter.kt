package network.o3.o3wallet.MarketPlace.Dapps

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.R
import network.o3.o3wallet.R.id.view
import org.jetbrains.anko.find

class DappsAdapter(private val dapps: ArrayList<Int>): RecyclerView.Adapter<DappsAdapter.DappHolder>() {

    override fun getItemCount(): Int {
        return 3 /*dapps.count()*/
    }

    override fun onBindViewHolder(holder: DappHolder, position: Int) {
        holder.bindDapp(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val view = layoutInflater.inflate(R.layout.marketplace_dapp_card, parent, false)
        return DappHolder(view)
    }

    class DappHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v

        fun bindDapp(position: Int) {
            val imageView = view.findViewById<ImageView>(R.id.dappSquareImage)
            if (position == 0) {
                view.find<TextView>(R.id.dappNameTextView).text = "Switcheo"
                view.find<TextView>(R.id.dappDescriptionTextView).text =
                        "A decentralized exchange that lets you trade directly from your mobile wallet"
                Glide.with(view.context).load("https://cdn.o3.network/img/neo/SWTH.png").into(imageView)
            } else if (position == 1) {
                view.find<TextView>(R.id.dappNameTextView).text = "NeoTracker"
                view.find<TextView>(R.id.dappDescriptionTextView).text =
                        "The best block explorer on the NEO platform"
                Glide.with(view.context).load("https://pbs.twimg.com/profile_images/888004106245201920/vMRWDGRt_400x400.jpg").into(imageView)
            } else {
                view.find<TextView>(R.id.dappNameTextView).text = "NNS"
                view.find<TextView>(R.id.dappDescriptionTextView).text =
                        "Register your NNS domain name, for a user friendly handle for your neo address"
                Glide.with(view.context).load("https://cdn.o3.network/img/neo/NNC.png").into(imageView)
            }

            view.setOnClickListener {
                val browserIntent = Intent(view.context, DAppBrowserActivity::class.java)

                if (position == 0) {
                    browserIntent.putExtra("url", "https://www.switcheo.exchange")
                } else if (position == 1) {
                    browserIntent.putExtra("url", "https://neotracker.io/")
                } else {
                    browserIntent.putExtra("url", "https://wallet.nel.group/")
                }
                view.context.startActivity(browserIntent)
            }
        }

        companion object {
            private val Dapp_key = "DAPP"
        }
    }
}
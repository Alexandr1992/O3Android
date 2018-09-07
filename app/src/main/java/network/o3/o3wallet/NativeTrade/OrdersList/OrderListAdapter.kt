package network.o3.o3wallet.NativeTrade.OrdersList

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class OrdersAdapter(private var orders: List<SwitcheoOrders>, private var mFragment: OrdersListFragment):
        RecyclerView.Adapter<OrdersAdapter.OrderHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.native_trade_order_card, parent, false)
        return OrderHolder(view, mFragment)

    }

    override fun getItemCount(): Int {
        return orders.count()
    }

    override fun onBindViewHolder(holder: OrderHolder, position: Int) {
        holder.bindOrder(orders[position])
    }

    class OrderHolder(v: View, fragment: OrdersListFragment): RecyclerView.ViewHolder(v) {
        val mView = v
        val mFragment = fragment

        fun bindOrder(order: SwitcheoOrders) {
            val orderType = order.side
            val orderAmount = order.want_amount.toDouble() / 100000000
            val orderAsset = order.want_asset_id
            val orderCreatedTime = order.created_at

            val baseAsset = order.offer_asset_id
            val baseAssetAmount = order.offer_amount.toDouble() / 100000000

            mView.find<TextView>(R.id.orderTypeTextView).text = orderType
            mView.find<TextView>(R.id.orderAssetAmountTextView).text = orderAmount.toString() + " " + "ABC"/*orderAsset*/
            mView.find<TextView>(R.id.orderTimeTextView).text = orderCreatedTime
            mView.find<TextView>(R.id.baseAssetAmountTextView).text = baseAssetAmount.toString() + " " + "DEF" /*baseAsset*/

            mView.setOnClickListener {
                mFragment.showOrderUpdateOptions(order)
            }
        }
    }
}
package network.o3.o3wallet.NativeTrade.OrdersList

import android.content.Context
import android.graphics.Color
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.shawnlin.numberpicker.NumberPicker
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.R
import network.o3.o3wallet.R.id.view
import network.o3.o3wallet.format
import network.o3.o3wallet.formattedPercentString
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.textColor
import java.text.SimpleDateFormat
import java.util.*

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

        fun calculatePercentFilled(order: SwitcheoOrders): Double {

            var fillSum = 0.0
            for (make in order.makes) {
                fillSum += make.filled_amount.toDouble()
            }

            for (fill in order.fills) {
                fillSum  += fill.filled_amount.toDouble()
            }
            val percentFilled = (fillSum / order.want_amount.toDouble()) * 100
            return percentFilled
        }

        fun getRate(order: SwitcheoOrders): String {
            var rate = 0.0
            if(order.side == "buy") {
                rate = order.makes[0].offer_amount.toDouble() / order.makes[0].want_amount.toDouble()
            } else {
                rate = order.makes[0].want_amount.toDouble()  / order.makes[0].offer_amount.toDouble()
            }

            return rate.removeTrailingZeros()
        }


        fun bindOrder(order: SwitcheoOrders) {
            val orderType = order.side.toUpperCase()



            val orderAmount = order.want_amount.toDouble() / 100000000
            val orderAsset = order.want_asset_id
            val orderCreatedTime = order.created_at

            val baseAsset = order.offer_asset_id
            val baseAssetAmount = order.offer_amount.toDouble() / 100000000
            val percentFilled = calculatePercentFilled(order)

            mView.find<TextView>(R.id.orderTypeTextView).text = orderType
            if (orderType == "BUY") {
                mView.find<TextView>(R.id.orderTypeTextView).textColor = mView.context.getColor(R.color.colorGain)
                mView.find<TextView>(R.id.orderAssetAmountTextView).text = orderAmount.removeTrailingZeros() + " " + "ABC"/*orderAsset*/
                mView.find<TextView>(R.id.baseAssetAmountTextView).text = baseAssetAmount.removeTrailingZeros() + " " + "DEF" /*baseAsset*/
                mView.find<TextView>(R.id.orderCryptoRateTextView).text = getRate(order) + "ABC / DEF"
                mView.find<ImageView>(R.id.orderArrowImage).image = mView.context.getDrawable(R.drawable.ic_arrow_buy)
            } else {
                mView.find<TextView>(R.id.orderTypeTextView).textColor = mView.context.getColor(R.color.colorSell)
                mView.find<TextView>(R.id.orderAssetAmountTextView).text = baseAssetAmount.removeTrailingZeros() + " " + "ABC"/*baseAsset*/
                mView.find<TextView>(R.id.baseAssetAmountTextView).text = orderAmount.removeTrailingZeros() + " " + "DEF" /*orderAsset*/
                mView.find<TextView>(R.id.orderCryptoRateTextView).text = getRate(order) + " ABC / DEF"
                mView.find<ImageView>(R.id.orderArrowImage).image = mView.context.getDrawable(R.drawable.ic_arrow_sell)
            }



            val sdfInput =  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            val date = sdfInput.parse(orderCreatedTime)
            val sdf =  SimpleDateFormat("MMM dd yyyy @ HH:mm", Locale.getDefault())
            val formatOut = sdf.format(date)

            //val date = java.util.Date(orderCreatedTime.toLong())
            mView.find<TextView>(R.id.orderTimeTextView).text = formatOut
            mView.find<TextView>(R.id.orderFillAmountTextView).text = String.format(mView.context.resources.getString(R.string.NATIVE_TRADE_order_fill_amount), percentFilled.formattedPercentString())
            mView.find<ProgressBar>(R.id.orderFillProgressBar).progress = percentFilled.toInt()

            mView.setOnClickListener {
                mFragment.showOrderUpdateOptions(order)
            }
        }
    }
}
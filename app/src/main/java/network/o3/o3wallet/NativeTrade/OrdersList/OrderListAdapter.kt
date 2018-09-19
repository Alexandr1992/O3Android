package network.o3.o3wallet.NativeTrade.OrdersList

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import network.o3.o3wallet.API.O3Platform.O3SwitcheoOrders
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.NativeTrade.DepositWithdrawal.DepositWithdrawalActivity
import network.o3.o3wallet.NativeTrade.NativeTradeRootActivity
import network.o3.o3wallet.R
import network.o3.o3wallet.formattedPercentString
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.textColor
import java.text.SimpleDateFormat
import java.util.*

class OrdersAdapter(private var orders: List<O3SwitcheoOrders>, private var mFragment: OrdersListFragment):
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

        fun calculatePercentFilled(order: O3SwitcheoOrders): Double {

            var fillSum = 0.0
            for (make in order.makes) {
                for (trade in make.trades) {
                    fillSum += trade["filled_amount"].asDouble
                }
            }

            for (fill in order.fills) {
                fillSum  += fill.filled_amount.toDouble()
            }
            val percentFilled = (fillSum / order.want_amount.toDouble()) * 100
            return percentFilled
        }

        fun getRate(order: O3SwitcheoOrders): String {
            var rate = 0.0
            if(order.side == "buy") {
                rate = order.makes[0].offer_amount.toDouble() / order.makes[0].want_amount.toDouble()
            } else {
                rate = order.makes[0].want_amount.toDouble()  / order.makes[0].offer_amount.toDouble()
            }

            return rate.removeTrailingZeros()
        }

        fun showOptionsMenu(order: O3SwitcheoOrders) {
            val popup = PopupMenu(mView.context, mView)
            popup.menuInflater.inflate(R.menu.orders_menu,popup.menu)
            popup.setOnMenuItemClickListener {
                val itemId = it.itemId

                if (itemId == R.id.cancel_order_menu_item) {
                    SwitcheoAPI().singleStepCancel(order.id) {
                        mFragment.displayCancelResult(it.first ?: false)
                    }
                }
                true
            }
            popup.show()
        }


        fun bindOrder(order: O3SwitcheoOrders) {
            mView.setOnClickListener {
                showOptionsMenu(order)
            }
            val orderType = order.side.toUpperCase()

            val orderAmount = order.want_amount.toDouble() / 100000000
            val orderAsset = order.wantAsset
            val orderCreatedTime = order.created_at

            val baseAsset = order.offerAsset
            val baseAssetAmount = order.offer_amount.toDouble() / 100000000
            val percentFilled = calculatePercentFilled(order)

            mView.find<TextView>(R.id.orderTypeTextView).text = orderType
            if (orderType == "BUY") {
                mView.find<TextView>(R.id.orderTypeTextView).textColor = mView.context.getColor(R.color.colorGain)
                mView.find<TextView>(R.id.orderAssetAmountTextView).text = orderAmount.removeTrailingZeros() + " " + orderAsset.symbol.toUpperCase()
                mView.find<TextView>(R.id.baseAssetAmountTextView).text = baseAssetAmount.removeTrailingZeros() + " " + baseAsset.symbol.toUpperCase()
                mView.find<TextView>(R.id.orderCryptoRateTextView).text =
                        getRate(order) +  " " + baseAsset.symbol.toUpperCase()  + "/" + orderAsset.symbol.toUpperCase()
                mView.find<ImageView>(R.id.orderArrowImage).image = mView.context.getDrawable(R.drawable.ic_arrow_buy)
                val orderAssetURL = String.format("https://cdn.o3.network/img/neo/%s.png", orderAsset.symbol.toUpperCase())
                val baseAssetURL = String.format("https://cdn.o3.network/img/neo/%s.png", baseAsset.symbol.toUpperCase())
                Glide.with(mView.context).load(orderAssetURL).into(mView.find(R.id.orderAssetLogoImageView))
                Glide.with(mView.context).load(baseAssetURL).into(mView.find(R.id.baseAssetLogoImageView))

            } else {
                mView.find<TextView>(R.id.orderTypeTextView).textColor = mView.context.getColor(R.color.colorSell)
                mView.find<TextView>(R.id.orderAssetAmountTextView).text = baseAssetAmount.removeTrailingZeros() + " " + baseAsset.symbol.toUpperCase()
                mView.find<TextView>(R.id.baseAssetAmountTextView).text = orderAmount.removeTrailingZeros() + " " +  orderAsset.symbol.toUpperCase()
                mView.find<TextView>(R.id.orderCryptoRateTextView).text =
                        getRate(order) +  " " + orderAsset.symbol.toUpperCase()  + "/" + baseAsset.symbol.toUpperCase()
                mView.find<ImageView>(R.id.orderArrowImage).image = mView.context.getDrawable(R.drawable.ic_arrow_sell)

                val orderAssetURL = String.format("https://cdn.o3.network/img/neo/%s.png", baseAsset.symbol.toUpperCase())
                val baseAssetURL = String.format("https://cdn.o3.network/img/neo/%s.png", orderAsset.symbol.toUpperCase())
                Glide.with(mView.context).load(orderAssetURL).into(mView.find(R.id.orderAssetLogoImageView))
                Glide.with(mView.context).load(baseAssetURL).into(mView.find(R.id.baseAssetLogoImageView))
            }



            val sdfInput =  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            val date = sdfInput.parse(orderCreatedTime)
            val sdf =  SimpleDateFormat("MMM dd yyyy @ HH:mm", Locale.getDefault())
            val formatOut = sdf.format(date)

            //val da    te = java.util.Date(orderCreatedTime.toLong())
            mView.find<TextView>(R.id.orderTimeTextView).text = formatOut
            mView.find<TextView>(R.id.orderFillAmountTextView).text = String.format(mView.context.resources.getString(R.string.NATIVE_TRADE_order_fill_amount), percentFilled.formattedPercentString())
            mView.find<ProgressBar>(R.id.orderFillProgressBar).progress = percentFilled.toInt()
        }
    }
}
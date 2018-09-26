package network.o3.o3wallet.NativeTrade.OrdersList

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3.FooterRow
import network.o3.o3wallet.API.O3Platform.O3SwitcheoOrders
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.NativeTrade.OrderResultDialog
import network.o3.o3wallet.R
import network.o3.o3wallet.formattedPercentString
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.find
import java.lang.Math.pow
import java.text.SimpleDateFormat
import java.util.*

class OrdersAdapter(private var orders: List<O3SwitcheoOrders>, private var mFragment: OrdersListFragment):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var showClosedOrders = false

    private var openOrders: MutableList<O3SwitcheoOrders> = mutableListOf()

    fun orderIsClosed(order: O3SwitcheoOrders): Boolean {
        if (order.makes!!.count() > 0 && order.status == "processed") {
            if (order.makes.find { it.status == "cancelled" } != null) {
                if (calculatePercentFilled(order) > 0.0) {
                    return true
                }
            } else if (100.0 - calculatePercentFilled(order) < 0.00000001) {
                return true
            }
        }
        return false
    }

    fun calculatePercentFilled(order: O3SwitcheoOrders): Double {
        var fillSum = 0.0
        for (make in order.makes) {
            for (trade in make.trades ?: listOf()) {
                fillSum += trade["filled_amount"].asDouble / order.want_amount.toDouble()
            }
        }

        for (fill in order.fills) {
            fillSum  += (fill.fill_amount.toDoubleOrNull() ?: 0.0) / order.offer_amount.toDouble()
        }
        val percentFilled = fillSum * 100
        return percentFilled
    }


    init {
        for (order in orders) {
            if (!orderIsClosed(order)) {
                openOrders.add(order)
            }
        }
    }


    companion object {
        val ORDERROW = 1
        val FOOTERROW = 2
    }

    override fun getItemViewType(position: Int): Int {
        if (showClosedOrders && position == orders.count()) {
            return FOOTERROW
        } else if (!showClosedOrders && position == openOrders.count()) {
            return FOOTERROW
        } else {
            return ORDERROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        if (viewType == ORDERROW) {
            val view = layoutInflater.inflate(R.layout.native_trade_order_card, parent, false)
            return OrderHolder(view, mFragment)
        } else {
            val view = layoutInflater.inflate(R.layout.button_footer, parent, false)
            return FooterHolder(view, mFragment, this)
        }
    }

    override fun getItemCount(): Int {
        if (showClosedOrders) {
            return orders.count() + 1
        } else {
            return openOrders.count() + 1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (showClosedOrders && position != orders.count()) {
            (holder as OrderHolder).bindOrder(orders[position])
        } else if (!showClosedOrders && position != openOrders.count()){
            (holder as OrderHolder).bindOrder(openOrders[position])
        } else {
            (holder as FooterHolder).bindFooter()
        }
    }

    class FooterHolder(v: View, fragment: OrdersListFragment, adapter: OrdersAdapter): RecyclerView.ViewHolder(v) {
        val mView = v
        val mFragment = fragment
        val mAdapter = adapter
        fun bindFooter() {
            val footerButton = mView.find<Button>(R.id.footerButton)

            if (mAdapter.showClosedOrders) {
                footerButton.text = mView.resources.getString(R.string.NATIVE_TRADE_hide_orders)
            } else {
                footerButton.text = mView.resources.getString(R.string.NATIVE_TRADE_show_orders)
            }

            footerButton.setOnClickListener {
                mAdapter.showClosedOrders = !mAdapter.showClosedOrders
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    class OrderHolder(v: View, fragment: OrdersListFragment): RecyclerView.ViewHolder(v) {
        val mView = v
        val mFragment = fragment

        fun calculatePercentFilled(order: O3SwitcheoOrders): Double {
            var fillSum = 0.0
            for (make in order.makes) {
                for (trade in make.trades ?: listOf()) {
                    fillSum += trade["filled_amount"].asDouble / order.want_amount.toDouble()
                }
            }

            for (fill in order.fills) {
                fillSum  += (fill.fill_amount.toDoubleOrNull() ?: 0.0) / order.offer_amount.toDouble()
            }
            val percentFilled = fillSum * 100
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

        fun orderIsClosed(order: O3SwitcheoOrders): Boolean {
            if (order.makes!!.count() > 0 && order.status == "processed") {
                if (order.makes.find { it.status == "cancelled" } != null) {
                    if (calculatePercentFilled(order) > 0.0) {
                        return true
                    }
                } else if (100.0 - calculatePercentFilled(order) < 0.00000001) {
                    return true
                }
            }
            return false
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

            val orderAmount = order.want_amount.toDouble() / pow(10.0, order.wantAsset.decimals.toDouble())
            val orderAsset = order.wantAsset
            val orderCreatedTime = order.created_at

            val baseAsset = order.offerAsset
            val baseAssetAmount = order.offer_amount.toDouble() / pow(10.0, order.offerAsset.decimals.toDouble())
            val percentFilled = calculatePercentFilled(order)

            if (orderIsClosed(order)) {
                mView.find<TextView>(R.id.orderIsClosedTextView).visibility = View.VISIBLE
                mView.setOnClickListener {  }
            } else {
                mView.find<TextView>(R.id.orderIsClosedTextView).visibility = View.INVISIBLE
                mView.setOnClickListener { showOptionsMenu(order) }
            }

            mView.find<TextView>(R.id.orderAssetAmountTextView).text = orderAmount.removeTrailingZeros() + "\n" + orderAsset.symbol.toUpperCase()
            mView.find<TextView>(R.id.baseAssetAmountTextView).text = baseAssetAmount.removeTrailingZeros() + "\n" + baseAsset.symbol.toUpperCase()
            mView.find<TextView>(R.id.orderCryptoRateTextView).text =
                        getRate(order) +  " " + baseAsset.symbol.toUpperCase()  + "/" + orderAsset.symbol.toUpperCase()
            val orderAssetURL = String.format("https://cdn.o3.network/img/neo/%s.png", orderAsset.symbol.toUpperCase())
            val baseAssetURL = String.format("https://cdn.o3.network/img/neo/%s.png", baseAsset.symbol.toUpperCase())
            Glide.with(mView.context).load(orderAssetURL).into(mView.find(R.id.orderAssetLogoImageView))
            Glide.with(mView.context).load(baseAssetURL).into(mView.find(R.id.baseAssetLogoImageView))

            val sdfInput =  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            val date = sdfInput.parse(orderCreatedTime)
            val sdf =  SimpleDateFormat("MMM dd yyyy @ HH:mm", Locale.getDefault())
            val formatOut = sdf.format(date)

            //val date = java.util.Date(orderCreatedTime.toLong())
            mView.find<TextView>(R.id.orderTimeTextView).text = formatOut
            mView.find<TextView>(R.id.orderFillAmountTextView).text = String.format(mView.context.resources.getString(R.string.NATIVE_TRADE_order_fill_amount), percentFilled.formattedPercentString())
            mView.find<ProgressBar>(R.id.orderFillProgressBar).progress = percentFilled.toInt()
        }
    }
}
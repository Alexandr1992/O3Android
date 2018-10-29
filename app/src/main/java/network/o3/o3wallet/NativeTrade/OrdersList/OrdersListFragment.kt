package network.o3.o3wallet.NativeTrade.OrdersList


import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.VERTICAL
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.amplitude.api.Amplitude
import kotlinx.android.synthetic.main.wallet_fragment_account.*
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.O3SwitcheoOrders
import network.o3.o3wallet.API.O3Platform.orderIsClosed
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.NativeTrade.NativeTradeBaseAssetBottomSheet
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.getColorFromAttr
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.yesButton
import org.json.JSONObject

class OrdersListFragment : Fragment() {
    private lateinit var mView: View

    private lateinit var ordersListView: RecyclerView
    private lateinit var swipeContainer: SwipeRefreshLayout

    fun postOpenOrdersCount(orders: List<O3SwitcheoOrders>) {
        var count = 0
        for (order in orders) {
            if (!order.orderIsClosed()) {
                count ++
            }
        }
        if (activity is MainTabbedActivity) {
            val tab = activity?.find<TabLayout>(R.id.tabLayout)?.getTabAt(2)
            if (tab != null) {
                if (count > 0) {
                    tab.text = resources.getString(R.string.NATIVE_TRADE_orders) + " (" + count.toString() + ")"
                } else {
                    tab.text = resources.getString(R.string.NATIVE_TRADE_orders)
                }
            }
        }
    }


    fun loadOrders() {
        O3PlatformClient().getPendingOrders {
            onUiThread {
                ordersListView.adapter = OrdersAdapter(it.first ?: listOf(), this)
                swipeContainer.isRefreshing = false
                postOpenOrdersCount(it.first!!)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.native_trade_orders_list_fragment, container, false)!!
        ordersListView = mView.find(R.id.ordersListView)
        val recyclerLayout = LinearLayoutManager(context)
        recyclerLayout.orientation = VERTICAL
        ordersListView.layoutManager = recyclerLayout
        loadOrders()

        swipeContainer = mView.find<SwipeRefreshLayout>(R.id.orderSwipeContainer)
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary)

        swipeContainer.setProgressBackgroundColorSchemeColor(context!!.getColorFromAttr(R.attr.secondaryBackgroundColor))
        swipeContainer.setOnRefreshListener {
            loadOrders()
        }

        return mView
    }

    fun cancelOrder(orderId: String) {
        SwitcheoAPI().singleStepCancel(orderId) {
            onUiThread {
                if (it.first == true) {
                    val orderDetailsAttrs = mapOf("order_id" to orderId)
                    Amplitude.getInstance().logEvent("Order Cancelled", JSONObject(orderDetailsAttrs))
                    alert(mView.context.getString(R.string.NATIVE_TRADE_cancel_order_succeeeded)) {
                        yesButton { }
                    }.show()
                } else {
                    alert(mView.context.getString(R.string.NATIVE_TRADE_cancel_order_failed)) {
                        yesButton { }
                    }.show()
                }
            }
        }
    }

    companion object {
        fun newInstance(): OrdersListFragment {
            return OrdersListFragment()
        }
    }
}
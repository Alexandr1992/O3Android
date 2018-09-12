package network.o3.o3wallet.NativeTrade.OrdersList


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.VERTICAL
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.wallet_fragment_account.*
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.NativeTrade.NativeTradeBaseAssetBottomSheet
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.getColorFromAttr
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.onUiThread

class OrdersListFragment : Fragment() {
    private lateinit var mView: View

    private lateinit var ordersListView: RecyclerView
    private lateinit var swipeContainer: SwipeRefreshLayout

    fun loadOrders() {
        SwitcheoAPI().getPendingOrders {
            onUiThread {
                ordersListView.adapter = OrdersAdapter(it.first!!, this)
                swipeContainer.isRefreshing = false
                if (it.first!!.isEmpty()) {
                    mView.find<TextView>(R.id.ordersEmptyTextView).visibility = View.VISIBLE
                } else {
                    mView.find<TextView>(R.id.ordersEmptyTextView).visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.native_trade_orders_list_fragment, container, false)!!
        ordersListView = mView.find<RecyclerView>(R.id.ordersListView)
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

    fun showOrderUpdateOptions(order: SwitcheoOrders) {
        val bundle = Bundle()
        val orderOptions = OrderUpdateOptionsBottomSheet()
        bundle.putString("id", order.id)
        orderOptions.arguments = bundle
        orderOptions.show(activity!!.supportFragmentManager, orderOptions.tag)
    }

    companion object {
        fun newInstance(): OrdersListFragment {
            return OrdersListFragment()
        }
    }
}
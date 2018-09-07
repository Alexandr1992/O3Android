package network.o3.o3wallet.NativeTrade.OrdersList


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.VERTICAL
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.NativeTrade.NativeTradeBaseAssetBottomSheet
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import org.jetbrains.anko.find

class OrdersListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.native_trade_orders_list_fragment, container, false)!!
        val ordersListView = view.find<RecyclerView>(R.id.ordersListView)
        val recyclerLayout = LinearLayoutManager(context)
        recyclerLayout.orientation = VERTICAL
        ordersListView.layoutManager = recyclerLayout
        SwitcheoAPI().getPendingOrders {
            ordersListView.adapter = OrdersAdapter(it.first!!, this)
        }
        return view
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
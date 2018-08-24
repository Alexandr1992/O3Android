package network.o3.o3wallet.Wallet.TransactionHistory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransactionHistoryEntry
import network.o3.o3wallet.Account
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.support.v4.onUiThread
import java.util.concurrent.CountDownLatch
import android.support.v7.widget.DividerItemDecoration
import android.support.v4.content.ContextCompat





/**
 * A simple [Fragment] subclass.
 */
class TransactionHistoryFragment : Fragment() {

    lateinit var recyclerView: RecyclerView
    private lateinit var swipeContainer: SwipeRefreshLayout

    var txHistory: Array<TransactionHistoryEntry>? = null
    var currentPage = 1
    var paginator: PaginationScrollListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.wallet_transaction_history_fragment, container, false)
        recyclerView = view.find<RecyclerView>(R.id.txHistoryRecyclerView)
        val entries = txHistory?.toMutableList() ?: mutableListOf()

        val layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = TransactionHistoryAdapter(entries, context!!)
        val itemDecorator = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.vertical_divider)!!)
        recyclerView.addItemDecoration(itemDecorator)

        swipeContainer = view.findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary)

        swipeContainer.setOnRefreshListener {
            swipeContainer.isRefreshing = true
            this.loadFirstPage()
        }


        paginator = object: PaginationScrollListener(layoutManager) {
            override fun loadMoreItems() {
                isLoading = true
                onUiThread { (recyclerView.adapter as TransactionHistoryAdapter).addLoadingFooter() }
                currentPage = currentPage + 1
                O3PlatformClient().getTransactionHistory(currentPage) {
                    isLoading = false
                    onUiThread {
                        (recyclerView.adapter as TransactionHistoryAdapter).removeLoadingFooter()
                        if (it.second != null) {
                            currentPage = currentPage - 1
                        } else {
                            totalPageCount = it.first!!.totalPage
                            isLastPage = currentPage == totalPageCount
                            (recyclerView.adapter as TransactionHistoryAdapter).addAllTransactions(it.first!!.history.toList())
                        }
                    }
                }
            }
        }

        recyclerView.addOnScrollListener(paginator as PaginationScrollListener)
        loadFirstPage()
        return view
    }

    fun loadFirstPage() {
        currentPage = 1
        if (paginator != null) {
            paginator?.isLastPage = false
        }
        onUiThread { (recyclerView.adapter as TransactionHistoryAdapter).removeAllTransactions() }
        O3PlatformClient().getTransactionHistory(currentPage) {
            onUiThread {  swipeContainer.isRefreshing = false }
            if (it.second != null) {
                return@getTransactionHistory
            }
            onUiThread {
                (recyclerView.adapter as TransactionHistoryAdapter).addAllTransactions(it.first!!.history.toList())
            }
        }
    }

    companion object {
        fun newInstance(): Fragment {
            return TransactionHistoryFragment()
        }
    }
}

package network.o3.o3wallet.Inbox


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import network.o3.o3wallet.API.O3.Message
import network.o3.o3wallet.API.O3.MessageAction
import network.o3.o3wallet.API.O3.MessageChannel
import network.o3.o3wallet.R
import network.o3.o3wallet.Wallet.TransactionHistory.PaginationScrollListener
import org.jetbrains.anko.find

/**
 * A simple [Fragment] subclass.
 *
 */
class InboxMessagesFragment : Fragment() {

    lateinit var mView: View
    lateinit var recyclerView: RecyclerView
    lateinit var paginator: PaginationScrollListener
    var inboxViewModel = InboxViewModel()


    val dummyMessage1 = Message(id="abcxyz", title="O3 Labs",
            timestamp = "1556090601", channel = MessageChannel(service="O3 Labs", topic= "General"),
            action= MessageAction(type="browser", title = "Click this button", url="https://www.o3.network"))
    val dummyMessage2 = Message(id="abcxyz", title="O3 Labs",
            timestamp = "1556090601", channel = MessageChannel(service="O3 Labs", topic= "General"),
            action= MessageAction(type="browser", title = "Click this button", url="https://www.o3.network"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.inbox_messages_fragment, container, false)
        recyclerView = mView.find(R.id.inboxRecyclerView)

        /*paginator = object: PaginationScrollListener(recyclerView.layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                isLoading = true
                runOnUiThread { (recyclerView.adapter as InboxAdapter).addLoadingFooter() }
                inboxViewModel.currentPage = inboxViewModel.currentPage + 1


                /*O3PlatformClient().getInboxItems(inboxViewModel.currentPage) {
                    runOnUiThread  {

                        isLoading = false
                        (recyclerView.adapter as InboxAdapter).removeLoadingFooter()
                        if (it.second != null || it.first == null || (it.first?.history ?: arrayOf()).isEmpty()) {
                            inboxViewModel.currentPage = inboxViewModel.currentPage - 1
                        } else {
                            val history = it.first!!
                            totalPageCount = it.first!!.totalPage
                            isLastPage = inboxViewModel.currentPage == totalPageCount
                            (recyclerView.adapter as InboxAdapter).addAllTransactions(history.history.toList())
                        }
                    }
                }*/
            }
        }

        //recyclerView.addOnScrollListener(paginator)*/
        inboxViewModel.loadFirstPage()



        return mView
    }

    class InboxAdapter: RecyclerView.Adapter<InboxAdapter.MessageHolder>() {
        override fun getItemCount(): Int {
            return 3
        }

        override fun onBindViewHolder(holder: MessageHolder, position: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }


        class MessageHolder(v: View): RecyclerView.ViewHolder(v) {
        }


        //for pagination
        fun addLoadingFooter() {

        }

        fun removeLoadingFooter() {

        }

    }


}

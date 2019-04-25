package network.o3.o3wallet.Inbox


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3.Message
import network.o3.o3wallet.R
import network.o3.o3wallet.Wallet.TransactionHistory.PaginationScrollListener
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * A simple [Fragment] subclass.
 *
 */
class InboxMessagesFragment : Fragment() {

    lateinit var mView: View
    lateinit var recyclerView: RecyclerView
    lateinit var paginator: PaginationScrollListener
    var inboxViewModel = InboxViewModel()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.inbox_messages_fragment, container, false)
        recyclerView = mView.find(R.id.inboxRecyclerView)
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = InboxAdapter()

        inboxViewModel.getInboxItems().observe(this, Observer { messages ->
            (recyclerView.adapter as InboxAdapter).addPage(messages)
        })

        inboxViewModel.getLoadingStatus().observe(this, Observer { isLoading ->
            if (isLoading) {
                // add the loading footer
            } else {
                //remove the loading footer
            }
        })

        paginator = object: PaginationScrollListener(recyclerView.layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                inboxViewModel.currentPage += 1
                inboxViewModel.loadInboxItems(inboxViewModel.currentPage)
            }
        }
        recyclerView.addOnScrollListener(paginator)

        return mView
    }

    class InboxAdapter: RecyclerView.Adapter<InboxAdapter.MessageHolder>() {
        var allMessages: MutableList<Message> = mutableListOf()

        fun addPage(newMessages: List<Message>) {
            allMessages.addAll(newMessages)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return allMessages.count()
        }

        override fun onBindViewHolder(holder: MessageHolder, position: Int) {
            holder.bindMessage(allMessages[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.inbox_message_row_layout, parent, false)
            return InboxAdapter.MessageHolder(view)
        }

        //for pagination
        fun addLoadingFooter() {
            notifyDataSetChanged()
        }

        fun removeLoadingFooter() {
            notifyDataSetChanged()
        }


        class MessageHolder(v: View): RecyclerView.ViewHolder(v) {
            private var mView: View = v

            fun bindMessage(message: Message) {
                mView.find<TextView>(R.id.messageTitleView).text = message.channel.service
                mView.find<TextView>(R.id.messageDescriptionTextView).text = message.title
                mView.find<TextView>(R.id.messageDateTextView).text = message.timestamp
                mView.find<Button>(R.id.messageActionButton).text = message.action.title
                mView.find<Button>(R.id.messageActionButton).onClick {}
                mView.find<ImageView>(R.id.messageImageView)
                Glide.with(mView.context).load("https://cdn.o3.network/img/neo/NEO.png").
                        into(mView.find<ImageView>(R.id.messageImageView))

            }
        }
    }
}

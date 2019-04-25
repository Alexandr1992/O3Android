package network.o3.o3wallet.Inbox


import android.content.Intent
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
import network.o3.o3wallet.Dapp.DappContainerActivity
import network.o3.o3wallet.R
import network.o3.o3wallet.Wallet.TransactionHistory.PaginationScrollListener
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.text.SimpleDateFormat
import java.util.*

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

        setupInboxListeners()
        setNotificationSettings()
        showOptInIfNecessary()

        return mView
    }

    fun showOptInIfNecessary() {
        if (true) {
            InboxOptInBottomSheet.newInstance().show(activity!!.supportFragmentManager, "optin")
        }
    }

    fun setupInboxListeners() {
        inboxViewModel.getInboxItems().observe(this, Observer { messages ->
            (recyclerView.adapter as InboxAdapter).addPage(messages)
        })

        inboxViewModel.getLoadingStatus().observe(this, Observer { isLoading ->
            if (isLoading) {
                (recyclerView.adapter as InboxAdapter).addLoadingFooter()
            } else {
                (recyclerView.adapter as InboxAdapter).removeLoadingFooter()
            }
        })

        paginator = object: PaginationScrollListener(recyclerView.layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                inboxViewModel.currentPage += 1
                inboxViewModel.loadInboxItems(inboxViewModel.currentPage)
            }
        }
        recyclerView.addOnScrollListener(paginator)
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
                mView.find<Button>(R.id.messageActionButton).onClick {}
                mView.find<ImageView>(R.id.messageImageView)
                Glide.with(mView.context).load("https://community.o3.network/uploads/default/original/1X/ef7f27e38a371cb7ef053ce13ee2085fd194292b.jpg").
                        into(mView.find<ImageView>(R.id.messageImageView))

                val sdf =   SimpleDateFormat("MMM dd yyyy @ HH:mm", Locale.getDefault())
                val date = java.util.Date(message.timestamp.toLong() * 1000)
                mView.find<TextView>(R.id.messageDateTextView).text = sdf.format(date)

                if (message.action == null) {
                    mView.find<Button>(R.id.messageActionButton).visibility = View.GONE
                } else {
                    mView.find<Button>(R.id.messageActionButton).visibility = View.VISIBLE
                    mView.find<Button>(R.id.messageActionButton).text = message.action.title
                    mView.find<Button>(R.id.messageActionButton).onClick {
                        val intent = Intent(mView.context, DappContainerActivity::class.java)
                        intent.putExtra("url", message.action.url)
                        mView.context.startActivity(intent)
                    }
                }
            }
        }
    }
}

package network.o3.o3wallet.Wallet.TransactionHistory

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import network.o3.o3wallet.*
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import android.content.Intent
import android.widget.ImageView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TokenListing
import network.o3.o3wallet.API.O3Platform.TransactionHistoryEntry
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by drei on 4/24/18.
 */

class TransactionHistoryAdapter(private var transactionHistoryEntries: MutableList<TransactionHistoryEntry>,
                                context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val TRANSACTION_ENTRY_VIEW = 0
    val LOADING_FOOTER_VIEW = 1
    val SECTION_HEADER = 2

    private var mContext = context
    private var isLoadingAdded = false
    private var availableTokens:Array<TokenListing> = arrayOf()

    init {
        O3PlatformClient().getNep5 {
            if (it.second != null) {
                return@getNep5
            } else {
                availableTokens = it.first!!.nep5tokens
            }
        }
    }

    fun pendingHeaderPosition(): Int? {
        if (PersistentStore.getPendingTransactions().count() > 0 ) {
            return 0
        }
        return null
    }

    fun txHeaderPostion(): Int? {
        if (PersistentStore.getPendingTransactions().count() == 0 ) {
            return null
        } else {
            return 1 + PersistentStore.getPendingTransactions().count()
        }
    }

    fun headerCount(): Int {
        if (PersistentStore.getPendingTransactions().count() > 0) {
            return 2
        }
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        if (viewType == TRANSACTION_ENTRY_VIEW) {
            val view = layoutInflater.inflate(R.layout.wallet_transaction_history_row_layout, parent, false)
            return TransactionHistoryAdapter.TransactionViewHolder(view)
        } else  if (viewType == LOADING_FOOTER_VIEW){
            val view = layoutInflater.inflate(R.layout.wallet_transaction_history_footer, parent, false)
            return TransactionHistoryAdapter.LoadingViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.wallet_transaction_history_header, parent, false)
            return TransactionHistoryAdapter.SectionHeaderViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val type = getItemViewType(position)
        if (type == LOADING_FOOTER_VIEW) {
            return
        }

        if (txHeaderPostion() != null && position == pendingHeaderPosition()) {
            (holder as SectionHeaderViewHolder).bindHeader(0)
            return
        }

        if (txHeaderPostion() != null && position == txHeaderPostion()) {
            (holder as SectionHeaderViewHolder).bindHeader(1)
            return
        }

        if(PersistentStore.getPendingTransactions().count() > 0) {
            if (position < PersistentStore.getPendingTransactions().count() + 1) {
                (holder as TransactionViewHolder).bindTransaction(
                        PersistentStore.getPendingTransactions()[position - 1], availableTokens)
                return
            } else {
                (holder as TransactionViewHolder).bindTransaction(
                        transactionHistoryEntries[position - PersistentStore.getPendingTransactions()!!.count() - 2], availableTokens)
                return
            }
        } else {
            (holder as TransactionViewHolder).bindTransaction(
                    transactionHistoryEntries[position], availableTokens)
        }
    }

    override fun getItemCount(): Int {
        var footerInserted = 0
        if (isLoadingAdded) {
            footerInserted = 1
        }
        return transactionHistoryEntries.count() + PersistentStore.getPendingTransactions()!!.count() + footerInserted + headerCount()
    }

    override fun getItemViewType(position: Int): Int {
        if (txHeaderPostion() != null && position == txHeaderPostion()) {
            return SECTION_HEADER
        }

        if (pendingHeaderPosition() != null && position == pendingHeaderPosition()) {
            return SECTION_HEADER
        }

        return if (position == transactionHistoryEntries.count() + PersistentStore.getPendingTransactions()!!.count() + headerCount()
                && isLoadingAdded) {
            LOADING_FOOTER_VIEW
        } else {
            TRANSACTION_ENTRY_VIEW
        }
    }

    fun addLoadingFooter() {
        isLoadingAdded = true
        notifyDataSetChanged()
    }

    fun removeLoadingFooter() {
        isLoadingAdded = false
        notifyDataSetChanged()
    }

    fun addAllTransactions(txList: List<TransactionHistoryEntry>) {
        for (tx in txList) {
            transactionHistoryEntries.add(tx)

        }
        notifyDataSetChanged()
    }

    fun removeAllTransactions() {
        transactionHistoryEntries.clear()
        notifyDataSetChanged()
    }

    class TransactionViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view = v
        private var supportedTokens: Array<TokenListing> = arrayOf()

        companion object {
            private val TRANSACTION_KEY = "TRANSACTION_KEY"
        }

        fun bindTransaction(tx: TransactionHistoryEntry, tokens: Array<TokenListing>) {
            supportedTokens = tokens

            val assetTextView = view.find<TextView>(R.id.assetTextView)
            val otherPartyTextView = view.find<TextView>(R.id.otherPartyTextView)

            val amountTextView = view.find<TextView>(R.id.amountTextView)
            val txDateTextView = view.find<TextView>(R.id.transactionDateTextView)
            val sdf =   SimpleDateFormat("MMM dd yyyy @ HH:mm", Locale.getDefault())
            val date = java.util.Date(tx.time * 1000)
            txDateTextView.text = sdf.format(date)


            val logoImageView = view.find<ImageView>(R.id.txHistoryLogo)
            Glide.with(view.context).load(tx.asset.logoURL).into(logoImageView)

            val txTypeTextView = view.find<TextView>(R.id.transactionTypeTextView)

            assetTextView.text = tx.asset.symbol.toUpperCase()

            var toNickname = PersistentStore.getContacts().find { it.address == tx.to }?.nickname
            if (toNickname == null) {
                toNickname = PersistentStore.getWatchAddresses().find {it.address == tx.to}?.nickname
            }

            var fromNickname = PersistentStore.getContacts().find { it.address == tx.from }?.nickname
            if (fromNickname == null) {
                fromNickname = PersistentStore.getWatchAddresses().find {it.address == tx.from }?.nickname
            }

            if (tx.to == Account.getWallet()?.address!!) {
                txTypeTextView.text = view.context.resources.getString(R.string.WALLET_Received)
                if (fromNickname  != null) {
                    otherPartyTextView.text = String.format(view.context.resources.getString(R.string.WALLET_from_formatted), fromNickname)
                } else {
                    otherPartyTextView.text = String.format(view.context.resources.getString(R.string.WALLET_from_formatted), tx.from)
                }
                amountTextView.text =  "+" + tx.amount
                amountTextView.textColor = O3Wallet.appContext!!.resources!!.getColor(R.color.colorGain)
            } else {
                txTypeTextView.text = view.context.resources.getString(R.string.WALLET_Sent)
                if (toNickname  != null) {
                    otherPartyTextView.text = String.format(view.context.resources.getString(R.string.WALLET_to_formatted), toNickname)
                } else {
                    otherPartyTextView.text = String.format(view.context.resources.getString(R.string.WALLET_to_formatted), tx.to)
                }
                amountTextView.text =  "-" + tx.amount
                amountTextView.textColor = O3Wallet.appContext!!.resources!!.getColor(R.color.colorLoss)
            }


            view.setOnClickListener {
                var url = "https://neoscan.io/transaction/" + tx.txid
                if (tx.asset.tokenHash.contains("000000000")) {
                    url = "https://explorer.ont.io/transaction/" + tx.txid
                }
                val i = Intent(view.context, DAppBrowserActivity::class.java)
                i.putExtra("url", url)
                view.context.startActivity(i)
            }
        }
    }

    class LoadingViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view = v

        companion object {
            private val LOADING_KEY = "LOADING_KEY"
        }
    }

    class SectionHeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view = v

        fun bindHeader(type: Int) {
            if (type == 0) {
                view.find<TextView>(R.id.headerTextView).text = view.context.resources.getString(R.string.WALLET_Pending)
            } else {
                view.find<TextView>(R.id.headerTextView).text = view.context.resources.getString(R.string.WALLET_Confirmed)
            }
        }
    }
}

abstract class PaginationScrollListener(internal var layoutManager: LinearLayoutManager) :
        RecyclerView.OnScrollListener() {

    var totalPageCount: Int = 0
    var isLastPage: Boolean = false
    var isLoading: Boolean = false
    protected abstract fun loadMoreItems()

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        if (!isLoading && !isLastPage) {
            if (visibleItemCount + firstVisibleItemPosition >= totalItemCount
                    && firstVisibleItemPosition >= 0
                    && totalItemCount >= totalPageCount) {
                loadMoreItems()
            }
        }
    }
}

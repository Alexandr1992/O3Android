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
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TokenListing
import network.o3.o3wallet.API.O3Platform.TransactionHistoryEntry
import network.o3.o3wallet.Dapp.DAppBrowserActivity


/**
 * Created by drei on 4/24/18.
 */

class TransactionHistoryAdapter(private var transactionHistoryEntries: MutableList<TransactionHistoryEntry>,
                                context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val TRANSACTION_ENTRY_VIEW = 0
    val LOADING_FOOTER_VIEW = 1
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        if (viewType == TRANSACTION_ENTRY_VIEW) {
            val view = layoutInflater.inflate(R.layout.wallet_transaction_history_row_layout, parent, false)
            return TransactionHistoryAdapter.TransactionViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.wallet_transaction_history_footer, parent, false)
            return TransactionHistoryAdapter.LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val type = getItemViewType(position)
        if (type == LOADING_FOOTER_VIEW) {
            return
        } else {
            (holder as TransactionViewHolder).bindTransaction(transactionHistoryEntries[position], availableTokens)
        }
    }

    override fun getItemCount(): Int {
        var footerInserted = 0
        if (isLoadingAdded) {
            footerInserted = 1
        }
        return transactionHistoryEntries.count() + footerInserted
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == transactionHistoryEntries.count() && isLoadingAdded) {
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
            val toTextView = view.find<TextView>(R.id.toTextView)
            val fromTextView = view.find<TextView>(R.id.fromTextView)
            val amountTextView = view.find<TextView>(R.id.amountTextView)
            val blockTextView = view.find<TextView>(R.id.blockNumberTextView)

            assetTextView.text = tx.asset.symbol.toUpperCase()
            val amountToDisplay = tx.amount

            var toNickname = PersistentStore.getContacts().find { it.address == tx.to }?.nickname
            if (toNickname == null) {
                toNickname = PersistentStore.getWatchAddresses().find {it.address == tx.to}?.nickname
            }

            if (tx.to == Account.getWallet()?.address!!) {
                toTextView.text = view.context.resources.getString(R.string.WALLET_to_O3_wallet) //"To: O3 Wallet"
                amountTextView.text =  "+" + tx.amount
                amountTextView.textColor = O3Wallet.appContext!!.resources!!.getColor(R.color.colorGain)
            } else if (toNickname != null) {
                toTextView.text =   String.format(view.context.resources.getString(R.string.WALLET_to_formatted), toNickname)
                amountTextView.text =  "-" + tx.amount
                amountTextView.textColor = O3Wallet.appContext!!.resources!!.getColor(R.color.colorLoss)
            }else {
                toTextView.text = String.format(view.context.resources.getString(R.string.WALLET_to_formatted), tx.to)
                amountTextView.text =  "-" + tx.amount
                amountTextView.textColor = O3Wallet.appContext!!.resources!!.getColor(R.color.colorLoss)
            }


            var fromNickname = PersistentStore.getContacts().find { it.address == tx.from }?.nickname
            if (fromNickname == null) {
                fromNickname = PersistentStore.getWatchAddresses().find {it.address == tx.from }?.nickname
            }

            if (tx.from == Account.getWallet()?.address!!) {
                fromTextView.text = view.context.resources.getString(R.string.WALLET_from_O3_wallet)
            } else if (fromNickname != null) {
                fromTextView.text = String.format(view.context.resources.getString(R.string.WALLET_from_formatted), toNickname)
            } else {
                fromTextView.text = String.format(view.context.resources.getString(R.string.WALLET_from_formatted), tx.from)
            }

            blockTextView.text = String.format(view.context.resources.getString(R.string.WALLET_block_number), tx.blockHeight.toString())

            view.setOnClickListener {
                val url = "https://neoscan.io/transaction/" + tx.txid
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

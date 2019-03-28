package network.o3.o3wallet.Wallet

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import network.o3.o3wallet.NativeTrade.OrdersList.OrdersListFragment
import network.o3.o3wallet.R
import network.o3.o3wallet.Wallet.TransactionHistory.TransactionHistoryFragment

/**
 * Created by apisit on 12/18/17.
 */
class AccountFragmentPagerAdapter(fm: FragmentManager, context: Context) : FragmentPagerAdapter(fm){

    private val PAGE_COUNT = 3
    private var tabTitles = arrayOf(context.resources.getString(R.string.WALLET_assets),
            context.resources.getString(R.string.WALLET_transactions), context.resources.getString(R.string.NATIVE_TRADE_orders))
    private val context: Context = context

    override fun getCount(): Int {
        return PAGE_COUNT
    }

    override fun getItem(position: Int): Fragment {
        if (position == 0) {
            return AccountFragment.newInstance()
        } else if (position == 1) {
            return TransactionHistoryFragment.newInstance()
        } else {
            return OrdersListFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        // Generate title based on item position
        return tabTitles[position]
    }
}
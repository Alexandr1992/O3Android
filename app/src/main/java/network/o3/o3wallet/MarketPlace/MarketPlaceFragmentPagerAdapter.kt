package network.o3.o3wallet.MarketPlace

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import network.o3.o3wallet.MarketPlace.NEP5Tokens.TokensFragment
import network.o3.o3wallet.MarketPlace.TokenSales.TokenSalesFragment
import network.o3.o3wallet.Wallet.AccountFragment
import network.o3.o3wallet.Wallet.TransactionHistory.TransactionHistoryFragment

class MarketPlaceFragmentPagerAdapter(fm: FragmentManager, context: Context) : FragmentPagerAdapter(fm) {

    private val PAGE_COUNT = 2
    private val tabTitles = arrayOf("Tokens", "Token sales")
    private val context: Context = context


    override fun getCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Fragment {
        if (position == 0) {
            return TokensFragment.newInstance()
        } else {
            return TokenSalesFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        // Generate title based on item position
        return tabTitles[position]
    }
}


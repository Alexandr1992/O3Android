package network.o3.o3wallet.MarketPlace

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import network.o3.o3wallet.MarketPlace.Dapps.DappsFragment
import network.o3.o3wallet.MarketPlace.NEP5Tokens.TokensFragment

class MarketPlaceFragmentPagerAdapter(fm: FragmentManager, context: Context) : FragmentPagerAdapter(fm) {

    private val PAGE_COUNT = 2
    private val tabTitles = arrayOf("Apps", "Tokens", "Token Sales")
    private val context: Context = context


    override fun getCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Fragment {
        if (position == 0) {
            return DappsFragment.newInstance()
        } else {
            return TokensFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        // Generate title based on item position
        return tabTitles[position]
    }
}


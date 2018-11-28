package network.o3.o3wallet.Portfolio

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.PersistentStore

import network.o3.o3wallet.Portfolio.PortfolioHeader

/**
 * Created by drei on 11/26/17.
 */
class PortfolioHeaderPagerAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        return PortfolioHeader.newInstance(position)
    }

    override fun getCount(): Int {
        if (NEP6.getFromFileSystem().accounts.count() == 0) {
            return 2
        }

        return NEP6.getFromFileSystem().accounts.count() + 1
    }
}
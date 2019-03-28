package network.o3.o3wallet.Portfolio

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import network.o3.o3wallet.NEP6

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
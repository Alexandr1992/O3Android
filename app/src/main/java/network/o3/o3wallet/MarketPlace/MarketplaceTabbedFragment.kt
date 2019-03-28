package network.o3.o3wallet.MarketPlace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import network.o3.o3wallet.R

class MarketplaceTabbedFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.marketplace_tabbed_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager>(R.id.marketplacePager)
        val tabLayout = view.findViewById<TabLayout>(R.id.marketplaceTabLayout)

        viewPager.adapter = MarketPlaceFragmentPagerAdapter(childFragmentManager, context!!)
        //android keeps one fragment on either side so if you select the third tab and select first tab again.
        //fragment will be created causing to load the data again.
        //use this offScreenPageLimit to tell it to keep 2 fragments instead
        viewPager.offscreenPageLimit = 2
        tabLayout.setupWithViewPager(viewPager)
    }

    companion object {
        fun newInstance(): MarketplaceTabbedFragment {
            return MarketplaceTabbedFragment()
        }
    }


}

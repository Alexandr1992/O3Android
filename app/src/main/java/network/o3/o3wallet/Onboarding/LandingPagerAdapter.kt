package network.o3.o3wallet.Onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * Created by drei on 11/22/17.
 */

class LandingPagerAdapter(fragmentManager: FragmentManager): FragmentStatePagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        return LandingFeatureScroll.newInstance(position)
    }

    override fun getCount(): Int {
        return 5
    }
}
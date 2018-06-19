package network.o3.o3wallet.Onboarding

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

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
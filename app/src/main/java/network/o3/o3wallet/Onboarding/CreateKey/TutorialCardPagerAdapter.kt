package network.o3.o3wallet.Onboarding.CreateKey

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class TutorialCardPagerAdapter(fragmentManager: FragmentManager): FragmentStatePagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        if (position == 0) {
            return PrivateKeyCardFragment.newInstance()
        }

        return TutorialCard.newInstance(position)
    }

    override fun getCount(): Int {
        return 6
    }
}
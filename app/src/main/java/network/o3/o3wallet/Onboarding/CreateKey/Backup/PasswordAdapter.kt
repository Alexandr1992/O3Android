package network.o3.o3wallet.Onboarding.CreateKey.Backup

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter


class PasswordAdapter(fragmentManager: FragmentManager): FragmentStatePagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        return Nep2BackupCardFragment.newInstance(position)
    }

    override fun getCount(): Int {
        return 2
    }
}
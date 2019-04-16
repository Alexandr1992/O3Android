package network.o3.o3wallet.Wallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import network.o3.o3wallet.R

class TabbedAccount : Fragment() {

    lateinit var viewPager: ViewPager
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.wallet_fragment_tabbed_account, container, false)
    }

    val needReloadAddressReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewPager.adapter = AccountFragmentPagerAdapter(childFragmentManager, context!!)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById<ViewPager>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = AccountFragmentPagerAdapter(childFragmentManager, context!!)
        //android keeps one fragment on either side so if you select the third tab and select first tab again.
        //fragment will be created causing to load the data again.
        //use this offScreenPageLimit to tell it to keep 2 fragments instead
        viewPager.offscreenPageLimit = 2
        tabLayout.setupWithViewPager(viewPager)
        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadAddressReciever,
                IntentFilter("need-update-watch-address-event"))
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this.context!!)
                .unregisterReceiver(needReloadAddressReciever)
        super.onDestroy()
    }

    companion object {
        fun newInstance(): TabbedAccount {
            return TabbedAccount()
        }
    }
}

package network.o3.o3wallet.Wallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.design.widget.TabLayout
import network.o3.o3wallet.R
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import kotlinx.android.synthetic.main.wallet_fragment_tabbed_account.*

class TabbedAccount : Fragment() {

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

        val viewPager = view.findViewById<ViewPager>(R.id.viewPager)
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

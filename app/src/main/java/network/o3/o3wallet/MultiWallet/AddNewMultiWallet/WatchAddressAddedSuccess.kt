package network.o3.o3wallet.MultiWallet.AddNewMultiWallet


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import network.o3.o3wallet.R

class WatchAddressAddedSuccess : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.multiwallet_watch_address_added, container, false)
    }


}

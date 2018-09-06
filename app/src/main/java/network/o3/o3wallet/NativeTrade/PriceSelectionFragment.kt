package network.o3.o3wallet.NativeTrade

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import network.o3.o3wallet.R

// TODO: Rename parameter arguments, choose names that match

class PriceSelectionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.native_trade_price_selection_fragment, container, false)
    }
}

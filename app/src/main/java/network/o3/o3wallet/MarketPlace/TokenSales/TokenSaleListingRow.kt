package network.o3.o3wallet.MarketPlace.TokenSales

import android.os.Bundle
import android.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.R

class TokenSaleListingRow : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle): View? {

        return inflater.inflate(R.layout.tokensales_listing_row, container, false)
    }

    fun newInstance(): TokenSaleListingRow {
        val fragment = TokenSaleListingRow()
        return fragment
    }
}

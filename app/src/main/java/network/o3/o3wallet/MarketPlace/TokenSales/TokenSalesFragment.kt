package network.o3.o3wallet.MarketPlace.TokenSales

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.API.O3.TokenSale
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class TokenSalesFragment : Fragment() {

    var model: TokenSalesViewModel? = null
    var tokenSales = ArrayList<TokenSale>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.tokensales_list_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listingsView = view.find<RecyclerView>(R.id.tokenSalesListingRecyclerView)
        listingsView.adapter = TokenSalesAdapter(tokenSales, "https://cdn.o3.network/newsletters/tokensales/index.html")
        model = TokenSalesViewModel()
        model?.getTokenSales(true)?.observe(this, Observer { tokenSales ->
            (listingsView.adapter as TokenSalesAdapter).setData(tokenSales?.live?.toCollection(ArrayList())!!,
                    tokenSales!!.subscribeURL)
        })
    }

    companion object {
        fun newInstance(): TokenSalesFragment {
            return TokenSalesFragment()
        }
    }
}

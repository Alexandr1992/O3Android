package network.o3.o3wallet.MarketPlace.TokenSales

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import network.o3.o3wallet.API.O3.Feature
import network.o3.o3wallet.API.O3.FeedData
import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3.TokenSales
import network.o3.o3wallet.Account

/**
 * Created by drei on 4/17/18.
 */

class TokenSalesViewModel: ViewModel() {
    var tokenSales: MutableLiveData<TokenSales>? = null

    fun getTokenSales(refresh: Boolean): LiveData<TokenSales> {
        if (tokenSales == null || refresh) {
            tokenSales = MutableLiveData()
            loadTokenSalesData()
        }
        return tokenSales!!
    }

    fun loadTokenSalesData() {
        O3API().getTokenSales(Account.getWallet()!!.address) {
            if (it.second != null) return@getTokenSales
            tokenSales?.postValue(it.first!!)
        }
    }
}
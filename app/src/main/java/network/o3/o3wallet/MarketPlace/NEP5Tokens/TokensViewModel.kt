package network.o3.o3wallet.MarketPlace.NEP5Tokens

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.gson.JsonObject
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TokenListing
import network.o3.o3wallet.API.O3Platform.TokenListings
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI

class TokensViewModel: ViewModel() {

    var listingData: MutableLiveData<Array<TokenListing>>? = null
    var switcheoTokenData: MutableLiveData<JsonObject>? = null

    fun getListingData(refresh: Boolean): LiveData<Array<TokenListing>> {
        if (listingData == null || refresh) {
            listingData = MutableLiveData()
            loadListingData()
        }
        return listingData!!
    }

    fun loadListingData() {
        O3PlatformClient().getMarketPlace {
            if (it.second != null) return@getMarketPlace
            listingData?.postValue(it.first?.assets!! + it.first?.nep5!!)
        }
    }

    fun getSwitcheoTokenData(refresh: Boolean): LiveData<JsonObject> {
        if (switcheoTokenData == null || refresh) {
            switcheoTokenData = MutableLiveData()
            loadSwitcheoTokenData()
        }
        return switcheoTokenData!!
    }

    fun loadSwitcheoTokenData() {
        SwitcheoAPI().getTokens {
            switcheoTokenData?.postValue(it.first)
        }
    }

    fun filteredTokens(query: String): List<TokenListing> {
        val tokens = getListingData(false).value ?: arrayOf()
        val filteredTokens = tokens.filter { it.symbol.startsWith(query, true)
                || it.name.startsWith(query, true) }
        return filteredTokens
    }
}
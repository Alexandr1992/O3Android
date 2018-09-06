package network.o3.o3wallet.NativeTrade

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.widget.Switch
import com.github.salomonbrys.kotson.jsonObject
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TokenListing
import network.o3.o3wallet.API.O3Platform.TradingAccount
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.Switcheo.ContractBalance
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.Account
import network.o3.o3wallet.PersistentStore
import java.util.concurrent.CountDownLatch

class NativeTradeViewModel: ViewModel() {
    val availableBaseAssets = arrayOf(Pair<String, Double?>("NEO", null), Pair<String, Double?>("GAS", null))

   // val isBuyOrder: Boolean = true
   // val selectedTradeAsset: TransferableAsset
   // val selectedTradeAssetAmount: Long

   // val selectedPriceCrypto: Long
   // val selectedPriceFiat: Long
    //val marketPrice: Double = 0.1

    var selectedBaseAsset: MutableLiveData<String>? = null
    var selectedBaseAssetAmount: MutableLiveData<Double> = MutableLiveData()

    var orderAssetAmount: MutableLiveData<Double> = MutableLiveData()

    var orders: MutableLiveData<List<SwitcheoOrders>>? = null

    var marketPrice: MutableLiveData<Pair<Double, Double>>? = null

    var orderAsset = "QLC"

   // val baseAssetTradeBalance: Long
   // val baseAssetAmount: Long
   // val baseAssetAmountFiat: Long

    var tradingAccount: MutableLiveData<TradingAccount>? = null

    var editingBaseAmount: MutableLiveData<Boolean> = MutableLiveData()

    var baseAssetBalance: MutableLiveData<Double?> = MutableLiveData()
    var baseAssetImageUrl: MutableLiveData<String> = MutableLiveData()

    fun getSelectedBaseAssetValue(): String? {
        return selectedBaseAsset!!.value
    }

    fun getSelectedBaseAssetObserver(): LiveData<String> {
        if (selectedBaseAsset == null) {
            selectedBaseAsset = MutableLiveData()
        }
        return selectedBaseAsset!!
    }

    fun setSelectedBaseAssetValue(value: String) {
        selectedBaseAsset!!.value = value
        selectedBaseAsset!!.postValue(value)
    }

    fun getOrders(): LiveData<List<SwitcheoOrders>> {
        if (orders == null) {
            orders = MutableLiveData()
            loadOrders()
        }
        return orders!!
    }

    fun loadOrders() {
        SwitcheoAPI().getPendingOrders {
            orders?.postValue(it.first)
        }
    }

    fun setIsEditingBaseAmount(editingBaseAmount: Boolean) {
        this.editingBaseAmount.value = editingBaseAmount
        this.editingBaseAmount.postValue(editingBaseAmount)
    }

    fun isEditingBaseAmount(): LiveData<Boolean> {
        return editingBaseAmount
    }

    fun getSelectedBaseAssetAmount(): LiveData<Double> {
        return selectedBaseAssetAmount
    }

    fun setSelectedBaseAssetAmount(amount: Double) {
        selectedBaseAssetAmount.value = amount
        selectedBaseAssetAmount.postValue(amount)
    }

    fun getOrderAssetAmount(): LiveData<Double> {
        return orderAssetAmount
    }

    fun setOrderAssetAmount(amount: Double) {
        orderAssetAmount.value = amount
        orderAssetAmount.postValue(amount)
    }

    fun getMarketPrice(): LiveData<Pair<Double, Double>> {
        if (marketPrice == null) {
            marketPrice = MutableLiveData()
            loadMarketPrice()
        }
        return marketPrice!!
    }

    fun loadMarketPrice() {
        val latch = CountDownLatch(2)
        var fiatPrice: Double = 0.0
        O3PlatformClient().getRealTimePrice(orderAsset, PersistentStore.getCurrency()) {
            if (it.second != null) {
                //TODO: SERIOUS ERROR OCCURRED HERE
            }
            if (it.first?.price == null) {
                //TODO: SERIOUS ERROR OCCURRED HERE
            }

            fiatPrice = it.first!!.price
            latch.countDown()
        }

        var cryptoPrice: Double = 0.0
        O3PlatformClient().getRealTimePrice("QLC", selectedBaseAsset!!.value!!) {
            if (it.second != null) {
                //TODO: SERIOUS ERROR OCCURRED HERE
            }
            if (it.first?.price == null) {
                //TODO: SERIOUS ERROR OCCURRED HERE
            }

            cryptoPrice = it.first!!.price
            latch.countDown()
        }
        latch.await()
        marketPrice?.value = Pair(fiatPrice, cryptoPrice)
        marketPrice?.postValue(Pair(fiatPrice, cryptoPrice))
    }

    fun getTradingAccountBalances(): LiveData<TradingAccount> {
        if (tradingAccount == null) {
            tradingAccount = MutableLiveData()
            loadTradingAccountBalances()
        }
        return tradingAccount!!
    }

    fun loadTradingAccountBalances() {
        O3PlatformClient().getTradingAccounts {
            if (it.first != null) {
                //TODO: SERIOUS ERROR OCCURRED HERE
            }
            //contractBalance?.value = it.first!!
            tradingAccount?.postValue(it.first!!)
            val neoAsset = it.first!!.switcheo.confirmed.find { it.symbol.toLowerCase() == "neo" }
            if(neoAsset != null) {
                setSelectedBaseAssetBalance(neoAsset.value.toDouble())
            } else {
                setSelectedBaseAssetBalance(0.0)
            }
        }
    }

    fun setSelectedBaseAssetBalance(balance: Double?) {
        baseAssetBalance.postValue(balance)
    }

    fun getSelectedBaseAssetBalance(): LiveData<Double?> {
        return baseAssetBalance
    }

    fun setSelectedBaseAssetImageUrl(url: String) {
        baseAssetImageUrl.postValue(url)
    }

    fun getSelectedBaseAssetImageUrl(): LiveData<String> {
        return baseAssetImageUrl
    }
}
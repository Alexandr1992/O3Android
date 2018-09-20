package network.o3.o3wallet.NativeTrade

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.common.graph.MutableValueGraph
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TradingAccount
import network.o3.o3wallet.API.Switcheo.Offer
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.PersistentStore
import java.util.concurrent.CountDownLatch

class NativeTradeViewModel: ViewModel() {
    val availableBaseAssets = arrayOf(Pair<String, Double?>("NEO", null), Pair<String, Double?>("GAS", null))

    var isBuyOrder: Boolean = true
   // val selectedTradeAsset: TransferableAsset
   // val selectedTradeAssetAmount: Long

   // val selectedPriceCrypto: Long
   // val selectedPriceFiat: Long
    //val selectedPrice: Double = 0.1

    var selectedBaseAsset: MutableLiveData<String> = MutableLiveData()
    var selectedBaseAssetAmount: MutableLiveData<Double> = MutableLiveData()

    var orderAssetAmount: MutableLiveData<Double> = MutableLiveData()

    var orders: MutableLiveData<List<SwitcheoOrders>>? = null

    var selectedPrice: MutableLiveData<Pair<Double, Double>>? = null
    var marketPrice: Pair<Double, Double>? = null
    var orderAsset = "QLC"

    var orderBookTopPrice: MutableLiveData<Double>? = null

    var isOrdering: MutableLiveData<Boolean> = MutableLiveData()

   // val baseAssetTradeBalance: Long
   // val baseAssetAmount: Long
   // val baseAssetAmountFiat: Long

    var tradingAccount: MutableLiveData<TradingAccount>? = null

    var editingBaseAmount: MutableLiveData<Boolean> = MutableLiveData()

    var baseAssetBalance: MutableLiveData<Double?> = MutableLiveData()
    var baseAssetImageUrl: MutableLiveData<String> = MutableLiveData()

    var marketRateDifference: MutableLiveData<Double> = MutableLiveData()

    var estimatedFillAmount: MutableLiveData<Double> = MutableLiveData()

    var error: MutableLiveData<Error> = MutableLiveData()

    init {
        estimatedFillAmount.value = 0.0
        selectedBaseAsset.value = "NEO"
        baseAssetImageUrl.value = "https://cdn.o3.network/img/neo/NEO.png"
        marketRateDifference.value = 1.0
    }

    var orderBook = listOf<Offer>()

    fun getSelectedBaseAssetValue(): String? {
        return selectedBaseAsset!!.value
    }

    fun getSelectedBaseAssetObserver(): LiveData<String> {
        return selectedBaseAsset!!
    }

    fun setSelectedBaseAssetValue(value: String) {
        selectedBaseAsset!!.value = value
        loadMarketPrice()
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
            if (it.second != null) {
                error.postValue(it.second)
                return@getPendingOrders
            }
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
        orderAssetAmount.value =  amount / selectedPrice?.value?.second!!
    }

    fun getOrderAssetAmount(): LiveData<Double> {
        return orderAssetAmount
    }

    fun setOrderAssetAmount(amount: Double) {
        orderAssetAmount.value = amount
        selectedBaseAssetAmount.value =  selectedPrice?.value?.second!! * amount
    }

    fun getSelectedPrice(): LiveData<Pair<Double, Double>> {
        if (selectedPrice == null) {
            selectedPrice = MutableLiveData()
            loadMarketPrice()
        }
        return selectedPrice!!
    }

    fun setManualPrice(newPrice: Double) {
        var previousCryptoPrice = selectedPrice?.value?.second!!

        var percentMultiple = (newPrice / marketPrice!!.second)
        var newFiatPrice = marketPrice!!.first * percentMultiple

        selectedPrice?.postValue(Pair(newFiatPrice, newPrice))
        selectedPrice?.value = Pair(newFiatPrice, newPrice)
        updateMarketRateDifference(newPrice)
        selectedBaseAssetAmount.value = selectedPrice?.value?.second!! * orderAssetAmount.value!!
        calculateFillAmount()

    }

    fun loadMarketPrice() {
        val latch = CountDownLatch(2)
        var fiatPrice: Double = 0.0
        O3PlatformClient().getRealTimePrice(orderAsset, PersistentStore.getCurrency()) {
            if (it.second != null) {
                error.postValue(it.second)
                return@getRealTimePrice
            }
            if (it.first?.price == null) {
                error.postValue(Error("Unknown error occured"))
                return@getRealTimePrice
            }

            fiatPrice = it.first!!.price
            latch.countDown()
        }

        var cryptoPrice: Double = 0.0
        O3PlatformClient().getRealTimePrice(orderAsset, selectedBaseAsset!!.value!!) {
            if (it.second != null) {
                error.postValue(it.second)
                return@getRealTimePrice
            }
            if (it.first?.price == null) {
                error.postValue(Error("Unknown error occured"))
                return@getRealTimePrice
            }

            cryptoPrice = it.first!!.price
            latch.countDown()
        }
        latch.await()
        marketPrice = Pair(fiatPrice, cryptoPrice)
        selectedPrice?.value = Pair(fiatPrice, cryptoPrice)
        selectedPrice?.postValue(Pair(fiatPrice, cryptoPrice))
        updateMarketRateDifference(cryptoPrice)
        if ((selectedBaseAssetAmount.value ?: 0.0) != 0.0) {
            orderAssetAmount.postValue(orderAssetAmount.value)
        }
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
            if (it.second != null) {
                error.postValue(it.second)
                return@getTradingAccounts
            }
            //contractBalance?.value = it.first!!
            tradingAccount?.postValue(it.first!!)
            val neoAsset = it.first!!.switcheo.confirmed.find { it.symbol.toLowerCase() == selectedBaseAsset.value!!.toLowerCase() }
            if(neoAsset != null) {
                setSelectedBaseAssetBalance(neoAsset.value.toDouble() / 100000000.0)
            } else {
                setSelectedBaseAssetBalance(0.0)
            }
        }
    }

    fun getOrderBookTopPrice(): LiveData<Double> {
        if (orderBookTopPrice == null) {
            orderBookTopPrice = MutableLiveData()
        }
        return orderBookTopPrice!!
    }

    fun loadTopOrderBookPrice() {
        val pair = orderAsset + "_" + selectedBaseAsset!!.value
        SwitcheoAPI().getOffersForPair(pair) {
            if (it.second != null) {
                error.postValue(it.second)
                return@getOffersForPair
            }

            if (it.first != null) {
                if (isBuyOrder) {
                    val filtered = it.first!!.filter { it.offer_asset.toLowerCase() == orderAsset.toLowerCase() }
                    if (filtered.isNotEmpty()) {
                        val sorted = filtered.sortedBy { it.want_amount.toDouble() / it.offer_amount.toDouble() }
                        orderBookTopPrice!!.postValue(sorted[0].want_amount.toDouble() / sorted[0].offer_amount.toDouble())
                        orderBook = sorted
                        calculateFillAmount()
                    }
                } else {
                    val filtered = it.first!!.filter { it.offer_asset.toLowerCase() == selectedBaseAsset!!.value!!.toLowerCase() }
                    if (filtered.isNotEmpty()) {
                        val sorted = filtered.sortedBy { it.want_amount.toDouble() / it.offer_amount.toDouble() }
                        orderBookTopPrice!!.postValue(sorted[0].offer_amount.toDouble() / sorted[0].want_amount.toDouble())
                        orderBook = sorted
                        calculateFillAmount()
                    }
                }
            }
        }
    }

    fun getFillAmount(): LiveData<Double> {
        return estimatedFillAmount
    }

    fun calculateFillAmount() {
        if (orderAssetAmount.value == null) {
            estimatedFillAmount.value = 0.0
            estimatedFillAmount.postValue(0.0)
            return
        }

        //Buy Side
        if (isBuyOrder) {
            var offersUnderPrice = mutableListOf<Offer>()
            for (offer in orderBook) {
                if (offer.want_amount.toDouble() / offer.offer_amount.toDouble() <= selectedPrice!!.value!!.second) {
                    offersUnderPrice.add(offer)
                } else {
                    break
                }
            }
            var fillSum = offersUnderPrice.sumByDouble { it.available_amount.toDouble() } / 100000000

            if (fillSum >= orderAssetAmount.value!!) {
                estimatedFillAmount.value = 1.0
                estimatedFillAmount.postValue(1.0)
            } else {
                estimatedFillAmount.value = fillSum / orderAssetAmount.value!!
                estimatedFillAmount.postValue(fillSum / orderAssetAmount.value!!)
            }
        } else {
            var offersOverPrice = mutableListOf<Offer>()
            for(offer in orderBook) {
                if (offer.offer_amount.toDouble() / offer.want_amount.toDouble() >= selectedPrice!!.value!!.second) {
                    offersOverPrice.add(offer)
                } else {
                    break
                }
            }
            var fillSum = offersOverPrice.sumByDouble { it.available_amount.toDouble() } / 100000000

            if (fillSum >= selectedBaseAssetAmount.value!!) {
                estimatedFillAmount.value = 1.0
                estimatedFillAmount.postValue(1.0)
            } else {
                estimatedFillAmount.value = fillSum / selectedBaseAssetAmount.value!!
                estimatedFillAmount.postValue(fillSum / selectedBaseAssetAmount.value!!)
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

    fun getMarketRatePercentDifference(): LiveData<Double> {
        return marketRateDifference!!

    }

    fun updateMarketRateDifference(newPrice: Double) {
        val newDifference = newPrice / marketPrice!!.second
        marketRateDifference.value = newDifference
        marketRateDifference.postValue(newDifference)
    }

    fun getIsOrdering(): LiveData<Boolean> {
        return isOrdering
    }

    fun getError(): LiveData<Error> {
        return error
    }

    fun setIsOrdering(isOrdering: Boolean) {
        this.isOrdering.postValue(isOrdering)
    }
}
package network.o3.o3wallet.Portfolio

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.API.O3.PriceData
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.TransferableAssets
import org.jetbrains.anko.coroutines.experimental.bg
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

class HomeViewModelV2: ViewModel() {
    enum class DisplayType(val position: Int) {
        HOT(0), COMBINED(1), COLD(2)
    }

    var assetsReadOnlyIntermediate: ArrayList<TransferableAsset> = arrayListOf()
    var assetsReadOnly: ArrayList<TransferableAsset>? = null
    var assetsWritable: ArrayList<TransferableAsset>? = null
    var displayedAssets: MutableLiveData<ArrayList<TransferableAsset>>? = null

    var portfolio: MutableLiveData<Portfolio>? = null
    private var initialPrice: PriceData? = null
    private var latestPrice: PriceData? = null

    private var currency: CurrencyType = CurrencyType.FIAT
    private var interval: String = O3Wallet.appContext!!.resources.getString(R.string.PORTFOLIO_one_day)
    private var displayType: DisplayType = DisplayType.HOT

    lateinit var delegate: HomeViewModelProtocol

    var latestPrices = floatArrayOf()

    fun setCurrency(currency: CurrencyType) {
        this.currency = currency
    }

    fun getCurrency(): CurrencyType {
        return currency
    }

    fun setInterval(interval: String) {
        this.interval = interval
    }

    fun getInterval(): String {
        return this.interval
    }

    fun setDisplayType(displayType: DisplayType) {
        this.displayType = displayType
        getDisplayedAssets(false)
    }

    fun getDisplayType(): DisplayType {
        return this.displayType
    }

    fun getAssetsReadOnly(refresh: Boolean) {
        if (assetsReadOnly == null || refresh) {
            loadAssetsReadOnly()
        } else {
            displayedAssets?.postValue(assetsReadOnly)
        }
    }

    fun getAssetsWritable(refresh: Boolean) {
        if (assetsWritable == null || refresh) {
            loadAssetsWritable()
        } else {
            displayedAssets?.postValue(assetsWritable)
        }
    }

    fun addReadOnlyAsset(asset: TransferableAsset) {
        val index = assetsReadOnlyIntermediate.indices.find {
            assetsReadOnlyIntermediate[it].name == asset.name
        } ?: -1
        if (index == -1) {
            assetsReadOnlyIntermediate.add(asset)
        } else {
            assetsReadOnlyIntermediate[index].value += asset.value
        }
    }

    fun addReadOnlyBalances(assets: TransferableAssets) {
        assetsReadOnlyIntermediate.clear()
        for (asset in assets.assets) {
            addReadOnlyAsset(asset)
        }
        for (token in assets.tokens) {
            addReadOnlyAsset(token)
        }
    }

    fun combineReadOnlyAndWritable(){
        var assets = arrayListOf<TransferableAsset>()
        for (asset in assetsWritable ?: arrayListOf()) {
            assets.add(asset.deepCopy())
        }

        //var assets = assetsWritable
        for (asset in assetsReadOnly ?: arrayListOf()) {
            val index = assets.indices.find { assets[it].name == asset.name } ?: -1
            if (index == -1) {
                assets.add(asset)
            } else {
                assets[index] = assets[index]
                assets[index].value += asset.value
            }
        }
        displayedAssets?.postValue(assets)
    }

    fun loadAssetsReadOnly() {
        bg {
            val cachedAssets = PersistentStore.getLatestWatchAddressBalances()
            if (cachedAssets != null) {
                assetsReadOnly = cachedAssets
                displayedAssets!!.postValue(cachedAssets)
            }
            val addresses = PersistentStore.getWatchAddresses()
            val latch = CountDownLatch(addresses.count())
            for (address in addresses) {
                O3PlatformClient().getTransferableAssets(address.address) {
                    if (it.second != null || it.first == null) {
                        latch.countDown()
                        return@getTransferableAssets
                    }
                    addReadOnlyBalances(it.first!!)
                    latch.countDown()
                }
            }
            latch.await()
            PersistentStore.setLatestWatchAddressBalances(assetsReadOnlyIntermediate)
            assetsReadOnly = assetsReadOnlyIntermediate
            if (displayType == DisplayType.COMBINED) {
                combineReadOnlyAndWritable()
            } else if (displayType == DisplayType.COLD) {
                displayedAssets?.postValue(assetsReadOnlyIntermediate)
            }
            delegate.hideAssetLoadingIndicator()
        }
    }

    fun loadAssetsWritable() {
        val cachedAssets = PersistentStore.getLatestBalances()
        if (cachedAssets != null) {
            assetsWritable = (cachedAssets.assets + cachedAssets.tokens).toCollection(ArrayList())
            displayedAssets!!.postValue((cachedAssets.assets + cachedAssets.tokens).toCollection(ArrayList()))
        }
        bg {
            O3PlatformClient().getTransferableAssets(Account.getWallet()?.address!!) {
                PersistentStore.setLatestBalances(it.first)
                val tokens = it.first?.tokens ?: arrayListOf()
                val assets = it.first?.assets ?: arrayListOf()
                assetsWritable = (assets + tokens).toCollection(ArrayList())
                displayedAssets?.postValue((assets + tokens).toCollection(ArrayList()))
                delegate.hideAssetLoadingIndicator()
            }
        }
    }

    fun reloadDisplayedAssets() {
        when (displayType) {
            DisplayType.HOT -> getAssetsWritable(true)
            DisplayType.COLD -> getAssetsReadOnly(true)
            DisplayType.COMBINED -> combineReadOnlyAndWritable()
        }
    }

    fun getDisplayedAssets(refresh: Boolean): LiveData<ArrayList<TransferableAsset>> {
        if (displayedAssets == null) {
            displayedAssets = MutableLiveData()
        }
        when (displayType) {
            DisplayType.HOT -> getAssetsWritable(refresh)
            DisplayType.COLD -> getAssetsReadOnly(refresh)
            DisplayType.COMBINED -> combineReadOnlyAndWritable()
        }
        return displayedAssets!!
    }

    fun getPriceFloats(): FloatArray {
        return latestPrices
    }

    fun getInitialPrice(): PriceData {
        return initialPrice!!
    }

    fun getLatestPrice(): PriceData {
        return latestPrice!!
    }

    private fun updatePriceFloats(portfolio: Portfolio) {
        val data: Array<Double>? = when (currency) {
            CurrencyType.FIAT -> portfolio.data.map { it.averageUSD }.toTypedArray()
            CurrencyType.BTC -> portfolio.data.map { it.averageBTC }.toTypedArray()
        }
        if (data == null) {
            latestPrices = FloatArray(0)
        }

        var floats = FloatArray(data!!.count())
        for (i in data.indices) {
            floats[i] = data[i].toFloat()
        }
        latestPrices = floats.reversedArray()
    }

    fun getCurrentPortfolioValue(): Double {
        return when(currency) {
            CurrencyType.BTC -> latestPrice?.averageBTC ?: 0.0
            CurrencyType.FIAT -> latestPrice?.averageUSD ?: 0.0
        }
    }

    fun getInitialPortfolioValue(): Double  {
        return when(currency) {
            CurrencyType.BTC -> initialPrice?.averageBTC ?: 0.0
            CurrencyType.FIAT -> initialPrice?.averageUSD ?: 0.0
        }
    }

    fun getInitialDate(): Date {
        val df1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        return try {
            df1.parse(initialPrice?.time ?: "")
        } catch (e: ParseException) {
            return Date()
        }
    }

    fun getPercentChange(): Double {
        if (getInitialPortfolioValue() == 0.0) return 0.0
        return ((getCurrentPortfolioValue() - getInitialPortfolioValue()) / getInitialPortfolioValue() * 100)
    }

    fun getPortfolio(): LiveData<Portfolio> {
        if (portfolio == null) {
            portfolio = MutableLiveData()
        }
        return portfolio!!
    }

    fun loadPortfolioValue(assets: ArrayList<TransferableAsset>) {
        delegate.showPortfolioLoadingIndicator()
        bg {
            O3API().getPortfolio(assets, this.interval) {
                if (it.second != null) {
                    return@getPortfolio
                }
                updatePriceFloats(it.first!!)
                initialPrice = it.first!!.data.last()
                latestPrice = it.first!!.data.first()
                portfolio?.postValue(it.first!!)
            }
        }
    }
}
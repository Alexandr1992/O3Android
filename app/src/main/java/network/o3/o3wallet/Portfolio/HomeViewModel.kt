package network.o3.o3wallet.Portfolio

import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3.PriceData
import network.o3.o3wallet.API.NEO.*
import android.util.Log
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.TransferableAssets
import network.o3.o3wallet.API.O3Platform.TransferableBalance
import org.jetbrains.anko.coroutines.experimental.bg
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList


/**
 * Created by drei on 12/6/17.
 */

interface HomeViewModelProtocol {
    fun updateBalanceData(assets: ArrayList<TransferableAsset>)
    fun updatePortfolioData(portfolio: Portfolio)
    fun showLoadingIndicator()
    fun hideLoadingIndicator()
}

class HomeViewModel {
    enum class DisplayType(val position: Int) {
        HOT(0), COMBINED(1), COLD(2)
    }

    private var displayType: DisplayType = DisplayType.HOT
    private var interval: String = O3Wallet.appContext!!.resources.getString(R.string.PORTFOLIO_one_day)
    private var currency = CurrencyType.USD
    private lateinit var portfolio: Portfolio
    private var balanceCountDownLatch: CountDownLatch? = null

    lateinit var delegate: HomeViewModelProtocol


    var assetsReadOnly = arrayListOf<TransferableAsset>()
    var assetsWritable = arrayListOf<TransferableAsset>()
    var watchAddresses = PersistentStore.getWatchAddresses()
    var isLoadingData = false
    private var latestPrice: PriceData? = null
    private var initialPrice: PriceData? = null

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

    fun getInitialPortfolioValue(): Double  {
        return when(currency) {
            CurrencyType.BTC -> initialPrice?.averageBTC ?: 0.0
            CurrencyType.USD -> initialPrice?.averageUSD ?: 0.0
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

    fun getCurrentPortfolioValue(): Double {
        return when(currency) {
            CurrencyType.BTC -> latestPrice?.averageBTC ?: 0.0
            CurrencyType.USD -> latestPrice?.averageUSD ?: 0.0
        }
    }

    fun getPercentChange(): Double {
        if (getInitialPortfolioValue() == 0.0) return 0.0
        return ((getCurrentPortfolioValue() - getInitialPortfolioValue()) / getInitialPortfolioValue() * 100)
    }


    fun setDisplayType(displayType: DisplayType) {
        this.displayType = displayType
    }

    fun getDisplayType(): DisplayType {
        return this.displayType
    }

    fun addReadOnlyAsset(asset: TransferableAsset) {
        val index = assetsReadOnly.indices.find { assetsReadOnly[it].name == asset.name } ?: -1
        if (index == -1) {
            assetsReadOnly.add(asset)
        } else {
            assetsReadOnly[index].value += asset.value
        }
    }

    fun addReadOnlyBalances(assets: TransferableAssets) {
        for (asset in assets.assets) {
            addReadOnlyAsset(asset)
        }
        for (token in assets.tokens) {
            addReadOnlyAsset(token)
        }
    }

    fun addWritableBalances(assets: TransferableAssets) {
        for (asset in assets.assets) {
            assetsWritable.add(asset)
        }
        for (token in assets.tokens) {
            assetsWritable.add(token)
        }
    }

    fun combineReadOnlyAndWritable(): ArrayList<TransferableAsset>{
        var assets = arrayListOf<TransferableAsset>()
        for (asset in assetsWritable) {
            assets.add(asset.deepCopy())
        }

        //var assets = assetsWritable
        for (asset in assetsReadOnly) {
            val index = assets.indices.find { assets[it].name == asset.name } ?: -1
            if (index == -1) {
                assets.add(asset)
            } else {
                assets[index] = assets[index]
                assets[index].value += asset.value
            }
        }
        return assets
    }

    fun getSortedAssets(): ArrayList<TransferableAsset> {
        val assetsToSort = when (displayType) {
            DisplayType.HOT -> assetsWritable
            DisplayType.COMBINED -> combineReadOnlyAndWritable()
            DisplayType.COLD -> assetsReadOnly
        }
        val sortedAssets = ArrayList<TransferableAsset>()
        assetsToSort.sortBy { it.name }
        sortedAssets.addAll(assetsToSort)

        val neoIndex = sortedAssets.indices.find { sortedAssets[it].name == "NEO" } ?: -1
        if (neoIndex != -1) {
            Collections.swap(sortedAssets, 0, neoIndex)
        }
        val gasIndex = sortedAssets.indices.find { sortedAssets[it].name == "GAS" } ?: -1
        if (gasIndex != -1) {
            Collections.swap(sortedAssets, 1, gasIndex)
        }

        return sortedAssets
    }

    //@Synchronized
    fun loadAssetsFromModel(useCached: Boolean) {
        if (balanceCountDownLatch != null && balanceCountDownLatch?.count?.toInt() != 0) {
            return
        }
        if (!useCached) {
            assetsReadOnly.clear()
            assetsWritable.clear()
            loadAssetsForAllAddresses()
        } else {
            delegate.updateBalanceData(getSortedAssets())
        }
    }

    private fun loadAssetsForAllAddresses() {
        balanceCountDownLatch = CountDownLatch(1 + watchAddresses.size)
        loadAssetsFor(Account.getWallet()?.address!!, false)
        for (address in watchAddresses) {
            loadAssetsFor(address.address, true)
        }
        balanceCountDownLatch?.await()
        delegate.updateBalanceData(getSortedAssets())
    }

    fun loadAssetsFor(address: String, isReadOnly: Boolean) {
        bg {
            O3PlatformClient().getTransferableAssets(address) {
                if (it.second != null || it.first == null) {
                    balanceCountDownLatch?.countDown()
                    return@getTransferableAssets
                }
                if (isReadOnly) {
                    this.addReadOnlyBalances(it.first!!)
                } else {
                    this.addWritableBalances(it.first!!)
                }
                balanceCountDownLatch?.countDown()
            }
        }
    }

    fun getPriceFloats(): FloatArray {

        val data: Array<Double>? = when (currency) {
            CurrencyType.USD -> portfolio.data.map { it.averageUSD }.toTypedArray()
            CurrencyType.BTC -> portfolio.data.map { it.averageBTC }.toTypedArray()
        }
        if (data == null) {
            return FloatArray(0)
        }

        var floats = FloatArray(data.count())
        for (i in data.indices) {
            floats[i] = data[i].toFloat()
        }
        return floats.reversedArray()
    }

    fun loadPortfolioValue() {
        delegate.showLoadingIndicator()
        O3API().getPortfolio(this.getSortedAssets(), this.interval) {
            if (it.second != null) {
                return@getPortfolio
            }
            this.portfolio = it.first!!
            this.initialPrice = this.portfolio.data.last()
            this.latestPrice = this.portfolio.data.first()
            delegate.updatePortfolioData(it.first!!)
        }
    }
}
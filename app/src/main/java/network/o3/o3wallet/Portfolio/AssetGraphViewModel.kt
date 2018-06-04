package network.o3.o3wallet.Portfolio

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3.PriceData
import network.o3.o3wallet.API.O3.PriceHistory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by drei on 12/8/17.
 */

class AssetGraphViewModel: ViewModel() {
    private var history: MutableLiveData<PriceHistory>? = null
    private var symbol = "NEO"
    private var interval = O3Wallet.appContext!!.resources.getString(R.string.PORTFOLIO_one_day)
    private var currency = CurrencyType.FIAT
    private var latestPrice: PriceData? = null
    private var initialPrice: PriceData? = null


    fun getCurrency(): CurrencyType {
        return currency
    }

    fun setCurrency(currency: CurrencyType) {
        this.currency = currency
    }

    fun getLatestPriceFormattedString(): String {
        return if (currency == CurrencyType.BTC) {
            latestPrice?.averageBTC?.formattedBTCString() ?: "$0.00"
        } else {
            latestPrice?.averageUSD?.formattedFiatString() ?: "0.0BTC"
        }
    }

    fun getInitialPrice(): Double {
        return when(currency) {
            CurrencyType.FIAT -> initialPrice?.averageUSD ?: 0.0
            CurrencyType.BTC -> initialPrice?.averageBTC ?: 0.0
        }
    }

    fun getCurrentPrice(): Double {
        return when(currency) {
            CurrencyType.FIAT -> latestPrice?.averageUSD ?: 0.0
            CurrencyType.BTC -> latestPrice?.averageBTC ?: 0.0
        }
    }

    fun getPercentChange(): Double {
        val currentPrice = getCurrentPrice()
        val initialPrice = getInitialPrice()
        return ((currentPrice - initialPrice) / initialPrice * 100.0)
    }


    fun getInitialDate(): Date {
        val df1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        return try {
            df1.parse(initialPrice?.time ?: "")
        } catch (e: ParseException) {
            return Date()
        }
    }

    fun setInterval(interval: String) {
        this.interval = interval
    }

    fun getInterval(): String {
        return this.interval
    }

    fun getHistoryFromModel(s:String, refresh: Boolean): LiveData<PriceHistory> {
        if (history == null || refresh) {
            symbol = s
            history = MutableLiveData()
            loadHistory()
        }
        return history!!
    }

    fun getPriceFloats(): FloatArray {
        val data = when (currency) {
            CurrencyType.FIAT -> history?.value?.data?.map { it.averageUSD }?.toTypedArray()!!
            CurrencyType.BTC -> history?.value?.data?.map { it.averageBTC }?.toTypedArray()!!
        }

        var floats = FloatArray(data.count())
        for (i in data.indices) {
            floats[i] = data[i].toFloat()
        }
        return floats.reversedArray()
    }

    private fun loadHistory() {
        O3API().getPriceHistory(symbol, interval) {
            if (it.second != null) return@getPriceHistory
            latestPrice = it.first?.data?.first()!!
            initialPrice = it.first?.data?.last()!!
            history?.postValue(it.first!!)
        }
    }
}
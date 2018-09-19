package network.o3.o3wallet.NativeTrade.DepositWithdrawal

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.github.salomonbrys.kotson.get
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.O3RealTimePrice
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.Account
import network.o3.o3wallet.PersistentStore
import java.lang.Math.pow
import java.math.BigDecimal

class DepositWithdrawalViewModel: ViewModel() {
    var ownedAssets: MutableLiveData<ArrayList<TransferableAsset>>? = null
    var selectedAsset: MutableLiveData<TransferableAsset>? = null
    var selectedAssetDecimals: Int = 0
    var realTimePrice: MutableLiveData<O3RealTimePrice>? = null
    var toSendAmount: BigDecimal = BigDecimal.ZERO
    var isDeposit = true

    fun getOwnedAssets(refresh: Boolean): LiveData<ArrayList<TransferableAsset>> {
        if (ownedAssets == null || refresh) {
            ownedAssets = MutableLiveData()
            loadOwnedAssets()
        }
        return ownedAssets!!
    }

    fun loadOwnedAssets() {
        if (isDeposit) {
            val supportedAssets = arrayListOf<TransferableAsset>()
            O3PlatformClient().getTransferableAssets(Account.getWallet().address) {
                val walletAssets = it.first?.assets!!
                SwitcheoAPI().getTokens {
                    for (asset in walletAssets) {
                        if (it.first!!.get(asset.symbol.toUpperCase()) != null) {
                            supportedAssets.add(asset)
                        }
                    }
                    ownedAssets?.postValue(supportedAssets)
                }
                PersistentStore.setLatestBalances(it.first)
            }
        } else {
            O3PlatformClient().getTradingAccounts {
                for (item in ArrayList(it.first!!.switcheo.confirmed)) {
                    item.value = item.value.divide(pow(10.0,item.decimals.toDouble()).toBigDecimal())
                }
                val gasAsset = it.first!!.switcheo.confirmed.find { it.symbol.toUpperCase() == "GAS" }
                val neoAsset = it.first!!.switcheo.confirmed.find { it.symbol.toUpperCase() == "NEO" }
                val sortedList = mutableListOf<TransferableAsset>()

                if (neoAsset != null) {
                    sortedList.add(neoAsset)
                }
                if (gasAsset != null) {
                    sortedList.add(gasAsset)
                }

                for (item in ArrayList(it.first!!.switcheo.confirmed)) {
                    if (item.symbol.toUpperCase() == "GAS" || item.symbol.toUpperCase() == "NEO") {
                        continue
                    }
                    sortedList.add(item)
                }
                ownedAssets?.postValue(ArrayList(sortedList))
            }
        }
    }

    fun setSelectedAsset(transferableAsset: TransferableAsset) {
        if (selectedAsset == null) {
            selectedAsset = MutableLiveData()
        }

        selectedAssetDecimals = transferableAsset.decimals
        if(transferableAsset.symbol.toUpperCase() == "NEO") {
            selectedAssetDecimals = 0
        }

        selectedAsset?.value = transferableAsset
        selectedAsset?.postValue(transferableAsset)
        O3PlatformClient().getRealTimePrice(transferableAsset.symbol, PersistentStore.getCurrency()) {
            if (it.first != null) {
                realTimePrice?.postValue(it.first)
            } else {
                realTimePrice?.postValue(null)
            }
        }
    }


    fun getSelectedAsset(): LiveData<TransferableAsset> {
        if (selectedAsset == null) {
            selectedAsset = MutableLiveData()
        }
        return selectedAsset!!
    }

    fun getRealTimePrice(forceRefresh: Boolean): LiveData<O3RealTimePrice> {
        if (realTimePrice == null || forceRefresh) {
            realTimePrice = MutableLiveData()
        }
        return realTimePrice!!
    }

    fun getSelectedSendAmount(): BigDecimal {
        return toSendAmount
    }

    fun setSelectedSendAmount(amount: BigDecimal) {
        toSendAmount = amount
    }
}
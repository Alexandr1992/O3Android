package network.o3.o3wallet.Wallet

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper
import android.util.Log
import neoutils.Neoutils
import neoutils.Wallet
import network.o3.o3wallet.API.NEO.Block
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3.PriceData
import network.o3.o3wallet.API.O3Platform.*
import network.o3.o3wallet.API.Ontology.OntologyClient
import network.o3.o3wallet.Account
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.formattedFiatString
import org.jetbrains.anko.coroutines.experimental.bg
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask
import kotlin.math.pow


class AccountViewModel: ViewModel() {
    private var assets: MutableLiveData<TransferableAssets>? = null
    private var tradingAccountAssets: MutableLiveData<List<TransferableAsset>>? = null
    private var tradingAccountPriceData: MutableLiveData<String> = MutableLiveData()
    private var walletAccountPriceData: MutableLiveData<String> = MutableLiveData()

    var cachedTradingAccountValue: Double? = null
    var cachedWalletAccountValue: Double? = null

    //ChainSyncProcess
    private var utxos: MutableLiveData<UTXOS>? = null
    private var claims: MutableLiveData<ClaimData>? = null
    private var swapInfo: MutableLiveData<List<O3InboxItem>>? = null

    private var ontologyClaims: MutableLiveData<OntologyClaimableGas>? = null

    private var lastDataLoadError: Error? = null
    private var claimError: Error? = null
    private var claimsDataRefreshing: Boolean = false
    var needsSync = true
    private var neoBalance: Int? = null
    private var storedClaims: ClaimData? = null
    var ontologyCanNotSync = false

    var wallet: Wallet = Account.getWallet()

    fun getAssets(): LiveData<TransferableAssets> {
        if (assets == null) {
            assets = MutableLiveData()
            loadAssets()
        }
        return assets!!
    }

    fun loadAssets() {
        val cachedAssets = PersistentStore.getLatestBalances()
        if (cachedAssets != null) {
            assets!!.postValue(cachedAssets)
        }

        O3PlatformClient().getTransferableAssets(wallet!!.address) {
            lastDataLoadError = it.second
            it.first?.assets?.let {
                for (asset in it) {
                    if (asset.asset.name.toUpperCase() == "NEO") {
                        neoBalance = asset.asset.value.toInt()
                    }
                }
            }
            PersistentStore.setLatestBalances(it.first)
            assets!!.postValue(it.first)
        }
    }

    fun getTradingAccountAssets(): LiveData<List<TransferableAsset>> {
        if (tradingAccountAssets == null) {
            tradingAccountAssets = MutableLiveData()
            loadTradingAccountAssets()
        }
        return tradingAccountAssets!!
    }

    fun loadTradingAccountAssets() {
        O3PlatformClient().getTradingAccounts {
            if (it.first != null) {
                val dividedValues = mutableListOf<TransferableAsset>()
                for (asset in it.first!!.switcheo.confirmed) {
                    asset.value = asset.value.divide(10.0.pow(asset.decimals).toBigDecimal())
                }
                val sortedAssets = mutableListOf<TransferableAsset>() //it.first!!.switcheo.confirmed
                val neoAsset = it.first!!.switcheo.confirmed.find { it.symbol.toUpperCase() == "NEO" }
                val gasAsset = it.first!!.switcheo.confirmed.find { it.symbol.toUpperCase() == "GAS" }
                if (neoAsset != null) {
                    sortedAssets.add(neoAsset)
                }
                if (gasAsset != null) {
                    sortedAssets.add(gasAsset)
                }
                val interAssets = it.first!!.switcheo.confirmed.sortedBy { it.symbol }
                for (asset in interAssets) {
                    if (asset.symbol.toUpperCase() != "NEO" && asset.symbol != "GAS") {
                        sortedAssets.add(asset)
                    }
                }
                tradingAccountAssets!!.postValue(sortedAssets)
            }
        }
    }

    fun getTradingAccountPriceData(): LiveData<String> {
        return tradingAccountPriceData
    }

    fun loadTradingAccountPriceData(assets: List<TransferableAsset>) {
        if (cachedTradingAccountValue == null) {
            cachedTradingAccountValue = PersistentStore.getTradingAccountValue()
            tradingAccountPriceData.postValue(cachedTradingAccountValue!!.formattedFiatString())
        }

        O3API().getPortfolioValue(assets as ArrayList<TransferableAsset>) {
            if (it.second != null) {
                return@getPortfolioValue
            }
            PersistentStore.setTradingAccountValue(it.first!!.total.toDouble())
            tradingAccountPriceData.postValue(it.first!!.total.toDouble().formattedFiatString())
        }
    }

    fun getWalletAccountPriceData(): LiveData<String> {
        return walletAccountPriceData!!
    }

    fun loadWalletAccountPriceData(assets: List<TransferableAsset>) {
        if (cachedWalletAccountValue == null) {
            cachedWalletAccountValue = PersistentStore.getMainAccountValue()
            walletAccountPriceData.postValue(cachedWalletAccountValue!!.formattedFiatString())
        }
        O3API().getPortfolioValue(assets as ArrayList<TransferableAsset>) {
            if (it.second != null) {
                return@getPortfolioValue
            }
            PersistentStore.setMainAccountValue(it.first!!.total.toDouble())
            walletAccountPriceData!!.postValue(it.first!!.total.toDouble().formattedFiatString())
        }
    }

    fun loadInboxItem() {
        O3PlatformClient().getInbox(wallet!!.address) {
            if (it.second != null) {
                swapInfo!!.postValue(null)
            } else {
                swapInfo!!.postValue(it.first!!)
            }
        }
    }

    fun getInboxItem(): LiveData<List<O3InboxItem>?> {
        if (swapInfo == null) {
            swapInfo = MutableLiveData()
            loadInboxItem()
        }
        return swapInfo!!
    }

    fun getClaims(): LiveData<ClaimData> {
        if (claims == null) {
            claims = MutableLiveData()
            loadClaims()
        }
        return claims!!
    }

    fun getNeoBalance(): Int {
        return neoBalance ?: 0
    }

    private fun loadUTXOs() {
        O3PlatformClient().getUTXOS(wallet!!.address) {
            claimError = it.second
            utxos!!.postValue(it.first)
        }
    }

    fun getUTXOs(): LiveData<UTXOS> {
        if(utxos == null) {
            utxos = MutableLiveData()
            loadUTXOs()
        }
        return utxos!!
    }

    fun loadClaims() {
        O3PlatformClient().getClaimableGAS(wallet!!.address) {
            claimsDataRefreshing = false
            lastDataLoadError = it.second
            claims!!.postValue(it.first)
            storedClaims = it.first
        }
    }

    fun checkSyncComplete(completion: (Boolean) -> Unit ) {
        Looper.prepare()
        val checker = Runnable {
            var claimData = O3PlatformClient().getClaimableGasBlocking(wallet!!.address)
            if (claimData != null && storedClaims != null && claimData.data.claims.size != storedClaims!!.data.claims.size) {
                storedClaims = claimData
                completion(true)
            } else {
                completion(false)
            }
        }
        Handler().postDelayed(checker, 60000)
        Looper.loop()
    }

    fun syncChain(completion: (Boolean) -> Unit) {
        if (neoBalance == null) {
            completion(false)
            return
        }

        NeoNodeRPC(PersistentStore.getNodeURL()).sendNativeAssetTransaction(wallet!!,
                NeoNodeRPC.Asset.NEO, neoBalance!!.toBigDecimal(), Account.getWallet()!!.address, null) {
            if (it.second != null || it.first == null) {
                completion(false)
            } else {
                checkSyncComplete {
                    if (it) {
                        this.needsSync = false
                    }
                    completion(it)
                }
            }
        }
    }

    fun getOntologyClaims(): LiveData<OntologyClaimableGas> {
        if (ontologyClaims == null) {
            ontologyClaims = MutableLiveData()
            loadOntologyClaims()
        }
        return ontologyClaims!!
    }

    fun loadOntologyClaims() {
        O3PlatformClient().getOntologyCalculatedGas(wallet!!.address) {
            if(it.first == null) {
                ontologyClaims?.postValue(OntologyClaimableGas("0", false))
            } else {
                ontologyClaims?.postValue(it.first)
            }
        }
    }

    fun performClaim(completion: (Boolean?, Error?) -> Unit) {
        NeoNodeRPC(PersistentStore.getNodeURL()).claimGAS(wallet!!, storedClaims) {
            completion(it.first, it.second)
        }
    }

    fun resyncOntologyClaims(completion: (Double?, Error?) -> Unit) {
        O3PlatformClient().getOntologyCalculatedGas(wallet!!.address) {
            if (it.first != null && it.first!!.calculated == false) {
                val doubleValue = it.first!!.ong.toLong() / OntologyClient().DecimalDivisor
                ontologyCanNotSync = doubleValue <= 0.02
                completion(doubleValue, null)
            } else {
                Handler().postDelayed(
                        {
                            O3PlatformClient().getOntologyCalculatedGas(wallet!!.address) {
                                if (it.first != null && it.first!!.calculated == false) {
                                    val doubleValue = it.first!!.ong.toLong() / OntologyClient().DecimalDivisor
                                    ontologyCanNotSync = doubleValue <= 0.02
                                    completion(doubleValue, null)
                                } else {
                                    completion(null, Error("Syncing Failed"))
                                }
                            }
                        }, 15000)
            }
        }
    }

    fun syncOntologyChain(completion: (Double?, Error?) -> Unit) {
        OntologyClient().transferOntologyAsset(OntologyClient.Asset.ONT.assetID(), wallet!!.address, 1.0) {
            if(it.first != null) {
                Looper.prepare()
                Handler().postDelayed (
                    {

                        resyncOntologyClaims { amount, error ->
                            completion(amount, error)
                        }
                    }, 15000)
                Looper.loop()
            } else {
                completion(null, Error(it.second))
            }
        }
    }

    fun getLastError(): Error {
        return lastDataLoadError ?: Error()
    }

    fun getStoredClaims(): ClaimData? {
        return storedClaims
    }
}
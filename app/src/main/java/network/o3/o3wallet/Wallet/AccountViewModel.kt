package network.o3.o3wallet.Wallet

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper
import android.util.Log
import network.o3.o3wallet.API.NEO.Block
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3Platform.*
import network.o3.o3wallet.Account
import network.o3.o3wallet.PersistentStore
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask


class AccountViewModel: ViewModel() {
    private var assets: MutableLiveData<TransferableAssets>? = null

    //ChainSyncProcess
    private var utxos: MutableLiveData<UTXOS>? = null
    private var utxosFetched: MutableLiveData<Boolean>? = null
    private var neoSent: MutableLiveData<Boolean>? = null
    private var claimInfoUpdated: MutableLiveData<Boolean>? = null
    private var claims: MutableLiveData<ClaimData>? = null
    private var currentBlock: MutableLiveData<Int?>? = null
    private var swapInfo: MutableLiveData<O3InboxItem?>? = null

    private var lastDataLoadError: Error? = null
    private var claimError: Error? = null
    private var claimingInProgress: Boolean = false
    private var claimsDataRefreshing: Boolean = false
    private var needsSync = true
    private var neoBalance: Int? = null
    private var storedClaims: ClaimData? = null


    init {
        getUTXOs(true)
    }

    fun getAssets(refresh: Boolean): LiveData<TransferableAssets> {
        if (assets == null || refresh) {
            assets = MutableLiveData()
            loadAssets()
        }
        return assets!!
    }

    private fun loadAssets() {
        O3PlatformClient().getTransferableAssets(Account.getWallet()!!.address) {
            lastDataLoadError = it.second
            it.first?.assets?.let {
                for (asset in it) {
                    if (asset.asset.name.toUpperCase() == "NEO") {
                        neoBalance = asset.asset.value.toInt()
                    }
                }
            }
            assets!!.postValue(it.first)
        }
    }

    private fun loadInboxItem() {
        O3PlatformClient().getInbox(Account.getWallet()!!.address) {
            if (it.second != null) {
                swapInfo!!.postValue(null)
            } else if (it.first != null && it.first!!.isNotEmpty()){
                swapInfo!!.postValue(it.first!![0])
            } else {
                swapInfo!!.postValue(null)
            }
        }
    }

    fun getInboxItem(refresh: Boolean): LiveData<O3InboxItem?> {
        if (swapInfo == null || refresh) {
            swapInfo = MutableLiveData()
            loadInboxItem()
        }
        return swapInfo!!
    }

    fun getClaims(refresh: Boolean): LiveData<ClaimData> {
        if (claims == null || refresh) {
            claims = MutableLiveData()
            loadClaims()
        }
        return claims!!
    }

    fun getBlock(refresh: Boolean): LiveData<Int?> {
        if (currentBlock == null || refresh) {
            currentBlock = MutableLiveData()
            loadBlock()
        }
        return currentBlock!!
    }

    fun loadBlock() {
        NeoNodeRPC(PersistentStore.getNodeURL()).getBlockCount {
            currentBlock?.postValue(it.first)
        }
    }

    fun getNeoBalance(): Int {
        return neoBalance!!
    }

    private fun loadUTXOs() {
        O3PlatformClient().getUTXOS(Account.getWallet()!!.address) {
            claimError = it.second
            utxos!!.postValue(it.first)
        }
    }

    fun getUTXOs(refresh: Boolean): LiveData<UTXOS> {
        if(utxos == null || refresh) {
            utxos = MutableLiveData()
            loadUTXOs()
        }
        return utxos!!
    }

    fun getEstimatedGas(claimData: ClaimData): Double {
        if (claimData.data.claims.isEmpty()) {
            return claimData.data.gas.toDouble()
        } else if (currentBlock == null) {
            return claimData.data.gas.toDouble()
        } else {
            val sortedClaims = claimData.data.claims.sortedBy { it.createdAtBlock }
            return (currentBlock?.value!! - sortedClaims[0].createdAtBlock) / 100000000.0 * 7 * neoBalance!!
        }
    }

    private fun loadClaims() {
        O3PlatformClient().getClaimableGAS(Account.getWallet()!!.address) {
            claimsDataRefreshing = false
            lastDataLoadError = it.second
            claims!!.postValue(it.first)
            storedClaims = it.first
        }
    }



    fun checkSyncComplete(completion: (Boolean) -> Unit ) {
        Looper.prepare()
        val checker = Runnable {
            var claimData = O3PlatformClient().getClaimableGasBlocking(Account.getWallet()!!.address)
            if (claimData != null && storedClaims != null && claimData.data.claims.size != storedClaims!!.data.claims.size) {
                storedClaims = claimData
                completion(true)
            } else {
                completion(false)
            }
        }
        Handler().postDelayed(checker, 45000)
        Looper.loop()
    }

    fun syncChain(completion: (Boolean) -> Unit) {
        if (neoBalance == null) {
            completion(false)
            return
        }

        NeoNodeRPC(PersistentStore.getNodeURL()).sendNativeAssetTransaction(Account.getWallet()!!,
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

    fun performClaim(completion: (Boolean?, Error?) -> Unit) {
        NeoNodeRPC(PersistentStore.getNodeURL()).claimGAS(Account.getWallet()!!, storedClaims) {
            completion(it.first, it.second)
        }
    }


    fun getClaimingStatus(): Boolean {
        return claimingInProgress
    }

    fun setClaimiable(isClaiming: Boolean) {
        this.claimingInProgress = isClaiming
    }

    fun getLastError(): Error {
        return lastDataLoadError ?: Error()
    }

    fun getStoredClaims(): ClaimData {
        return storedClaims!!
    }
}
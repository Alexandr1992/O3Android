package network.o3.o3wallet.Wallet.SendV2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import neoutils.Neoutils
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.O3RealTimePrice
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.VerifiedAddress
import network.o3.o3wallet.API.Ontology.OntologyClient
import network.o3.o3wallet.Account
import network.o3.o3wallet.AnalyticsService
import network.o3.o3wallet.Contact
import network.o3.o3wallet.PersistentStore
import org.json.JSONObject
import java.math.BigDecimal

class SendViewModel: ViewModel() {
    var ownedAssets: MutableLiveData<ArrayList<TransferableAsset>>? = null
    var selectedAsset: MutableLiveData<TransferableAsset>? = null

    var selectedAddress: MutableLiveData<String>? = null
    var selectedContact: MutableLiveData<Contact>? = MutableLiveData()

    var verifiedAddress: MutableLiveData<VerifiedAddress?>? = null
    var realTimePrice: MutableLiveData<O3RealTimePrice>? = null

    var sendInProgress: MutableLiveData<Boolean> = MutableLiveData()
    var sendResult: MutableLiveData<String?>? = MutableLiveData()
    var ontologyNetworkFee: MutableLiveData<Double>? = null

    var neoNetworkFee: MutableLiveData<Double>? = null

    var mempoolHeight: MutableLiveData<Int?>? = null

    var nnsResolvedAddress: MutableLiveData<String>? = null
    var nnsLoadingStatus: MutableLiveData<Boolean> = MutableLiveData()
    var nnsName: String = ""

    var selectedAssetDecimals: Int = 0

    var toSendAmount: BigDecimal = BigDecimal.ZERO
    var txID = ""

    fun getOwnedAssets(refresh: Boolean): LiveData<ArrayList<TransferableAsset>> {
        if (ownedAssets == null || refresh) {
            ownedAssets = MutableLiveData()
            loadOwnedAssets()
        }
        return ownedAssets!!
    }

    fun loadOwnedAssets() {
        val cachedAssets = PersistentStore.getLatestBalances()
        if (cachedAssets != null) {
            ownedAssets!!.postValue(cachedAssets.assets)
        }
        O3PlatformClient().getTransferableAssets(Account.getWallet().address) {
            PersistentStore.setLatestBalances(it.first)
            ownedAssets?.postValue(it.first?.assets ?: arrayListOf())
        }
    }

    fun setSelectedAsset(transferableAsset: TransferableAsset) {
        if (selectedAsset == null) {
            selectedAsset = MutableLiveData()
        }
        selectedAsset?.value = transferableAsset
        selectedAsset?.postValue(transferableAsset)
        selectedAssetDecimals = transferableAsset.decimals
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

    fun setSelectedContact(contact: Contact) {
        selectedContact?.postValue(contact)
        selectedAddress?.value = contact.address
    }

    fun getSelectedContact(): LiveData<Contact> {
        if (selectedContact == null) {
            selectedContact = MutableLiveData()
        }
        return selectedContact!!
    }

    fun setSelectedAddress(address: String) {
        if (selectedAddress == null) {
            selectedAddress = MutableLiveData()
        }

        val contacts = PersistentStore.getContacts()
        val foundContact = contacts.find { it.address == address }

        if (foundContact != null) {
            setSelectedContact(foundContact)
        } else {
            selectedContact?.value = null
            selectedAddress?.value = address
            selectedAddress?.postValue(address)
        }
    }

    fun getSelectedAddress(): LiveData<String> {
        if (selectedAddress == null) {
            selectedAddress = MutableLiveData()
        }
        return selectedAddress!!
    }

    fun getVerifiedAddress(refresh: Boolean, address: String): LiveData<VerifiedAddress?> {
        if (verifiedAddress == null || refresh) {
            verifiedAddress = MutableLiveData()
            loadVerifiedAddress(address)
        }
        return verifiedAddress!!
    }

    fun getRealTimePrice(forceRefresh: Boolean): LiveData<O3RealTimePrice> {
        if (realTimePrice == null || forceRefresh) {
            realTimePrice = MutableLiveData()
        }
        return realTimePrice!!
    }

    fun loadVerifiedAddress(address: String) {
        if (Neoutils.validateNEOAddress(address)) {
            O3PlatformClient().getVerifiedAddress(address) {
                verifiedAddress?.postValue(it.first)
            }
        } else {
            verifiedAddress?.postValue(null)
        }
    }

    fun getMemPoolHeight(): LiveData<Int?> {
        if (mempoolHeight == null) {
            mempoolHeight = MutableLiveData()
            loadMemPoolHeight()
        }
        return mempoolHeight!!
    }

    fun loadMemPoolHeight() {
        NeoNodeRPC(PersistentStore.getNodeURL()).getMemPoolHeight {
            mempoolHeight?.postValue(it.first)
        }
    }

    fun getSelectedSendAmount(): BigDecimal {
        return toSendAmount
    }

    fun setSelectedSendAmount(amount: BigDecimal) {
        toSendAmount = amount
    }

    fun sendOntAsset() {
        val toSendAsset = selectedAsset!!.value!!
        val recipientAddress = selectedAddress!!.value!!
        val amount = getSelectedSendAmount().toDouble()
        OntologyClient().transferOntologyAsset(toSendAsset.symbol.toUpperCase(), recipientAddress, amount) {
            if (it.first != null ) {
                val attrs = mapOf("blockchain" to "Ontology",
                        "asset" to toSendAsset.symbol,
                        "amount" to amount.toString(),
                        "isLedger" to false)
                AnalyticsService.Wallet.logSend(JSONObject(attrs))
                sendResult?.postValue(it.first!!)
            } else {
                sendResult?.postValue(null)
            }
            setSendInProgress(false)
        }
    }

    fun sendNativeNeoAsset() {
        val wallet = Account.getWallet()
        var toSendAsset: NeoNodeRPC.Asset? = null
        val toSendAssetString = selectedAsset!!.value!!.symbol.toUpperCase()
        toSendAsset = if (toSendAssetString == "NEO") {
            NeoNodeRPC.Asset.NEO
        } else {
            NeoNodeRPC.Asset.GAS
        }

        val recipientAddress = selectedAddress!!.value!!
        val amount = getSelectedSendAmount()

        NeoNodeRPC(PersistentStore.getNodeURL()).sendNativeAssetTransaction(wallet, toSendAsset,
                amount, recipientAddress, null, BigDecimal(neoNetworkFee?.value ?: 0.0)) {
            val error = it.second
            val txid = it.first
            if (txid != null) {
                txID = txid
                val attrs = mapOf("blockchain" to "NEO",
                        "asset" to toSendAssetString,
                        "amount" to amount.toPlainString(),
                        "isLedger" to false)
                AnalyticsService.Wallet.logSend(JSONObject(attrs))
                sendResult?.postValue(txid)
            } else {
                txID = ""
                sendResult?.postValue(null)
            }
            setSendInProgress(false)
        }
    }

    fun sendNeoTokenAsset() {
        val wallet = Account.getWallet()
        val toSendAsset = selectedAsset!!.value!!
        val recipientAddress = selectedAddress!!.value!!
        val amount = getSelectedSendAmount()

        if (neoNetworkFee?.value ?: 0.0 == 0.0) {
            NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(wallet, null, toSendAsset.id, wallet.address, recipientAddress, amount, toSendAsset.decimals, BigDecimal.ZERO) {
                val error = it.second
                val txid = it.first
                if (txid != null) {
                    val attrs = mapOf("blockchain" to "NEO",
                            "asset" to toSendAsset.symbol,
                            "amount" to amount.toPlainString(),
                            "isLedger" to false)
                    AnalyticsService.Wallet.logSend(JSONObject(attrs))
                    sendResult?.postValue(txid)
                } else {
                    sendResult?.postValue(null)
                }
                setSendInProgress(false)
            }
        } else {
            O3PlatformClient().getUTXOS(wallet.address) {
                var assets = it.first
                var error = it.second
                if (error != null) {
                    sendResult?.postValue(null)
                } else {
                    NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(wallet, assets, toSendAsset.id, wallet.address,
                            recipientAddress, amount, toSendAsset.decimals, BigDecimal(neoNetworkFee?.value ?: 0.0)) {
                        val error = it.second
                        val txid = it.first
                        if (txid != null) {
                            val attrs = mapOf("blockchain" to "NEO",
                                    "asset" to toSendAsset.symbol,
                                    "amount" to amount.toPlainString(),
                                    "isLedger" to false)
                            AnalyticsService.Wallet.logSend(JSONObject(attrs))
                            sendResult?.postValue(txid)
                        } else {
                            sendResult?.postValue(null)
                        }
                        setSendInProgress(false)
                    }
                }
            }
        }
    }

    fun getSendResult(): LiveData<String?> {
        //reset send result every time
        sendResult = MutableLiveData()
        return sendResult!!
    }

    fun getOntologyNetworkFee(): LiveData<Double> {
        if (ontologyNetworkFee == null) {
            ontologyNetworkFee = MutableLiveData()
            loadOntologyNetworkFee()
        }
        return ontologyNetworkFee!!
    }

    fun getNeoNetworkFee(): LiveData<Double> {
        if (neoNetworkFee == null) {
            neoNetworkFee = MutableLiveData()
        }
        return neoNetworkFee!!
    }

    fun setNeoNetworkFee(fee: Double) {
        neoNetworkFee?.value = fee
        neoNetworkFee?.postValue(fee)
    }

    fun loadOntologyNetworkFee() {
        if (!isOntAsset()) {
            ontologyNetworkFee?.postValue(-1.0)
            return
        } else {
            ontologyNetworkFee?.postValue(500 * 20000.0)
        }

        OntologyClient().getGasPrice {
            if (it.first != null) {
                return@getGasPrice
            } else {
                ontologyNetworkFee?.postValue(it.first!! * 20000.0)
            }
        }
    }

    fun loadResolvedNNS(nnsQuery: String) {
        nnsLoadingStatus.postValue(true)
        O3PlatformClient().resolveNNS(nnsQuery) {
            if(it.first != null) {
                nnsName = nnsQuery
            }
            nnsLoadingStatus.postValue(false)
            nnsResolvedAddress?.postValue(it.first)
        }
    }

    fun getResolvedNNS(): LiveData<String> {
        if (nnsResolvedAddress == null ) {
            nnsResolvedAddress = MutableLiveData()
        }
        return nnsResolvedAddress!!
    }

    fun getNNSLoadingStatus(): LiveData<Boolean> {
        return nnsLoadingStatus
    }

    fun isOntAsset(): Boolean {
        val toSendAsset = selectedAsset!!.value!!
        return toSendAsset.id.contains("00000000")
    }

    fun isNeoAsset(): Boolean {
        return !isOntAsset()
    }

    fun isNEOTokenAsset(): Boolean {
        val toSendAsset = selectedAsset!!.value!!
        if (isNeoAsset() && toSendAsset.symbol.toUpperCase() != "GAS"
                && toSendAsset.symbol.toUpperCase() != "NEO") {
            return true
        }
        return false
    }

    fun getIsSending(): LiveData<Boolean> {
        return sendInProgress
    }

    fun setSendInProgress(isSending: Boolean) {
        sendInProgress.postValue(isSending)
    }

    fun send() {
        val toSendAsset = selectedAsset!!.value!!
        setSendInProgress(true)
        if (isOntAsset()) {
            sendOntAsset()
        } else if (toSendAsset.symbol.toUpperCase() == "GAS"
                || toSendAsset.symbol.toUpperCase() == "NEO") {
            sendNativeNeoAsset()
        } else {
            sendNeoTokenAsset()
        }
    }
}

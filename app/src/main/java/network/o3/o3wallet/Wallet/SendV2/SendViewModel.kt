package network.o3.o3wallet.Wallet.SendV2

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import neoutils.Neoutils
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.O3RealTimePrice
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.VerifiedAddress
import network.o3.o3wallet.API.Ontology.OntologyClient
import network.o3.o3wallet.Account
import network.o3.o3wallet.Contact
import network.o3.o3wallet.PersistentStore
import java.math.BigDecimal

class SendViewModel: ViewModel() {
    var ownedAssets: MutableLiveData<ArrayList<TransferableAsset>>? = null
    var selectedAsset: MutableLiveData<TransferableAsset>? = null

    var selectedAddress: MutableLiveData<String>? = null
    var selectedContact: MutableLiveData<Contact>? = MutableLiveData()

    var verifiedAddress: MutableLiveData<VerifiedAddress?>? = null
    var realTimePrice: MutableLiveData<O3RealTimePrice>? = null

    //var sendInProgress: MutableLiveData<Boolean>? = null
    var sendResult: MutableLiveData<String?>? = MutableLiveData()
    var ontologyNetworkFee: MutableLiveData<Double>? = null

    var neoNetworkFee: MutableLiveData<Double>? = null

    var mempoolHeight: MutableLiveData<Int?>? = null

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
        O3PlatformClient().getTransferableAssets(Account.getWallet()?.address!!) {
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
        val wallet = Account.getWallet()
        OntologyClient().transferOntologyAsset(toSendAsset.symbol.toUpperCase(), recipientAddress, amount) {
            if (it.first == true) {
                sendResult?.postValue("")
            } else {
                sendResult?.postValue(null)
            }
        }

    }

    fun sendNativeNeoAsset() {
        val wallet = Account.getWallet()
        var toSendAsset: NeoNodeRPC.Asset? = null
        toSendAsset = if (selectedAsset!!.value!!.symbol.toUpperCase() == "NEO") {
            NeoNodeRPC.Asset.NEO
        } else {
            NeoNodeRPC.Asset.GAS
        }

        val recipientAddress = selectedAddress!!.value!!
        val amount = getSelectedSendAmount()
        NeoNodeRPC(PersistentStore.getNodeURL()).
                sendNativeAssetTransaction(wallet!!, toSendAsset, amount, recipientAddress, null, BigDecimal(neoNetworkFee?.value ?: 0.0)) {
            val error = it.second
            val txid = it.first
            if (txid != null) {
                txID = txid
                sendResult?.postValue(txid)
            } else {
                txID = ""
                sendResult?.postValue(null)
            }
        }
    }

    fun sendNeoTokenAsset() {
        val wallet = Account.getWallet()
        val toSendAsset = selectedAsset!!.value!!
        val recipientAddress = selectedAddress!!.value!!
        val amount = getSelectedSendAmount()

        if (neoNetworkFee?.value ?: 0.0 == 0.0) {
            NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(wallet!!, null, toSendAsset.id, wallet.address, recipientAddress, amount, toSendAsset.decimals, BigDecimal.ZERO) {
                val error = it.second
                val txid = it.first
                if (txid != null) {
                    sendResult?.postValue(txid)
                } else {
                    sendResult?.postValue(null)
                }
            }
        } else {
            O3PlatformClient().getUTXOS(wallet!!.address) {
                var assets = it.first
                var error = it.second
                if (error != null) {
                    sendResult?.postValue(null)
                } else {
                    NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(wallet!!, assets, toSendAsset.id, wallet.address,
                            recipientAddress, amount, toSendAsset.decimals, BigDecimal(neoNetworkFee?.value ?: 0.0)) {
                        val error = it.second
                        val txid = it.first
                        if (txid != null) {
                            sendResult?.postValue(txid)
                        } else {
                            sendResult?.postValue(null)
                        }
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

    fun isOntAsset(): Boolean {
        val toSendAsset = selectedAsset!!.value!!
        return toSendAsset.id.contains("00000000")
    }

    fun isNeoAsset(): Boolean {
        return !isOntAsset()
    }

    fun send() {
        val toSendAsset = selectedAsset!!.value!!
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

package network.o3.o3wallet.Wallet.SendV2

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import neoutils.Neoutils
import network.o3.o3wallet.API.O3.Feature
import network.o3.o3wallet.API.O3.FeedData
import network.o3.o3wallet.API.O3.O3API
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.O3RealTimePrice
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.VerifiedAddress
import network.o3.o3wallet.Account
import network.o3.o3wallet.Contact
import network.o3.o3wallet.PersistentStore

class SendViewModel: ViewModel() {
    var ownedAssets: MutableLiveData<ArrayList<TransferableAsset>>? = null
    var selectedAsset: MutableLiveData<TransferableAsset>? = null

    var selectedAddress: MutableLiveData<String>? = null
    var selectedContact: MutableLiveData<Contact>? = null

    var verifiedAddress: MutableLiveData<VerifiedAddress?>? = null
    var realTimePrice: MutableLiveData<O3RealTimePrice>? = null

    var toSendAmount: Double = 0.0

    var isFiatEntryType = false

    fun getOwnedAssets(refresh: Boolean): LiveData<ArrayList<TransferableAsset>> {
        if (ownedAssets == null || refresh) {
            ownedAssets = MutableLiveData()
            loadOwnedAssets()
        }
        return ownedAssets!!
    }

    fun loadOwnedAssets() {
        O3PlatformClient().getTransferableAssets(Account.getWallet()?.address!!) {
            ownedAssets?.postValue(it.first?.assets ?: arrayListOf())
        }
    }

    fun setSelectedAsset(transferableAsset: TransferableAsset) {
        selectedAsset?.postValue(transferableAsset)
        O3PlatformClient().getRealTimePrice(transferableAsset.symbol, PersistentStore.getCurrency()) {
            if (it.first != null) {
                realTimePrice?.postValue(it.first)
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
    }

    fun getSelectedContact(): LiveData<Contact> {
        if (selectedContact == null) {
            selectedContact = MutableLiveData()
        }
        return selectedContact!!
    }

    fun setSelectedAddress(address: String) {
        val contacts = PersistentStore.getContacts()
        val foundContact = contacts.find { it.address == address }
        if (foundContact != null) {
            setSelectedContact(foundContact)
        } else {
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

    fun getRealTimePrice(): LiveData<O3RealTimePrice> {
        if (realTimePrice == null) {
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

    fun getFiatEntryType(): Boolean {
        return isFiatEntryType
    }

    fun toggleFiatEntryType(): Boolean {
        isFiatEntryType = !isFiatEntryType
        return isFiatEntryType
    }

    fun getSelectedSendAmount(): Double {
        return toSendAmount
    }

    fun setSelectedSendAmount(amount: Double) {
        toSendAmount = amount
    }

}

package network.o3.o3wallet

import android.preference.PreferenceManager
import android.util.Base64
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.API.NEO.NEP5Token
import network.o3.o3wallet.API.O3.O3Response
import network.o3.o3wallet.API.O3Platform.TransactionHistoryEntry
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.TransferableAssets

/**
 * Created by drei on 11/29/17.
 */

data class Contact(val address: String, val nickname: String)

object PersistentStore {

    fun clearPersistentStore() {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit().clear().apply()
    }

    fun addContact(address: String, nickname: String): ArrayList<Contact> {
        val currentContacts = getContacts().toCollection(ArrayList<Contact>())
        val toInsert = Contact(address, nickname)

        if (currentContacts.contains(toInsert)) {
            return currentContacts
        }
        currentContacts.add(toInsert)
        val gson = Gson()
        val jsonString = gson.toJson(currentContacts)

        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putString("CONTACTS", jsonString)
        settingPref.apply()

        return currentContacts
    }

    fun removeContact(address: String, nickname: String): ArrayList<Contact> {
        val currentContacts = getContacts().toCollection(ArrayList<Contact>())
        currentContacts.remove(Contact(address, nickname))
        val gson = Gson()
        val jsonString = gson.toJson(currentContacts)

        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putString("CONTACTS", jsonString)
        settingPref.apply()

        return currentContacts
    }

    fun getContacts(): Array<Contact> {
        var jsonString = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("CONTACTS", null)

        if (jsonString == null) {
            return arrayOf<Contact>()
        }

        val gson = Gson()
        val contacts = gson.fromJson<Array<Contact>>(jsonString)
        return contacts
    }

    fun setColdStorageVaultAddress(address: String) {
        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putString("COLD_STORAGE_VAULT_ADDRESS", address)
        settingPref.apply()
    }

    fun getColdStorageVaultAddress(): String {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("COLD_STORAGE_VAULT_ADDRESS", "")
    }

    fun removeColdStorageVaultAddress() {
        setColdStorageVaultAddress("")
    }

    fun setNodeURL(url: String) {
        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putString("NODE_URL", url)
        settingPref.apply()
    }

    fun setOverrideNodeURL(url: String) {
        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putString("OVERRIDE_NODE_URL", url)
        settingPref.apply()
    }

    fun getOverrideNodeURL(): String {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("OVERRIDE_NODE_URL", "")!!
    }

    fun getOntologyNodeURL(): String {
        return  PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("ONTOLOGY_NODE_URL", "http://dappnode2.ont.io:20336")!!
    }

    fun setOntologyNodeURL(url: String) {
        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putString("ONTOLOGY_NODE_URL", url)
        settingPref.apply()
    }

    fun getNodeURL(): String {
        if (getOverrideNodeURL() == "") {
            return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                    .getString("NODE_URL", "http://seed2.neo.org:10332")!!
        } else {
            return getOverrideNodeURL()
        }
    }

    fun setNetworkType(network: String) {
        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putString("NETWORK_TYPE", network)
        settingPref.apply()
    }

    fun getNetworkType(): String {
        return  PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("NETWORK_TYPE", "Main")
    }

    fun getFirstTokenAppeared(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getBoolean("FIRST_TOKEN", true)
    }

    fun setFirstTokenAppeared(firstToken: Boolean) {
        val settingPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingPref.putBoolean("FIRST_TOKEN", firstToken)
        settingPref.apply()
    }
  
    fun setCurrency(currency: String) {
        val settingsPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        settingsPref.putString("CURRENCY", currency)
        settingsPref.apply()
    }

    fun getCurrency(): String {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("CURRENCY", "usd")
    }

    fun setLatestBalances(assets: TransferableAssets?) {
        if (assets == null) {
            return
        }
        val settingsPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        val assets = Gson().toJson(assets)
        settingsPref.putString("BALANCES", assets)
        settingsPref.apply()
    }

    fun getLatestBalances(): TransferableAssets? {
        val assetsJson = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("BALANCES", "")
        if (assetsJson == "") {
            return null
        } else {
            return Gson().fromJson(assetsJson)
        }
    }

    fun setSavedAddressBalances(address: String, assets: ArrayList<TransferableAsset>?) {
        if (assets == null) {
            return
        }
        val settingsPref = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
        val assetsJson = Gson().toJson(assets)
        settingsPref.putString("SAVED_ADDR_" + address, assetsJson)
        settingsPref.apply()
    }

    fun getSavedAddressBalances(address: String): ArrayList<TransferableAsset>? {
        val assetsJson = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("SAVED_ADDR_" + address, "")
        if (assetsJson == "") {
            return null
        } else {
            return Gson().fromJson(assetsJson)
        }
    }

    fun clearSavedAdressBalances(address: String) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putString("SAVED_ADDR_" + address, "").apply()
    }


    fun shouldShowSwitcheoOnPortfolio(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getBoolean("SHOW_SWITCHEO", true)
    }

    fun setShouldShowSwitcheoOnPortfolio(shouldShow: Boolean) {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .edit().putBoolean("SHOW_SWITCHEO", shouldShow).apply()
    }

    fun getPendingTransactions(): ArrayList<TransactionHistoryEntry> {
        val json = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("PENDING_TRANSACTIONS", "")
        if (json == "") {
            return arrayListOf()
        }
        return Gson().fromJson(json)
    }

    fun setPendingTransactions(transactions: ArrayList<TransactionHistoryEntry>) {
        val pendingTx = Gson().toJson(transactions)
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putString("PENDING_TRANSACTIONS", pendingTx).apply()
    }

    fun clearPendingTransactions() {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putString("PENDING_TRANSACTIONS", "").apply()
    }

    fun setTheme(theme: String) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putString("THEME", theme).apply()
    }

    fun getTheme(): String {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("THEME", "Light")
    }

    fun getTradingAccountValue(): Double {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("TRADING_ACCOUNT_VALUE", "")?.toDoubleOrNull() ?: 0.0
    }

    fun getMainAccountValue(): Double {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext)
                .getString("MAIN_ACCOUNT_VALUE", "")?.toDoubleOrNull() ?: 0.0
    }

    fun setTradingAccountValue(value: Double) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putString("TRADING_ACCOUNT_VALUE", value.toString()).apply()
    }

    fun setMainAccountValue(value: Double) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putString("MAIN_ACCOUNT_VALUE", value.toString()).apply()
    }

    // Make this default to ttrie
    fun getHasInitiatedBackup(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).
                getBoolean("HAS_INITIATED_BACKUP", true)
    }

    fun getHasDismissedBackup(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).
                getBoolean("HAS_DISMISSED_BACKUP", true)
    }

    fun setHasInitiatedBackup(value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putBoolean("HAS_INITIATED_BACKUP", value).apply()
    }

    fun getHasLoggedFirstWallet(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).
                getBoolean("HAS_LOGGED_FIRST_WALLET", false)
    }

    fun setHasLoggedFirstWallet(value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putBoolean("HAS_LOGGED_FIRST_WALLET", value).apply()
    }

    fun didGenerateFirstWallet(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).
                getBoolean("DID_GENERATE_FIRST_WALLET", false)
    }

    fun setDidGenerateFirstWallet(value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putBoolean("DID_GENERATE_FIRST_WALLET", value).apply()
    }

    fun setHasDismissedBackup(value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit()
                .putBoolean("HAS_DISMISSED_BACKUP", value).apply()
    }

    enum class VerificationType() {
        SCREENSHOT,
        BYHAND,
        OTHER
    }

    fun getManualVerificationType(address: String): List<VerificationType> {
        val jsonString = PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).getString(address + "_VERIFICATIONTYPE", "")
        if (jsonString == "") {
            return listOf()
        }
        val list = Gson().fromJson<List<String>>(jsonString)
        return list.map { VerificationType.valueOf(it)}

    }

    fun setManualVerificationType(address: String, types: List<VerificationType>) {
        var jsonString = Gson().toJson(types.map { it.name })
        PreferenceManager.getDefaultSharedPreferences(O3Wallet.appContext).edit().
                putString(address + "_VERIFICATIONTYPE", jsonString).apply()
    }
}
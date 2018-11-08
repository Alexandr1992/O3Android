package network.o3.o3wallet

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import java.io.File

data class NEP6(var name: String, var version: String, var scrypt: ScryptParams, var accounts: MutableList<Account>) {

    data class Account(var address: String, var label: String, var isDefault: Boolean, var key: String?)
    data class ScryptParams(var n: Int, var r: Int, var p: Int)



    fun addWatchAddress(address: String, name: String) {
        //No duplicate accounts
        if (accounts.find { it.address == address } != null) {
            return
        }

        if (accounts.find { it.label == name } != null) {
            return
        }

        val newAccount = Account(address, name, false, null)
        accounts.add(newAccount)
    }

    fun addEncryptedKey(address: String, name: String, key: String) {
        //No duplicate accounts
        if (accounts.find { it.address == address } != null) {
            return
        }

        if (accounts.find { it.label == name } != null) {
            return
        }

        val newAccount = Account(address, name, false, key)
        accounts.add(newAccount)
    }

    fun removeWatchAddress(address: String) {
        val index = accounts.indexOfFirst { it.address == address && it.key == null }
        if (index == -1) {
            return
        }
        accounts.removeAt(index)
    }

    fun removeEncryptedKey(key: String) {
        val index = accounts.indexOfFirst { it.key == null }
        if (index == -1) {
            return
        }
        accounts.removeAt(index)
    }

    fun getWalletAccounts(): List<Account> {
        val walletAccounts: MutableList<Account> = mutableListOf()
        val indexDefault = accounts.indexOfFirst { it.isDefault }
        if (indexDefault == -1) {
            return listOf()
        }

        walletAccounts.add(accounts[indexDefault])

        for (account in accounts) {
            if (account.key != null) {
                walletAccounts.add(account)
            }
        }

        return walletAccounts
    }

    fun convertWatchAddressToWallet(address: String, key: String) {
        val index = accounts.indexOfFirst { it.address == address }
        accounts[index].key = key
    }

    fun makeNewDefault(address: String) {
        val defaultIndex = accounts.indexOfFirst { it.isDefault }
        val newDefaultIndex = accounts.indexOfFirst { it.address == address }

        if (defaultIndex == -1) {
            accounts[newDefaultIndex].isDefault = true
            return
        }

        accounts[defaultIndex].isDefault = false
        accounts[newDefaultIndex].isDefault = true

        var oldDefaultAccount = accounts[defaultIndex]
        accounts[defaultIndex] = accounts[newDefaultIndex]
        accounts[newDefaultIndex] = oldDefaultAccount
    }

    fun writeToFileSystem() {
        val file = File(O3Wallet.appContext!!.filesDir, "O3.json")
        file.writeText(Gson().toJson(this))
    }

    companion object  {
        fun getFromFileSystem(): NEP6 {
            try {
                val file = File(O3Wallet.appContext!!.filesDir, "O3.json")
                return Gson().fromJson<NEP6>(file.readText())
            } catch(e: Exception)  {
                val scryptParams = ScryptParams(16384, 8, 8)
                val nep6 = NEP6("O3 Wallet", "1.0", scryptParams, mutableListOf())
                nep6.writeToFileSystem()
                return nep6
            }
        }

        fun nep6HasActivated(): Boolean {
            val nep6InFile = getFromFileSystem()
            return nep6InFile.accounts.isNotEmpty()
        }

        fun getFromFileSystemAsFile(): File {
            try {
                val file = File(O3Wallet.appContext!!.filesDir, "O3.json")
                return file
            } catch(e: Exception)  {
                val scryptParams = ScryptParams(16384, 8, 8)
                val nep6 = NEP6("O3 Wallet", "1.0", scryptParams, mutableListOf())
                nep6.writeToFileSystem()
                return File(O3Wallet.appContext!!.filesDir, "O3.json")
            }
        }


        fun removeFromDevice() {
            val file = File(O3Wallet.appContext!!.filesDir, "O3.json")
            file.delete()
        }

        fun hasMultipleAccounts(): Boolean {
            try {
                val file = File(O3Wallet.appContext!!.filesDir, "O3.json")
                val wallet = Gson().fromJson<NEP6>(file.readText())
                if (wallet.accounts.size > 1) {
                    return true
                }
                return false
            } catch(e: Exception) {
                return false
            }
        }
    }
}
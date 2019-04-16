package network.o3.o3wallet.MultiWallet.Activate

import androidx.lifecycle.ViewModel
import neoutils.Neoutils
import network.o3.o3wallet.Account
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.PersistentStore


class ActivateMultiWalletViewModel: ViewModel() {
    var password: String = ""
    var encryptedKey: String = ""

    fun encryptKey(password: String, quickSwap: Boolean) {
        this.password = password
        val nep2 = Neoutils.neP2Encrypt(Account.getWallet().wif, password)
        encryptedKey = nep2.encryptedKey

        val nep6 = NEP6.getFromFileSystem()
        nep6.addEncryptedKey(nep2.address, "My O3 Wallet", encryptedKey)
        nep6.writeToFileSystem()
        nep6.makeNewDefault(nep2.address, password)
        Account.deleteKeyFromDevice()

        if (quickSwap) {
            PersistentStore.setHasQuickSwapEnabled(true, nep2.address, password)
        }
    }
}
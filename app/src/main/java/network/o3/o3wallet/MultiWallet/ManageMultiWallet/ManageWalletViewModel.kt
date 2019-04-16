package network.o3.o3wallet.MultiWallet.ManageMultiWallet

import androidx.lifecycle.ViewModel

class ManageWalletViewModel: ViewModel() {
    var key: String? = null
    var isDefault: Boolean = false
    var shouldNavToVerify = false
    lateinit var address: String
    lateinit var name: String
}
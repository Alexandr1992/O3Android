package network.o3.o3wallet.MultiWallet.AddNewMultiWallet

import android.arch.lifecycle.ViewModel

class AddNewMultiwalletViewModel: ViewModel() {
    var address: String = ""
    var encryptedKey: String = ""
    var wif: String = ""
    var password: String = ""
    var nickname: String = ""
}
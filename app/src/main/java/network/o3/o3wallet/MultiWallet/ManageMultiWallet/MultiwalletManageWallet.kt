package network.o3.o3wallet.MultiWallet.ManageMultiWallet

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.PrivateKeyFragment
import org.jetbrains.anko.layoutInflater

class MultiwalletManageWallet : AppCompatActivity() {

    var viewModel = ManageWalletViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val address = intent.getStringExtra("address")
        val encryptedKey = intent.getStringExtra("key")
        val name = intent.getStringExtra("name")
        val isDefault = intent.getBooleanExtra("isDefault", false)

        viewModel.address = address
        if (encryptedKey == "") {
            viewModel.key = null
        } else {
            viewModel.key = encryptedKey
        }
        viewModel.name = name
        viewModel.isDefault = isDefault
        setContentView(R.layout.multiwallet_manage_wallet_activity)
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_Dark, true)
        } else {
            theme.applyStyle(R.style.AppTheme_White, true)
        }
        return theme
    }
}

package network.o3.o3wallet.Onboarding.LoginNEP6

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3Platform.Dapp
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import java.util.logging.LoggingPermission

class LoginNEP6Activity : AppCompatActivity() {

    lateinit var mView: View
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mView = layoutInflater.inflate(R.layout.onboarding_login_nep6, null)
        recyclerView = mView.find(R.id.nep6_wallets_recycler)

        var accounts = NEP6.getFromFileSystem()
        Log.d("asdas", accounts.accounts.toString())
       // recyclerView.adapter = LoginNEP6Adapter(accounts)
        setContentView(mView)
    }

    class LoginNEP6Adapter(accounts: List<NEP6.Account>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var mAccounts = accounts

        val HEADER_VIEW_TYPE = 0
        val WALLET_VIEW_TYPE = 1

        override fun getItemCount(): Int {
            return 1 + mAccounts.count()
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0) {
                return HEADER_VIEW_TYPE
            } else {
                return WALLET_VIEW_TYPE
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position == 0) {
                (holder as HeaderHolder).bindHeader()
            } else {
                (holder as WalletHolder).bindWallet(mAccounts[position])
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)

            if (viewType == HEADER_VIEW_TYPE) {
                val view = layoutInflater.inflate(R.layout.onboarding_login_nep6_header, parent, false)
                return HeaderHolder(view)
            } else {
                val view = layoutInflater.inflate(R.layout.onboarding_login_nep_6_wallet_row, parent, false)
                return WalletHolder(view)
            }
        }

        class WalletHolder(v: View) : RecyclerView.ViewHolder(v) {
            private var view: View = v
            fun bindWallet(wallet: NEP6.Account) {
                view.find<TextView>(R.id.walletNameTextView).text = wallet.label
                //view.find<ImageView(R.id.walletLockIcon).image = walletLockIcon

            }
            companion object {
                private val wallet_key = "wallet"
            }
        }

        class HeaderHolder(v: View) : RecyclerView.ViewHolder(v) {
            private var view: View = v
            fun bindHeader() {}
        }
    }
}

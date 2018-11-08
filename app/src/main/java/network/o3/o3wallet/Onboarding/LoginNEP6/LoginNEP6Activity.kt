package network.o3.o3wallet.Onboarding.LoginNEP6

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.zxing.integration.android.IntentIntegrator
import network.o3.o3wallet.API.O3Platform.Dapp
import network.o3.o3wallet.Account
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.Onboarding.SelectingBestNode
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onClick
import java.util.logging.LoggingPermission

class LoginNEP6Activity : AppCompatActivity() {

    lateinit var mView: View
    lateinit var recyclerView: RecyclerView
    var deepLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mView = layoutInflater.inflate(R.layout.onboarding_login_nep6, null)
        recyclerView = mView.find(R.id.nep6_wallets_recycler)

        var accounts = NEP6.getFromFileSystem().accounts
        if (accounts.isEmpty()) {
            var account = NEP6.Account("", "My O3 Wallet", true, null)
            recyclerView.adapter = LoginNEP6Adapter(listOf(account), this)
        } else {
            recyclerView.adapter = LoginNEP6Adapter(NEP6.getFromFileSystem().accounts, this)
        }

        setContentView(mView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {

        } else {
            if (resultCode == -1) {
                Account.restoreWalletFromDevice()
                val intent = Intent(this, SelectingBestNode::class.java)
                if (deepLink != null) {
                    intent.putExtra("deepLink", deepLink!!)
                }
                startActivity(intent)
            }
        }
    }

    class LoginNEP6Adapter(accounts: List<NEP6.Account>, activity: Activity): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var mAccounts = accounts
        var mActivity = activity

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
                (holder as WalletHolder).bindWallet(mAccounts[position - 1])
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)

            if (viewType == HEADER_VIEW_TYPE) {
                val view = layoutInflater.inflate(R.layout.onboarding_login_nep6_header, parent, false)
                return HeaderHolder(view)
            } else {
                val view = layoutInflater.inflate(R.layout.onboarding_login_nep_6_wallet_row, parent, false)
                return WalletHolder(view, mActivity)
            }
        }

        class WalletHolder(v: View, a: Activity) : RecyclerView.ViewHolder(v) {
            private var view: View = v
            private var activity: Activity = a

            fun requestPasscode(v: View) {
                val mKeyguardManager =  v.context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (!mKeyguardManager.isKeyguardSecure) {
                    // Show a message that the user hasn't set up a lock screen.

                    Toast.makeText(v.context,
                            R.string.ALERT_no_passcode_setup,
                            Toast.LENGTH_LONG).show()
                    return
                } else {
                    val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
                    if (intent != null) {
                        activity.startActivityForResult(intent, 1)
                    }
                }
            }

            fun bindWallet(wallet: NEP6.Account) {
                view.find<TextView>(R.id.walletNameTextView).text = wallet.label
                if (wallet.isDefault) {
                    view.find<ImageView>(R.id.walletLockIcon).image = view.context.getDrawable(R.drawable.ic_unlocked)
                } else {
                    view.find<ImageView>(R.id.walletLockIcon).image = view.context.getDrawable(R.drawable.ic_locked)
                }

                view.onClick {
                    if (wallet.isDefault) {
                        requestPasscode(view)
                    }
                }
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

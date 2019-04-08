package network.o3.o3wallet.Onboarding.LoginNEP6

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import network.o3.o3wallet.*
import network.o3.o3wallet.MultiWallet.Activate.MultiwalletActivateActivity
import network.o3.o3wallet.MultiWallet.ManageMultiWallet.DialogUnlockEncryptedKey
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onClick

class LoginNEP6Activity : AppCompatActivity() {

    lateinit var mView: View
    lateinit var recyclerView: RecyclerView
    var deepLink: String? = null
    var quickSwapWallet: NEP6.Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mView = layoutInflater.inflate(R.layout.onboarding_login_nep6, null)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorLanding)
        recyclerView = mView.find(R.id.nep6_wallets_recycler)

        var accounts = NEP6.getFromFileSystem().getWalletAccounts()
        if (accounts.isEmpty()) {
            var account = NEP6.Account("", "My O3 Wallet", true, null)
            recyclerView.adapter = LoginNEP6Adapter(listOf(account), this)
        } else {
            recyclerView.adapter = LoginNEP6Adapter(accounts, this)
        }
        setContentView(mView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {

        } else {
            if (resultCode == -1) {
                if (quickSwapWallet!!.isDefault == false) {
                    NEP6.getFromFileSystem().makeNewDefault(quickSwapWallet!!.address,
                            Account.getStoredPassForNEP6Entry(quickSwapWallet!!.address))
                }
                Account.restoreWalletFromDevice()
                if (NEP6.nep6HasActivated() == false) {
                    val intent = Intent(this, MultiwalletActivateActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    val intent = Intent(this, MainTabbedActivity::class.java)
                    if (deepLink != null) {
                        intent.putExtra("deepLink", deepLink!!)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
    }

    class LoginNEP6Adapter(accounts: List<NEP6.Account>, activity: AppCompatActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                return HeaderHolder(view, mActivity)
            } else {
                val view = layoutInflater.inflate(R.layout.onboarding_login_nep_6_wallet_row, parent, false)
                return WalletHolder(view, mActivity)
            }
        }

        class WalletHolder(v: View, a: AppCompatActivity) : RecyclerView.ViewHolder(v) {
            private var view: View = v
            private var activity: AppCompatActivity = a

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
                    if (wallet.isDefault || PersistentStore.getHasQuickSwapEnabled(wallet.address)) {
                        (activity as LoginNEP6Activity).quickSwapWallet = wallet
                        requestPasscode(view)
                    } else {
                        val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()
                        neo2DialogFragment.decryptionSucceededCallback = { pass, _ ->
                            NEP6.getFromFileSystem().makeNewDefault(wallet.address, pass)
                            Account.restoreWalletFromDevice()
                            if (NEP6.nep6HasActivated() == false) {
                                val intent = Intent(activity, MultiwalletActivateActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                activity.startActivity(intent)
                            } else {
                                val intent = Intent(activity, MainTabbedActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                activity.startActivity(intent)
                            }
                        }

                        neo2DialogFragment.encryptedKey = wallet.key!!
                        neo2DialogFragment.showNow(activity.supportFragmentManager, "backupkey")
                    }
                }
            }
        }


        class HeaderHolder(v: View, a: AppCompatActivity) : RecyclerView.ViewHolder(v) {
            private var view: View = v
            private var activity: AppCompatActivity = a

            fun bindHeader() {}
        }
    }
}

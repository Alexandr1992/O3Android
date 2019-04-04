package network.o3.o3wallet.MultiWallet.ManageMultiWallet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.amplitude.api.Amplitude
import network.o3.o3wallet.AnalyticsService
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.json.JSONObject

class SwapWalletBottomSheet: RoundedBottomSheetDialogFragment() {
    lateinit var mView: View
    lateinit var listView: ListView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.multiwallet_manage_wallets_bottom_sheet, container, false)
        listView = mView.find(R.id.manage_wallets_list)
        listView.adapter = SwapWalletsAdapter(context!!, this)

        val headerView = layoutInflater.inflate(R.layout.settings_header_row, null)
        headerView.findViewById<TextView>(R.id.headerTextView).text = resources.getString(R.string.MULTIWALLET_swap_wallets)

        listView.addHeaderView(headerView)
        return mView
    }

    class SwapWalletsAdapter(context: Context, fragment: Fragment): BaseAdapter() {
        var mContext: Context
        var mFragment: Fragment
        init {
            mContext = context
            mFragment = fragment
        }

        override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View {
            val walletRow = mContext.layoutInflater.inflate(R.layout.multiwallet_manage_wallets_wallet_row, viewGroup, false)
            val account = getItem(position)
            walletRow.find<TextView>(R.id.walletNameTextView).text = account.label
            walletRow.find<TextView>(R.id.walletAddressTextView).text = account.address
            if (account.isDefault) {
                walletRow.find<ImageView>(R.id.walletLockIcon).image = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_unlocked, null)
            } else {
                walletRow.find<ImageView>(R.id.walletLockIcon).image = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_locked, null)
            }

            walletRow.onClick {
                val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()
                neo2DialogFragment.decryptionSucceededCallback = { pass, _ ->
                    (mFragment as RoundedBottomSheetDialogFragment).dismiss()
                    neo2DialogFragment.dismiss()
                    NEP6.getFromFileSystem().makeNewDefault(getItem(position).address, pass)
                    AnalyticsService.Wallet.logWalletUnlocked()
                }

                neo2DialogFragment.encryptedKey = getItem(position).key!!
                neo2DialogFragment.showNow(mFragment.activity!!.supportFragmentManager, "backupkey")
            }
            return walletRow
        }

        override fun getItem(position: Int): NEP6.Account {
            return NEP6.getFromFileSystem().getNonDefaultAccounts()[position]
        }

        override fun getCount(): Int {
            return NEP6.getFromFileSystem().getNonDefaultAccounts().count()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }

    companion object {
        fun newInstance(): SwapWalletBottomSheet {
            return SwapWalletBottomSheet()
        }
    }
}
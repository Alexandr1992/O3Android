package network.o3.o3wallet.Settings

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import network.o3.o3wallet.*
import network.o3.o3wallet.Dapp.DappContainerActivity
import network.o3.o3wallet.MultiWallet.ManageMultiWallet.ManageWalletsBottomSheet
import network.o3.o3wallet.Onboarding.OnboardingV2.OnboardingRootActivity
import org.jetbrains.anko.*


/**
 * Created by drei on 12/8/17.
 */

class SettingsAdapter(context: Context, fragment: SettingsFragment): BaseAdapter() {
    private val mContext: Context
    private var mFragment: SettingsFragment
    var settingsTitles = context.resources.getStringArray(R.array.SETTINGS_settings_menu_titles)
    var images =  listOf(R.drawable.ic_credit_card, R.drawable.ic_address_book, R.drawable.ic_wallet_swap,
            R.drawable.ic_currency, R.drawable.ic_moon, R.drawable.ic_moon)
    init {
        mContext = context
        mFragment = fragment
    }

    enum class CellType {
        HEADER, GENERAL, SECURITY_CENTER, HELP,
        VERSION, LOGOUT, ADVANCED

    }

    override fun getItem(position: Int): Pair<String, Int> {
        return Pair(settingsTitles[position - 1], images[position - 1])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
       if (BuildConfig.DEBUG) {
            return settingsTitles.count() + 1
       }
       return settingsTitles.count() - 1
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        if (position == 0) {
            val view = layoutInflater.inflate(R.layout.settings_header_row, viewGroup, false)
            view.findViewById<TextView>(R.id.headerTextView).text = mContext.resources.getString(R.string.SETTINGS_settings_title)
            return view
        }

        val view = layoutInflater.inflate(R.layout.settings_row_layout, viewGroup, false)
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = settingsTitles[position - 1]
        if (position == CellType.VERSION.ordinal) {
            val view = layoutInflater.inflate(R.layout.settings_version_row_layout, viewGroup, false)
            val version = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
            view.find<TextView>(R.id.titleTextView).text = mContext.resources.getString(R.string.SETTINGS_version, version)
            view.find<TextView>(R.id.subtitleTextView).text = mContext.resources.getString(R.string.SETTINGS_terms_and_privacy)
            view.setOnClickListener {
                getClickListenerForPosition(position)
            }
            return view
        }

        if (position == CellType.LOGOUT.ordinal) {
            titleTextView.textColor = ContextCompat.getColor(mContext, R.color.colorLoss)
        }

        view.findViewById<ImageView>(R.id.settingsIcon).image = mContext.getDrawable(images[position - 1])
        if (position == CellType.VERSION.ordinal) {
            view.findViewById<ImageView>(R.id.settingsIcon).image = null
        }

        view.setOnClickListener {
            getClickListenerForPosition(position)
        }
        return view
    }

    fun getClickListenerForPosition(position: Int) {
        if (position == CellType.GENERAL.ordinal) {
            val intent = Intent(mContext, GeneralSettingsActivity::class.java)
            ContextCompat.startActivity(mContext, intent, null)
        } else if (position == CellType.SECURITY_CENTER.ordinal) {
            val manageWalletsModal = ManageWalletsBottomSheet.newInstance()
            manageWalletsModal.show(mFragment.activity!!.supportFragmentManager, manageWalletsModal.tag)
            return
        } else if (position == CellType.HELP.ordinal) {
            var intent = Intent(mContext, SettingsHelpActivity::class.java)
            startActivity(mContext, intent, null)
        } else if (position == CellType.VERSION.ordinal) {
            val intent = Intent(mContext, DappContainerActivity::class.java)
            intent.putExtra("url", "https://o3.network/privacy/")
            mContext.startActivity(intent)
        } else if (position == CellType.LOGOUT.ordinal) {

            mContext.alert(O3Wallet.appContext!!.resources.getString(R.string.SETTINGS_logout_warning)) {
                yesButton {
                    mFragment.activity?.finish()
                    Account.deleteKeyFromDevice()
                    Account.deleteNEP6PassFromDevice()
                    NEP6.removeFromDevice()
                    val intent = Intent(mContext, OnboardingRootActivity::class.java)
                    ContextCompat.startActivity(mContext, intent, null)
                }
                noButton {

                }
            }.show()
        } else if (position == CellType.ADVANCED.ordinal) {
            val intent = Intent(mContext, AdvancedSettingsActivity::class.java)
            mFragment.startActivity(intent)
        }
    }
}
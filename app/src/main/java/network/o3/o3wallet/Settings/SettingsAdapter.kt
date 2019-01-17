package network.o3.o3wallet.Settings

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent
import android.net.Uri
import network.o3.o3wallet.*
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.commit451.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import network.o3.o3wallet.MultiWallet.Activate.MultiwalletActivateActivity
import network.o3.o3wallet.MultiWallet.ManageMultiWallet.ManageWalletsBottomSheet
import network.o3.o3wallet.Onboarding.OnboardingV2.OnboardingRootActivity
import network.o3.o3wallet.Settings.Help.HelpRootActivity
import org.jetbrains.anko.*
import zendesk.support.request.RequestActivity


/**
 * Created by drei on 12/8/17.
 */

class SettingsAdapter(context: Context, fragment: SettingsFragment): BaseAdapter() {
    private val mContext: Context
    private var mFragment: SettingsFragment
    var settingsTitles = context.resources.getStringArray(R.array.SETTINGS_settings_menu_titles)
    var images =  listOf(R.drawable.ic_credit_card, R.drawable.ic_address_book, R.drawable.ic_wallet_swap,
            R.drawable.ic_currency, R.drawable.ic_moon,
            R.drawable.ic_comment,
            R.drawable.ic_mobile_android_alt, R.drawable.ic_trash, R.drawable.ic_bug)
    init {
        mContext = context
        mFragment = fragment
    }

    enum class CellType {
        HEADER, BUY, CONTACTS, MULTIWALLET,
        CURRENCY, THEME,
        SUPPORT,
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
            val version = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
            titleTextView.text = mContext.resources.getString(R.string.SETTINGS_version, version)
            titleTextView.textColor = ContextCompat.getColor(mContext, R.color.colorSubtitleGrey)
        }

        if (position == CellType.LOGOUT.ordinal) {
            titleTextView.textColor = ContextCompat.getColor(mContext, R.color.colorLoss)
        }

        view.findViewById<ImageView>(R.id.settingsIcon).image = mContext.getDrawable(images[position - 1])
        if (position == CellType.VERSION.ordinal) {
            view.findViewById<ImageView>(R.id.settingsIcon).image = null
        }

        if (position == CellType.BUY.ordinal) {
            view.findViewById<ImageView>(R.id.settingsIcon).image?.setTint(ContextCompat.getColor(mContext, R.color.colorGain))
            view.findViewById<TextView>(R.id.titleTextView).textColor = ContextCompat.getColor(mContext, R.color.colorGain)
        }

        view.setOnClickListener {
            getClickListenerForPosition(position)
        }
        return view
    }

    fun getClickListenerForPosition(position: Int) {
        if (position == CellType.BUY.ordinal) {
            ModalBottomSheetDialogFragment.Builder()
                    .add(R.menu.buy_menu)
                    .show(mFragment.childFragmentManager, "buy_options")
        } else if (position == CellType.CURRENCY.ordinal) {
            val currencyModal = CurrencyFragment.newInstance()
            currencyModal.show((mContext as AppCompatActivity).supportFragmentManager, currencyModal.tag)
            return
        } else if (position == CellType.CONTACTS.ordinal) {
            val contactsModal = ContactsFragment.newInstance()
            val args = Bundle()
            args.putBoolean("canAddAddress", true)
            contactsModal.arguments = args
            contactsModal.show((mContext as AppCompatActivity).supportFragmentManager, contactsModal.tag)
            return
        } else if(position == CellType.THEME.ordinal) {
            val themeModal = ThemeModalFragment.newInstance()
            themeModal.show((mContext as AppCompatActivity).supportFragmentManager, themeModal.tag)
            return
        } else if (position == CellType.MULTIWALLET.ordinal) {
            if (NEP6.nep6HasActivated()) {
                val manageWalletsModal = ManageWalletsBottomSheet.newInstance()
                manageWalletsModal.show(mFragment.activity!!.supportFragmentManager, manageWalletsModal.tag)
                return
            } else {
                val intent = Intent(mContext, MultiwalletActivateActivity::class.java)
                startActivity(mContext, intent, null)
                return
            }
        } else if (position == CellType.SUPPORT.ordinal) {
            var intent = Intent(mContext, HelpRootActivity::class.java)
            startActivity(mContext, intent, null)
        } else if (position == CellType.LOGOUT.ordinal) {

            mContext.alert(O3Wallet.appContext!!.resources.getString(R.string.SETTINGS_logout_warning)) {
                yesButton {
                    mFragment.activity?.finish()
                    Account.deleteKeyFromDevice()
                    Account.deleteNEP6PassFromDevice()
                    NEP6.removeFromDevice()
                    val intent = Intent(mContext, OnboardingRootActivity::class.java)
                    startActivity(mContext, intent, null)
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
package network.o3.o3wallet.Settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk27.coroutines.onCheckedChange
import org.jetbrains.anko.sdk27.coroutines.onClick

class GeneralSettingsActivity : AppCompatActivity() {

    val needReloadCurrencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            (find<ListView>(R.id.generalSettingsList).adapter as GeneralSettingsAdapter).notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_general_activity)
        val list = find<ListView>(R.id.generalSettingsList)
        list.adapter = GeneralSettingsAdapter(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(needReloadCurrencyReceiver,
                IntentFilter("need-update-currency-event"))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(needReloadCurrencyReceiver)
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_Dark_NoTopBar, true)
        } else {
            theme.applyStyle(R.style.AppTheme_White_NoTopBar, true)
        }
        return theme
    }

    class GeneralSettingsAdapter(context: Context): BaseAdapter() {
        private val mContext: Context
        init {
            mContext = context
        }

        override fun getCount(): Int {
            return 3
        }

        override fun getItem(row: Int): Int {
            return row
        }

        override fun getItemId(row: Int): Long {
            return row.toLong()
        }

        override fun getView(row: Int, view: View?, viewGroup: ViewGroup?): View {
            if (row == 0 ) {
                val view = mContext.layoutInflater.inflate(R.layout.settings_row_no_icon_layout, null)
                view.find<TextView>(R.id.titleLabel).text = mContext.resources.getString(R.string.SETTINGS_Manage_Address_Book)
                view.onClick {
                    val contactsModal = ContactsFragment.newInstance()
                    val args = Bundle()
                    args.putBoolean("canAddAddress", true)
                    contactsModal.arguments = args
                    contactsModal.show((mContext as AppCompatActivity).supportFragmentManager, contactsModal.tag)
                }
                return view
            } else if (row == 1) {
                val view = mContext.layoutInflater.inflate(R.layout.settings_row_with_value_label_layout, null)
                view.onClick {
                    val currencyModal = CurrencyFragment.newInstance()
                    currencyModal.show((mContext as AppCompatActivity).supportFragmentManager, currencyModal.tag)
                }
                view.find<TextView>(R.id.titleLabel).text = mContext.resources.getString(R.string.SETTINGS_currency)
                view.find<TextView>(R.id.settingValueLabel).text = PersistentStore.getCurrency().toUpperCase()
                return view
            } else {
                val view = mContext.layoutInflater.inflate(R.layout.settings_row_no_icon_switch_layout, null)
                view.onClick {
                    view.find<Switch>(R.id.settingValueSwitch).isChecked = !view.find<Switch>(R.id.settingValueSwitch).isChecked
                }
                view.find<Switch>(R.id.settingValueSwitch).isChecked = PersistentStore.getTheme() == "Dark"
                view.find<Switch>(R.id.settingValueSwitch).onCheckedChange { buttonView, isChecked ->
                    if (isChecked) {
                        PersistentStore.setTheme("Dark")
                    } else {
                        PersistentStore.setTheme("Light")
                    }
                    (mContext as GeneralSettingsActivity).finish()
                    val intent = Intent("need-reload-theme")
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
                }
                view.find<TextView>(R.id.titleLabel).text = mContext.resources.getString(R.string.SETTINGS_Night_Mode)
                return view
            }
        }
    }
}

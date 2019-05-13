package network.o3.o3wallet.Settings

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import network.o3.o3wallet.Dapp.DappContainerActivity
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk27.coroutines.onClick
import zendesk.support.request.RequestActivity

class SettingsHelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_help_activity)
        find<ListView>(R.id.helpSettingsList).adapter = SettingsHelpAdapter(this)
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

    class SettingsHelpAdapter(context: Context): BaseAdapter() {
        val mContext: Context
        init {
            mContext = context
        }
        override fun getCount(): Int {
            return 5 //To change body of created functions use File | Settings | File Templates.
        }

        override fun getItem(row: Int): Int {
            return row
        }

        override fun getItemId(row: Int): Long {
            return row.toLong()
        }

        override fun getView(row: Int, parent: View?, viewGroup: ViewGroup?): View {
            when(row) {
                0 -> {
                    val view = mContext.layoutInflater.inflate(R.layout.settings_header_row, null)
                    view.find<TextView>(R.id.headerTextView).text = "Help Guides"
                    return view
                }
                1 -> {
                    val view = mContext.layoutInflater.inflate(R.layout.settings_row_no_icon_layout, null)
                    view.find<TextView>(R.id.titleLabel).text = "Crypto 101"
                    view.onClick {
                        val intent = Intent(mContext, DappContainerActivity::class.java)
                        intent.putExtra("url", "https://docs.o3.network/docs/privateKeysAddressesAndSignatures/?mode=embed")
                        mContext.startActivity(intent)
                    }
                    return view
                }
                2 -> {
                    val view = mContext.layoutInflater.inflate(R.layout.settings_header_row, null)
                    view.find<TextView>(R.id.headerTextView).text = "Get Support"
                    return view
                }
                3 -> {
                    val view = mContext.layoutInflater.inflate(R.layout.settings_row_no_icon_layout, null)
                    view.find<TextView>(R.id.titleLabel).text = "O3 community"
                    view.onClick {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://community.o3.network/"))
                        ContextCompat.startActivity(mContext, browserIntent, null)
                    }
                    return view
                }
                4 -> {
                    val view = mContext.layoutInflater.inflate(R.layout.settings_row_no_icon_layout, null)
                    view.find<TextView>(R.id.titleLabel).text = "Contact Us"
                    view.onClick {
                        RequestActivity.builder()
                                .withTags("Android", mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName)
                                .withRequestSubject("Android Support Ticket")
                                .show(mContext)
                    }
                    return view
                }
                else -> {
                    return View(mContext)
                }
            }
        }
    }
}

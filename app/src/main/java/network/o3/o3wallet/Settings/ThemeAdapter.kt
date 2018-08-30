package network.o3.o3wallet.Settings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import network.o3.o3wallet.*
import network.o3.o3wallet.Onboarding.LandingActivity
import org.jetbrains.anko.*

class ThemeAdapter(context: Context): BaseAdapter() {
    private val mContext: Context

    init {
        mContext = context
    }


    override fun getItem(position: Int): String {
        if (position == 0) {
            return "Light"
        } else {
            return "Dark"
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(R.layout.settings_theme_row, viewGroup, false)

        val textViewTheme = view.find<TextView>(R.id.textViewTheme)
        val themeCheckbox = view.find<CheckBox>(R.id.themeCheckbox)

        if (getItem(position) == PersistentStore.getTheme()) {
            textViewTheme.textColor = mContext.getColor(R.color.colorPrimary)
            themeCheckbox.visibility = View.VISIBLE
            themeCheckbox.isChecked = true
        } else {
            textViewTheme.textColor = mContext.getColorFromAttr(R.attr.defaultTextColor)
            themeCheckbox.visibility = View.INVISIBLE
            themeCheckbox.isChecked = false
        }


        if (position == 1) {
            textViewTheme.text = mContext.resources.getString(R.string.SETTINGS_Dark)
            view.setOnClickListener {
                PersistentStore.setTheme("Dark")
                notifyDataSetChanged()
                val intent = Intent("need-reload-theme")
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
            }
        } else {
            textViewTheme.text = mContext.resources.getString(R.string.SETTINGS_Light)
            view.setOnClickListener {
                PersistentStore.setTheme("Light")
                notifyDataSetChanged()
                val intent = Intent("need-reload-theme")
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
            }
        }

        return view
    }
}
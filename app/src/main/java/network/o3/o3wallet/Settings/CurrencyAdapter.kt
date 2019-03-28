package network.o3.o3wallet.Settings

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.getColorFromAttr
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor

/**
 * Created by drei on 12/11/17.
 */



/**
 * Created by drei on 12/8/17.
 */

class CurrencyAdapter(context: Context, fragment: CurrencyFragment): BaseAdapter() {

    private val mContext: Context
    private val mFragment: CurrencyFragment
    private val supportedCurrencies = arrayOf("usd", "jpy", "eur", "krw", "cny", "aud", "gbp", "rub", "cad", "myr", "inr")
    private val supportedSymbols = arrayOf("($)", "(¥)", "(€)", "(₩)", "(¥)", "($)", "(£)", "(\u20BD)", "($)", "(RM)", "(₹)")


    init {
        mContext = context
        mFragment = fragment
    }


    override fun getItem(position: Int): String {
        return supportedCurrencies[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return supportedCurrencies.size
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(R.layout.settings_currency_row, viewGroup, false)
        val textViewCurrency = view.find<TextView>(R.id.currencyTextView)
        val textViewSymbol = view.find<TextView>(R.id.currencySymbolTextView)
        val checkbox = view.findViewById<CheckBox>(R.id.currencyCheckBox)

        textViewCurrency.text = getItem(position).toUpperCase()
        textViewSymbol.text = supportedSymbols[position]

        if (getItem(position) == PersistentStore.getCurrency()) {
            textViewCurrency.textColor = mContext.getColor(R.color.colorPrimary)
            textViewSymbol.textColor = mContext.getColor(R.color.colorPrimary)
            checkbox.visibility = View.VISIBLE
            checkbox.isChecked = true
        } else {
            textViewCurrency.textColor = mContext.getColorFromAttr(R.attr.defaultTextColor)
            textViewSymbol.textColor = mContext.getColorFromAttr(R.attr.defaultTextColor)
            checkbox.visibility = View.INVISIBLE
            checkbox.isChecked = false
        }


        view.setOnClickListener {
            PersistentStore.setCurrency(getItem(position))
            notifyDataSetChanged()
            val intent = Intent("need-update-currency-event")
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
        }
        return view
    }
}
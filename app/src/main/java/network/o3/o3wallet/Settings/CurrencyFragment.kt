package network.o3.o3wallet.Settings


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import network.o3.o3wallet.R

class CurrencyFragment : BottomSheetDialogFragment() {

    val supportedCurrencies = arrayOf("usd", "jpy", "eur", "krw", "cny", "aud", "gbp", "rub", "cad")

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.settings_currency_fragment, null)
        dialog.setContentView(contentView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.settings_currency_fragment, container, false)


        val headerView = layoutInflater.inflate(R.layout.settings_header_row, null)
        headerView.findViewById<TextView>(R.id.headerTextView).text = resources.getString(R.string.SETTINGS_currency)

        val listView = view.findViewById<ListView>(R.id.currencyListView)
        listView.addHeaderView(headerView)

        val adapter = CurrencyAdapter(context!!, this)
        listView.adapter = adapter

        return view
    }

    companion object {
        fun newInstance(): CurrencyFragment {
            return CurrencyFragment()
        }
    }
}

package network.o3.o3wallet.Settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment


class ThemeModalFragment : RoundedBottomSheetDialogFragment() {
    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.settings_theme_fragment, null)
        dialog.setContentView(contentView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.settings_theme_fragment, container, false)


        val headerView = layoutInflater.inflate(R.layout.settings_header_row, null)
        headerView.findViewById<TextView>(R.id.headerTextView).text = resources.getString(R.string.SETTINGS_Theme)

        val listView = view.findViewById<ListView>(R.id.themeListView)
        listView.addHeaderView(headerView)

        val adapter = ThemeAdapter(context!!)
        listView.adapter = adapter

        return view
    }

    companion object {
        fun newInstance(): ThemeModalFragment {
            return ThemeModalFragment()
        }
    }
}

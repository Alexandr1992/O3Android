package network.o3.o3wallet.Onboarding.CreateKey.Backup


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.Settings.SettingsAdapter

class BackupOptionsFragment : RoundedBottomSheetDialogFragment() {
    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.onboarding_backup_options_fragment, null)
        dialog.setContentView(contentView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.onboarding_backup_options_fragment, container, false)
        val headerView = layoutInflater.inflate(R.layout.settings_header_row, null)
        headerView.findViewById<TextView>(R.id.headerTextView).text = resources.getString(R.string.ONBOARDING_backup_options)

        val listView = view.findViewById<ListView>(R.id.backupOptionsListView)
        listView.addHeaderView(headerView)

        val basicAdapter = BackupAdapter(this.context!!, this)
        listView.adapter = basicAdapter
        return view
    }

    companion object {
        fun newInstance(): BackupOptionsFragment {
            return BackupOptionsFragment()
        }
    }
}

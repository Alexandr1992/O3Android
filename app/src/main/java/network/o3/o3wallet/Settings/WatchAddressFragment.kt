/**
 * Created by drei on 12/11/17.
 */
package network.o3.o3wallet.Settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.ActivityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import network.o3.o3wallet.*
import network.o3.o3wallet.Wallet.Send.SendActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import android.support.v4.content.LocalBroadcastManager




/**
 * Created by drei on 12/8/17.
 */

class WatchAddressFragment : BottomSheetDialogFragment() {
    var adapter: WatchAddressAdapter? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.settings_fragment_watch_address, null)
        dialog.setContentView(contentView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.settings_fragment_watch_address, container, false)
        val headerView = layoutInflater.inflate(R.layout.settings_header_row, null)
        headerView.findViewById<TextView>(R.id.headerTextView).text = resources.getString(R.string.SETTINGS_watch_addresses)

        val listView = view.findViewById<ListView>(R.id.watchAddressListView)
        listView.addHeaderView(headerView)

        adapter = WatchAddressAdapter(this.context!!, this)
        listView.adapter = adapter
        return view
    }

    fun sendReloadDataIntent() {
        val intent = Intent("need-update-watch-address-event")
        LocalBroadcastManager.getInstance(this.context!!).sendBroadcast(intent)
    }

    fun showRemoveAlert(watchAddress: WatchAddress) {
        PersistentStore.removeWatchAddress(watchAddress.address, watchAddress.nickname)
        adapter?.updateData()
        sendReloadDataIntent()
    }

    fun sendToAddress(address: WatchAddress) {
        val intent: Intent = Intent(
                context,
                SendActivity::class.java
        )
        intent.putExtra("address",address.address)
        ActivityCompat.startActivity(context!!, intent, null)
    }

    val RELOAD_DATA = 1
    fun addNewAddress() {
        val intent = Intent(context, AddWatchAddress::class.java)
        startActivityForResult(intent,RELOAD_DATA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RELOAD_DATA) {
            adapter!!.updateData()
            sendReloadDataIntent()
        }
    }

    companion object {
        fun newInstance(): WatchAddressFragment {
            return WatchAddressFragment()
        }
    }
}
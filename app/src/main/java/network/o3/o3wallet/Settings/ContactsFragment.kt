package network.o3.o3wallet.Settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.ActivityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.view.*
import android.widget.ListView
import android.widget.TextView
import kotlinx.android.synthetic.main.settings_fragment_contacts.*
import network.o3.o3wallet.Contact
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import network.o3.o3wallet.Wallet.Send.SendActivity
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.yesButton


/**
 * Created by drei on 12/11/17.
 */

class ContactsFragment : RoundedBottomSheetDialogFragment() {
    var adapter: ContactsAdapter? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.settings_fragment_contacts, null)
        dialog.setContentView(contentView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.settings_fragment_contacts, container, false)
        val headerView = layoutInflater.inflate(R.layout.settings_header_row, null)
        headerView.findViewById<TextView>(R.id.headerTextView).text = resources.getString(R.string.WALLET_address_book)

        val listView = view.findViewById<ListView>(R.id.contactsListView)
        listView.addHeaderView(headerView)

        adapter = ContactsAdapter(this.context!!, this, arguments!!["canAddAddress"] as Boolean)
        listView?.adapter = adapter

        view.find<SwipeRefreshLayout>(R.id.contactsSwipeContainer).setColorSchemeResources(R.color.colorPrimary)
        view.find<SwipeRefreshLayout>(R.id.contactsSwipeContainer).onRefresh {
            adapter?.updateData()
            view.find<SwipeRefreshLayout>(R.id.contactsSwipeContainer).isRefreshing = false
        }
        return view
    }

    fun showRemoveAlert(contact: Contact) {
        alert(resources.getString(R.string.WALLET_remove_contact_warning),resources.getString(R.string.WALLET_remove_contact)) {
            yesButton {
                PersistentStore.removeContact(contact.address, contact.nickname)
                adapter?.updateData()
            }
        }.show()
    }

    fun sendToAddress(contact: Contact) {
        val intent: Intent = Intent(
                context,
                SendActivity::class.java
        )
        intent.putExtra("address", contact.address)
        intent.putExtra("payload", "")
        ActivityCompat.startActivity(context!!, intent, null)
    }

    val RELOAD_DATA = 5
    fun addNewAddress() {
        val intent = Intent(context, AddContact::class.java)
        startActivityForResult(intent, RELOAD_DATA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RELOAD_DATA) {
            adapter!!.updateData()
        }
    }

    companion object {
        fun newInstance(): ContactsFragment {
            return ContactsFragment()
        }
    }
}
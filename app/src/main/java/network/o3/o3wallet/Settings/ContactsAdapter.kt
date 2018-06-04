/**
 * Created by drei on 12/11/17.
 */
package network.o3.o3wallet.Settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.Contact
import network.o3.o3wallet.R
import android.content.ClipData
import android.content.ClipboardManager
import android.opengl.Visibility
import network.o3.o3wallet.Wallet.Send.SendActivity
import network.o3.o3wallet.Wallet.toast


/**
 * Created by drei on 12/8/17.
 */

class ContactsAdapter(context: Context, fragment: ContactsFragment, canAddAddress: Boolean): BaseAdapter() {

    private val mContext: Context
    private val mFragment: ContactsFragment
    private val mCanAddAddress: Boolean
    var contacts = PersistentStore.getContacts()

    init {
        mCanAddAddress = canAddAddress
        mContext = context
        mFragment = fragment
    }

    fun updateData() {
        contacts = PersistentStore.getContacts()
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Contact {
        return contacts[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        if (mCanAddAddress) {
            return contacts.count() + 1
        } else {
            return contacts.count()
        }
    }

    fun setOptionMenu(optionButton: ImageButton, position: Int) {
        optionButton.setOnClickListener {
            val popup = PopupMenu(mContext,optionButton)
            popup.menuInflater.inflate(R.menu.contact_menus,popup.menu)
            popup.setOnMenuItemClickListener {
                val itemId = it.itemId

                if (itemId == R.id.send_to_address) {
                    mFragment.sendToAddress(getItem(position))
                } else if (itemId == R.id.remove_address) {
                    mFragment.showRemoveAlert(getItem(position))
                } else if (itemId == R.id.copy_address) {
                    val clipboard = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val contact = getItem(position)
                    val clip = ClipData.newPlainText(mContext.resources.getString(R.string.WALLET_copied_address),contact.address)
                    clipboard.primaryClip = clip
                    mContext.toast(mContext.resources.getString(R.string.WALLET_copied_address))
                }
                true
            }
            popup.show()
        }
    }

    fun getAddressView(layoutInflater: LayoutInflater, position: Int, viewGroup: ViewGroup?): View {
        val view = layoutInflater.inflate(R.layout.settings_address_entry_row, viewGroup, false)
        val titleTextView = view.findViewById<TextView>(R.id.addressNickNameTextView)
        val subtitleTextView = view.findViewById<TextView>(R.id.addressTextView)
        val optionButton = view.findViewById<ImageButton>(R.id.contact_option_button)

        titleTextView.text = getItem(position).nickname
        subtitleTextView.text = getItem(position).address

        if (mCanAddAddress) {
            setOptionMenu(optionButton, position)
        } else {
            optionButton.visibility = View.GONE
            view.setOnClickListener {
                (mContext as SendActivity).addressTextView.text = subtitleTextView.text
                mFragment.dismiss()
            }
        }


        return view
    }

    fun getAddView(layoutInflater: LayoutInflater, viewGroup: ViewGroup?): View {
        val view = layoutInflater.inflate(R.layout.settings_add_address_row, viewGroup, false)
        view.findViewById<TextView>(R.id.footerTextView).text = ""
        view.findViewById<Button>(R.id.AddButton).text =  mContext.getString(R.string.WALLET_add_contact)
        view.findViewById<Button>(R.id.AddButton).setOnClickListener {
            mFragment.addNewAddress()
        }
        return view
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        if (position != count - 1 || !mCanAddAddress) {
            return getAddressView(layoutInflater, position, viewGroup)
        } else {
            return getAddView(layoutInflater, viewGroup)
        }
    }
}
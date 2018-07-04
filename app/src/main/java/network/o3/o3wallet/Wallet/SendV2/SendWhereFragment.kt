package network.o3.o3wallet.Wallet.SendV2


import android.arch.lifecycle.Observer
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.InputType
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.experimental.channels.Send
import neoutils.Neoutils
import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.ContactsFragment
import network.o3.o3wallet.Wallet.Send.afterTextChanged
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.textColor

class SendWhereFragment : Fragment() {
    lateinit var addressEditText: EditText
    lateinit var nicknameField: TextView
    lateinit var mView: View
    lateinit var nicknameBadge: ImageView
    lateinit var continueToSendReviewButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView =  inflater.inflate(R.layout.send_where_fragment, container, false)
        continueToSendReviewButton = mView.find(R.id.continueToSendReviewButton)
        continueToSendReviewButton.setOnClickListener {
            mView.findNavController().navigate(R.id.action_sendFragmentWhere_to_sendWhatFragment)
            (activity as SendV2Activity).sendViewModel.setSelectedAddress(addressEditText.text.toString().trim())
        }

        mView.find<Button>(R.id.contactsButton).setOnClickListener { showContactsModal() }
        mView.find<Button>(R.id.scanButton).setOnClickListener { scanAddressTapped() }
        mView.find<Button>(R.id.pasteButton).setOnClickListener { pasteTapped() }

        nicknameField = mView.find(R.id.nicknameField)
        nicknameBadge = mView.find(R.id.nicknameBadge)
        addressEditText = mView.find(R.id.addressEntryEditText)
        setupContactListener()
        setupOtherAddressListener()
        setupVerifiedListener()
        setupAddressEditText()

        return mView
    }

    fun setupAddressEditText() {
        addressEditText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        addressEditText.afterTextChanged { checkEnableSendButton() }
    }

    fun checkEnableSendButton() {
        continueToSendReviewButton.isEnabled = (Neoutils.validateNEOAddress(addressEditText.text.trim().toString()))
        (activity as SendV2Activity).sendViewModel.setSelectedAddress(addressEditText.text.trim().toString())
        if (continueToSendReviewButton.isEnabled) {
            val colorStateList = ColorStateList.valueOf(resources.getColor(R.color.colorGain))
            addressEditText.backgroundTintList = colorStateList
        } else {
            val colorStateList = ColorStateList.valueOf(resources.getColor(R.color.colorSubtitleGrey))
            addressEditText.backgroundTintList = colorStateList
        }
    }

    fun setupContactListener() {
        (activity as SendV2Activity).sendViewModel.getSelectedContact().observe(this, Observer { contact ->
            if (contact == null) {
                nicknameField.text = ""
            } else {
                nicknameField.text = contact.nickname
                nicknameBadge.visibility = View.GONE
                //don't set the adresss again if its already equal otherwise infinite loop
                if (addressEditText.text.toString() != contact.address) {
                    addressEditText.text = SpannableStringBuilder(contact.address)
                }
            }
        })
    }

    fun setupOtherAddressListener() {
        (activity as SendV2Activity).sendViewModel.getSelectedAddress().observe(this, Observer { address ->
            nicknameField.text = ""
            nicknameBadge.visibility = View.GONE
            (activity as SendV2Activity).sendViewModel.getVerifiedAddress(true, address!!).observe(this, Observer { verifiedAddress ->
                if (verifiedAddress == null) {

                } else {
                    nicknameField.text = verifiedAddress.displayName
                    nicknameBadge.image = resources.getDrawable(R.drawable.ic_verified)
                    nicknameBadge.visibility = View.VISIBLE
                }
            })
        })
    }

    fun setupVerifiedListener() {

    }

    fun showContactsModal() {
        val contactsModal = ContactsFragment.newInstance()
        val args = Bundle()
        args.putBoolean("canAddAddress", false)
        contactsModal.arguments = args
        contactsModal.show(activity!!.supportFragmentManager, contactsModal.tag)
    }

    fun scanAddressTapped() {
        val integrator = IntentIntegrator(activity)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
        integrator.setPrompt(resources.getString(R.string.SEND_scan_prompt_qr))
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    fun pasteTapped() {
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null) {
            val item = clip.getItemAt(0)
            addressEditText.text = SpannableStringBuilder(item.text.toString())
        }
    }
}

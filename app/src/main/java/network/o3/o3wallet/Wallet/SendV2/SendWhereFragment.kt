package network.o3.o3wallet.Wallet.SendV2


import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.google.zxing.integration.android.IntentIntegrator
import neoutils.Neoutils
import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.ContactsFragment
import network.o3.o3wallet.afterTextChanged
import network.o3.o3wallet.getColorFromAttr
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.textColor

class SendWhereFragment : Fragment() {
    lateinit var addressEditText: EditText
    lateinit var nicknameField: TextView
    lateinit var mView: View
    lateinit var nicknameBadge: ImageView
    lateinit var continueToSendReviewButton: Button
    lateinit var nnsStatusTextView: TextView
    lateinit var nnsLoadingIndicator: LottieAnimationView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView =  inflater.inflate(R.layout.send_where_fragment, container, false)
        continueToSendReviewButton = mView.find(R.id.continueToSendReviewButton)
        continueToSendReviewButton.setOnClickListener {
            if ((activity as SendV2Activity).sendViewModel.nnsResolvedAddress?.value != null) {
                (activity as SendV2Activity).sendViewModel.setSelectedAddress(nnsStatusTextView.text.toString())
            } else {
                (activity as SendV2Activity).sendViewModel.setSelectedAddress(addressEditText.text.toString().trim())
            }

            val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view = activity?.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            Handler().postDelayed({
                mView.findNavController().navigate(R.id.action_sendFragmentWhere_to_sendWhatFragment)
            }, 400)
        }

        mView.find<Button>(R.id.contactsButton).setOnClickListener { showContactsModal() }
        mView.find<Button>(R.id.scanButton).setOnClickListener { scanAddressTapped() }
        mView.find<Button>(R.id.pasteButton).setOnClickListener { pasteTapped() }

        nicknameField = mView.find(R.id.nicknameField)
        nicknameBadge = mView.find(R.id.nicknameBadge)
        addressEditText = mView.find(R.id.addressEntryEditText)
        setupContactListener()
        setupOtherAddressListener()
        setupNNSListener()
        setupAddressEditText()

        return mView
    }

    fun setupAddressEditText() {
        addressEditText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        addressEditText.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            source.toString().filterNot { it.isWhitespace() }
        })
        addressEditText.afterTextChanged { checkEnableSendButton() }
        if ((activity as SendV2Activity).sendViewModel.selectedAddress?.value != null) {
            addressEditText.text = SpannableStringBuilder((activity as SendV2Activity).sendViewModel.selectedAddress?.value!!)
        }
    }

    fun validateNormalAddress() {
        (activity as SendV2Activity).sendViewModel.nnsResolvedAddress?.value = null
        (activity as SendV2Activity).sendViewModel.nnsName = ""
        continueToSendReviewButton.isEnabled = (Neoutils.validateNEOAddress(addressEditText.text.trim().toString()))
        (activity as SendV2Activity).sendViewModel.setSelectedAddress(addressEditText.text.trim().toString())
        if (continueToSendReviewButton.isEnabled) {
            val colorStateList = ColorStateList.valueOf(context!!.getColor(R.color.colorGain))
            addressEditText.backgroundTintList = colorStateList
        } else {
            val colorStateList = ColorStateList.valueOf(context!!.getColorFromAttr(R.attr.defaultSubtitleTextColor))
            addressEditText.backgroundTintList = colorStateList
        }
    }

    fun validateNNSAddress(nnsAddr: String) {
        (activity as SendV2Activity).sendViewModel.loadResolvedNNS(nnsAddr)
    }

    fun checkEnableSendButton() {
        if(addressEditText.text.trim().toString().endsWith(".neo")) {
            validateNNSAddress(addressEditText.text.trim().toString())
        } else {
            validateNormalAddress()
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
            if(addressEditText.text.toString() != address && !addressEditText.text.toString().endsWith(".neo")) {
                addressEditText.text = SpannableStringBuilder(address)
            }

            (activity as SendV2Activity).sendViewModel.getVerifiedAddress(true, address!!).observe(this, Observer { verifiedAddress ->
                if (verifiedAddress == null) {

                } else {
                    nicknameField.text = verifiedAddress.displayName
                    nicknameBadge.image = context!!.getDrawable(R.drawable.ic_verified)
                    nicknameBadge.visibility = View.VISIBLE
                }
            })
        })
    }

    fun setupNNSListener() {
        nnsStatusTextView = mView.find(R.id.nnsAddressTextView)
        nnsLoadingIndicator = mView.find(R.id.nnsLoadingIndicator)
        nnsStatusTextView.visibility = View.INVISIBLE
        (activity as SendV2Activity).sendViewModel.getResolvedNNS().observe(this, Observer { address ->
            if(!addressEditText.text.toString().endsWith(".neo")) {
                nnsStatusTextView.visibility = View.INVISIBLE
                return@Observer
            }
            nnsStatusTextView.visibility = View.VISIBLE
            if (address != null) {
                nnsStatusTextView.text = address
                nnsStatusTextView.textColor = context!!.getColor(R.color.colorGain)
                (activity as SendV2Activity).sendViewModel.selectedAddress?.postValue(address)
                continueToSendReviewButton.isEnabled = true
            } else {
                nnsStatusTextView.text = resources.getString(R.string.SEND_could_not_resolve)
                nnsStatusTextView.textColor = context!!.getColor(R.color.colorLoss)
            }
        })

        (activity as SendV2Activity).sendViewModel.getNNSLoadingStatus().observe(this, Observer { isLoading ->
            if (isLoading!!) {
                nnsLoadingIndicator.visibility = View.VISIBLE
            } else {
                nnsLoadingIndicator.visibility = View.INVISIBLE
            }
        })
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

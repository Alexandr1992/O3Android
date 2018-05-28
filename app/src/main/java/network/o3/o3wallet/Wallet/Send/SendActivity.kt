package network.o3.o3wallet.Wallet.Send

import android.app.Activity
import android.app.Fragment
import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.NavUtils
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import android.widget.*
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.wallet_activity_send.*
import neoutils.Neoutils
import neoutils.Neoutils.parseNEP9URI
import neoutils.Neoutils.validateNEOAddress
import network.o3.o3wallet.API.NEO.AccountAsset
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.TransferableAssets
import network.o3.o3wallet.Account
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.ContactsFragment
import network.o3.o3wallet.Wallet.toast
import network.o3.o3wallet.Wallet.toastUntilCancel
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton

class SendActivity: AppCompatActivity() {

    lateinit var addressTextView: TextView
    lateinit var amountTextView: TextView
    lateinit var selectedAssetTextView: TextView
    lateinit var sendButton: Button
    lateinit var pasteAddressButton: Button
    lateinit var scanAddressButton: Button
    lateinit var view: View

    var ownedAssets: ArrayList<TransferableAsset> = arrayListOf()

    var isNativeAsset = true
    //if native asset this refers to assetid, otherwise tokenhash
    var assetID = NeoNodeRPC.Asset.NEO.assetID()
    var shortName = "NEO"

    val ARG_REVEAL_SETTINGS: String = "arg_reveal_settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wallet_activity_send)

        this.title = resources.getString(R.string.SEND_send)
        view = findViewById<View>(R.id.root_layout)
        addressTextView = findViewById<EditText>(R.id.addressTextView)
        amountTextView = findViewById<EditText>(R.id.amountTextView)
        sendButton = findViewById<Button>(R.id.sendButton)
        pasteAddressButton = findViewById<Button>(R.id.pasteAddressButton)
        scanAddressButton = findViewById<Button>(R.id.scanAddressButton)
        selectedAssetTextView = findViewById<TextView>(R.id.selectedAssetTextView)

        amountTextView.keyListener = DigitsKeyListener.getInstance("0123456789")
        selectedAssetTextView.text = shortName.toUpperCase()
        addressTextView.afterTextChanged { checkEnableSendButton() }
        amountTextView.afterTextChanged { checkEnableSendButton() }
        selectedAssetTextView.setOnClickListener { displayAssets() }

        sendButton.isEnabled = false
        sendButton.setOnClickListener { sendTapped() }

        pasteAddressButton.setOnClickListener { pasteAddressTapped() }
        scanAddressButton.setOnClickListener { scanAddressTapped() }
        fromAddressButton.setOnClickListener { addFromAddressTapped() }

        val extras = intent.extras
        val address = extras.getString("address")
        O3PlatformClient().getTransferableAssets(Account.getWallet()?.address!!) {
            ownedAssets = it.first?.assets ?: arrayListOf()
        }

        if (address != "") {
            addressTextView.text = address
            amountTextView.requestFocus()
            showFoundContact(address)
        }
    }

    fun showFoundContact(address: String) {
        val contacts = PersistentStore.getContacts()
        val foundContact = contacts.find { it.address == address }
        val toLabel = findViewById<TextView>(R.id.sendToLabel)
        if (foundContact != null) {
            toLabel.text = String.format(resources.getString(R.string.WALLET_to_formatted), foundContact.nickname)
        } else {
            toLabel.text = String.format(resources.getString(R.string.WALLET_to_formatted), "")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun checkEnableSendButton() {
        showFoundContact(addressTextView.text.trim().toString())
        sendButton.isEnabled = (addressTextView.text.trim().count() > 0 && amountTextView.text.count() > 0)
    }

    private fun displayAssets() {
        val assetSelectorSheet = AssetSelectionBottomSheet()
        assetSelectorSheet.assets = ownedAssets
        assetSelectorSheet.show(this.supportFragmentManager, assetSelectorSheet.tag)
    }


    fun updateSelectedAsset() {
        selectedAssetTextView.text = shortName
        if (shortName != "NEO") {
            amountTextView.keyListener = DigitsKeyListener.getInstance("0123456789.")
        } else {
            amountTextView.keyListener = DigitsKeyListener.getInstance("0123456789")
        }
    }

    private fun verifyPassCode() {
        val mKeyguardManager =  getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(this,
                    resources.getString(R.string.ALERT_no_passcode_setup),
                    Toast.LENGTH_LONG).show()
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                startActivityForResult(intent, 1)
            }
        }
    }

    private fun send() {
        //validate field
        val address = addressTextView.text.trim().toString()
        var amount = amountTextView.text.trim().toString().toDouble()

        if (amount == 0.0) {
            baseContext.toast(resources.getString(R.string.SEND_amount_must_be_nonzero))
            return
        }

        sendButton.isEnabled = false
        if (isNativeAsset) {
            sendNativeAsset(address, amount)
        } else {
            sendTokenAsset(address, amount)
        }
    }

    private fun sendNativeAsset(address: String, amount: Double) {
        val wallet = Account.getWallet()
        val toast = baseContext.toastUntilCancel(resources.getString(R.string.SEND_sending_in_progress))
        var toSendAsset: NeoNodeRPC.Asset
        if (shortName == "NEO") {
            toSendAsset = NeoNodeRPC.Asset.NEO
        } else {
            toSendAsset = NeoNodeRPC.Asset.GAS
        }

        NeoNodeRPC(PersistentStore.getNodeURL()).sendNativeAssetTransaction(wallet!!, toSendAsset, amount, address, null) {
            runOnUiThread {
                toast.cancel()
                val error = it.second
                val success = it.first
                if (success == true) {
                    Answers().logCustom(CustomEvent("Native Asset Sent")
                            .putCustomAttribute("Asset Name", toSendAsset.name)
                            .putCustomAttribute("Amount", amount))

                    baseContext!!.toast(resources.getString(R.string.SEND_sent_successfully))
                    Handler().postDelayed(Runnable {
                        finish()
                    }, 1000)
                } else {
                    this.checkEnableSendButton()
                    val message = resources.getString(R.string.SEND_send_error)
                    val snack = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    snack.setAction("Close") {
                        finish()
                    }
                    snack.show()
                }
            }
        }
    }

    private fun sendTokenAsset(address: String, amount: Double) {
        val wallet = Account.getWallet()
        val toast = baseContext.toastUntilCancel(resources.getString(R.string.SEND_sending_in_progress))
        val gasIndex  = ownedAssets.indices.find { ownedAssets[it].name.toUpperCase() == "GAS"}
        if (gasIndex == null || gasIndex == -1 || ownedAssets[gasIndex].value.toDouble() == 0.0) {
            baseContext.toast(resources.getString(R.string.SEND_Not_Enough_GAS_For_Token_Send))
            return
        }
        NeoNodeRPC(PersistentStore.getNodeURL()).sendNEP5Token(wallet!!, assetID, wallet.address, address, amount) {
            runOnUiThread {
                toast.cancel()
                val error = it.second
                val success = it.first
                if (success == true) {
                    Answers().logCustom(CustomEvent("Native Asset Sent")
                            .putCustomAttribute("Asset Name", selectedAssetTextView.text.toString())
                            .putCustomAttribute("Amount", amount))
                    baseContext!!.toast(resources.getString(R.string.SEND_sent_successfully))
                    Handler().postDelayed(Runnable {
                        finish()
                    }, 1000)
                } else {
                    this.checkEnableSendButton()
                    val message = resources.getString(R.string.SEND_send_error)
                    val snack = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    snack.setAction(resources.getString(R.string.ALERT_Close)) {
                        finish()
                    }
                    snack.show()
                }
            }
        }
    }

    private fun sendTapped() {
        this.hideKeyboard()
        //validate field
        val address = addressTextView.text.trim().toString()
        var amount = amountTextView.text.trim().toString().toDouble()

        if (amount == 0.0) {
            baseContext.toast(resources.getString(R.string.SEND_amount_must_be_nonzero))
            return
        }

        NeoNodeRPC(PersistentStore.getNodeURL()).validateAddress(address) {
            if (it.second != null || it.first == false) {
                runOnUiThread {
                    alert(resources.getString(R.string.ALERT_invalid_neo_address), resources.getString(R.string.ALERT_error)) {
                        yesButton {
                            addressTextView.requestFocus()
                        }
                    }.show()
                }
            } else {
                runOnUiThread {
                    alert(resources.getString(R.string.SEND_send_confirmation, amount.toString(),
                            this.shortName.toUpperCase(), address)) {
                        positiveButton(resources.getString(R.string.SEND_send)) {
                            verifyPassCode()
                        }
                        negativeButton(resources.getString(R.string.ALERT_cancel)) {

                        }
                    }.show()
                }
            }
        }
    }

    private fun pasteAddressTapped() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null) {
            val item = clip.getItemAt(0)
            addressTextView.text = item.text.toString()
        }
    }

    private fun addFromAddressTapped() {
        val contactsModal = ContactsFragment.newInstance()
        val args = Bundle()
        args.putBoolean("canAddAddress", false)
        contactsModal.arguments = args
        contactsModal.show(supportFragmentManager, contactsModal.tag)
    }


    fun scanAddressTapped() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
        integrator.setPrompt(resources.getString(R.string.SEND_scan_prompt_qr))
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
        } else {
            if (requestCode == 1) {
                send()
                return
            }
        }

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
            } else if (Neoutils.validateNEOAddress(result.contents.trim())) {
                addressTextView.text = result.contents.trim()
            } else try {
                val uri = parseNEP9URI(result.contents.trim())
                addressTextView.text = uri.to
                amountTextView.text = uri.amount.toString()
                isNativeAsset = true
                assetID = uri.assetID
                if (assetID.contains(NeoNodeRPC.Asset.NEO.assetID())) {
                    shortName = "NEO"
                } else {
                    shortName = "GAS"
                }
                updateSelectedAsset()

            } catch (e: Exception) {
            }
        }
    }
}

fun TextView.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}


fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.hideKeyboard() {
    activity.hideKeyboard(view)
}

fun Activity.hideKeyboard() {
    hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
}
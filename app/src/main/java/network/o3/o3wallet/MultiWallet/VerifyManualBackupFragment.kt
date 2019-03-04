package network.o3.o3wallet.MultiWallet


import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.Account
import network.o3.o3wallet.MultiWallet.ManageMultiWallet.MultiwalletManageWallet
import network.o3.o3wallet.PersistentStore

import network.o3.o3wallet.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.textColor

class VerifyManualBackupFragment : Fragment() {
    lateinit var qrImageView: ImageView
    lateinit var qrTypeImageView: ImageView
    lateinit var keyTextView: TextView
    lateinit var descriptionTextView: TextView
    lateinit var useRawSwitch: Switch
    lateinit var screenshotCheckBox: CheckBox
    lateinit var byHandCheckBox: CheckBox
    lateinit var otherCheckBox: CheckBox

    lateinit var verifyButton: Button
    lateinit var cancelButton: Button

    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.multiwallet_verify_manual_backup_fragment, container, false)
        screenshotCheckBox = mView.find(R.id.screenshotCheckbox)
        byHandCheckBox = mView.find(R.id.byHandCheckbox)
        otherCheckBox = mView.find(R.id.otherCheckbox)

        qrImageView = mView.find(R.id.QRCodeImageView)
        qrTypeImageView = mView.find(R.id.qrTypeImageView)
        descriptionTextView = mView.find(R.id.keyDescriptionTextView)

        keyTextView = mView.find(R.id.keyTextView)
        useRawSwitch = mView.find(R.id.useRawSwitch)

        cancelButton = mView.find(R.id.cancelButton)
        verifyButton = mView.find(R.id.verifyButton)

        initiateSwitch()
        setQRData()
        initiateCheckboxes()
        initiateVerifyButton()
        initiateCancelButton()
        return mView
    }

    fun checkEnableVerifyButton() {
        if (screenshotCheckBox.isChecked || byHandCheckBox.isChecked || otherCheckBox.isChecked) {
            verifyButton.isEnabled = true
            verifyButton.textColor = ContextCompat.getColor(mView.context!!, R.color.colorPrimary)
        } else {
            verifyButton.isEnabled = false
            verifyButton.textColor = ContextCompat.getColor(mView.context!!, R.color.colorSubtitleGrey)
        }
    }

    fun initiateCancelButton() {
        cancelButton.onClick { activity?.onBackPressed() }
    }

    fun initiateVerifyButton() {
        verifyButton.onClick {
            val states = mutableListOf<PersistentStore.VerificationType>()
            val address = (activity as MultiwalletManageWallet).viewModel.address
            if (screenshotCheckBox.isChecked) {
                states.add(PersistentStore.VerificationType.SCREENSHOT)
            }
            if (otherCheckBox.isChecked) {
                states.add(PersistentStore.VerificationType.OTHER)
            }
            if (byHandCheckBox.isChecked) {
                states.add(PersistentStore.VerificationType.BYHAND)
            }
            PersistentStore.setManualVerificationType(address, states.toList())
            if (states.isNotEmpty()) {
                PersistentStore.setHasDismissedBackup(true)
            }
            activity?.onBackPressed()
        }

        if (screenshotCheckBox.isChecked == false &&
                otherCheckBox.isChecked == false && byHandCheckBox.isChecked == false) {
            verifyButton.isEnabled = false
            verifyButton.textColor = ContextCompat.getColor(mView.context!!, R.color.colorSubtitleGrey)
        }
    }

    fun verifyPasscode() {
        val mKeyguardManager = context!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(this.context,
                    resources.getString(R.string.ALERT_no_passcode_setup),
                    Toast.LENGTH_LONG).show()
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                activity!!.startActivityForResult(intent, 1)
            }
        }
    }

    fun initiateCheckboxes() {
        val address = (activity as MultiwalletManageWallet).viewModel.address
        val verifyState = PersistentStore.getManualVerificationType(address)
        for (state in verifyState) {
            if (state == PersistentStore.VerificationType.BYHAND) {
                byHandCheckBox.isChecked = true
            } else if (state == PersistentStore.VerificationType.OTHER){
                otherCheckBox.isChecked = true
            } else if (state == PersistentStore.VerificationType.SCREENSHOT) {
                screenshotCheckBox.isChecked = true
            }
        }

        screenshotCheckBox.onClick { checkEnableVerifyButton() }
        byHandCheckBox.onClick { checkEnableVerifyButton() }
        otherCheckBox.onClick { checkEnableVerifyButton() }
    }


    fun setQRData() {
        if (useRawSwitch.isChecked) {
            verifyPasscode()
        } else {
            descriptionTextView.text = resources.getString(R.string.MULTIWALLET_encrypted_description)
            val bitmap = QRCode.from((activity as MultiwalletManageWallet).viewModel.key).withSize(1000, 1000).bitmap()
            qrImageView.setImageBitmap(bitmap)
            keyTextView.text = (activity as MultiwalletManageWallet).viewModel.key
        }
    }

    fun initiateSwitch() {
        useRawSwitch.onClick {
            setQRData()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == -1) {
            if ((activity as MultiwalletManageWallet).viewModel.isDefault) {
                find<TextView>(R.id.keyDescriptionTextView).text =
                        resources.getString(R.string.MULTIWALLET_raw_key_description)
                val bitmap = QRCode.from(Account.getWallet().wif).withSize(1000, 1000).bitmap()
                find<ImageView>(R.id.QRCodeImageView).setImageBitmap(bitmap)
                find<TextView>(R.id.keyTextView).text = Account.getWallet().wif
            } else {
                alert { resources.getString(R.string.MULTIWALLET_password_protected) }
                find<Switch>(R.id.useRawSwitch).isChecked = false
            }
        } else {
            find<Switch>(R.id.useRawSwitch).isChecked = false
        }
    }
}

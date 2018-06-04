package network.o3.o3wallet


import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import kotlinx.android.synthetic.main.dialog_backup_key_fragment.*
import network.o3.o3wallet.Settings.PrivateKeyFragment
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor

class DialogBackupKeyFragment : DialogFragment() {
    private lateinit var allDoneButton: Button
    private lateinit var showMyKeyButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        dialog.setCanceledOnTouchOutside(false)
        return inflater.inflate(R.layout.dialog_backup_key_fragment, container, false)
    }

    fun initCheckboxes(view: View) {
        val checkBox1 = view.find<CheckBox>(R.id.backupCheckBox1)
        val checkBox2 = view.find<CheckBox>(R.id.backupCheckBox2)
        val checkBox3 = view.find<CheckBox>(R.id.backupCheckBox3)

        checkBox2.isEnabled = false
        checkBox3.isEnabled = false

        checkBox1.setOnClickListener {
            checkBox1.isClickable = false
            checkBox2.isEnabled = true
            checkBox2.textColor = ContextCompat.getColor(context!!, R.color.colorBlack)
        }

        checkBox2.setOnClickListener {
            checkBox2.isClickable = false
            checkBox3.isEnabled = true
            checkBox3.textColor = ContextCompat.getColor(context!!, R.color.colorBlack)
        }

        checkBox3.setOnClickListener {
            checkBox3.isClickable = false
            allDoneButton.isEnabled = true
            allDoneButton.textColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
        }
    }

    private fun setupShowKey() {
        val mKeyguardManager =  context?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(context,
                    O3Wallet.appContext!!.resources.getString(R.string.ALERT_no_passcode_setup),
                    Toast.LENGTH_LONG).show()
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                startActivityForResult( intent, 0, null)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        allDoneButton = view.find(R.id.backupDoneButton)
        allDoneButton.isEnabled = false

        allDoneButton.setOnClickListener {
            dismiss()
        }

        showMyKeyButton = view.find<Button>(R.id.showMyKeyButton)
        showMyKeyButton.setOnClickListener {
            setupShowKey()
        }

        initCheckboxes(view)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            if (resultCode == -1) {
                val privateKeyModal = PrivateKeyFragment.newInstance()
                privateKeyModal.show((context as AppCompatActivity).supportFragmentManager, privateKeyModal.tag)
            }
        }
    }


    companion object {
        fun newInstance(): DialogBackupKeyFragment {
            return DialogBackupKeyFragment()
        }
    }
}

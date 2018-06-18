package network.o3.o3wallet.Onboarding.CreateKey.Backup


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import network.o3.o3wallet.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import android.content.Intent
import android.R.attr.path
import android.graphics.Bitmap
import android.net.Uri
import com.github.salomonbrys.kotson.toJson
import neoutils.Neoutils
import network.o3.o3wallet.core.Utils
import java.io.File
import java.net.URI
import android.support.v4.content.FileProvider
import net.glxn.qrgen.android.QRCode
import java.io.FileOutputStream


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class Nep2BackupCardFragment : Fragment() {

    var position = 0
    var passwordHidden = true
    lateinit var passwordField: EditText
    lateinit var backButton: Button
    lateinit var continueButton: Button
    lateinit var headerTextView: TextView

    var BACKUP_REQUEST_CODE = 101

    fun initiatePasswordEnterCard() {
        headerTextView.text = getString(R.string.ONBOARDING_enter_a_password_description)
        backButton.setOnClickListener {
            activity?.finish()
        }
        continueButton.setOnClickListener {
            if (passwordField.text.length < 8) {
                alert (resources.getString(R.string.ONBOARDING_password_length_error)) {
                    yesButton { resources.getString(R.string.ALERT_OK_Confirm_Button) }
                }.show()
            } else {
                (activity as Nep2BackupActivity).enteredPassword = passwordField.text.toString()
                activity?.find<ViewPager>(R.id.passwordViewPager)?.setCurrentItem(1, true)
            }
        }
    }

    fun initiatePasswordReenterCard() {
        headerTextView.text = getString(R.string.ONBOARDING_reenter_description)
        backButton.setOnClickListener {
            activity?.find<ViewPager>(R.id.passwordViewPager)?.setCurrentItem(0, true)
        }

        continueButton.setOnClickListener {
            if (passwordField.text.toString() == (activity as Nep2BackupActivity).enteredPassword ) {
                sendBackupEmail(passwordField.text.toString())
            } else {
                alert (resources.getString(R.string.ONBOARDING_password_mismatch_error)) {
                    yesButton { resources.getString(R.string.ALERT_OK_Confirm_Button) }
                }.show()
            }
        }
    }

    fun sendBackupEmail(password: String) {
        var wif = (activity as Nep2BackupActivity).wif
        val nep2 = Neoutils.neP2Encrypt(wif, password)
        val nep6 = Neoutils.generateNEP6FromEncryptedKey("O3 Wallet", "O3 Wallet", nep2.address, nep2.encryptedKey)


        val tmpDir = File(context?.filesDir?.absolutePath + "/tmp")
        tmpDir.mkdirs()
        val fileJson = File(tmpDir, "o3wallet.json")
        fileJson.writeText(nep6)

        val fileImage = File(tmpDir, "o3wallet.png")
        val fout = FileOutputStream(fileImage)
        val bitmap = QRCode.from(wif).withSize(2000, 2000).bitmap()
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fout)

        val imageUri = FileProvider.getUriForFile(context!!, "network.o3.o3wallet", fileImage)
        val contentUri = FileProvider.getUriForFile(context!!, "network.o3.o3wallet", fileJson)


        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.setType("message/rfc822")
        intent.putExtra(Intent.EXTRA_SUBJECT, "O3 Wallet Encrypted Backup")
        intent.putExtra(Intent.EXTRA_TEXT, String.format(resources.getString(R.string.ONBOARDING_backup_email_body), nep2.encryptedKey))
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(imageUri, contentUri))

        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)
        startActivityForResult(Intent.createChooser(intent, "Send Email using:"), BACKUP_REQUEST_CODE)

        fout.flush()
        fout.close()
        tmpDir.delete()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        position = arguments!!.get("position") as Int
        val view = inflater.inflate(R.layout.onboarding_nep2_backup_card_fragment, container, false)
        backButton = view.find(R.id.backNep2Button)
        continueButton = view.find(R.id.continueNep2Button)
        passwordField = view.find(R.id.passwordEditText)
        headerTextView = view.find(R.id.nep2HeaderTextView)


        if (position == 0) {
            initiatePasswordEnterCard()
        } else {
            initiatePasswordReenterCard()
        }

        val hidePasswordButton = view.find<ImageButton>(R.id.hidePasswordButton)

        passwordField.transformationMethod = PasswordTransformationMethod()
        hidePasswordButton.setOnClickListener {
            passwordHidden = !passwordHidden
            if (passwordHidden) {
                passwordField.transformationMethod = PasswordTransformationMethod()
                hidePasswordButton.alpha = 0.3f
            } else {
                passwordField.transformationMethod = null
                hidePasswordButton.alpha = 1.0f
            }
            passwordField.setSelection(passwordField?.text?.length ?: 0)
        }

        return view
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == BACKUP_REQUEST_CODE) {
            val paperDialog = DialogCompletedBackupFragment.newInstance()
            var args = Bundle()
            args.putString("wif", (activity as Nep2BackupActivity).wif)
            args.putString("title", resources.getString(R.string.ONBOARDING_encrypted_backup_dialog_title))
            args.putString("subtitle", resources.getString(R.string.ONBOARDING_encrypted_backup_dialog_subtitle))
            args.putString("buttonTitle", resources.getString(R.string.ONBOARDING_encrypted_backup_dialog_button))
            paperDialog.arguments = args
            paperDialog.show(activity!!.fragmentManager, paperDialog.tag)
        }
    }

    companion object {
        fun newInstance(position: Int): Nep2BackupCardFragment {
            val args = Bundle()
            args.putInt("position", position)
            val fragment = Nep2BackupCardFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

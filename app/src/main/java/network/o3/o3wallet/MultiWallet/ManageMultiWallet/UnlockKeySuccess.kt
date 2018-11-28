package network.o3.o3wallet.MultiWallet.ManageMultiWallet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.MultiWallet.Activate.MultiwalletActivateActivity
import network.o3.o3wallet.MultiWallet.AddNewMultiWallet.AddNewMultiwalletRootActivity
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import java.io.File
import java.io.FileOutputStream

class UnlockKeySuccessFragment: Fragment() {
    lateinit var mView: View
    lateinit var qrImageView: ImageView
    lateinit var encryptedKeyTextView: TextView
    lateinit var backupButton: Button

    lateinit var doneButton: Button
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mView = inflater.inflate(R.layout.multiwallet_encryption_success, container, false)
        qrImageView = mView.find(R.id.encryptedKeyQRCodeImageView)
        encryptedKeyTextView = mView.find(R.id.encryptedKeyTextView)
        doneButton = mView.find(R.id.backupDoneButton)
        backupButton = mView.find(R.id.backupButton)
        initiateEncryptedKeyValues()

        doneButton.setOnClickListener { activity?.finish() }
        backupButton.setOnClickListener { sendBackupEmail() }

        return mView
    }

    fun sendBackupEmail() {

        val key = (activity as MultiwalletManageWallet).viewModel.key!!
        val tmpDir = File(context?.filesDir?.absolutePath + "/tmp")
        tmpDir.mkdirs()
        val fileImage = File(tmpDir, "o3wallet.png")
        val fileJson = NEP6.getFromFileSystemAsFile()
        val fout = FileOutputStream(fileImage)
        val bitmap = QRCode.from(key).withSize(2000, 2000).bitmap()
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fout)

        val imageUri = FileProvider.getUriForFile(context!!, "network.o3.o3wallet", fileImage)
        val contentUri = FileProvider.getUriForFile(context!!, "network.o3.o3wallet", fileJson)

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.type = "message/rfc822"
        intent.putExtra(Intent.EXTRA_SUBJECT, "O3 Wallet Encrypted Backup")
        intent.putExtra(Intent.EXTRA_TEXT, String.format(resources.getString(R.string.ONBOARDING_backup_email_body), key))
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(imageUri, contentUri))

        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)
        startActivityForResult(Intent.createChooser(intent, "Send Email using:"), 101)

        fout.flush()
        fout.close()
        tmpDir.delete()
    }

    fun initiateEncryptedKeyValues() {
        val key =  (activity as MultiwalletManageWallet).viewModel.key!!
        encryptedKeyTextView.text = key
        val bitmap = QRCode.from(key).withSize(2000, 2000).bitmap()
        qrImageView.setImageBitmap(bitmap)
    }
}
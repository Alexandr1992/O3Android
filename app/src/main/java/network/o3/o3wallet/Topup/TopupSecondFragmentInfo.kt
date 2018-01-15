package network.o3.o3wallet.Topup

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.topup_activity_topup_second_fragment_info.*
    import net.glxn.qrgen.android.QRCode
import android.content.Intent
import android.os.Environment
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.imageURI
import java.io.File.separator
import android.os.Environment.getExternalStorageDirectory
import java.io.File
import java.io.FileOutputStream
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.net.Uri
import android.support.v4.content.FileProvider
import network.o3.o3wallet.*
import java.net.URI
import android.support.v4.app.ShareCompat




class TopupSecondFragmentInfo : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.topup_activity_topup_second_fragment_info)
        val secretPieceTwo = intent.getStringExtra("SecretPieceTwo")
        secretPieceTwoTextView.text = secretPieceTwo
        secretKeySavedCheckbox.text = resources.getString(R.string.save_fragment_confirm)
        val bitmap = QRCode.from(secretPieceTwo).withSize(1000, 1000).bitmap()
        secretPieceQrCodeView.setImageBitmap(bitmap)

        secondFragmentDoneButton.isEnabled = false
        secondFragmentDoneButton.backgroundColor = resources.getColor(R.color.colorDisabledButton)
        secretKeySavedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                secondFragmentDoneButton.isEnabled = true
                secondFragmentDoneButton.backgroundColor = resources.getColor(R.color.colorAccent)
            } else {
                secondFragmentDoneButton.isEnabled = false
                secondFragmentDoneButton.backgroundColor = resources.getColor(R.color.colorDisabledButton)
            }
        }

        secondFragmentDoneButton.setOnClickListener {
            val secretPieceOne = intent.getStringExtra("SecretPieceOne")
            val address = intent.getStringExtra("Address")
            Account.storeColdStorageKeyFragmentOnDevice(secretPieceOne)
            PersistentStore.setColdStorageVaultAddress(address)
            PersistentStore.setColdStorageEnabledStatus(true)
            val intent = Intent(this, TopupColdStorageBalanceActivity::class.java)
            startActivity(intent)
        }

        saveSecondFragmentButton.setOnClickListener {
            //TODO: SWITCH THIS TO SEND QR IMAGE
            sendImage()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                           permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                   sendImage()
                } else {
                    return
                }
                return
            }
        }
    }

    fun sendImage() {
        val qrFile = QRCode.from(intent.getStringExtra("SecretPieceTwo")).withSize(1000, 1000).stream()
        val fo = FileOutputStream(U)
        fo.write(qrFile.toByteArray())
        val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID,
                File("content://network.o3.o3wallet.fileprovider/images/temp.jpg"))
        val intentBuilder = ShareCompat.IntentBuilder.from(this).addStream(uri)
        val chooserIntent = intentBuilder.createChooserIntent()
        startActivity(chooserIntent)


        /*
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        val qrStream = QRCode.from(intent.getStringExtra("SecretPieceTwo")).withSize(1000, 1000).stream()
        val f = File(Environment.getExternalStorageDirectory().path + File.separator + "temp_qr.jpg")
        f.createNewFile()
        val fo = FileOutputStream(f)
        fo.write(qrStream.toByteArray())
        fo.close()

        sendIntent.type = "image/jpeg"
        sendIntent.putExtra(Intent.EXTRA_STREAM,
                Uri.fromFile(f))
        startActivity(sendIntent)*/
    }
}
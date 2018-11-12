package network.o3.o3wallet.MultiWallet.ManageMultiWallet


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.coroutines.experimental.android.UI
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.NEP6

import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.PrivateKeyFragment
import network.o3.o3wallet.Wallet.toast
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk15.coroutines.onClick
import java.io.File
import java.io.FileOutputStream

class ManageWalletBaseFragment : Fragment() {

    lateinit var mView: View
    lateinit var addressQrView: ImageView
    lateinit var addressTextView: TextView
    lateinit var encryptedKeyQrView: ImageView
    lateinit var encryptedKeyTextView: TextView
    lateinit var optionsListView: ListView

    lateinit var vm: ManageWalletViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_manage_wallet_base, container, false)
        vm = (activity as MultiwalletManageWallet).viewModel
        initiateQrViews()
        initiateListView()

        return mView
    }

    fun initiateListView() {
        val listView = mView.find<ListView>(R.id.optionsListView)
        listView.adapter = ManageWalletAdapter(context!!, this, vm)
    }

    fun initiateQrViews() {
        addressQrView = mView.find(R.id.addressQrImageView)
        encryptedKeyQrView = mView.find(R.id.encryptedKeyQRCodeImageView)
        addressTextView = mView.find(R.id.addressTextView)
        encryptedKeyTextView = mView.find(R.id.keyTextView)


        addressTextView.text = vm.address
        encryptedKeyTextView.text = vm.key

        val bitmapAddress = QRCode.from(vm.address).withSize(1000, 1000).bitmap()
        addressQrView.setImageBitmap(bitmapAddress)

        val bitmapKey = QRCode.from(vm.key).withSize(1000, 1000).bitmap()
        encryptedKeyQrView.setImageBitmap(bitmapKey)
    }

    class ManageWalletAdapter(context: Context, fragment: Fragment, vm: ManageWalletViewModel): BaseAdapter() {
        var mContext: Context
        var mVm: ManageWalletViewModel
        var mFragment: Fragment
        init {
            mContext = context
            mVm = vm
            mFragment = fragment
        }

        fun removeWalletAction() {
            if (mVm.isDefault == true)  {
                mContext.toast (mContext.resources.getString(R.string.MULTIWALLET_cannot_delete_default))
                return
            } else {
              mContext.alert(mContext.resources.getString(R.string.MULTIWALLET_are_you_sure)) {
                  yesButton {
                      val nep6 = NEP6.getFromFileSystem()
                      nep6.removeAccount(mVm.address)
                      nep6.writeToFileSystem()
                      mFragment.activity!!.finish()
                  }
                  noButton {
                  }
              }.show()
            }
        }

        fun showRawKeyAction() {
            val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()

            neo2DialogFragment.decryptionSucceededCallback = {
                val privateKeyModal = PrivateKeyFragment.newInstance()
                val bundle = Bundle()
                bundle.putString("key", neo2DialogFragment.decryptedKey)
                privateKeyModal.arguments = bundle
                privateKeyModal.show(mFragment.activity?.supportFragmentManager, privateKeyModal.tag)
            }

            neo2DialogFragment.encryptedKey = mVm.key!!
            neo2DialogFragment.showNow(mFragment.activity!!.supportFragmentManager, "backupkey")
        }

        fun unlockAction() {
            if (mVm.isDefault) {
                mContext.toast (mContext.resources.getString(R.string.MULTIWALLET_cannot_delete_default))
                return
            } else if (mVm.key == null){
                val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()
                neo2DialogFragment.decryptionSucceededCallback = {
                    NEP6.getFromFileSystem().makeNewDefault(mVm.address)
                }

                neo2DialogFragment.encryptedKey = mVm.key!!
                neo2DialogFragment.showNow(mFragment.activity!!.supportFragmentManager, "backupkey")

            } else {
                val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()
                neo2DialogFragment.decryptionSucceededCallback = {
                    //add watch addr
                }

                neo2DialogFragment.encryptedKey = mVm.key!!
                neo2DialogFragment.showNow(mFragment.activity!!.supportFragmentManager, "backupkey")
            }
        }

        fun backupAction() {
            val key = mVm.key
            val tmpDir = File(mContext?.filesDir?.absolutePath + "/tmp")
            tmpDir.mkdirs()
            val fileImage = File(tmpDir, "o3wallet.png")
            val fileJson = NEP6.getFromFileSystemAsFile()
            val fout = FileOutputStream(fileImage)
            val bitmap = QRCode.from(key).withSize(2000, 2000).bitmap()
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fout)

            val imageUri = FileProvider.getUriForFile(mContext!!, "network.o3.o3wallet", fileImage)
            val contentUri = FileProvider.getUriForFile(mContext!!, "network.o3.o3wallet", fileJson)

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "message/rfc822"
            intent.putExtra(Intent.EXTRA_SUBJECT, "O3 Wallet Encrypted Backup")
            intent.putExtra(Intent.EXTRA_TEXT, String.format(mContext.resources.getString(R.string.ONBOARDING_backup_email_body), key))
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(imageUri, contentUri))

            val imm = mContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mFragment.view!!.windowToken, 0)
            mFragment.startActivityForResult(Intent.createChooser(intent, "Send Email using:"), 101)

            fout.flush()
            fout.close()
            tmpDir.delete()
        }

        fun setAction(v: View, position: Int) {
            when (position) {
                0 -> {
                    v.onClick { backupAction() }
                } 1 -> {
                    v.onClick { showRawKeyAction() }
                } 2 -> {
                    v.onClick { unlockAction()}
                } 3 -> {
                    v.onClick { removeWalletAction() }
                }
            }
        }


        override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View {
            val titles = mutableListOf<String>(mContext.resources.getString(R.string.MULTIWALLET_backup_existing),
                    mContext.resources.getString(R.string.MULTIWALLET_show_raw_key),
                    mContext.resources.getString(R.string.MULTIWALLET_unlock),
                    mContext.resources.getString(R.string.MULTIWALLET_remove_wallet))


            val view = mContext.layoutInflater.inflate(R.layout.settings_row_layout, null, false)
            val nameTextView = view.find<TextView>(R.id.titleTextView)
            view.find<ImageView>(R.id.settingsIcon).visibility = View.GONE
            nameTextView.text = titles[position]
            if (position == 3) {
                nameTextView.textColor = ContextCompat.getColor(mContext, R.color.colorLoss)
            } else {
                nameTextView.textColor = ContextCompat.getColor(mContext, R.color.colorPrimary)
            }

            setAction(view, position)

            return view
        }

        override fun getItem(position: Int): String {
            return ""
        }

        override fun getCount(): Int {
            return 4
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }
}

package network.o3.o3wallet.MultiWallet.ManageMultiWallet


import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import jp.wasabeef.blurry.Blurry
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.MultiWallet.DialogInputEntryFragment
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.O3Wallet

import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.PrivateKeyFragment
import network.o3.o3wallet.Wallet.toast
import network.o3.o3wallet.toBitmap
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
        mView = inflater.inflate(R.layout.multiwallet_manage_wallet_base, container, false)
        vm = (activity as MultiwalletManageWallet).viewModel
        initiateQrViews()
        initiateListView()
        initiateActionBar()

        return mView
    }

    fun initiateListView() {
        val listView = mView.find<ListView>(R.id.optionsListView)
        listView.adapter = ManageWalletAdapter(context!!, this, vm)
    }

    fun setTitleIcon() {
        val titleIcon = activity?.find<ImageView>(R.id.titleIcon)!!
        titleIcon.visibility = View.VISIBLE
        if (vm.isDefault) {
            titleIcon.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unlocked)
        } else if (vm.key == null) {
            titleIcon.image = ContextCompat.getDrawable(context!!, R.drawable.ic_eye)
        } else {
            titleIcon.image = ContextCompat.getDrawable(context!!, R.drawable.ic_locked)
        }
    }

    fun initiateActionBar() {
        (activity as AppCompatActivity).supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        (activity as AppCompatActivity).supportActionBar?.setCustomView(R.layout.actionbar_layout)
        activity?.find<TextView>(R.id.mytext)?.text = vm.name
        activity?.find<ImageButton>(R.id.rightNavButton)?.visibility = View.VISIBLE
        activity?.find<ImageButton>(R.id.rightNavButton)?.image = ContextCompat.getDrawable(context!!, R.drawable.ic_edit)

        setTitleIcon()

        activity?.find<ImageButton>(R.id.rightNavButton)?.setOnClickListener {
            val newNameEntry = DialogInputEntryFragment.newInstance()
            newNameEntry.submittedInput = { newName ->
                var nep6 = NEP6.getFromFileSystem()
                val index = nep6.accounts.indexOfFirst { it.address == vm.address }!!
                nep6.accounts[index].label = newName
                nep6.writeToFileSystem()
                vm.name = newName
                activity?.find<TextView>(R.id.mytext)?.text = vm.name
                val intent = Intent("need-update-watch-address-event")
                intent.putExtra("reset", true)
                LocalBroadcastManager.getInstance(O3Wallet.appContext!!).sendBroadcast(intent)
            }

            newNameEntry.showNow(activity!!.supportFragmentManager, "change name")
        }
    }

    fun initiateUnlockWatchAddr() {
        val unlockButton = mView.find<Button>(R.id.unlockWatchAddrButton)
        unlockButton.visibility = View.VISIBLE

        val bitmap = QRCode.from(vm.address).withSize(1000, 1000).bitmap()
        Blurry.with(context).radius(15).sampling(3).from(bitmap).into(encryptedKeyQrView)
        unlockButton.setOnClickListener {
            mView.findNavController()?.navigate(R.id.action_manageWalletBaseFragment_to_unlockWatchAddressFragment)
        }
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

        if (vm.key != null && vm.key != "") {
            val bitmapKey = QRCode.from(vm.key).withSize(1000, 1000).bitmap()
            encryptedKeyQrView.setImageBitmap(bitmapKey)
        } else {
            initiateUnlockWatchAddr()
        }
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
            } else if (mVm.key != null){
                val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()
                neo2DialogFragment.decryptionSucceededCallback = { pass ->
                    NEP6.getFromFileSystem().makeNewDefault(mVm.address, pass)
                    (mFragment as ManageWalletBaseFragment).setTitleIcon()
                }

                neo2DialogFragment.encryptedKey = mVm.key!!
                neo2DialogFragment.showNow(mFragment.activity!!.supportFragmentManager, "backupkey")

            } else {
                mFragment.view?.findNavController()?.navigate(R.id.action_manageWalletBaseFragment_to_unlockWatchAddressFragment)
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
                view.find<ImageView>(R.id.settingsIcon).visibility = View.VISIBLE
                view.find<ImageView>(R.id.settingsIcon).image = ContextCompat.getDrawable(mContext, R.drawable.ic_trash)
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
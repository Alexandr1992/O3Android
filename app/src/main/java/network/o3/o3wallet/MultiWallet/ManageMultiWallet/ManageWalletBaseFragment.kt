package network.o3.o3wallet.MultiWallet.ManageMultiWallet


import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import jp.wasabeef.blurry.Blurry
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.*
import network.o3.o3wallet.MultiWallet.DialogInputEntryFragment
import network.o3.o3wallet.Settings.PrivateKeyFragment
import network.o3.o3wallet.Wallet.toast
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
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

    val needReloadWalletPage = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setTitleIcon()
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this.context!!)
                .unregisterReceiver(needReloadWalletPage)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.multiwallet_manage_wallet_base, container, false)
        vm = (activity as MultiwalletManageWallet).viewModel
        initiateQrViews()
        initiateListView()
        initiateActionBar()
        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadWalletPage,
                IntentFilter("need-update-watch-address-event"))
        initiateQuickSwap()
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if ((activity as MultiwalletManageWallet).viewModel.shouldNavToVerify) {
            (activity as MultiwalletManageWallet).viewModel.shouldNavToVerify = false
            findNavController().navigate(R.id.action_manageWalletBaseFragment_to_verifyManualBackupFragment)
        }
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

        setTitleIcon()
        activity?.find<ImageButton>(R.id.rightNavButton)?.image = ContextCompat.getDrawable(context!!, R.drawable.ic_edit)
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

        val copyAddress = {
            val clipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(resources.getString(R.string.WALLET_copied_address), vm.address)
            clipboard.primaryClip = clip
            toast(resources.getString(R.string.WALLET_copied_address))
        }

        val copyKey = {
            val clipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(resources.getString(R.string.WALLET_copied_address), vm.key)
            clipboard.primaryClip = clip
            toast(resources.getString(R.string.WALLET_copied_key))
        }

        addressQrView.onClick { copyAddress() }
        encryptedKeyQrView.onClick { copyKey() }

        addressTextView = mView.find(R.id.addressTextView)
        encryptedKeyTextView = mView.find(R.id.keyTextView)

        addressTextView.onClick { copyAddress() }
        encryptedKeyQrView.onClick { copyKey() }

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

    fun initiateQuickSwap() {
        if (vm.key == null) {
            mView.find<View>(R.id.quickSwapLayout).visibility = View.INVISIBLE
            return
        }
        val switch = mView.find<Switch>(R.id.quickSwapSwitch)
        switch.isChecked = PersistentStore.getHasQuickSwapEnabled(vm.address)
        switch.onClick {
            if (switch.isChecked == false) {
                alert("Are you sure you want to remove quick swap, you will have to enter the password from now on ") {
                    yesButton {
                        PersistentStore.setHasQuickSwapEnabled(false, vm.address)
                    }
                    noButton {

                    }
                }.show()
            } else {
                val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()
                neo2DialogFragment.decryptionSucceededCallback = { pass, _ ->
                    neo2DialogFragment.dismiss()
                    PersistentStore.setHasQuickSwapEnabled(true, vm.address, pass)
                    AnalyticsService.Wallet.logWalletUnlocked()
                }

                neo2DialogFragment.encryptedKey = vm.key!!
                neo2DialogFragment.showNow(activity!!.supportFragmentManager, "backupkey")
            }
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

            neo2DialogFragment.decryptionSucceededCallback = { _, _ ->
                val privateKeyModal = PrivateKeyFragment.newInstance()
                val bundle = Bundle()
                bundle.putString("key", neo2DialogFragment.decryptedKey)
                privateKeyModal.arguments = bundle
                privateKeyModal.show(mFragment.activity?.supportFragmentManager!!, privateKeyModal.tag)
            }

            neo2DialogFragment.encryptedKey = mVm.key!!
            neo2DialogFragment.showNow(mFragment.activity!!.supportFragmentManager, "backupkey")
        }

        fun unlockAction() {
            if (mVm.isDefault) {
                mContext.toast (mContext.resources.getString(R.string.MULTIWALLET_cannot_unlock_default))
                return
            } else if (mVm.key != null){
                if (Account.isStoredPasswordForNep6KeyPresent(mVm.address)) {
                    var pass = Account.getStoredPassForNEP6Entry(mVm.address)
                    AnalyticsService.Wallet.logWalletUnlocked()
                    NEP6.getFromFileSystem().makeNewDefault(mVm.address, pass)
                    mVm.isDefault = true
                    (mFragment as ManageWalletBaseFragment).setTitleIcon()
                } else {
                    val neo2DialogFragment = DialogUnlockEncryptedKey.newInstance()
                    neo2DialogFragment.decryptionSucceededCallback = { pass, _ ->
                        NEP6.getFromFileSystem().makeNewDefault(mVm.address, pass)
                        AnalyticsService.Wallet.logWalletUnlocked()
                        mVm.isDefault = true
                        (mFragment as ManageWalletBaseFragment).setTitleIcon()
                    }

                    neo2DialogFragment.encryptedKey = mVm.key!!
                    neo2DialogFragment.showNow(mFragment.activity!!.supportFragmentManager, "backupkey")
                }

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

        fun verifyManualBackupAction() {
            mFragment.findNavController()?.navigate(R.id.action_manageWalletBaseFragment_to_verifyManualBackupFragment)
        }

        fun setAction(v: View, position: Int) {
            if (mVm.key == null) {
                v.onClick { removeWalletAction() }
                return
            }


            when (position) {
                0 -> {
                    v.onClick { backupAction() }
                } 1 -> {
                    v.onClick { showRawKeyAction() }
                } 2 -> {
                    v.onClick { unlockAction()}
                } 3-> {
                    v.onClick { verifyManualBackupAction() }
                } 4 -> {
                    v.onClick { removeWalletAction() }
                }
            }
        }


        override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View {
            val titles = mutableListOf<String>(mContext.resources.getString(R.string.MULTIWALLET_backup_existing),
                    mContext.resources.getString(R.string.MULTIWALLET_show_raw_key),
                    mContext.resources.getString(R.string.MULTIWALLET_unlock),
                    mContext.resources.getString(R.string.MULTIWALLET_verify_manual_backup),
                    mContext.resources.getString(R.string.MULTIWALLET_remove_wallet)
                    )


            val view = mContext.layoutInflater.inflate(R.layout.settings_row_layout, null, false)
            val nameTextView = view.find<TextView>(R.id.titleTextView)

            if (mVm.key == null) {
                nameTextView.text = titles[4]
                setAction(view, 4)
                nameTextView.textColor = ContextCompat.getColor(mContext, R.color.colorLoss)
                view.find<ImageView>(R.id.settingsIcon).visibility = View.VISIBLE
                view.find<ImageView>(R.id.settingsIcon).image = ContextCompat.getDrawable(mContext, R.drawable.ic_trash)
                return view
            }


            view.find<ImageView>(R.id.settingsIcon).visibility = View.GONE
            nameTextView.text = titles[position]
            if (position == 4) {
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
            if (mVm.key == null) {
                return 1
            }
            return 5
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }
}

package network.o3.o3wallet.Onboarding.CreateKey.Backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import network.o3.o3wallet.Account
import network.o3.o3wallet.Onboarding.CreateKey.CreateNewWalletActivity
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.textAppearance
import org.jetbrains.anko.textColor
import java.text.FieldPosition

class BackupAdapter(context: Context, fragment: Fragment): BaseAdapter() {
    var images = listOf(R.drawable.ic_envelope, R.drawable.ic_screenshot, R.drawable.ic_copy_backup, R.drawable.ic_edit)
    var backupTitles = context.resources.getStringArray(R.array.ONBOARDING_backup_options)

    val EMAIL = 0
    val SCREENSHOT = 1
    val CLIPBOARD = 2
    val PAPER = 3
    val CLOSE = 4

    private val mContext: Context
    private val mFragment: Fragment
    init {
        mContext = context
        mFragment = fragment
    }

    override fun getItem(position: Int): Int {
        return position
    }

    override fun getCount(): Int {
        return 5
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun createDialogForClipboardCopy() {
        val clipboardDialog = DialogCompletedBackupFragment.newInstance()
        var args = Bundle()
        args.putString("wif", (mFragment.activity as CreateNewWalletActivity).wif)
        args.putString("title", mContext.resources.getString(R.string.ONBOARDING_copy_dialog_title))
        args.putString("subtitle", mContext.resources.getString(R.string.ONBOARDING_copy_dialog_subtitle))
        args.putString("buttonTitle", mContext.resources.getString(R.string.ONBOARDING_COPY_dialog_button))
        clipboardDialog.arguments = args
        clipboardDialog.show(mFragment.activity!!.fragmentManager, clipboardDialog.tag)

        val clipboard = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(mContext.resources.getString(R.string.SETTINGS_copied_key),
                (mFragment.activity as CreateNewWalletActivity).wif)
        clipboard.primaryClip = clip
    }

    fun createDialogForScreenShot() {
        val screenshotDialog = DialogCompletedBackupFragment.newInstance()
        var args = Bundle()
        args.putString("wif", (mFragment.activity as CreateNewWalletActivity).wif)
        args.putString("title", mContext.resources.getString(R.string.ONBOARDING_screenshot_dialog_title))
        args.putString("subtitle", mContext.resources.getString(R.string.ONBOARDING_screenshot_dialog_subtitle))
        args.putString("buttonTitle", mContext.resources.getString(R.string.ONBOARDING_screenshot_dialog_button))
        screenshotDialog.arguments = args
        screenshotDialog.show(mFragment.activity!!.fragmentManager, screenshotDialog.tag)
    }

    fun verifyPaperKey() {
        val intent = Intent(mContext, VerifyPaperKeyActivity::class.java)
        intent.putExtra("wif", (mFragment.activity as CreateNewWalletActivity).wif)
        (mFragment as BottomSheetDialogFragment).dismiss()
        mContext.startActivity(intent)
    }

    fun encryptKeyWithNep2() {
        val intent = Intent(mContext, Nep2BackupActivity::class.java)
        intent.putExtra("wif", (mFragment.activity as CreateNewWalletActivity).wif)
        (mFragment as BottomSheetDialogFragment).dismiss()
        mContext.startActivity(intent)
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(R.layout.onboarding_backup_option_row, viewGroup, false)

        view.find<TextView>(R.id.backupOptionTextView).text = backupTitles[position]
        if (position < CLOSE) {
            view.find<ImageView>(R.id.backupOptionIcon).image = mContext.getDrawable(images[position])
        }

        if (position == EMAIL) {
            view.setOnClickListener { encryptKeyWithNep2() }
            view.find<TextView>(R.id.securityLevelTextView).text = mContext.resources.getString(R.string.ONBOARDING_most_secure)
        } else {
            view.find<TextView>(R.id.securityLevelTextView).visibility = View.INVISIBLE
        }

        if (position == SCREENSHOT) {
            view.setOnClickListener { createDialogForScreenShot() }
        }

        if (position == CLIPBOARD) {
            view.setOnClickListener { createDialogForClipboardCopy() }
        }

        if (position == PAPER) {
            view.setOnClickListener { verifyPaperKey() }
        }

        if (position == CLOSE) {
            view.find<ImageView>(R.id.backupOptionIcon).visibility = View.INVISIBLE
            view.find<TextView>(R.id.backupOptionTextView).textColor = mContext.resources.getColor(R.color.colorSubtitleGrey)
            view.setOnClickListener { (mFragment as BottomSheetDialogFragment).dismiss() }
        }



        return view
    }
}
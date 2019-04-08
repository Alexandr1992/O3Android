package network.o3.o3wallet.Dapp

import android.content.Intent
import android.graphics.Color
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.skydoves.powermenu.CustomPowerMenu
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.OnMenuItemClickListener
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

fun DAPPBrowser.initateBrowserHeader(allowSearch: Boolean) {
    if (allowSearch) {
        searchBar.isFocusable = true
        searchBar.isEnabled = true
        searchBar.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_GO) {
                webView.loadUrl(textView.text.toString())
            }
            true
        }
    } else {
        searchBar.isEnabled = false
    }
    mView.find<ImageView>(R.id.returnToO3ImageView).setColorFilter(Color.argb(255, 191, 191, 191))
    mView.find<ImageView>(R.id.returnToO3ImageView).onClick {
        activity?.finish()
    }
    setMoreActions()
}

fun DAPPBrowser.setMoreActions() {
    val moreButton = mView!!.find<ImageView>(R.id.moreButton)
    moreButton.onClick {
        val o3Logo = ContextCompat.getDrawable(this@setMoreActions.context!!, R.drawable.ic_o3_logo_white)
        o3Logo!!.setTint(resources.getColor(R.color.colorDappMenuItem))
        val customPowerMenu = CustomPowerMenu.Builder(this@setMoreActions.activity, DappPopupMenuAdapter())
                .setWidth(800)
                .addItem(DappPopupMenuItem(
                        resources.getString(R.string.DAPP_refresh),
                        ContextCompat.getDrawable(this@setMoreActions.context!!, R.drawable.ic_refresh))
                )

                .addItem(DappPopupMenuItem(
                        resources.getString(R.string.DAPP_share),
                        ContextCompat.getDrawable(this@setMoreActions.context!!, R.drawable.ic_share_alt))
                )

                .addItem(DappPopupMenuItem(
                        resources.getString(R.string.DAPP_return_to_o3),
                        o3Logo)
                )

                .addItem(DappPopupMenuItem(
                        "Connected to " + PersistentStore.getNetworkType() + "Net",
                        null)
                )

                .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .build()

        val onIconMenuItemClickListener = OnMenuItemClickListener<DappPopupMenuItem> { position, item ->
            if (position == 0) {
                webView.reload()
            } else if (position == 1) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, activity?.intent?.getStringExtra("url"));
                startActivity(Intent.createChooser(shareIntent, ""))
            } else if (position == 2) {
                dappViewModel.setLockStatus(true)
                this@setMoreActions.activity?.finish()
            }
            customPowerMenu.dismiss()
        }
        customPowerMenu.setOnMenuItemClickListener(onIconMenuItemClickListener)
        customPowerMenu.showAsDropDown(moreButton)
    }
}
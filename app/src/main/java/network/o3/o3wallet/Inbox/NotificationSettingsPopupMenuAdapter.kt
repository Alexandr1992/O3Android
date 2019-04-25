package network.o3.o3wallet.Inbox

import android.content.Context
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.skydoves.powermenu.CustomPowerMenu
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.MenuBaseAdapter
import com.skydoves.powermenu.OnMenuItemClickListener
import network.o3.o3wallet.Dapp.DappPopupMenuItem
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

class NotificationSettingsPopupMenuItem(val title: String)

class NotificationSettingsPopupMenuAdapter: MenuBaseAdapter<NotificationSettingsPopupMenuItem>() {
    fun setupMuteAllSwitch(switch: Switch) {
        switch.isChecked = PersistentStore.getInboxServices().isEmpty()
        switch.onClick {
            if (switch.isChecked) {
                PersistentStore.setInboxServices(mutableListOf())
                switch.isClickable = false
                Handler().postDelayed({
                    notifyDataSetChanged()
                    switch.isClickable = false
                }, 300)
            } else {
                switch.isClickable = false
                Handler().postDelayed({
                    notifyDataSetChanged()
                    switch.isClickable = true
                }, 300)
            }
        }
    }

    override fun getView(index: Int, view: View?, viewGroup: ViewGroup): View {
        val context = viewGroup.context
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val optionView = inflater.inflate(R.layout.inbox_notification_setting_row, viewGroup, false)
        val item = getItem(index) as NotificationSettingsPopupMenuItem
        optionView.find<TextView>(R.id.notificationTypeTitleTextView).text = item.title

        val switch = optionView.find<Switch>(R.id.notificationSettingSwitch)
        val services = PersistentStore.getInboxServices()


        //mute all
        if (index == count - 1) {
            setupMuteAllSwitch(switch)
        } else {
            switch.isChecked = services.contains(item.title)
            switch.onClick {
                if (switch.isChecked) {
                    services.add(item.title)
                    PersistentStore.setInboxServices(services)
                    //let switch animation finish before notifying data changed
                    switch.isClickable = false
                    Handler().postDelayed({
                        notifyDataSetChanged()
                        switch.isClickable = false
                    }, 300)
                } else {
                    services.remove(item.title)
                    PersistentStore.setInboxServices(services)
                    //let switch animation finish before notifying data changed
                    switch.isClickable = false
                    Handler().postDelayed({
                        notifyDataSetChanged()
                        switch.isClickable = true
                    }, 300)
                }
            }
        }

        return optionView
    }
}

fun InboxMessagesFragment.setNotificationSettings() {
    val settingsButton = mView!!.find<ImageView>(R.id.notificationSettingsButton)
    val displayMetrics = DisplayMetrics()
    activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
    var width = displayMetrics.widthPixels

    settingsButton.onClick {
        val o3Logo = ContextCompat.getDrawable(this@setNotificationSettings.context!!, R.drawable.ic_o3_logo_white)
        o3Logo!!.setTint(resources.getColor(R.color.colorDappMenuItem))
        val customPowerMenu = CustomPowerMenu.Builder(this@setNotificationSettings.activity, NotificationSettingsPopupMenuAdapter())
                .setWidth(width)
                .addItem(NotificationSettingsPopupMenuItem("O3Labs"))
                .addItem(NotificationSettingsPopupMenuItem("Switcheo"))
                .addItem(NotificationSettingsPopupMenuItem("NGD"))
                .addItem(NotificationSettingsPopupMenuItem("All"))
                .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .build()

        val onIconMenuItemClickListener = OnMenuItemClickListener<DappPopupMenuItem> { position, item ->
            if (position == 0) {
                // do somethng on clicks
            }
            customPowerMenu.dismiss()
        }
        customPowerMenu.setOnMenuItemClickListener(onIconMenuItemClickListener)
        customPowerMenu.showAsDropDown(settingsButton)
    }
}
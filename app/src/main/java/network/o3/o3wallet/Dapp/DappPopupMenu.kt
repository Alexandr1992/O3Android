package network.o3.o3wallet.Dapp

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.skydoves.powermenu.MenuBaseAdapter
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image


class DappPopupMenuItem(val title: String, val image: Drawable?)

class DappPopupMenuAdapter : MenuBaseAdapter<DappPopupMenuItem>() {

    override fun getView(index: Int, view: View?, viewGroup: ViewGroup): View {
        val context = viewGroup.context
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val optionView = inflater.inflate(R.layout.dapp_popup_option_row, viewGroup, false)
        val item = getItem(index) as DappPopupMenuItem
        optionView.find<TextView>(R.id.optionTitle).text = item.title
        optionView.find<ImageView>(R.id.optionLogo).image  = item.image

        return optionView
    }
}
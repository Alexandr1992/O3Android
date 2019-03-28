package network.o3.o3wallet.NativeTrade

import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.yesButton

class NativeTradeRootActivity : AppCompatActivity() {
    val viewModel = NativeTradeViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setOrderAsset(intent.getStringExtra("asset"))
        viewModel.isBuyOrder = intent.getBooleanExtra("is_buy", true)
        if (viewModel.getOrderAsset() == "NEO") {
            viewModel.setSelectedBaseAssetImageUrl("https://cdn.o3.network/img/neo/SDUSD.png")
            viewModel.setSelectedBaseAssetValue("SDUSD")
        }

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.actionbar_layout_trade)
        if (viewModel.isBuyOrder) {
            find<TextView>(R.id.orderTypeTextView).text = resources.getString(R.string.Native_TRADE_Buy)
        } else {
            find<TextView>(R.id.orderTypeTextView).text = resources.getString(R.string.Native_TRADE_Sell)
        }

        find<TextView>(R.id.orderAssetSymbolTextView).text = viewModel.getOrderAsset()
        Glide.with(this).load(String.format("https://cdn.o3.network/img/neo/%s.png", viewModel.getOrderAsset()))
                .into(find<ImageView>(R.id.orderAssetLogo))
        setContentView(R.layout.native_trade_root_activity)

        find<ImageView>(R.id.pendingOrdersToolbarButton).setOnClickListener {
            findNavController(R.id.orderSubmissionFragment).navigate(R.id.action_orderSubmissionFragment_to_ordersListFragment)
        }

        findNavController(R.id.orderSubmissionFragment).addOnDestinationChangedListener { controller,
                                                                      destination, args ->
            if (destination.id == R.id.orderSubmissionFragment && viewModel.orders?.value?.count() ?: 0 > 0) {
                find<ImageView>(R.id.pendingOrdersToolbarButton).visibility = View.VISIBLE
                find<TextView>(R.id.pendingOrderCountBadge).visibility = View.VISIBLE
            } else {
                find<ImageView>(R.id.pendingOrdersToolbarButton).visibility = View.GONE
                find<TextView>(R.id.pendingOrderCountBadge).visibility = View.GONE
            }
        }

        viewModel.getError().observe(this, Observer { error ->
            runOnUiThread {
                alert(error!!.localizedMessage) {
                    yesButton {
                        finish()
                    }
                }.show()
            }
        })
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_Dark, true)
        } else {
            theme.applyStyle(R.style.AppTheme_White, true)
        }
        return theme
    }
}
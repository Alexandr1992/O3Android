package network.o3.o3wallet.NativeTrade

import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import network.o3.o3wallet.R
import org.jetbrains.anko.contentView
import org.jetbrains.anko.find

class NativeTradeRootActivity : AppCompatActivity() {
    val viewModel = NativeTradeViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.orderAsset = intent.getStringExtra("asset")
        viewModel.isBuyOrder = intent.getBooleanExtra("is_buy", true)

        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        supportActionBar?.setCustomView(R.layout.actionbar_layout_trade)
        if (viewModel.isBuyOrder) {
            find<TextView>(R.id.orderTypeTextView).text = resources.getString(R.string.Native_TRADE_Buy)
        } else {
            find<TextView>(R.id.orderTypeTextView).text = resources.getString(R.string.Native_TRADE_Sell)
        }

        find<TextView>(R.id.orderAssetSymbolTextView).text = viewModel.orderAsset

        Glide.with(this).load(String.format("https://cdn.o3.network/img/neo/%s.png", viewModel.orderAsset))
                .into(find<ImageView>(R.id.orderAssetLogo))
        setContentView(R.layout.native_trade_root_activity)

        find<ImageView>(R.id.pendingOrdersToolbarButton).setOnClickListener {
            findNavController(R.id.orderSubmissionFragment).navigate(R.id.action_orderSubmissionFragment_to_ordersListFragment)
            //find<ImageView>(R.id.pendingOrdersToolbarButton).visibility = View.GONE
        }

        findNavController(R.id.orderSubmissionFragment).addOnNavigatedListener { controller,
                                                                      destination ->
            if (destination.id == R.id.orderSubmissionFragment) {
                find<ImageView>(R.id.pendingOrdersToolbarButton).visibility = View.VISIBLE
            } else {
                find<ImageView>(R.id.pendingOrdersToolbarButton).visibility = View.GONE
            }
        }
    }
}

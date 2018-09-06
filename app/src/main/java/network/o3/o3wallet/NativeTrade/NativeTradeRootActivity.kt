package network.o3.o3wallet.NativeTrade

import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class NativeTradeRootActivity : AppCompatActivity() {
    val viewModel = NativeTradeViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.orderAsset = intent.getStringExtra("asset")

        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        supportActionBar?.setCustomView(R.layout.actionbar_layout_trade)
        find<TextView>(R.id.orderTypeTextView).text = resources.getString(R.string.Native_TRADE_Buy)
        find<TextView>(R.id.orderAssetSymbolTextView).text = viewModel.orderAsset
        Glide.with(this).load(String.format("https://cdn.o3.network/img/neo/%s.png", viewModel.orderAsset))
                .into(find<ImageView>(R.id.orderAssetLogo))
        setContentView(R.layout.native_trade_root_activity)

    }
}

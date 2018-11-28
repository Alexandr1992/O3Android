package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.res.Resources
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.widget.TextView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import kotlinx.android.synthetic.main.token_sale_root_activity.*
import network.o3.o3wallet.API.O3.TokenSale
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class TokenSaleRootActivity : AppCompatActivity() {
    lateinit var tokenSale: TokenSale

    fun resetBestNode() {
        if (PersistentStore.getNetworkType() == "Private") {
            PersistentStore.setNodeURL(PersistentStore.getNodeURL())
            return
        }

        O3PlatformClient().getChainNetworks {
            if (it.first == null) {
                return@getChainNetworks
            } else {
                PersistentStore.setOntologyNodeURL(it.first!!.ontology.best)
                PersistentStore.setNodeURL(it.first!!.neo.best)
            }
        }
    }

    val currentFragment: Fragment?
        get() = add_multiwallet_nav_host.childFragmentManager.findFragmentById(R.id.add_multiwallet_nav_host)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetBestNode()
        tokenSale = Gson().fromJson(intent.getStringExtra("TOKENSALE_JSON"))
        setContentView(R.layout.token_sale_root_activity)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.actionbar_layout)
        find<TextView>(R.id.mytext).text = resources.getString(R.string.TOKENSALE_Token_Sale)
    }

    override fun onBackPressed() {
        val f = currentFragment
        if (f is TokenSaleReceiptFragment) {
            finish()
        } else {
            super.onBackPressed()
        }
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

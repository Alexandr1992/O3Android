package network.o3.o3wallet.MarketPlace.TokenSales

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import kotlinx.android.synthetic.main.token_sale_root_activity.*
import network.o3.o3wallet.API.O3.TokenSale
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R

class TokenSaleRootActivity : AppCompatActivity() {
    lateinit var tokenSale: TokenSale

    fun resetBestNode() {
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
        get() = my_nav_host_fragment.childFragmentManager.findFragmentById(R.id.my_nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetBestNode()
        tokenSale = Gson().fromJson(intent.getStringExtra("TOKENSALE_JSON"))
        setContentView(R.layout.token_sale_root_activity)
    }

    override fun onBackPressed() {
        val f = currentFragment
        if (f is TokenSaleReceiptFragment) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}

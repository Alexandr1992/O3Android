package network.o3.o3wallet.MarketPlace.TokenSales

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import kotlinx.android.synthetic.main.token_sale_root_activity.*
import network.o3.o3wallet.API.O3.TokenSale
import network.o3.o3wallet.R

class TokenSaleRootActivity : AppCompatActivity() {
    lateinit var tokenSale: TokenSale

    val currentFragment: Fragment?
        get() = my_nav_host_fragment.childFragmentManager.findFragmentById(R.id.my_nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

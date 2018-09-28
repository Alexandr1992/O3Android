package network.o3.o3wallet.Onboarding

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import neoutils.Neoutils.selectBestSeedNode
import neoutils.SeedNodeResponse
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.find

class SelectingBestNode : AppCompatActivity() {
    var deepLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.portfolio_activity_selecting_best_node)
        val sharedPref = O3Wallet.appContext!!.defaultSharedPreferences
        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        supportActionBar?.setCustomView(R.layout.actionbar_layout)
        find<TextView>(R.id.mytext).text = resources.getString(R.string.WALLET_my_o3_wallet)
        with (sharedPref.edit()) {
            putBoolean("USING_PRIVATE_NET", false)
            commit()
        }
        if (intent != null) {
            if (intent.getStringExtra("deepLink") != null) {
                deepLink = intent.getStringExtra("deepLink")
            }
        }
        getBestNode()
    }

    fun gotBestNode(node: String) {
        PersistentStore.setNodeURL(node)
        //close activity and start the main tabbed one fresh
        val intent = Intent(this, MainTabbedActivity::class.java)
        if (deepLink != null) {
            intent.putExtra("deepLink", deepLink!!)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun getBestNode() {
        if (PersistentStore.getNetworkType() == "Private") {
            gotBestNode(PersistentStore.getNodeURL())
            return
        }

        O3PlatformClient().getChainNetworks {
            if (it.first == null) {
                getBestNode()
            } else {
                PersistentStore.setOntologyNodeURL(it.first!!.ontology.best)
                gotBestNode(it.first!!.neo.best)
            }
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

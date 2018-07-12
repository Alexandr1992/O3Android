package network.o3.o3wallet.Onboarding

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
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

class SelectingBestNode : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.portfolio_activity_selecting_best_node)
        val sharedPref = O3Wallet.appContext!!.defaultSharedPreferences
        with (sharedPref.edit()) {
            putBoolean("USING_PRIVATE_NET", false)
            commit()
        }
        getBestNode()
    }

    fun gotBestNode(node: String) {
        PersistentStore.setNodeURL(node)
        //close activity and start the main tabbed one fresh
        val intent = Intent(this, MainTabbedActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun getBestNode() {
        O3PlatformClient().getChainNetworks {
            if (it.first == null) {
                getBestNode()
            } else {
                PersistentStore.setOntologyNodeURL(it.first!!.ontology.best)
                gotBestNode(it.first!!.neo.best)
            }
        }
    }
}

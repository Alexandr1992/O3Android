package network.o3.o3wallet.Onboarding

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import neoutils.Neoutils.selectBestSeedNode
import neoutils.SeedNodeResponse
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

    fun gotBestNode(node: SeedNodeResponse) {
        PersistentStore.setNodeURL(node.url)
        //close activity and start the main tabbed one fresh
        val intent = Intent(this, MainTabbedActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun getBestNode() {

        val mainNetNodes = "https://seed1.neo.org:10331," +
                "http://seed2.neo.org:10332," +
                "http://seed3.neo.org:10332," +
                "http://seed4.neo.org:10332," +
                "http://seed5.neo.org:10332," +
              //  "http://seed1.cityofzion.io:8080," +
               // "http://seed2.cityofzion.io:8080," +
               // "http://seed3.cityofzion.io:8080," +
               // "http://seed4.cityofzion.io:8080," +
               // "http://seed5.cityofzion.io:8080," +
                "http://seed1.o3node.org:10332," +
                "http://seed2.o3node.org:10332," +
                "http://seed3.o3node.org:10332"

        val testNetNodes = "https://seed1.neo.org:20332," +
                "http://seed2.neo.org:20332," +
                "http://seed3.neo.org:20332," +
                "http://seed4.neo.org:20332," +
                "http://seed5.neo.org:20332," +
                "http://test1.cityofzion.io:8080," +
                "http://test2.cityofzion.io:8080," +
                "http://test3.cityofzion.io:8080," +
                "http://test4.cityofzion.io:8080," +
                "http://test5.cityofzion.io:8080"

        val privateNetNodes = "https://privatenet.o3.network:30333"

        var nodes = ""
        if (PersistentStore.getNetworkType() == "Main") {
            nodes = mainNetNodes
        } else if (PersistentStore.getNetworkType() == "Test") {
            nodes = testNetNodes
        } else {
            nodes = mainNetNodes
        }

        async(UI) {
            val data: Deferred<SeedNodeResponse> = bg {
                selectBestSeedNode(nodes)
            }
            gotBestNode(data.await())
        }
    }
}

package network.o3.o3wallet.Dapp

import android.content.Context
import android.content.res.Resources
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater

class ContractInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dapp_contract_info_activity)


        val contractHash = intent.getStringExtra("contract")
        val invokeRequestString = intent.getStringExtra("invokeRequestString")
        val contractView = find<ListView>(R.id.contractDetailsListView)
        NeoNodeRPC(PersistentStore.getNodeURL()).getContractState(contractHash) {
            if (it.first != null) {
                val contract = it.first!!
                var params: List<Pair<String, String>> = listOf(
                        "name" to contract.name,
                        "author" to contract.author,
                        "email" to contract.email,
                        "description" to contract.description,
                        "version" to contract.version.toString(),
                        "contract hash" to contract.hash,
                        "parameters" to contract.parameters.toString(),
                        "return type" to contract.returntype,
                        "uses storage" to contract.properties.storage.toString(),
                        "dynamic invoke" to contract.properties.dynamic_invoke.toString()
                        )
                runOnUiThread {
                    contractView.adapter = ContractInfoAdapter(this, params)
                }
            }
        }
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_NoTopBar_Dark, true)
        } else {
            theme.applyStyle(R.style.AppTheme_NoTopBar_White, true)
        }
        return theme
    }

    class ContractInfoAdapter(val mContext: Context, val params: List<Pair<String,String>>): BaseAdapter() {
        override fun getCount(): Int {
            return params.count()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Pair<String, String> {
            return params[position]
        }

        override fun getView(position: Int, p1: View?, viewGroup: ViewGroup?): View {
            val view = mContext.layoutInflater.inflate(R.layout.dapp_contract_info_row, viewGroup, false)
            var item = getItem(position)
            view.find<TextView>(R.id.dappInfoTitle).text = item.first
            view.find<TextView>(R.id.dappInfoValue).text = item.second
            return view
        }
    }
}

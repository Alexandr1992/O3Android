package network.o3.o3wallet.API.Ontology

import android.content.Intent
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.Gson
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import neoutils.Neoutils
import neoutils.SeedNodeResponse
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3Platform.O3Inbox
import network.o3.o3wallet.API.O3Platform.O3InboxItem
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.PlatformResponse
import network.o3.o3wallet.Account
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.PersistentStore
import org.jetbrains.anko.coroutines.experimental.bg
import java.lang.Exception
import java.util.*

class OntologyClient {
    val DecimalDivisor = 1000000000.0

    enum class Asset {
        ONT,
        ONG;

        fun assetID(): String {
            if (this == ONT) {
                return "ont"
            } else if (this == ONG) {
                return "ong"
            }
            return ""
        }
    }

    enum class OntologyRPC {
        GETGASPRICE;

        fun methodName(): String {
            return this.name.toLowerCase(Locale.US)
        }
    }

    fun transferOntologyAsset(assetID: String, toAddress: String, amount: Double, completion: (Pair<Boolean, Error?>) -> Unit) {
        getGasPrice {
            if (it.second != null) {
                completion(Pair<Boolean, Error?>(false, Error(it.second!!.localizedMessage)))
            } else {
                try {
                    Neoutils.ontologyTransfer(PersistentStore.getOntologyNodeURL(), it.first!!, 20000,
                            Account.getWallet()?.wif!!, assetID, toAddress, amount)
                    completion(Pair<Boolean, Error?>(true, null))
                }
                catch (e: Exception) {
                     completion(Pair<Boolean, Error?>(false, Error("Transfer Failed")))
                }
            }
        }
    }

    fun claimOntologyGas(completion: (Pair<Boolean, Error?>) -> Unit) {
        getGasPrice {
            if (it.second != null) {
                completion(Pair<Boolean, Error?>(false, Error(it.second!!.localizedMessage)))
            } else {
                try {
                    Neoutils.claimONG(PersistentStore.getOntologyNodeURL(), it.first!!, 20000,
                            Account.getWallet()?.wif!!)
                    completion(Pair<Boolean, Error?>(true, null))
                }
                catch (e: Exception) {
                    completion(Pair<Boolean, Error?>(false, Error(e.localizedMessage)))
                }
            }
        }
    }

    fun getGasPrice(completion: (Pair<Long?, Error?>) -> Unit) {
        val url = PersistentStore.getOntologyNodeURL()
        val dataJson = jsonObject(
                "jsonrpc" to "2.0",
                "method" to OntologyRPC.GETGASPRICE.methodName(),
                "params" to jsonArray(),
                "id" to 1
        )
        var request = url.httpPost().body(dataJson.toString())
        request.headers["User-Agent"] = "O3Android"
        request.timeout(600000).responseString { _, _, result ->
            val (data, error) = result
            if (error == null) {
                val gson = Gson()
                val ontologyResponse = gson.fromJson<OntologyDataResponse>(data!!)
                val gasPrice = Gson().fromJson<GasPrice>(ontologyResponse.result)
                completion(Pair<Long?, Error?>(gasPrice.gasprice, null))
            } else {
                completion(Pair<Long?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
}
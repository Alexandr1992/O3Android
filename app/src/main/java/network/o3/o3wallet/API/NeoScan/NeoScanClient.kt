package network.o3.o3wallet.API.NeoScan

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import java.util.*

/**
 * Created by drei on 4/24/18.
 */
class NeoScanClient {
    val baseAPIURL = "https://neoscan.io/api/main_net/v1/"

    enum class Route {
        GET_ADDRESS_ABSTRACTS;

        fun routeName(): String {
            return this.name.toLowerCase(Locale.US) + "/"
        }
    }

    fun getNeoScanTransactionHistory(address: String, page: Int, completion: (Pair<NeoScanTransactionHistory?, Error?>) -> (Unit)) {
        val url = baseAPIURL  + Route.GET_ADDRESS_ABSTRACTS.routeName() + address + "/" + page.toString()
        var request = url.httpGet()
        request.headers["User-Agent"] =  ""
        request.responseString { request, response, result ->
            val (data, error) = result
            print(response)
            if (error == null) {
                val history = Gson().fromJson<NeoScanTransactionHistory>(data!!)
                completion(Pair<NeoScanTransactionHistory?, Error?>(history, null))
            } else {
                completion(Pair<NeoScanTransactionHistory?, Error?>(null, Error(error.localizedMessage)))
            }
        }
    }
}
package network.o3.o3wallet.Dapp

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class DappMessage(val platform: String, val blockchain: String, val messageId: String,
                       val version: String, val command: String, val network: String?,
                       var data: Any)

data class DappMetadata(val title: String?, val iconURL: String?, val description: String?)

typealias GetBalanceRequest = List<NeoDappProtocol.GetBalanceRequestElement>
typealias GetBalanceResponse = MutableMap<String, NeoDappProtocol.GetBalanceResponseElement>
typealias InvokeReadResponse = JsonObject


class NeoDappProtocol {
    data class GetNetworkResponse(val networks: List<String>)

    data class GetAccountResponse(val address: String,
                                  val publicKey: String)

    data class GetProviderResponse(val compatibility: List<String>,
                                   val name: String,
                                   val version: String,
                                   val website: String,
                                   val extra: JsonObject)

    //Get Balances
    data class GetBalanceRequest(val params: List<GetBalanceRequestElement>, val network: String?)
    data class GetBalanceRequestElement(val address: String,
                                        val assets: List<String>)
    data class GetBalanceResponseElement(val amount: String,
                                         val scriptHash: String,
                                         val symbol: String,
                                         val unspent: List<Unspent>) {
        data class Unspent(val n: Int,
                           val txid: String,
                           val value: String)
    }

    data class GetStorageRequest(val scriptHash: String,
                                 val key: String,
                                 val network: String?)



    // Invoke
    data class InvokeReadRequest(val operation: String,
                                 val scriptHash: String,
                                 val args: List<Arg>,
                                 val network: String)

    data class InvokeRequest(val operation: String,
                             val scriptHash: String,
                             val assetIntentOverrides: JsonElement?,
                             val attachedAssets: AttachedAssets?,
                             val triggerContractVerification: Boolean,
                             var fee: String,
                             val args: List<Arg>?,
                             val network: String?) {

        data class AssetIntentOverrides(val inputs: List<Input>,
                                        val outputs: List<Output>) {
            data class Input(val txid: String,
                             val index: Int)

            data class Output(val asset: String,
                              val address: String,
                              val value: String)
        }

        data class AttachedAssets(val GAS: String?,
                                  val NEO: String?)
    }

    data class InvokeResponse(val txid: String,
                              val nodeUrl: String)

    data class SendRequest(val fromAddress: String,
                           var toAddress: String,
                           val asset: String,
                           val amount: String,
                           val remark: String?,
                           var fee: String?,
                           val network: String?
                           )

    data class SendResponse(val txid: String,
                            val nodeUrl: String)

    data class errorResponse(val error: String)


    data class Arg(val type: String,
                   val value: String)

    companion object {
        val availableCommmands = listOf("getProvider", "getNetworks", "getAccount",
                "getBalance", "getStorage", "invokeRead", "invoke", "send", "disconnect")
        val needAuthCommands = listOf("getAccount", "getAddress", "invoke", "send")
    }
}
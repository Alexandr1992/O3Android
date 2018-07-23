package network.o3.o3wallet.API.Ontology

import com.google.gson.JsonObject

data class OntologyDataResponse(val desc: String, val error: Int,
                                val id: Int, val jsonrpc: String,
                                val result: JsonObject)

data class GasPrice(val gasprice: Long, val height: Long)

data class OntologyError(val code: Int, val Id: Int, val result: OntologyErrorResult)
data class OntologyErrorResult(val code: Int, val data: String, val message: String)
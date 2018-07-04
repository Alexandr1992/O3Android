package network.o3.o3wallet.API.Ontology

import android.content.Intent
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import neoutils.Neoutils
import neoutils.SeedNodeResponse
import network.o3.o3wallet.Account
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.PersistentStore
import org.jetbrains.anko.coroutines.experimental.bg
import java.lang.Exception

class OntologyClient {
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

    val testNetNodes = "http://polaris1.ont.io:20336," +
            "http://polaris2.ont.io:20336," +
            "http://polaris3.ont.io:20336," +
            "http://polaris4.ont.io:20336"

    //TODO: SWAP THESE OUT FOR THE MAINNET NODES
    val mainNetNodes = "http://polaris1.ont.io:20336," +
            "http://polaris2.ont.io:20336," +
            "http://polaris3.ont.io:20336," +
            "http://polaris4.ont.io:20336"

    fun transferOntology(node: SeedNodeResponse, assetID: String, toAddress: String, amount: Double): Error? {
        try {
            Neoutils.ontologyTransfer(node.url, Account.getWallet()!!.wif, assetID, toAddress, amount)
            return null
        } catch (e: Exception) {
            return Error("Transfer Failed")
        }

    }

    fun sendOntologyAsset(assetID: String, toAddress: String, amount: Double): Error? {
        if (PersistentStore.getNetworkType() == "Main") {
            val seedResponse = Neoutils.selectBestSeedNode(mainNetNodes)
            return transferOntology(seedResponse, assetID, toAddress, amount)
        } else {
            val seedResponse = Neoutils.selectBestSeedNode(testNetNodes)
            return transferOntology(seedResponse, assetID, toAddress, amount)
        }
    }
}
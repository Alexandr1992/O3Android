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

    fun transferOntology(assetID: String, toAddress: String, amount: Double): Error? {
        try {
            Neoutils.ontologyTransfer(PersistentStore.getOntologyNodeURL(),
                    Account.getWallet()!!.wif, assetID, toAddress, amount)
            return null
        } catch (e: Exception) {
            return Error("Transfer Failed")
        }

    }

    fun sendOntologyAsset(assetID: String, toAddress: String, amount: Double): Error? {
        return transferOntology(assetID, toAddress, amount)
    }
}
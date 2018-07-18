package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import java.text.DecimalFormat
import java.util.*

class TokenSaleReceiptFragment : Fragment() {
    private lateinit var tokenSaleName: String
    private lateinit var txID: String
    private lateinit var assetSendSymbol: String
    private lateinit var assetReceiveSymbol: String
    private lateinit var dateString: String
    private lateinit var assetSendString: String
    private lateinit var assetReceiveString: String

    private var assetSendAmount: Double = 0.0
    private var assetReceiveAmount: Double = 0.0
    private var priorityEnabled: Boolean = false

    lateinit var mView: View


    fun setRecieptValues() {
        val df = DecimalFormat()
        df.maximumFractionDigits = 8

        val txidview = mView.find<TextView>(R.id.receiptTxIdValueTextView)
        txidview.text = txID

        val tokenSaleTextView = mView.find<TextView>(R.id.receiptSaleNameValueTextView)
        tokenSaleTextView.text = tokenSaleName

        if (assetSendSymbol == "NEO") { df.maximumFractionDigits = 0 }
        val assetSendTextView = mView.find<TextView>(R.id.receiptSendingValueTextView)
        assetSendString = df.format(assetSendAmount) + " " + assetSendSymbol
        assetSendTextView.text = assetSendString

        df.maximumFractionDigits = 8
        val assetReceiveTextView = mView.find<TextView>(R.id.receiptForValueTextView)
        assetReceiveString = df.format(assetReceiveAmount) + " " + assetReceiveSymbol
        assetReceiveTextView.text = assetReceiveString

        val dateTextView = mView.find<TextView>(R.id.receiptDateValueTextView)
        dateString = Date().toString()
        dateTextView.text = dateString

        if (!priorityEnabled) {
            mView.find<TextView>(R.id.receiptPriorityValueTextView).visibility = View.INVISIBLE
            mView.find<TextView>(R.id.receiptPriorityLabelTextView).visibility = View.INVISIBLE
        }
    }

    fun initiateReceiptEmail() {
        val emailTextView = mView.find<TextView>(R.id.tokenSaleEmailReceiptTextView)
        emailTextView.setOnClickListener {


            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.data = Uri.parse("mailto:")
            emailIntent.type = "text/plain"

            emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(resources.getString(R.string.TOKENSALE_Email_Title), tokenSaleName))
            val emailString = String.format(resources.getString(R.string.TOKENSALE_Email_Full_text), dateString, tokenSaleName, txID, assetSendString, assetReceiveString)


            emailIntent.putExtra(Intent.EXTRA_TEXT, emailString)
            startActivity(Intent.createChooser(emailIntent, ""))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        mView = inflater.inflate(R.layout.tokensale_receipt_activity, container, false)

        val args = arguments!!
        assetSendSymbol = args.getString("assetSendSymbol")
        assetSendAmount = args.getDouble("assetSendAmount", 0.0)
        assetReceiveSymbol = args.getString("assetReceiveSymbol")
        assetReceiveAmount = args.getDouble("assetReceiveAmount", 0.0)
        priorityEnabled = args.getBoolean("priorityEnabled", false)
        txID = args.getString("transactionID")
        tokenSaleName = args.getString("tokenSaleName")


        val returnButton = mView.find<TextView>(R.id.returnToMainButton)
        returnButton.setOnClickListener {
            activity?.finish()
        }

        setRecieptValues()
        initiateReceiptEmail()
        return mView

    }
}

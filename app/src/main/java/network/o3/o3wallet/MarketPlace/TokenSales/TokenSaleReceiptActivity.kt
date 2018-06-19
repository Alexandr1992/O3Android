package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.R
import java.text.DecimalFormat
import java.util.*

class TokenSaleReceiptActivity : AppCompatActivity() {
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


    fun setRecieptValues() {
        val df = DecimalFormat()
        df.maximumFractionDigits = 8

        val txidview = findViewById<TextView>(R.id.receiptTxIdValueTextView)
        txidview.text = txID

        val tokenSaleTextView = findViewById<TextView>(R.id.receiptSaleNameValueTextView)
        tokenSaleTextView.text = tokenSaleName

        if (assetSendSymbol == "NEO") { df.maximumFractionDigits = 0 }
        val assetSendTextView = findViewById<TextView>(R.id.receiptSendingValueTextView)
        assetSendString = df.format(assetSendAmount) + " " + assetSendSymbol
        assetSendTextView.text = assetSendString

        df.maximumFractionDigits = 8
        val assetReceiveTextView = findViewById<TextView>(R.id.receiptForValueTextView)
        assetReceiveString = df.format(assetReceiveAmount) + " " + assetReceiveSymbol
        assetReceiveTextView.text = assetReceiveString

        val dateTextView = findViewById<TextView>(R.id.receiptDateValueTextView)
        dateString = Date().toString()
        dateTextView.text = dateString

        if (!priorityEnabled) {
            findViewById<TextView>(R.id.receiptPriorityValueTextView).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.receiptPriorityLabelTextView).visibility = View.INVISIBLE
        }
    }

    override fun onBackPressed() {
        returnToMain()
        super.onBackPressed()
    }

    fun returnToMain() {
        intent = Intent(this, MainTabbedActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    fun initiateReceiptEmail() {
        val emailTextView = findViewById<TextView>(R.id.tokenSaleEmailReceiptTextView)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tokensale_receipt_activity)

        assetSendSymbol = intent.getStringExtra("assetSendSymbol")
        assetSendAmount = intent.getDoubleExtra("assetSendAmount", 0.0)
        assetReceiveSymbol = intent.getStringExtra("assetReceiveSymbol")
        assetReceiveAmount = intent.getDoubleExtra("assetReceiveAmount", 0.0)
        priorityEnabled = intent.getBooleanExtra("priorityEnabled", false)
        txID = intent.getStringExtra("transactionID")
        tokenSaleName = intent.getStringExtra("tokenSaleName")


        val returnButton = findViewById<TextView>(R.id.returnToMainButton)
        returnButton.setOnClickListener {
            returnToMain()
        }

        setRecieptValues()
        initiateReceiptEmail()

    }
}

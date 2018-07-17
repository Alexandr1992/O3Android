package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.Account
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.yesButton
import java.text.DecimalFormat

class TokenSaleReviewActivity : AppCompatActivity() {

    private lateinit var bannerURL: String
    private lateinit var assetSendSymbol: String
    private lateinit var assetSendId: String
    private lateinit var assetReceiveSymbol: String
    private lateinit var assetReceiveContractHash: String
    private lateinit var tokenSaleName: String
    private lateinit var tokenSaleWebURL: String

    private var assetSendAmount: Double = 0.0
    private var assetReceiveAmount: Double = 0.0
    private var priorityEnabled: Boolean = false

    private lateinit var participateButton: Button
    private lateinit var loadingConstraintView: ConstraintLayout
    private lateinit var mainConstraintView: ConstraintLayout

    fun initiateViews(whitelisted: Boolean) {
        val sendAmountView = findViewById<TextView>(R.id.tokenSaleReviewSendAmountTextView)
        val receiveAmountTextView = findViewById<TextView>(R.id.tokenSaleReviewReceiveAmountTextView)
        val priorityTextView = findViewById<TextView>(R.id.tokenSaleReviewPriorityTextView)
        val whiteListFloatingActionButton = findViewById<FloatingActionButton>(R.id.whiteListFloatingActionButton)
        val whiteListErrorTextView = findViewById<TextView>(R.id.whiteListErrorTextView)
        val issuerAgreementCheckbox = findViewById<CheckBox>(R.id.issuerDisclaimerCheckbox)
        val o3AgreementCheckbox = findViewById<CheckBox>(R.id.o3DisclaimerCheckbox)

        mainConstraintView = findViewById<ConstraintLayout>(R.id.tokenSaleReviewConstraintView)
        loadingConstraintView = findViewById<ConstraintLayout>(R.id.tokenSaleLoadingConstraintView)


        val df = DecimalFormat()
        if (assetSendSymbol == "NEO") {
            df.maximumFractionDigits = 0
        }  else {
            df.maximumFractionDigits = 8
        }
        sendAmountView.text = df.format(assetSendAmount) + " " + assetSendSymbol
        df.maximumFractionDigits = 8
        receiveAmountTextView.text = df.format(assetReceiveAmount) + " " + assetReceiveSymbol
        if (!priorityEnabled) {
            priorityTextView.visibility = View.GONE
        }

        o3AgreementCheckbox.setOnClickListener {
            if (o3AgreementCheckbox.isChecked && issuerAgreementCheckbox.isChecked) {
                participateButton.isEnabled = true
                participateButton.backgroundColor = resources.getColor(R.color.colorPrimary)
            } else {
                participateButton.isEnabled = false
                participateButton.backgroundColor = resources.getColor(R.color.colorDisabledButton)
            }
        }

        issuerAgreementCheckbox.setOnClickListener {
            if (o3AgreementCheckbox.isChecked && issuerAgreementCheckbox.isChecked) {
                participateButton.isEnabled = true
                participateButton.backgroundColor = resources.getColor(R.color.colorPrimary)
            } else {
                participateButton.isEnabled = false
                participateButton.backgroundColor = resources.getColor(R.color.colorDisabledButton)
            }
        }

        //TODO: READD WHITELISTING WHEN SHIPPING
        if (!whitelisted) {
            whiteListFloatingActionButton.visibility = View.VISIBLE
            whiteListFloatingActionButton.setOnClickListener {
                val browserIntent = Intent(this, DAppBrowserActivity::class.java)
                browserIntent.putExtra("url", tokenSaleWebURL)
                startActivity(browserIntent)
            }
            whiteListErrorTextView.text = resources.getString(R.string.TOKENSALE_Not_Whitelisted)
        } else {
            issuerAgreementCheckbox.visibility = View.VISIBLE
            o3AgreementCheckbox.visibility = View.VISIBLE
            participateButton.visibility = View.VISIBLE
            whiteListErrorTextView.visibility = View.GONE
        }
    }

    fun moveToReceipt(txId: String) {
        val intent = Intent(this, TokenSaleReceiptActivity::class.java)
        intent.putExtra("assetSendSymbol", assetSendSymbol)
        intent.putExtra("assetSendAmount", assetSendAmount)
        intent.putExtra("assetReceiveSymbol", assetReceiveSymbol)
        intent.putExtra("assetReceiveAmount", assetReceiveAmount)
        intent.putExtra("priorityEnabled", priorityEnabled)
        intent.putExtra("transactionID", txId)
        intent.putExtra("tokenSaleName", tokenSaleName)
        intent.putExtra("tokenSaleWebURL", tokenSaleWebURL)
        startActivity(intent)
    }

    fun performMinting() {
        val remark = String.format("O3X%s", tokenSaleName)
        var fee: Double = 0.0
        if (priorityEnabled) { fee = 0.0011 }
        NeoNodeRPC(PersistentStore.getNodeURL()).participateTokenSales(assetReceiveContractHash, assetSendId,
                assetSendAmount, remark, fee) {
            runOnUiThread {
                if (it.second != null) {
                    loadingConstraintView.visibility = View.GONE
                    mainConstraintView.visibility = View.VISIBLE
                    alert(resources.getString(R.string.ALERT_Something_Went_Wrong)) { yesButton { resources.getString(R.string.ALERT_OK_Confirm_Button) } }.show()
                } else if (it.first == null) {
                    loadingConstraintView.visibility = View.GONE
                    mainConstraintView.visibility = View.VISIBLE
                    alert(resources.getString(R.string.ALERT_Something_Went_Wrong)) { yesButton { resources.getString(R.string.ALERT_OK_Confirm_Button) } }.show()
                } else {
                   moveToReceipt(it.first!!)
                }
            }
        }
    }


    fun initiateParticipateButton() {
        participateButton.isEnabled = false
        participateButton.backgroundColor = resources.getColor(R.color.colorDisabledButton)
        participateButton.setOnClickListener {
            mainConstraintView.visibility = View.GONE
            loadingConstraintView.visibility = View.VISIBLE
            val handler = Handler()
            handler.postDelayed( {
                performMinting()
            }, 3000)

        }
    }

    fun parseIntent() {
        bannerURL = intent.getStringExtra("bannerURL")
        assetSendSymbol = intent.getStringExtra("assetSendSymbol")
        assetSendAmount = intent.getDoubleExtra("assetSendAmount", 0.0)
        assetSendId = intent.getStringExtra("assetSendId")
        assetReceiveSymbol = intent.getStringExtra("assetReceiveSymbol")
        assetReceiveContractHash = intent.getStringExtra("assetReceiveContractHash")
        assetReceiveAmount = intent.getDoubleExtra("assetReceiveAmount", 0.0)
        priorityEnabled = intent.getBooleanExtra("priorityEnabled", false)
        tokenSaleName = intent.getStringExtra("tokenSaleName")
        tokenSaleWebURL = intent.getStringExtra("tokenSaleWebURL")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseIntent()
        setContentView(R.layout.tokensale_review_activity)
        val bannerView = findViewById<ImageView>(R.id.tokenSaleReviewBannerImageView)
        participateButton = findViewById<Button>(R.id.tokenSaleReviewParticipateButton)
        Glide.with(this).load(bannerURL).into(bannerView)
        NeoNodeRPC(PersistentStore.getNodeURL()).getWhiteListStatus(assetReceiveContractHash, Account.getWallet()?.address!!) {
            runOnUiThread {
                if (it.second != null) {
                    initiateViews(false)
                    initiateParticipateButton()

                } else {
                    initiateViews(it.first!!)
                    initiateParticipateButton()
                }
            }
        }
    }
}

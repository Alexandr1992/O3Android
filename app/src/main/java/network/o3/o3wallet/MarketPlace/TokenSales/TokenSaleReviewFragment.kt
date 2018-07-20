package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.NEO.TransactionAttribute
import network.o3.o3wallet.Account
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.yesButton
import java.math.BigDecimal
import java.text.DecimalFormat

class TokenSaleReviewFragment : Fragment() {

    private lateinit var bannerURL: String
    private lateinit var assetSendSymbol: String
    private lateinit var assetSendId: String
    private lateinit var assetReceiveSymbol: String
    private lateinit var assetReceiveContractHash: String
    private lateinit var tokenSaleName: String
    private lateinit var tokenSaleWebURL: String
    private lateinit var tokenSaleAddress: String
    private lateinit var tokenSaleCompanyID: String
    private var isVerified: Boolean = false

    private var assetSendAmount: Double = 0.0
    private var assetReceiveAmount: Double = 0.0
    private var priorityEnabled: Boolean = false

    private lateinit var participateButton: Button
    private lateinit var loadingConstraintView: ConstraintLayout
    private lateinit var mainConstraintView: ConstraintLayout
    private lateinit var mView: View

    fun initiateViews(whitelisted: Boolean) {
        val sendAmountView = mView.findViewById<TextView>(R.id.tokenSaleReviewSendAmountTextView)
        val receiveAmountTextView = mView.findViewById<TextView>(R.id.tokenSaleReviewReceiveAmountTextView)
        val priorityTextView = mView.findViewById<TextView>(R.id.tokenSaleReviewPriorityTextView)
        val whiteListFloatingActionButton = mView.findViewById<FloatingActionButton>(R.id.whiteListFloatingActionButton)
        val whiteListErrorTextView = mView.findViewById<TextView>(R.id.whiteListErrorTextView)
        val issuerAgreementCheckbox = mView.findViewById<CheckBox>(R.id.issuerDisclaimerCheckbox)
        val o3AgreementCheckbox = mView.findViewById<CheckBox>(R.id.o3DisclaimerCheckbox)

        mainConstraintView = mView.findViewById<ConstraintLayout>(R.id.tokenSaleReviewConstraintView)
        loadingConstraintView = mView.findViewById<ConstraintLayout>(R.id.tokenSaleLoadingConstraintView)


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
                val browserIntent = Intent(context, DAppBrowserActivity::class.java)
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
        val intent = Intent(context, TokenSaleReceiptFragment::class.java)
        val bundle = Bundle()
        bundle.putString("assetSendSymbol", assetSendSymbol)
        bundle.putDouble("assetSendAmount", assetSendAmount)
        bundle.putString("assetReceiveSymbol", assetReceiveSymbol)
        bundle.putDouble("assetReceiveAmount", assetReceiveAmount)
        bundle.putBoolean("priorityEnabled", priorityEnabled)
        bundle.putString("transactionID", txId)
        bundle.putString("tokenSaleName", tokenSaleName)
        bundle.putString("tokenSaleWebURL", tokenSaleWebURL)
        mView.findNavController().navigate(R.id.action_tokenSaleReviewFragment_to_tokenSaleReceiptFragment, bundle)
    }

    fun performMinting() {
        val remark = String.format("O3X%s", tokenSaleCompanyID)
        var fee: Double = 0.0
        if (priorityEnabled) { fee = 0.0011 }


        //Smart Contract Based Participation
        if (tokenSaleAddress == "") {
            NeoNodeRPC(PersistentStore.getNodeURL()).participateTokenSales(assetReceiveContractHash, assetSendId,
                    assetSendAmount, remark, fee) {
                onUiThread {
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
        } else {
            var asset = NeoNodeRPC.Asset.NEO
            if (assetSendSymbol.toUpperCase() == "GAS") {
                asset = NeoNodeRPC.Asset.GAS
            }
            val attributes = arrayOf(TransactionAttribute().remarkAttribute(remark))
            NeoNodeRPC(PersistentStore.getNodeURL()).sendNativeAssetTransaction(
                    Account.getWallet()!!, asset, BigDecimal(assetSendAmount), tokenSaleAddress, null) {
                onUiThread {
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

    fun parseBundle() {
        val bundle = arguments!!
        bannerURL = bundle.getString("bannerURL")
        assetSendSymbol = bundle.getString("assetSendSymbol")
        assetSendAmount = bundle.getDouble("assetSendAmount", 0.0)
        assetSendId = bundle.getString("assetSendId")
        assetReceiveSymbol = bundle.getString("assetReceiveSymbol")
        assetReceiveContractHash = bundle.getString("assetReceiveContractHash")
        assetReceiveAmount = bundle.getDouble("assetReceiveAmount", 0.0)
        priorityEnabled = bundle.getBoolean("priorityEnabled", false)
        tokenSaleName = bundle.getString("tokenSaleName")
        tokenSaleWebURL = bundle.getString("tokenSaleWebURL")
        isVerified = bundle.getBoolean("verified")
        tokenSaleAddress = bundle.getString("tokenSaleAddress")
        tokenSaleCompanyID = bundle.getString("tokenSaleCompanyID")
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.tokensale_review_fragment, container, false)
        parseBundle()
        val bannerView = mView.findViewById<ImageView>(R.id.tokenSaleReviewBannerImageView)
        participateButton = mView.findViewById<Button>(R.id.tokenSaleReviewParticipateButton)
        Glide.with(this).load(bannerURL).into(bannerView)
        initiateViews(isVerified)
        initiateParticipateButton()
        return mView
    }
}

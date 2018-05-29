package network.o3.o3wallet.TokenSales

import android.content.Intent
import android.net.Uri
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.CardView
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.*
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.API.O3.TokenSale
import network.o3.o3wallet.R
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.NEO.NeoNodeRPC
import network.o3.o3wallet.API.O3.AcceptingAsset
import network.o3.o3wallet.Account
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.afterTextChanged
import org.jetbrains.anko.alert
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor
import org.jetbrains.anko.yesButton
import java.text.DecimalFormat
import network.o3.o3wallet.DecimalDigitsInputFilter
import android.text.InputFilter
import android.view.Gravity


class TokenSaleInfoActivity : AppCompatActivity() {
    lateinit var tokenSale: TokenSale
    lateinit var gasInfo: AcceptingAsset
    lateinit var neoInfo: AcceptingAsset
    lateinit var selectedAsset: AcceptingAsset
    private lateinit var footerView: View
    private lateinit var headerView: View
    private lateinit var amountEditText: EditText
    private lateinit var participateButton: Button
    private lateinit var gasCardBalanceTextView: TextView
    private lateinit var neoCardBalanceTextView: TextView

    var priorityEnabled = false

    private var gasBalance = 0.0
    private var neoBalance = 0

    fun loadBalance() {
        NeoNodeRPC(PersistentStore.getNodeURL()).getAccountState(Account.getWallet()?.address!!) {
            if (it.second != null) {
                return@getAccountState
            }
            var neoAsset =  it.first!!.balances.find { it.asset.contains(NeoNodeRPC.Asset.NEO.assetID()) }
            var gasAsset = it.first!!.balances.find { it.asset.contains(NeoNodeRPC.Asset.GAS.assetID()) }
            gasBalance = gasAsset?.value ?: 0.0
            neoBalance = (neoAsset?.value ?: 0.0).toInt()
            runOnUiThread {
                gasCardBalanceTextView.text =  String.format(resources.getString(R.string.TOKENSALE_Balance), gasBalance.toString())
                neoCardBalanceTextView.text = String.format(resources.getString(R.string.TOKENSALE_Balance), neoBalance.toString())
            }
        }
    }

    fun initiateAssetSelectorCards() {
        val gasCard = footerView.findViewById<CardView>(R.id.gasAssetCardView)
        val neoCard = footerView.findViewById<CardView>(R.id.neoAssetCardView)

        if (gasInfo.basicRate.toInt() == 0) {
            gasCard.visibility = View.GONE
            neoCard.foregroundGravity = Gravity.CENTER_HORIZONTAL
        }


        val gasCardTitleTextView = footerView.findViewById<TextView>(R.id.gasCardTitleTextView)
        val neoCardTitleTextView = footerView.findViewById<TextView>(R.id.neoCardTitleTextView)

        gasCardBalanceTextView = footerView.findViewById<TextView>(R.id.gasCardBalanceTextView)
        neoCardBalanceTextView = footerView.findViewById<TextView>(R.id.neoCardBalanceTextView)

        gasCardTitleTextView.text = resources.getString(R.string.TOKENSALE_Use_Gas)
        neoCardTitleTextView.text = resources.getString(R.string.TOKENSALE_Use_Neo)

        val gasCardDescriptionTextView = footerView.findViewById<TextView>(R.id.gasCardDecriptionTextView)
        val neoCardDescriptionTextView = footerView.findViewById<TextView>(R.id.neoCardDescriptionTextView)
        gasCardDescriptionTextView.text = "1 GAS = " + gasInfo.basicRate + " " + tokenSale.symbol
        neoCardDescriptionTextView.text = "1 NEO = " + neoInfo.basicRate + " " + tokenSale.symbol

        neoCardTitleTextView.textColor = resources.getColor(R.color.colorPrimary)
        neoCardDescriptionTextView.textColor = resources.getColor(R.color.colorAccent)
        neoCardBalanceTextView.textColor = resources.getColor(R.color.colorPrimary)

        gasCardTitleTextView.textColor = resources.getColor(R.color.colorDisabledButton)
        gasCardDescriptionTextView.textColor = resources.getColor(R.color.colorDisabledButton)
        gasCardBalanceTextView.textColor = resources.getColor(R.color.colorDisabledButton)

        selectedAsset = neoInfo

        gasCard.setOnClickListener {
            gasCardTitleTextView.textColor = resources.getColor(R.color.colorPrimary)
            gasCardDescriptionTextView.textColor = resources.getColor(R.color.colorAccent)
            gasCardBalanceTextView.textColor = resources.getColor(R.color.colorPrimary)

            neoCardTitleTextView.textColor = resources.getColor(R.color.colorDisabledButton)
            neoCardDescriptionTextView.textColor = resources.getColor(R.color.colorDisabledButton)
            neoCardBalanceTextView.textColor = resources.getColor(R.color.colorDisabledButton)

            selectedAsset = gasInfo
            amountEditText.text = SpannableStringBuilder("")
            amountEditText.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        neoCard.setOnClickListener {
            neoCardTitleTextView.textColor = resources.getColor(R.color.colorPrimary)
            neoCardDescriptionTextView.textColor = resources.getColor(R.color.colorAccent)
            neoCardBalanceTextView.textColor = resources.getColor(R.color.colorPrimary)

            gasCardTitleTextView.textColor = resources.getColor(R.color.colorDisabledButton)
            gasCardDescriptionTextView.textColor = resources.getColor(R.color.colorDisabledButton)
            gasCardBalanceTextView.textColor = resources.getColor(R.color.colorDisabledButton)

            selectedAsset = neoInfo
            amountEditText.text = SpannableStringBuilder("")
            amountEditText.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL)
        }
    }

    fun updateTokenRecieveAmount() {
        val doubleValue = amountEditText.text.toString().toDoubleOrNull()
        val recieveAmountTextView = footerView.findViewById<TextView>(R.id.tokenSaleRecieveAmountTextView)
        if (doubleValue != null) {
            participateButton.isEnabled = true
            participateButton.backgroundColor = resources.getColor(R.color.colorPrimary)
            val recieveAmount = doubleValue * selectedAsset.basicRate
            val df = DecimalFormat()
            if (recieveAmount - recieveAmount.toLong() == 0.0) {
                df.maximumFractionDigits = 0
                val numString = df.format(recieveAmount)
                recieveAmountTextView.text = numString + " " + tokenSale.symbol
            } else {
                df.maximumFractionDigits = 8
                val numString = df.format(recieveAmount)
                recieveAmountTextView.text = numString + " " + tokenSale.symbol
            }
        } else if (amountEditText.text.toString() == "") {
            recieveAmountTextView.text = "0" + " " + tokenSale.symbol
            participateButton.isEnabled = false
            participateButton.backgroundColor = resources.getColor(R.color.colorDisabledButton)
        }
    }

    fun initiatePartcipationEditText() {
        amountEditText.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL)
        amountEditText.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8, 8))
        amountEditText.afterTextChanged {
            updateTokenRecieveAmount()
        }
    }

    fun initiatePriority() {
        val priorityCheckbox = footerView.findViewById<CheckBox>(R.id.tokenSalePriorityCheckbox)
        priorityCheckbox.setOnClickListener {
            priorityEnabled = !priorityEnabled
        }
        val priorityInfoTextView = footerView.findViewById<TextView>(R.id.priorityInfoTextView)
        priorityInfoTextView.setOnClickListener {
            alert ("Priority uses some of your gas to give your transaction priority in the blockchain. " +
                    "This makes sure you always get in on a token sale before everyone else.") {
                yesButton { resources.getString(R.string.ALERT_OK_Confirm_Button) }
            }.show()
        }
    }

    fun initiateParticipateButton() {
        participateButton = footerView.findViewById(R.id.tokenSaleInfoParticipateButton)
        participateButton.isEnabled = false
        participateButton.backgroundColor = resources.getColor(R.color.colorDisabledButton)
        participateButton.setOnClickListener {

            if (!validateEditText()) {
                return@setOnClickListener
            }

            val tokenSaleReviewIntent = Intent(this, TokenSaleReviewActivity::class.java)
            val sendAssetAmount = amountEditText.text.toString().toDouble()
            tokenSaleReviewIntent.putExtra("bannerURL", tokenSale.imageURL)
            tokenSaleReviewIntent.putExtra("assetSendAmount", sendAssetAmount)
            tokenSaleReviewIntent.putExtra("assetSendSymbol", selectedAsset.asset.toUpperCase())
            val assetID = if (selectedAsset.asset.toUpperCase() == "NEO") {
                NeoNodeRPC.Asset.NEO.assetID()
            } else {
                NeoNodeRPC.Asset.GAS.assetID()
            }

            tokenSaleReviewIntent.putExtra("assetSendId", assetID)
            tokenSaleReviewIntent.putExtra("assetReceiveSymbol", tokenSale.symbol.toUpperCase())
            tokenSaleReviewIntent.putExtra("assetReceiveAmount", sendAssetAmount * selectedAsset.basicRate)
            tokenSaleReviewIntent.putExtra("assetReceiveContractHash", tokenSale.scriptHash)
            tokenSaleReviewIntent.putExtra("withPriority", priorityEnabled)
            tokenSaleReviewIntent.putExtra("tokenSaleName", tokenSale.name)
            tokenSaleReviewIntent.putExtra("tokenSaleWebURL", tokenSale.webURL)
            startActivity(tokenSaleReviewIntent)
        }
    }

    fun validateEditText(): Boolean {
        val doubleValue = amountEditText.text.toString().toDoubleOrNull()
        if (selectedAsset.asset.toUpperCase() == "NEO") {
            if (doubleValue == null) {
                alert(resources.getString(R.string.TOKENSALE_Error_Invalid_Amount)) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (doubleValue - doubleValue.toInt() != 0.0) {
                    alert(resources.getString(R.string.TOKENSALE_Error_Must_Send_Whole_NEO)) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if(doubleValue.toInt() > neoBalance) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Not_Enough_Balance), "NEO")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if(doubleValue.toInt() > neoInfo.max) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Max_Contribution), "NEO")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (doubleValue < neoInfo.min) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Min_Contribution), "NEO")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (priorityEnabled && gasBalance < 0.0011) {
                alert(resources.getString(R.string.TOKENSALE_Error_Not_Enough_For_Priority)) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            }
            return true
        }

        if (selectedAsset.asset.toUpperCase() == "GAS") {
            if (doubleValue == null) {
                alert(resources.getString(R.string.TOKENSALE_Error_Invalid_Amount)) { yesButton {
                    resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (doubleValue > gasBalance) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Not_Enough_Balance), "GAS")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (doubleValue > gasInfo.max) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Max_Contribution), "GAS")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (doubleValue < gasInfo.min) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Min_Contribution), "GAS")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (priorityEnabled && gasBalance - doubleValue < 0.0011) {
                alert(resources.getString(R.string.TOKENSALE_Error_Not_Enough_For_Priority)) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            }
            return true
        }
        return false
    }

    fun initiateActionButton() {
        val fab = headerView.findViewById<FloatingActionButton>(R.id.websiteActionButton)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tokenSale.webURL))
        fab.setOnClickListener {
            startActivity(browserIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tokensale_info_activity)

        val tokenJSON = intent.getStringExtra("TOKENSALE_JSON")
        tokenSale = Gson().fromJson(tokenJSON)
        gasInfo = tokenSale.acceptingAssets.find { it.asset.toUpperCase() == "GAS" } ?: AcceptingAsset("GAS", 0,0.0,0.0)
        neoInfo = tokenSale.acceptingAssets.find { it.asset.toUpperCase() == "NEO" }!!

        val listView = findViewById<ListView>(R.id.tokenInfoListView)
        listView.adapter = TokenSaleInfoAdapter(this)
        (listView.adapter as TokenSaleInfoAdapter).setData(tokenSale)

        headerView = layoutInflater.inflate(R.layout.tokensale_info_header, null)
        val bannerImageView = headerView.findViewById<ImageView>(R.id.tokensaleBannerView)
        Glide.with(this).load(tokenSale.imageURL).into(bannerImageView)
        footerView = layoutInflater.inflate(R.layout.tokensale_info_footer, null)

        amountEditText = footerView.findViewById<EditText>(R.id.tokenSaleParticipationAmountEditText)
        footerView.findViewById<TextView>(R.id.tokenSaleRecieveAmountTextView).text = "0 " + tokenSale.symbol

        initiateAssetSelectorCards()
        initiateParticipateButton()
        initiatePartcipationEditText()
        initiateActionButton()
        initiatePriority()
        loadBalance()
        listView.addHeaderView(headerView)
        listView.addFooterView(footerView)

    }
}

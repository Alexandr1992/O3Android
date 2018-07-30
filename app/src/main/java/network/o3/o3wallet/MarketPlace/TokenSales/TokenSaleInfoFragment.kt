package network.o3.o3wallet.MarketPlace.TokenSales

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
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
import java.text.DecimalFormat
import network.o3.o3wallet.DecimalDigitsInputFilter
import android.text.InputFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onUiThread


class TokenSaleInfoFragment : Fragment() {
    lateinit var tokenSale: TokenSale
    var gasInfo: AcceptingAsset? = null
    var neoInfo: AcceptingAsset? = null
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
            onUiThread {
                gasCardBalanceTextView.text =  String.format(resources.getString(R.string.TOKENSALE_Balance), gasBalance.toString())
                neoCardBalanceTextView.text = String.format(resources.getString(R.string.TOKENSALE_Balance), neoBalance.toString())
            }
        }
    }

    fun initiateAssetSelectorCards() {
        val gasCard = footerView.findViewById<CardView>(R.id.gasAssetCardView)
        val neoCard = footerView.findViewById<CardView>(R.id.neoAssetCardView)

        if (gasInfo == null) {
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
        gasCardDescriptionTextView.text = "1 GAS = " + gasInfo?.basicRate?.toInt() + " " + tokenSale.symbol
        neoCardDescriptionTextView.text = "1 NEO = " + neoInfo?.basicRate?.toInt() + " " + tokenSale.symbol

        neoCardTitleTextView.textColor = resources.getColor(R.color.colorPrimary)
        neoCardDescriptionTextView.textColor = resources.getColor(R.color.colorAccent)
        neoCardBalanceTextView.textColor = resources.getColor(R.color.colorPrimary)

        gasCardTitleTextView.textColor = resources.getColor(R.color.colorDisabledButton)
        gasCardDescriptionTextView.textColor = resources.getColor(R.color.colorDisabledButton)
        gasCardBalanceTextView.textColor = resources.getColor(R.color.colorDisabledButton)

        selectedAsset = neoInfo!!

        if (gasInfo != null) {
            gasCard.setOnClickListener {
                gasCardTitleTextView.textColor = resources.getColor(R.color.colorPrimary)
                gasCardDescriptionTextView.textColor = resources.getColor(R.color.colorAccent)
                gasCardBalanceTextView.textColor = resources.getColor(R.color.colorPrimary)

                neoCardTitleTextView.textColor = resources.getColor(R.color.colorDisabledButton)
                neoCardDescriptionTextView.textColor = resources.getColor(R.color.colorDisabledButton)
                neoCardBalanceTextView.textColor = resources.getColor(R.color.colorDisabledButton)

                selectedAsset = gasInfo!!
                amountEditText.text = SpannableStringBuilder("")
                amountEditText.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            }
        }

        if (neoInfo != null) {
            neoCard.setOnClickListener {
                neoCardTitleTextView.textColor = resources.getColor(R.color.colorPrimary)
                neoCardDescriptionTextView.textColor = resources.getColor(R.color.colorAccent)
                neoCardBalanceTextView.textColor = resources.getColor(R.color.colorPrimary)

                gasCardTitleTextView.textColor = resources.getColor(R.color.colorDisabledButton)
                gasCardDescriptionTextView.textColor = resources.getColor(R.color.colorDisabledButton)
                gasCardBalanceTextView.textColor = resources.getColor(R.color.colorDisabledButton)

                selectedAsset = neoInfo!!
                amountEditText.text = SpannableStringBuilder("")
                amountEditText.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL)
            }
        }
    }

    fun updateTokenRecieveAmount() {
        val doubleValue = amountEditText.text.toString().toDoubleOrNull()
        val recieveAmountTextView = footerView.findViewById<TextView>(R.id.tokenSaleRecieveAmountTextView)
        if (doubleValue != null) {
            participateButton.isEnabled = true
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
        val whatIsPriority = footerView.findViewById<TextView>(R.id.priorityInfoTextView)
        if (tokenSale.address != "") {
            priorityCheckbox.visibility = View.GONE
            whatIsPriority.visibility = View.GONE
        }
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
        participateButton.setOnClickListener {

            if (!validateEditText()) {
                return@setOnClickListener
            }
            val action = TokenSaleReviewFragment()
            val bundle = Bundle()
            val sendAssetAmount = amountEditText.text.toString().toDouble()
            bundle.putString("bannerURL", tokenSale.squareLogoURL)
            bundle.putString("tokenSaleCompanyID", tokenSale.companyID)
            bundle.putString("tokenSaleAddress", tokenSale.address)
            bundle.putDouble("assetSendAmount", sendAssetAmount)
            bundle.putString("assetSendSymbol", selectedAsset.asset.toUpperCase())
            val assetID = if (selectedAsset.asset.toUpperCase() == "NEO") {
                NeoNodeRPC.Asset.NEO.assetID()
            } else {
                NeoNodeRPC.Asset.GAS.assetID()
            }

            bundle.putString("assetSendId", assetID)
            bundle.putString("assetReceiveSymbol", tokenSale.symbol.toUpperCase())
            bundle.putDouble("assetReceiveAmount", sendAssetAmount * selectedAsset.basicRate)
            bundle.putDouble("basicRate", selectedAsset.basicRate)
            bundle.putString("assetReceiveContractHash", tokenSale.scriptHash)
            bundle.putBoolean("withPriority", priorityEnabled)
            bundle.putBoolean("verified", tokenSale.kycStatus.verified)
            bundle.putString("tokenSaleName", tokenSale.name)
            bundle.putString("tokenSaleWebURL", tokenSale.webURL)
            view?.findNavController()?.navigate(R.id.action_tokenSaleInfoFragment_to_tokenSaleReviewFragment, bundle)
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
            } else if(doubleValue.toInt() > neoInfo!!.max) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Max_Contribution), "NEO")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (doubleValue < neoInfo!!.min) {
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
            } else if (doubleValue > gasInfo?.max ?: 0.0) {
                alert(String.format(resources.getString(R.string.TOKENSALE_Error_Max_Contribution), "GAS")) {
                    yesButton {resources.getString(R.string.ALERT_OK_Confirm_Button)} }.show()
                return false
            } else if (doubleValue < gasInfo?.min ?: 0.0) {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tokensale_info_fragment, container, false)
        //NEED DUMMY JSON HERE

        val tokenJSON = activity!!.intent.getStringExtra("TOKENSALE_JSON")
        tokenSale = Gson().fromJson(tokenJSON)
        gasInfo = tokenSale.acceptingAssets.find { it.asset.toUpperCase() == "GAS" }
        neoInfo = tokenSale.acceptingAssets.find { it.asset.toUpperCase() == "NEO" }

        val listView = view.findViewById<ListView>(R.id.tokenInfoListView)
        listView.adapter = TokenSaleInfoAdapter(this.context!!)
        (listView.adapter as TokenSaleInfoAdapter).setData(tokenSale)

        headerView = inflater.inflate(R.layout.tokensale_info_header, null)
        val bannerImageView = headerView.findViewById<ImageView>(R.id.tokensaleBannerView)
        Glide.with(this).load(tokenSale.squareLogoURL).into(bannerImageView)
        footerView = inflater.inflate(R.layout.tokensale_info_footer, null)

        amountEditText = footerView.findViewById<EditText>(R.id.tokenSaleParticipationAmountEditText)
        footerView.findViewById<TextView>(R.id.tokenSaleRecieveAmountTextView).text = "0 " + tokenSale.symbol

        initiateAssetSelectorCards()
        initiateParticipateButton()
        initiatePartcipationEditText()
        initiatePriority()
        loadBalance()
        listView.addHeaderView(headerView)
        listView.addFooterView(footerView)
        return view
    }
}

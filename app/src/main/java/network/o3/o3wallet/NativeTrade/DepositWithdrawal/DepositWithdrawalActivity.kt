package network.o3.o3wallet.NativeTrade.DepositWithdrawal

import android.arch.lifecycle.Observer
import android.content.res.Resources
import android.media.Image
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.ActionBar
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.dialog_backup_key_fragment.*
import kotlinx.android.synthetic.main.native_trade_deposit_withdrawal_activity.*
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.O3RealTimePrice
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.Wallet.SendV2.AssetSelectionBottomSheet
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import network.o3.o3wallet.Wallet.toast
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onLongClick
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class DepositWithdrawalActivity : AppCompatActivity() {
    private lateinit var mView: View
    private lateinit var amountEditText: EditText
    private lateinit var decimalButton: ImageButton
    private lateinit var depositWithDrawalButton: Button
    val viewModel = DepositWithdrawalViewModel()

    private var pricingData = O3RealTimePrice("NEO", PersistentStore.getCurrency(), 0.0, 0)
    var enteredCurrencyDouble = 0.0
    var firstLoad = true


    fun digitTapped(digit: String) {
        amountEditText.text = SpannableStringBuilder(amountEditText.text.toString() + digit)
        calculateAndDisplaySendAmount()
    }

    fun initiatePinPadButtons() {
        mView.find<Button>(R.id.button0).setOnClickListener {
            val curVal = amountEditText.text.toString()
            if (curVal.isNotBlank()) {
                digitTapped("0")
            } else if (viewModel.selectedAssetDecimals > 0) {
                amountEditText.text = SpannableStringBuilder(curVal + "0" + DecimalFormatSymbols().decimalSeparator)
            }
        }

        mView.find<Button>(R.id.button1).setOnClickListener { digitTapped("1") }
        mView.find<Button>(R.id.button2).setOnClickListener { digitTapped("2") }
        mView.find<Button>(R.id.button3).setOnClickListener { digitTapped("3") }
        mView.find<Button>(R.id.button4).setOnClickListener { digitTapped("4") }
        mView.find<Button>(R.id.button5).setOnClickListener { digitTapped("5") }
        mView.find<Button>(R.id.button6).setOnClickListener { digitTapped("6") }
        mView.find<Button>(R.id.button7).setOnClickListener { digitTapped("7") }
        mView.find<Button>(R.id.button8).setOnClickListener { digitTapped("8") }
        mView.find<Button>(R.id.button9).setOnClickListener { digitTapped("9") }

        mView.find<ImageButton>(R.id.buttonBackSpace).setOnClickListener {
            val curVal = amountEditText.text.toString()
            if (curVal.isNotBlank()) {
                amountEditText.text = SpannableStringBuilder(curVal.substring(0, curVal.length - 1))
            }
            calculateAndDisplaySendAmount()
        }

        mView.find<ImageButton>(R.id.buttonBackSpace).onLongClick {
            amountEditText.text = SpannableStringBuilder("")
        }

        decimalButton = mView.find<ImageButton>(R.id.buttonDecimal)
        if (DecimalFormatSymbols().decimalSeparator == ',') {
            decimalButton.image = getDrawable(R.drawable.ic_comma)
        } else {
            decimalButton.image = getDrawable(R.drawable.ic_decimal)
        }

        decimalButton.setOnClickListener {
            if (viewModel.selectedAssetDecimals == 0) {
                return@setOnClickListener
            }

            var currString = amountEditText.text.toString()
            if (currString.contains(DecimalFormatSymbols().decimalSeparator)) {
                return@setOnClickListener
            }
            if (currString.isBlank()) {
                digitTapped("0")
                return@setOnClickListener
            }

            amountEditText.text = SpannableStringBuilder(SpannableStringBuilder(amountEditText.text.toString() + DecimalFormatSymbols().decimalSeparator))
        }
    }

    fun initiateAssetSelector() {
        val assetContainer = mView.find<ConstraintLayout>(R.id.assetSelectorContainer)
        val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", pricingData.symbol)
        Glide.with(this).load(imageURL).into(mView.find(R.id.assetLogoImageView))
        var formatter = NumberFormat.getNumberInstance()
        viewModel.getOwnedAssets(true).observe ( this, Observer { ownedAssets ->
            if(viewModel.selectedAsset?.value == null) {
                val defaultAsset = ownedAssets?.find { it.symbol.toUpperCase() == pricingData.symbol }
                formatter.maximumFractionDigits = 0
                find<TextView>(R.id.assetBalanceTextView).text = formatter.format(defaultAsset?.value ?: 0)
                if (defaultAsset != null) {
                    viewModel.setSelectedAsset(defaultAsset)
                }
            }
        })
        assetContainer.setNoDoubleClickListener(View.OnClickListener { v ->
            val assetSelectorSheet = AssetSelectionBottomSheet()
            viewModel.getOwnedAssets(false).observe ( this, Observer { ownedAssets ->
                //weird bug where this can be added twice potentially dont add until it is fully dismissed and destoryed
                if (assetSelectorSheet.isAdded) {
                    return@Observer
                } else {
                    assetSelectorSheet.assets = ownedAssets!!
                    assetSelectorSheet.show(supportFragmentManager, assetSelectorSheet.tag)
                }
            })
        })

        viewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            formatter.maximumFractionDigits = selectedAsset!!.decimals

            if (viewModel.selectedAssetDecimals > 0) {
                decimalButton.visibility = View.VISIBLE
            } else {
                decimalButton.visibility = View.INVISIBLE
            }

            find<TextView>(R.id.assetBalanceTextView).text = formatter.format(selectedAsset!!.value)
            find<TextView>(R.id.assetNameTextView).text = selectedAsset!!.symbol

            formatter.maximumFractionDigits = viewModel.selectedAssetDecimals
            if (firstLoad) {
                if (viewModel.selectedAsset?.value != null) {
                    viewModel.setSelectedAsset(viewModel.selectedAsset?.value!!)
                }
                val toSendAmount = viewModel.toSendAmount
                if (toSendAmount != BigDecimal.ZERO) {
                    var formatter = NumberFormat.getNumberInstance()
                    formatter.maximumFractionDigits = viewModel.selectedAssetDecimals
                    amountEditText.text = SpannableStringBuilder(formatter.format(toSendAmount))
                }
                firstLoad = false
            }
            calculateAndDisplaySendAmount()
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset.symbol)
            Glide.with(this).load(imageURL).into(find(R.id.assetLogoImageView))
        })
    }

    fun listenForNewPricingData() {
        viewModel.getRealTimePrice(true).observe(this, Observer { realTimePrice ->
            if (realTimePrice == null) {
                pricingData = O3RealTimePrice("NEO", PersistentStore.getCurrency(), 0.0, 0)
                val preloadedAsset = viewModel.getSelectedAsset().value?.symbol?.toUpperCase()
                if (preloadedAsset != null) {
                    pricingData = O3RealTimePrice(preloadedAsset, PersistentStore.getCurrency(), 0.0, 0)
                }

                otherAmountTextView.visibility = View.INVISIBLE
                mView.find<TextView>(R.id.sendPricingUnavailableTextView).visibility = View.VISIBLE
            } else {
                pricingData = realTimePrice!!
                otherAmountTextView.visibility = View.VISIBLE
                mView.find<TextView>(R.id.sendPricingUnavailableTextView).visibility = View.INVISIBLE
            }
            assetBalanceTextView.textColor = getColor(R.color.colorSubtitleGrey)
            val displayedString =  amountEditText.text.toString()
            val usFormattedString = displayedString.replace(',', '.')
            if (usFormattedString == "" || usFormattedString.isEmpty() || BigDecimal(usFormattedString) == BigDecimal.ZERO) {
                otherAmountTextView.visibility = View.INVISIBLE
            } else {
                val amount = pricingData.price *  usFormattedString.toDouble()
                otherAmountTextView.text = amount.formattedFiatString()
                otherAmountTextView.visibility = View.VISIBLE
            }
        })
    }

    fun calculateAndDisplaySendAmount() {
        var displayedString = amountEditText.text.toString()
        val usFormattedString = displayedString.replace(',', '.')

        if (usFormattedString == "" || usFormattedString.isEmpty() || BigDecimal(usFormattedString) == BigDecimal.ZERO) {
            amountEditText.isCursorVisible = false
            assetBalanceTextView.textColor = getColor(R.color.colorSubtitleGrey)
            otherAmountTextView.visibility = View.INVISIBLE
            enteredCurrencyDouble = 0.0
            depositWithDrawalButton.isEnabled = false
            return
        }
        depositWithDrawalButton.isEnabled = true
        amountEditText.isCursorVisible = true
        val amount = pricingData.price * usFormattedString.toDouble()
        enteredCurrencyDouble = amount
        var assetBalance = mView.find<TextView>(R.id.assetBalanceTextView).text.toString()
        if (assetBalance.isNotEmpty()) {
            val balance = NumberFormat.getInstance().parse(assetBalance).toDouble()
            //val underlineVuew = mView.find<View>(R.id.underlineView)
            if (usFormattedString.toDouble() > balance) {
                assetBalanceTextView.textColor = getColor(R.color.colorLoss)
                amountEditText.textColor = getColor(R.color.colorLoss)
                depositWithDrawalButton.isEnabled = false
            } else {
                assetBalanceTextView.textColor = getColor(R.color.colorSubtitleGrey)
                amountEditText.textColor = getColorFromAttr(R.attr.defaultTextColor)
                depositWithDrawalButton.isEnabled = true
            }
            mView.find<TextView>(R.id.otherAmountTextView).text = amount.formattedFiatString()
            otherAmountTextView.visibility = View.VISIBLE
        }


        viewModel.setSelectedSendAmount(BigDecimal(usFormattedString))
    }

    fun initiateDepositWithdrawalButton() {
        depositWithDrawalButton = mView.find(R.id.placeOrderButton)
        depositWithDrawalButton.isEnabled = false
        depositWithDrawalButton.setOnClickListener {
            val symbol = viewModel.selectedAsset!!.value!!.symbol.toUpperCase()
            val amount = (viewModel.toSendAmount * BigDecimal(100000000)).toLong().toString()
            val bundle = Bundle()
            if (viewModel.isDeposit) {
                val resultFragment = DepositWithdrawalResultDialog.newInstance()
                bundle.putBoolean("isDeposit", true)
                resultFragment.arguments = bundle
                resultFragment.show(supportFragmentManager, "depositResult")
                SwitcheoAPI().singleStepDeposit(symbol, amount) {
                    runOnUiThread {
                        if (it.first!! == true) {
                            resultFragment.showSuccess()
                        } else {
                            resultFragment.showFailure()
                        }
                    }
                }
            } else {
                val resultFragment = DepositWithdrawalResultDialog.newInstance()
                bundle.putBoolean("isDeposit", false)
                resultFragment.arguments = bundle
                resultFragment.show(supportFragmentManager, "depositResult")
                SwitcheoAPI().singleStepWithdrawal(symbol, amount) {
                    runOnUiThread {
                        if (it.first!! == true) {
                            resultFragment.showSuccess()
                        } else {
                            resultFragment.showFailure()
                        }
                    }
                }
            }
        }
    }

    fun initiateHeader() {
        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        supportActionBar?.setCustomView(R.layout.actionbar_layout)

        if (viewModel.isDeposit) {
            find<TextView>(R.id.mytext).text = resources.getString(R.string.WALLET_Deposit)
        } else {
            find<TextView>(R.id.mytext).text = resources.getString(R.string.WALLET_Withdraw)
        }
    }

    fun initiateWithDrawAllButton() {
        val withdrawAllButton = mView.find<Button>(R.id.withdrawAllButton)
        if (viewModel.isDeposit) {
            withdrawAllButton.visibility = View.INVISIBLE
        }  else {
            withdrawAllButton.setOnClickListener {
                amountEditText.text = SpannableStringBuilder(find<TextView>(R.id.assetBalanceTextView).text)
                calculateAndDisplaySendAmount()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.isDeposit = intent.getBooleanExtra("isDeposit", true)
        val asset = intent.getStringExtra("asset")
        if (asset != null) {
            pricingData = O3RealTimePrice(asset, PersistentStore.getCurrency(), 0.0, 0)
        }

        mView = layoutInflater.inflate(R.layout.native_trade_deposit_withdrawal_activity, null)
        amountEditText = mView.find(R.id.withdrawalDepositAmountEditText)

        initiateDepositWithdrawalButton()
        initiatePinPadButtons()
        initiateAssetSelector()
        listenForNewPricingData()
        initiateHeader()
        initiateWithDrawAllButton()
        setContentView(mView)
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_Dark, true)
        } else {
            theme.applyStyle(R.style.AppTheme_White, true)
        }
        return theme
    }
}

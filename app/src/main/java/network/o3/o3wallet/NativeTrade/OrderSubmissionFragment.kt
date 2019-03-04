package network.o3.o3wallet.NativeTrade


import android.arch.lifecycle.Observer
import android.graphics.Color
import android.graphics.ColorFilter
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.TextUtils.replace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.Theme
import com.amplitude.api.Amplitude
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.android.synthetic.main.native_trade_order_card.*
import network.o3.o3wallet.*

import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onFocusChange
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files.find
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class OrderSubmissionFragment : Fragment() {
    lateinit var mView: View

    lateinit var baseAssetSelectionContainer: ConstraintLayout
    lateinit var orderAssetAmountEditText: EditText

    lateinit var baseAssetNameTextView: TextView
    lateinit var orderAssetBalanceTextView: TextView
    lateinit var baseAssetAmountEditText: EditText
    lateinit var totalFiatAmountTextView: TextView
    lateinit var pendingOrdersContainer: ConstraintLayout
    lateinit var percentageButtonsLayout: LinearLayout
    lateinit var twentyFivePercentButton: Button
    lateinit var fiftyPercentButton: Button
    lateinit var seventyFivePercentButton: Button
    lateinit var oneHundredPercentButton: Button

    lateinit var baseAssetBalanceTextView: TextView

    lateinit var fiatPriceTextView: TextView
    lateinit var cryptoPriceTextView: TextView

    lateinit var placeOrderButton: Button

    var editingAmountView: EditText? = null

    fun updateAmountsBasedOnInput(amountString: String) {
        val double = amountString.decimalNoGrouping().toDoubleOrNull() ?: 0.0
        if (editingAmountView == baseAssetAmountEditText) {
            (activity as NativeTradeRootActivity).viewModel.setSelectedBaseAssetAmount(double)
        } else {
            (activity as NativeTradeRootActivity).viewModel.setOrderAssetAmount(double)
        }

        val baseAssetAmountWithoutGrouping = baseAssetAmountEditText.text.toString().decimalNoGrouping()
        val baseAssetBalanceWithoutGrouping = baseAssetBalanceTextView.text.toString().decimalNoGrouping()
        if (baseAssetAmountWithoutGrouping.toDoubleOrNull() ?: 0.0 > baseAssetBalanceWithoutGrouping.toDoubleOrNull() ?: 0.0 &&
                (activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            baseAssetAmountEditText.textColor = context!!.getColor(R.color.colorLoss)
        } else {
            baseAssetAmountEditText.textColor = context!!.getColor(R.color.colorPrimary)
        }

        val orderAssetAmountWithoutGrouping = orderAssetAmountEditText.text.toString().decimalNoGrouping()
        val orderAssetBalanceWithoutGrouping = orderAssetBalanceTextView.text.toString().decimalNoGrouping()
        if (orderAssetAmountWithoutGrouping.toDoubleOrNull() ?: 0.0 > orderAssetBalanceWithoutGrouping.toDoubleOrNull() ?: 0.0 &&
                !(activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            orderAssetAmountEditText.textColor = context!!.getColor(R.color.colorLoss)
        } else {
            orderAssetAmountEditText.textColor = context!!.getColor(R.color.colorPrimary)
        }
    }

    fun digitTappedBuyOrder(digit: String) {
        val baseAssetAmountWithoutGrouping = baseAssetAmountEditText.text.toString().decimalNoGrouping()
        val baseAssetBalanceWithoutGrouping = baseAssetBalanceTextView.text.toString().decimalNoGrouping()
        if (baseAssetAmountWithoutGrouping.toDoubleOrNull() ?: 0.0 > baseAssetBalanceWithoutGrouping.toDoubleOrNull() ?: 0.0) {
            baseAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
            if (editingAmountView == baseAssetAmountEditText) {
                return
            }
        }

        if (baseAssetAmountWithoutGrouping.toDoubleOrNull() ?: 0.0 > 10000000) {
            baseAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
            return
        }

        editingAmountView?.text = SpannableStringBuilder(editingAmountView?.text.toString() + digit)
        updateAmountsBasedOnInput(editingAmountView?.text.toString())

        if (baseAssetAmountWithoutGrouping.toDoubleOrNull() ?: 0.0 > baseAssetBalanceWithoutGrouping.toDoubleOrNull() ?: 0.0) {
            baseAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
        }
    }

    fun digitTappedSellOrder(digit: String) {
        val orderAssetAmountWithoutGrouping = orderAssetAmountEditText.text.toString().decimalNoGrouping()
        val orderAssetBalanceWithoutGrouping = orderAssetBalanceTextView.text.toString().decimalNoGrouping()
        if (orderAssetAmountWithoutGrouping.toDoubleOrNull() ?: 0.0 > orderAssetBalanceWithoutGrouping.toDoubleOrNull() ?: 0.0) {
            orderAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
            if (editingAmountView == orderAssetAmountEditText) {
                return
            }
        }

        editingAmountView?.text = SpannableStringBuilder(editingAmountView?.text.toString() + digit)
        updateAmountsBasedOnInput(editingAmountView?.text.toString())

        if (orderAssetAmountWithoutGrouping.toDoubleOrNull() ?: 0.0 > orderAssetBalanceWithoutGrouping.toDoubleOrNull() ?: 0.0) {
            orderAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
        }
    }

    fun digitTapped(digit: String) {
        if (editingAmountView?.text.toString().hasMaxDecimals(8) && editingAmountView == baseAssetAmountEditText) {
            return
        }

        if (editingAmountView?.text.toString().hasMaxDecimals((activity as NativeTradeRootActivity).viewModel.orderPrecision)
                && editingAmountView == orderAssetAmountEditText) {
            return
        }

        if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            digitTappedBuyOrder(digit)
        } else {
            digitTappedSellOrder(digit)
        }
    }

    fun initiatePinPadButtons() {
        mView.find<Button>(R.id.button0).setOnClickListener {
            val curVal = editingAmountView?.text.toString()
            if (curVal.isNotBlank()) {
                digitTapped("0")
            } else  {
                editingAmountView?.text = SpannableStringBuilder(curVal + "0" + DecimalFormatSymbols().decimalSeparator)
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
            val curVal = editingAmountView?.text.toString()
            if (curVal.isNotBlank()) {
                editingAmountView?.text = SpannableStringBuilder(curVal.substring(0, curVal.length - 1))
            }
            updateAmountsBasedOnInput(editingAmountView?.text.toString())
        }

        mView.find<ImageButton>(R.id.buttonBackSpace).onLongClick {
            editingAmountView?.text = SpannableStringBuilder("")
            updateAmountsBasedOnInput(editingAmountView?.text.toString())
        }

        val decimalButton = mView.find<ImageButton>(R.id.buttonDecimal)
        decimalButton.visibility = View.VISIBLE
        if (DecimalFormatSymbols().decimalSeparator == ',') {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_comma)
        } else {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_decimal)
        }

        decimalButton.setOnClickListener {
            var currString = editingAmountView?.text.toString()
            if (currString.contains(DecimalFormatSymbols().decimalSeparator)) {
                return@setOnClickListener
            }
            if (currString.isBlank()) {
                editingAmountView?.text = SpannableStringBuilder("0" + DecimalFormatSymbols().decimalSeparator)
                return@setOnClickListener
            }

            editingAmountView?.text = SpannableStringBuilder(SpannableStringBuilder(editingAmountView?.text.toString() + DecimalFormatSymbols().decimalSeparator))
            updateAmountsBasedOnInput(editingAmountView?.text.toString())
        }
    }

    fun initializeBaseAssetContainer() {
        baseAssetSelectionContainer = mView.find(R.id.baseAssetSelectionContainer)
        baseAssetNameTextView = mView.find(R.id.baseAssetName)
        if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            mView.find<TextView>(R.id.orderTypeForBaseAsset).text = resources.getString(R.string.NATIVE_TRADE_order_base_asset_buy)
        } else {
            mView.find<TextView>(R.id.orderTypeForBaseAsset).text = resources.getString(R.string.NATIVE_TRADE_order_base_asset_sell)
        }


        (activity as NativeTradeRootActivity).viewModel.getTradingAccountBalances().observe(this, Observer { tradingAccount ->
            val orderAsset = tradingAccount!!.switcheo.confirmed.find { it.symbol.toUpperCase() ==  (activity as NativeTradeRootActivity).viewModel.getOrderAsset()}


            orderAssetBalanceTextView.text = (orderAsset?.value?.divide(BigDecimal(100000000.0)) ?: BigDecimal.ZERO).toDouble().removeTrailingZeros()
            baseAssetSelectionContainer.setNoDoubleClickListener(View.OnClickListener { v ->
                val args = Bundle()
                args.putString("trading_account", Gson().toJson(tradingAccount))
                args.putString("order_asset", orderAsset?.symbol ?: "")
                args.putBoolean("is_buy_order", (activity as NativeTradeRootActivity).viewModel.isBuyOrder)
                val assetSelectorSheet = NativeTradeBaseAssetBottomSheet()
                assetSelectorSheet.arguments = args
                assetSelectorSheet.show(activity!!.supportFragmentManager, assetSelectorSheet.tag)
            })
        })

        (activity as NativeTradeRootActivity).viewModel.getSelectedBaseAssetObserver().observe ( this, Observer { selectedAsset ->
            baseAssetNameTextView.text = selectedAsset!!
        })

        (activity as NativeTradeRootActivity).viewModel.getSelectedBaseAssetImageUrl().observe ( this, Observer { url ->
            Glide.with(activity).load(url).into(mView.find<ImageView>(R.id.baseAssetLogo))
        })
    }


    fun initializeOrdersView() {
        (activity as NativeTradeRootActivity).viewModel.getOrders().observe ( this, Observer { orders ->
            if (orders == null) {
                context?.toast("ERROR")
            } else {
                val pendingButton = (activity as NativeTradeRootActivity).find<ImageView>(R.id.pendingOrdersToolbarButton)
                val pendingBadge = (activity as NativeTradeRootActivity).find<TextView>(R.id.pendingOrderCountBadge)
                if (orders.count() > 0) {
                    pendingButton.setColorFilter(ContextCompat.getColor(context!!, R.color.colorPrimary))
                    pendingButton.isEnabled = true
                    pendingBadge.text = orders.count().toString()
                    pendingBadge.visibility = View.VISIBLE
                    pendingButton.visibility = View.VISIBLE
                } else {
                    pendingButton.isEnabled = false
                    pendingButton.setColorFilter(ContextCompat.getColor(context!!, R.color.colorSubtitleGrey))
                    pendingBadge.visibility = View.GONE
                    pendingButton.visibility = View.GONE
                }
            }
        })
    }

    fun initializeOrderAmountSelector() {
        orderAssetBalanceTextView = mView.find(R.id.orderAssetBalanceTextView)
        if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            orderAssetBalanceTextView.visibility = View.INVISIBLE
        }

        orderAssetAmountEditText = mView.find(R.id.orderAssetAmountEditText)
        orderAssetAmountEditText.afterTextChanged {
            orderAssetAmountEditText.setSelection(orderAssetAmountEditText.text.length)
        }


        orderAssetAmountEditText.showSoftInputOnFocus = false
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        orderAssetAmountEditText.onFocusChange { v, hasFocus ->
            if (hasFocus) {
                (activity as NativeTradeRootActivity).viewModel.setIsEditingBaseAmount(false)
            }
        }

        // listen for the other text field and update as necessary
        (activity as NativeTradeRootActivity).viewModel.getSelectedBaseAssetAmount().observe(this, Observer { baseAssetAmount ->
            if (editingAmountView == orderAssetAmountEditText) {
                return@Observer
            }
            val marketPriceCrypto = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.second
            val marketPriceFiat = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.first
            if (marketPriceCrypto != null) {
                val newValue = baseAssetAmount!! / (marketPriceCrypto)
                if (newValue == orderAssetAmountEditText.text.decimalNoGrouping().toDoubleOrNull() ?: 0.0) {
                    return@Observer
                }
                if (newValue == 0.0) {
                    orderAssetAmountEditText.text = SpannableStringBuilder("")
                    placeOrderButton.isEnabled = false
                } else {
                    val newVal = BigDecimal(newValue).setScale((activity as NativeTradeRootActivity).viewModel.orderPrecision, RoundingMode.HALF_UP).toDouble()
                    orderAssetAmountEditText.text = SpannableStringBuilder(newVal.removeTrailingZeros())
                    placeOrderButton.isEnabled = true
                }
            }

            if (marketPriceFiat != null) {
                val newTotalFiat = baseAssetAmount!! / (marketPriceCrypto!!) * (marketPriceFiat)
                totalFiatAmountTextView.text = SpannableStringBuilder(newTotalFiat.formattedFiatString())
            }
        })
    }

    fun initializeBaseAmountSelector() {
        baseAssetAmountEditText = mView.find(R.id.baseAssetAmountEditText)
        baseAssetAmountEditText .afterTextChanged {
            baseAssetAmountEditText.setSelection(baseAssetAmountEditText.text.length)
        }
        baseAssetAmountEditText.showSoftInputOnFocus = false
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        baseAssetAmountEditText.onFocusChange  { v, hasFocus ->
            if (hasFocus) {
                (activity as NativeTradeRootActivity).viewModel.setIsEditingBaseAmount(true)
            }
        }

        // listen for the other text field and update as necessary
        (activity as NativeTradeRootActivity).viewModel.getOrderAssetAmount().observe(this, Observer { orderAssetAmount ->
            if (editingAmountView == baseAssetAmountEditText) {
                return@Observer
            }

            val marketPriceCrypto = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.second
            val marketPriceFiat = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.first
            if (marketPriceCrypto != null ) {
                val newValue = orderAssetAmount!! * (marketPriceCrypto)
                if (newValue == 0.0) {
                    baseAssetAmountEditText.text = SpannableStringBuilder("")
                    placeOrderButton.isEnabled = false
                } else {
                    baseAssetAmountEditText.text = SpannableStringBuilder(newValue.removeTrailingZeros())
                    placeOrderButton.isEnabled = true
                }
            }

            if (marketPriceFiat != null) {
                val newTotalFiat = orderAssetAmount!! * marketPriceFiat
                totalFiatAmountTextView.text = SpannableStringBuilder(newTotalFiat.formattedFiatString())
            }
        })
    }

    fun initiateEditingFieldListener() {
        (activity as NativeTradeRootActivity).viewModel.isEditingBaseAmount().observe ( this, Observer { isEditingBaseAmount ->
            if (isEditingBaseAmount!!) {
                editingAmountView = baseAssetAmountEditText
                if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
                    percentageButtonsLayout.visibility = View.VISIBLE
                } else {
                    percentageButtonsLayout.visibility = View.GONE
                }
            } else {
                editingAmountView = orderAssetAmountEditText
                if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
                    percentageButtonsLayout.visibility = View.GONE
                } else {
                    percentageButtonsLayout.visibility = View.VISIBLE
                }
            }
        })
        (activity as NativeTradeRootActivity).viewModel.setIsEditingBaseAmount(false)
    }

    fun initializeRealTimePriceListeners() {
        val priceRow = mView.find<ConstraintLayout>(R.id.priceConstraintLayout)
        priceRow.setOnClickListener {
            mView.findNavController().navigate(R.id.action_orderSubmissionFragment_to_priceSelectionFragment)
        }

        fiatPriceTextView = mView.find(R.id.selectedFiatPriceTextView)
        cryptoPriceTextView = mView.find(R.id.selectedCryptoPriceTextView)


        (activity as NativeTradeRootActivity).viewModel.getSelectedPrice().observe( this, Observer { marketPrice ->
            fiatPriceTextView.text = marketPrice!!.first.formattedFiatString()
            cryptoPriceTextView.text = marketPrice.second.removeTrailingZeros()
        })
    }

    fun initializeAssetBalanceListener() {
        baseAssetBalanceTextView = mView.find<TextView>(R.id.baseAssetBalanceTextView)
        if (!(activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            baseAssetBalanceTextView.visibility = View.INVISIBLE
        }

        (activity as NativeTradeRootActivity).viewModel.getSelectedBaseAssetBalance().observe(this, Observer { balance ->
            baseAssetBalanceTextView.text = balance?.removeTrailingZeros()
        })
    }

    fun initiateOrderButton() {
        placeOrderButton = mView.find(R.id.placeOrderButton)
        if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            placeOrderButton.background = ContextCompat.getDrawable(this.activity!!, R.drawable.buy_button_background)
        } else {
            placeOrderButton.background = ContextCompat.getDrawable(this.activity!!, R.drawable.sell_button_background)
        }

        if (baseAssetAmountEditText.text.decimalNoGrouping().toDoubleOrNull() ?: 0.0 == 0.0) {
            placeOrderButton.isEnabled = false
        }
        placeOrderButton.setOnClickListener {

            if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
                if (baseAssetAmountEditText.text.decimalNoGrouping().toDoubleOrNull() ?: 0.0 >
                        baseAssetBalanceTextView.text.decimalNoGrouping().toDoubleOrNull() ?: 0.0) {
                    alert("You need a larger balance in your trading account").show()
                    Amplitude.getInstance().logEvent("Not_enough_trading_balance_error")
                } else {
                    mView.findNavController().navigate(R.id.action_orderSubmissionFragment_to_reviewOrderFragment)
                }
            } else {
                if (orderAssetAmountEditText.text.decimalNoGrouping().toDoubleOrNull() ?: 0.0 >
                        orderAssetBalanceTextView.text.decimalNoGrouping().toDoubleOrNull() ?: 0.0) {
                    alert("You need a larger balance in your trading account").show()
                    Amplitude.getInstance().logEvent("Not_enough_trading_balance_error")
                } else {
                    mView.findNavController().navigate(R.id.action_orderSubmissionFragment_to_reviewOrderFragment)
                }
            }
        }
    }

    fun percentageTapped(ratio: Double) {
        val vm = (activity as NativeTradeRootActivity).viewModel
        val isBuyOrder = vm.isBuyOrder
        if (isBuyOrder) {
            val baseAssetBalance = baseAssetBalanceTextView.text.toString().decimalNoGrouping().toDouble()
            var doubleString = (baseAssetBalance * ratio).toString().replace(',', DecimalFormatSymbols().decimalSeparator)
            doubleString = doubleString.replace('.', DecimalFormatSymbols().decimalSeparator)
            baseAssetAmountEditText.text = SpannableStringBuilder(doubleString)
        } else {
            val orderAssetBalance = orderAssetBalanceTextView.text.toString().decimalNoGrouping().toDouble()
            var doubleString = (orderAssetBalance * ratio).toString().replace(',', DecimalFormatSymbols().decimalSeparator)
            doubleString = doubleString.replace('.', DecimalFormatSymbols().decimalSeparator)
            orderAssetAmountEditText.text = SpannableStringBuilder(doubleString)
        }
        updateAmountsBasedOnInput(editingAmountView?.text.toString())
    }

    fun initiatePercentageButtons() {
        percentageButtonsLayout = mView.find(R.id.percentageButtonsLayout)
        twentyFivePercentButton = mView.find(R.id.twentyFivePercentButton)
        fiftyPercentButton = mView.find(R.id.fiftyPercentButton)
        seventyFivePercentButton = mView.find(R.id.seventyFivePercentButton)
        oneHundredPercentButton = mView.find(R.id.oneHundredPercentButton)


        twentyFivePercentButton.setOnClickListener {
            percentageTapped(0.25)
        }

        fiftyPercentButton.setOnClickListener {
            percentageTapped(0.5)
        }

        seventyFivePercentButton.setOnClickListener {
            percentageTapped(0.75)
        }

        oneHundredPercentButton.setOnClickListener {
            percentageTapped(1.0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.native_trade_order_submission_fragment, container, false)
        totalFiatAmountTextView = mView.find(R.id.totalFiatAmountTextView)
        initializeBaseAssetContainer()
        initializeOrdersView()
        initializeOrderAmountSelector()
        initializeBaseAmountSelector()
        initiateOrderButton()
        initiatePinPadButtons()
        initiateEditingFieldListener()
        initializeRealTimePriceListeners()
        initializeAssetBalanceListener()
        initiatePercentageButtons()
        return mView
    }
}

package network.o3.o3wallet.NativeTrade


import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.android.synthetic.main.send_success_fragment.*

import network.o3.o3wallet.R
import network.o3.o3wallet.afterTextChanged
import network.o3.o3wallet.formattedFiatString
import network.o3.o3wallet.setNoDoubleClickListener
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onFocusChange
import org.jetbrains.anko.sdk15.coroutines.onLongClick
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import java.text.DecimalFormatSymbols

class OrderSubmissionFragment : Fragment() {
    lateinit var mView: View

    lateinit var baseAssetSelectionContainer: ConstraintLayout
    lateinit var orderAssetAmountEditText: EditText

    lateinit var baseAssetNameTextView: TextView
    lateinit var baseAssetAmountEditText: EditText
    lateinit var totalFiatAmountTextView: TextView
    lateinit var pendingOrdersContainer: ConstraintLayout

    lateinit var fiatPriceTextView: TextView
    lateinit var cryptoPriceTextView: TextView


    var editingAmountView: EditText? = null

    fun updateAmountsBasedOnInput(amountString: String) {
        val double = amountString.toDoubleOrNull() ?: 0.0
        if (editingAmountView == baseAssetAmountEditText) {
            (activity as NativeTradeRootActivity).viewModel.setSelectedBaseAssetAmount(double)
        } else {
            (activity as NativeTradeRootActivity).viewModel.setOrderAssetAmount(double)
        }
    }

    fun digitTapped(digit: String) {
        editingAmountView?.text = SpannableStringBuilder(editingAmountView?.text.toString() + digit)
        updateAmountsBasedOnInput(editingAmountView?.text.toString())
    }

    fun initiatePinPadButtons() {
        mView.find<Button>(R.id.button0).setOnClickListener {
            val curVal = editingAmountView?.text.toString()
            /*if (curVal.isNotBlank()) {
                editingAmountView?.text = SpannableStringBuilder(curVal + "0")
            } else if ((activity as NativeTradeRootActivity).viewModel.selectedAssetDecimals > 0) {
                editingAmountView?.text = SpannableStringBuilder(curVal + "0" + DecimalFormatSymbols().decimalSeparator)
            }*/
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
        if (DecimalFormatSymbols().decimalSeparator == ',') {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_comma)
        } else {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_decimal)
        }

        decimalButton.setOnClickListener {
            /*if ((activity as NativeTradeRootActivity).viewModel.selectedAssetDecimals == 0) {
                return@setOnClickListener
            }*/

            var currString = editingAmountView?.text.toString()
            if (currString.isBlank() || currString.contains(DecimalFormatSymbols().decimalSeparator)) {
                return@setOnClickListener
            }
            editingAmountView?.text = SpannableStringBuilder(SpannableStringBuilder(editingAmountView?.text.toString() + DecimalFormatSymbols().decimalSeparator))
            updateAmountsBasedOnInput(editingAmountView?.text.toString())
        }
    }

    fun initializeBaseAssetContainer() {
        baseAssetSelectionContainer = mView.find(R.id.baseAssetSelectionContainer)
        baseAssetNameTextView = mView.find(R.id.baseAssetName)

        (activity as NativeTradeRootActivity).viewModel.getTradingAccountBalances().observe(this, Observer { tradingAccount ->
            baseAssetSelectionContainer.setNoDoubleClickListener(View.OnClickListener { v ->
                val args = Bundle()
                args.putString("trading_account", Gson().toJson(tradingAccount!!))
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

        (activity as NativeTradeRootActivity).viewModel.setSelectedBaseAssetValue("NEO")
        (activity as NativeTradeRootActivity).viewModel.setSelectedBaseAssetImageUrl("https://cdn.o3.network/img/neo/NEO.png")
    }

    fun initializeOrdersView() {
        pendingOrdersContainer = mView.find(R.id.pendingOrdersContainer)
        val pendingsOrderLabel: TextView = mView.find(R.id.pendingOrdersLabel)
        (activity as NativeTradeRootActivity).viewModel.getOrders().observe ( this, Observer { orders ->
            if (orders == null) {
                context?.toast("ERROR")
            } else {
                pendingsOrderLabel.text = String.format(resources.getString(R.string.NATIVE_TRADE_pendings_orders_count), orders.count())
                pendingOrdersContainer.visibility = View.VISIBLE
            }
        })
    }

    fun initializeOrderAmountSelector() {
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
            val marketPriceCrypto = (activity as NativeTradeRootActivity).viewModel.marketPrice?.value?.second
            val marketPriceFiat = (activity as NativeTradeRootActivity).viewModel.marketPrice?.value?.first
            if (marketPriceCrypto != null) {
                val newValue = baseAssetAmount!! / (marketPriceCrypto)
                orderAssetAmountEditText.text = SpannableStringBuilder(newValue.toString())
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
        baseAssetAmountEditText.onFocusChange { v, hasFocus ->
            if (hasFocus) {
                (activity as NativeTradeRootActivity).viewModel.setIsEditingBaseAmount(true)
            }
        }

        // listen for the other text field and update as necessary
        (activity as NativeTradeRootActivity).viewModel.getOrderAssetAmount().observe(this, Observer { orderAssetAmount ->
            val marketPriceCrypto = (activity as NativeTradeRootActivity).viewModel.marketPrice?.value?.second
            val marketPriceFiat = (activity as NativeTradeRootActivity).viewModel.marketPrice?.value?.first
            if (marketPriceCrypto != null ) {
                val newValue = orderAssetAmount!! * (marketPriceCrypto)
                baseAssetAmountEditText.text = SpannableStringBuilder(newValue.toString())
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
            } else {
                editingAmountView = orderAssetAmountEditText
            }
        })
        (activity as NativeTradeRootActivity).viewModel.setIsEditingBaseAmount(false)
    }

    fun initializeRealTimePriceListeners() {
        fiatPriceTextView = mView.find(R.id.selectedFiatPriceTextView)
        cryptoPriceTextView = mView.find(R.id.selectedCryptoPriceTextView)

        (activity as NativeTradeRootActivity).viewModel.getMarketPrice().observe( this, Observer { marketPrice ->
            fiatPriceTextView.text = marketPrice!!.first.formattedFiatString()
            cryptoPriceTextView.text = marketPrice.second.toString()
        })
    }

    fun initializeAssetBalanceListener() {
        (activity as NativeTradeRootActivity).viewModel.getSelectedBaseAssetBalance().observe(this, Observer { balance ->
            mView.find<TextView>(R.id.baseAssetBalanceTextView).text = balance?.toString()
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.native_trade_order_submission_fragment, container, false)
        totalFiatAmountTextView = mView.find(R.id.totalFiatAmountTextView)
        initializeBaseAssetContainer()
        initializeOrdersView()
        initializeOrderAmountSelector()
        initializeBaseAmountSelector()
        initiatePinPadButtons()
        initiateEditingFieldListener()
        initializeRealTimePriceListeners()
        initializeAssetBalanceListener()

        return mView
    }
}

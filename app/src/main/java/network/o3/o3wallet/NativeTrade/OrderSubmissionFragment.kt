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
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.gson.Gson

import network.o3.o3wallet.R
import network.o3.o3wallet.afterTextChanged
import network.o3.o3wallet.formattedFiatString
import network.o3.o3wallet.setNoDoubleClickListener
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onFocusChange
import org.jetbrains.anko.sdk15.coroutines.onLongClick
import org.jetbrains.anko.textColor
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

    lateinit var baseAssetBalanceTextView: TextView

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

        if (baseAssetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0 > baseAssetBalanceTextView.text.toString().toDoubleOrNull() ?: 0.0) {
            baseAssetAmountEditText.textColor = context!!.getColor(R.color.colorLoss)
        } else {
            baseAssetAmountEditText.textColor = context!!.getColor(R.color.colorPrimary)
        }
    }

    fun digitTapped(digit: String) {
        if (baseAssetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0 > baseAssetBalanceTextView.text.toString().toDoubleOrNull() ?: 0.0) {
            baseAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
            if (editingAmountView == baseAssetAmountEditText) {
                return
            }
        }

        if (baseAssetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0 > 10000000) {
            baseAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
            return
        }

        editingAmountView?.text = SpannableStringBuilder(editingAmountView?.text.toString() + digit)
        updateAmountsBasedOnInput(editingAmountView?.text.toString())

        if (baseAssetAmountEditText.text.toString().toDoubleOrNull() ?: 0.0 > baseAssetBalanceTextView.text.toString().toDoubleOrNull() ?: 0.0) {
            baseAssetAmountEditText.startAnimation(AnimationUtils.loadAnimation(context!!, R.anim.shake))
        }
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

    /*
    fun initializeOrdersView() {
        pendingOrdersContainer = mView.find(R.id.pendingOrdersContainer)
        pendingOrdersContainer.setOnClickListener {
            mView.findNavController().navigate(R.id.action_orderSubmissionFragment_to_ordersListFragment)
        }
        val pendingsOrderLabel: TextView = mView.find(R.id.pendingOrdersLabel)
        (activity as NativeTradeRootActivity).viewModel.getOrders().observe ( this, Observer { orders ->
            if (orders == null) {
                context?.toast("ERROR")
            } else {
                pendingsOrderLabel.text = String.format(resources.getString(R.string.NATIVE_TRADE_pendings_orders_count), orders.count())
                pendingOrdersContainer.visibility = View.VISIBLE
            }
        })
    }*/

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
            val marketPriceCrypto = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.second
            val marketPriceFiat = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.first
            if (marketPriceCrypto != null) {
                val newValue = baseAssetAmount!! / (marketPriceCrypto)
                orderAssetAmountEditText.text = SpannableStringBuilder("%.8f".format(newValue))
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
            val marketPriceCrypto = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.second
            val marketPriceFiat = (activity as NativeTradeRootActivity).viewModel.selectedPrice?.value?.first
            if (marketPriceCrypto != null ) {
                val newValue = orderAssetAmount!! * (marketPriceCrypto)
                baseAssetAmountEditText.text = SpannableStringBuilder("%.8f".format(newValue))
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
        val priceRow = mView.find<ConstraintLayout>(R.id.priceConstraintLayout)
        priceRow.setOnClickListener {
            mView.findNavController().navigate(R.id.action_orderSubmissionFragment_to_priceSelectionFragment)
        }

        fiatPriceTextView = mView.find(R.id.selectedFiatPriceTextView)
        cryptoPriceTextView = mView.find(R.id.selectedCryptoPriceTextView)


        (activity as NativeTradeRootActivity).viewModel.getSelectedPrice().observe( this, Observer { marketPrice ->
            fiatPriceTextView.text = marketPrice!!.first.formattedFiatString()
            cryptoPriceTextView.text = marketPrice.second.toString()
        })
    }

    fun initializeAssetBalanceListener() {
        baseAssetBalanceTextView = mView.find<TextView>(R.id.baseAssetBalanceTextView)
        (activity as NativeTradeRootActivity).viewModel.getSelectedBaseAssetBalance().observe(this, Observer { balance ->
            baseAssetBalanceTextView.text = balance?.toString()
        })
    }

    fun initiateOrderButton() {
        mView.find<TextView>(R.id.placeOrderButton).setOnClickListener {
            val vm = (activity as NativeTradeRootActivity).viewModel
            val pair = vm.orderAsset + "_" + vm.selectedBaseAsset?.value!!
            val price = cryptoPriceTextView.text.toString()
            val side = "buy"
            val wantAmount = (orderAssetAmountEditText.text.toString().toDouble() * 100000000.0).toLong().toString()
            val orderType = "limit"
            mView.findNavController().navigate(R.id.action_orderSubmissionFragment_to_reviewOrderFragment)


           /* SwitcheoAPI().singleStepOrder(pair, side, price, wantAmount, orderType) {
                if(it.first != true) {
                    //TODO: "Some serios error occured"
                } else {
                    mView.findNavController().navigate(R.id.action_orderSubmissionFragment_to_orderPlacedFragment)

                }
            }*/
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.native_trade_order_submission_fragment, container, false)
        totalFiatAmountTextView = mView.find(R.id.totalFiatAmountTextView)
        initializeBaseAssetContainer()
        //initializeOrdersView()
        initializeOrderAmountSelector()
        initializeBaseAmountSelector()
        initiatePinPadButtons()
        initiateEditingFieldListener()
        initializeRealTimePriceListeners()
        initializeAssetBalanceListener()
        initiateOrderButton()

        return mView
    }
}

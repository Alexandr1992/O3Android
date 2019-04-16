package network.o3.o3wallet.NativeTrade

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.pinpad_layout.*
import network.o3.o3wallet.*
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.textColor
import java.text.DecimalFormatSymbols
import kotlin.math.absoluteValue


// TODO: Rename parameter arguments, choose names that match

class PriceSelectionFragment : Fragment() {
    lateinit var mView: View
    lateinit var priceEditText: EditText
    lateinit var fiatPriceTextView: TextView
    var precision = 2

    fun digitTapped(digit: String) {
        (activity as NativeTradeRootActivity).viewModel.priceSelectionType = "manual"
        if (priceEditText.text.toString().hasMaxDecimals(precision)) {
            return
        }

        priceEditText.text = SpannableStringBuilder(priceEditText.text.toString() + digit)
        (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.decimalNoGrouping().toDouble())
        placeOrderButton.isEnabled = priceEditText.text.decimalNoGrouping().toDouble() > 0.0
    }

    fun initiatePinPadButtons() {
        mView.find<Button>(R.id.button0).setOnClickListener {
            val curVal = priceEditText.text.toString()
            if (curVal.isNotBlank()) {
                priceEditText.text = SpannableStringBuilder(curVal + "0")
            } else {
                priceEditText.text = SpannableStringBuilder(curVal + "0" + DecimalFormatSymbols().decimalSeparator)
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
            val curVal = priceEditText.text.toString()
            if (curVal.isNotBlank()) {
                priceEditText.text = SpannableStringBuilder(curVal.substring(0, curVal.length - 1))
            }
            if (priceEditText.text.toString() == "") {
                (activity as NativeTradeRootActivity).viewModel.setManualPrice(0.0)
            } else {
                (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.decimalNoGrouping().toDouble())
            }

            placeOrderButton.isEnabled = priceEditText.text.decimalNoGrouping().toDoubleOrNull() ?: 0.0 != 0.0
        }

        mView.find<ImageButton>(R.id.buttonBackSpace).onLongClick {
            priceEditText.text = SpannableStringBuilder("")
            placeOrderButton.isEnabled = false
            (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.decimalNoGrouping().toDouble())
        }

        val decimalButton = mView.find<ImageButton>(R.id.buttonDecimal)
        decimalButton.visibility = View.VISIBLE
        if (DecimalFormatSymbols().decimalSeparator == ',') {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_comma)
        } else {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_decimal)
        }

        decimalButton.setOnClickListener {
            var currString = priceEditText.text.toString()
            if (currString.contains(DecimalFormatSymbols().decimalSeparator)) {
                return@setOnClickListener
            }
            if (currString.isBlank()) {
                priceEditText.text = SpannableStringBuilder("0" + DecimalFormatSymbols().decimalSeparator)
                return@setOnClickListener
            }
            priceEditText.text = SpannableStringBuilder(SpannableStringBuilder(priceEditText.text.toString() + DecimalFormatSymbols().decimalSeparator))
        }
    }

    fun initiatePriceListeners() {
        (activity as NativeTradeRootActivity).viewModel.getSelectedPrice().observe(this, Observer { prices ->
            fiatPriceTextView.text = prices!!.first.formattedFiatString()
        })
    }

    fun initiatePriceEditText() {
        priceEditText = mView.find(R.id.priceEditText)
        priceEditText.showSoftInputOnFocus = false
        fiatPriceTextView = mView.find(R.id.fiatPriceTextView)
        priceEditText.afterTextChanged {
            priceEditText.setSelection(priceEditText.text.length)
        }

        priceEditText.text = SpannableStringBuilder((activity as NativeTradeRootActivity).
                viewModel.selectedPrice!!.value!!.second.removeTrailingZeros())
        fiatPriceTextView.text = (activity as NativeTradeRootActivity).
                viewModel.selectedPrice!!.value!!.first.formattedFiatString()
    }

    fun initiateIncrementButtons() {
        mView.find<Button>(R.id.plusButton).setOnClickListener {
            (activity as NativeTradeRootActivity).viewModel.priceSelectionType = "percentage"
            val vm = (activity as NativeTradeRootActivity).viewModel
            var currentDifference = vm.marketRateDifference.value!!
            currentDifference += 0.01
            val newPrice = vm.marketPrice!!.second * currentDifference
            vm.setManualPrice(newPrice)
            priceEditText.text = SpannableStringBuilder(("%." + precision +"f").format(newPrice))

        }

        mView.find<Button>(R.id.minusButton).setOnClickListener {
            (activity as NativeTradeRootActivity).viewModel.priceSelectionType = "percentage"
            val vm = (activity as NativeTradeRootActivity).viewModel
            var currentDifference = vm.marketRateDifference.value!!
            currentDifference -= 0.01
            val newPrice = vm.marketPrice!!.second * currentDifference
            vm.setManualPrice(newPrice)
            priceEditText.text = SpannableStringBuilder(("%." + precision +"f").format(newPrice))
        }
    }

    fun initiateAdvancedPriceSelection() {
        mView.find<TextView>(R.id.manualEntryButton).setOnClickListener {
            val constraintSet = ConstraintSet()
            val constraintLayout = mView.find<ConstraintLayout>(R.id.pricingConstraints)
            constraintSet.clone(constraintLayout)
            constraintSet.clear(R.id.pricingCard, ConstraintSet.TOP)
            constraintSet.clear(R.id.pricingCard, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.pricingCard, ConstraintSet.TOP, R.id.pricingConstraints, ConstraintSet.TOP, 16)
            constraintSet.applyTo(constraintLayout)
            mView.find<ConstraintLayout>(R.id.priceSelectionPinPad).visibility = View.VISIBLE
            mView.find<TextView>(R.id.manualEntryButton).visibility = View.GONE
            mView.find<Button>(R.id.placeOrderButton).text = context!!.getString(R.string.ONBOARDING_done_action)
            mView.find<Button>(R.id.placeOrderButton).onClick {
                activity?.onBackPressed()
            }
        }
    }

    fun initiatePercentDifference() {
        val percentDiffTextView = mView.find<TextView>(R.id.percentDifferenceTextView)
        (activity as NativeTradeRootActivity).viewModel.getMarketRatePercentDifference().observe(this, Observer { rate ->
            var percent = (rate!! - 1.0) * 100
            if (percent == 0.0) {
                percentDiffTextView.text =
                        String.format(resources.getString(R.string.NATIVE_Trade_percent_below_market), percent.formattedPercentString())
                percentDiffTextView.textColor = context!!.getColor(R.color.colorSubtitleGrey)
            } else if (percent < 0.0) {
                percentDiffTextView.text =
                        String.format(resources.getString(R.string.NATIVE_Trade_percent_below_market), percent.absoluteValue.formattedPercentString())
            } else {
                percentDiffTextView.text =
                        String.format(resources.getString(R.string.NATIVE_Trade_percent_above_market), percent.formattedPercentString())
                percentDiffTextView.textColor = context!!.getColor(R.color.colorLoss)
            }

            if(percent.absoluteValue > 10.0) {
                percentDiffTextView.textColor = context!!.getColor(R.color.colorLoss)
            } else {
                percentDiffTextView.textColor = context!!.getColorFromAttr(R.attr.defaultTextColor)

            }
        })
    }

    fun initiateTopOrderBookPrice() {
        val vm = (activity as NativeTradeRootActivity).viewModel
        val topOrderLabel = mView.find<TextView>(R.id.topOrderBookPrice)
        vm.getOrderBookTopPrice().observe(this, Observer { rate ->
            onUiThread {
                topOrderLabel.text = ("%." + precision +"f").format(rate)
            }
        })

        topOrderLabel.setOnClickListener {
            if (topOrderLabel.text.decimalNoGrouping().toDoubleOrNull() != null) {
                (activity as NativeTradeRootActivity).viewModel.priceSelectionType = "top_order"
                priceEditText.text = SpannableStringBuilder(topOrderLabel.text)
                vm.setManualPrice(topOrderLabel.text.decimalNoGrouping().toDoubleOrNull()!!)
            }
        }

        val medianPriceLabel = mView.find<TextView>(R.id.currentMedianPriceTextView)
        medianPriceLabel.text =
                vm.marketPrice!!.second.removeTrailingZeros()
        medianPriceLabel.setOnClickListener {
            (activity as NativeTradeRootActivity).viewModel.priceSelectionType = "median"
            priceEditText.text = SpannableStringBuilder(medianPriceLabel.text)
            vm.setManualPrice(vm.marketPrice!!.second)
        }
    }

    fun initiateEstimatedFill() {
        onUiThread {
            mView.find<TextView>(R.id.instantFillLabel).visibility = View.GONE
            mView.find<TextView>(R.id.estimatedFillAmount).visibility = View.GONE
        }
            //TODO: MAYBE readd Instant fill in the future
            /*
            if ((activity as NativeTradeRootActivity).viewModel.orderAssetAmount.value ?: 0.0 == 0.0) {
            } else {
                mView.find<TextView>(R.id.estimatedFillAmount).visibility = View.VISIBLE
                mView.find<TextView>(R.id.instantFillLabel).visibility = View.VISIBLE
            }
        }

        (activity as NativeTradeRootActivity).viewModel.getFillAmount().observe(this, Observer { fillAmount ->
            if ((activity as NativeTradeRootActivity).viewModel.orderAssetAmount.value ?: 0.0 == 0.0) {
                mView.find<TextView>(R.id.estimatedFillAmount).visibility = View.GONE
                mView.find<TextView>(R.id.instantFillLabel).visibility = View.GONE
            } else {
                mView.find<TextView>(R.id.estimatedFillAmount).visibility = View.VISIBLE
                mView.find<TextView>(R.id.instantFillLabel).visibility = View.VISIBLE
            }
            mView.find<TextView>(R.id.estimatedFillAmount).text = (fillAmount!! * 100).formattedPercentString()
        })*/

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.native_trade_price_selection_fragment, container, false)
        precision = (activity as NativeTradeRootActivity).viewModel.pricePrecision
        initiateEstimatedFill()
        initiatePinPadButtons()
        initiatePriceEditText()
        initiatePriceListeners()
        initiateIncrementButtons()
        initiateAdvancedPriceSelection()
        initiatePercentDifference()
        initiateTopOrderBookPrice()
        (activity as NativeTradeRootActivity).viewModel.loadTopOrderBookPrice()
        return mView
    }
}

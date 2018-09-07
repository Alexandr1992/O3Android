package network.o3.o3wallet.NativeTrade

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

import network.o3.o3wallet.R
import network.o3.o3wallet.afterTextChanged
import network.o3.o3wallet.formattedFiatString
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onLongClick
import java.text.DecimalFormatSymbols

// TODO: Rename parameter arguments, choose names that match

class PriceSelectionFragment : Fragment() {
    lateinit var mView: View
    lateinit var priceEditText: EditText
    lateinit var fiatPriceTextView: TextView

    fun digitTapped(digit: String) {
        priceEditText.text = SpannableStringBuilder(priceEditText.text.toString() + digit)
        (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.toString().toDouble())
    }

    fun initiatePinPadButtons() {
        mView.find<Button>(R.id.button0).setOnClickListener {
            val curVal = priceEditText.text.toString()
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
            val curVal = priceEditText?.text.toString()
            if (curVal.isNotBlank()) {
                priceEditText?.text = SpannableStringBuilder(curVal.substring(0, curVal.length - 1))
            }
            if (priceEditText.text.toString() == "") {
                (activity as NativeTradeRootActivity).viewModel.setManualPrice(0.0)
            } else {
                (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.toString().toDouble())
            }


        }

        mView.find<ImageButton>(R.id.buttonBackSpace).onLongClick {
            priceEditText.text = SpannableStringBuilder("")
            (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.toString().toDouble())
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

            var currString = priceEditText.text.toString()
            if (currString.isBlank() || currString.contains(DecimalFormatSymbols().decimalSeparator)) {
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
        fiatPriceTextView = mView.find(R.id.fiatPriceTextView)
        priceEditText.afterTextChanged {
            priceEditText.setSelection(priceEditText.text.length)
        }

        priceEditText.text = SpannableStringBuilder((activity as NativeTradeRootActivity).
                viewModel.selectedPrice!!.value!!.second.toString())
        fiatPriceTextView.text = (activity as NativeTradeRootActivity).
                viewModel.selectedPrice!!.value!!.first.formattedFiatString()
    }

    fun initiateIncrementButtons() {
        mView.find<Button>(R.id.plusButton).setOnClickListener {
            (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.toString().toDouble() * 1.1)
            priceEditText.text = SpannableStringBuilder("%.8f".format(priceEditText.text.toString().toDouble() * 1.1))

        }

        mView.find<Button>(R.id.minusButton).setOnClickListener {
            (activity as NativeTradeRootActivity).viewModel.setManualPrice(priceEditText.text.toString().toDouble() * 0.9)
            priceEditText.text = SpannableStringBuilder("%.8f".format(priceEditText.text.toString().toDouble() * 0.9))
        }
    }

    fun initiateAdvancedPriceSelection() {
        mView.find<TextView>(R.id.manualEntryButton).setOnClickListener {
            mView.find<ConstraintLayout>(R.id.priceSelectionPinPad).visibility = View.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.native_trade_price_selection_fragment, container, false)
        initiatePinPadButtons()
        initiatePriceEditText()
        initiatePriceListeners()
        initiateIncrementButtons()
        initiateAdvancedPriceSelection()
        return mView
    }
}

package network.o3.o3wallet.Wallet.SendV2


import android.arch.lifecycle.Observer
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import org.jetbrains.anko.find
import com.xw.repo.BubbleSeekBar
import kotlinx.android.synthetic.main.send_what_fragment.*
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.O3RealTimePrice
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.textColor
import org.w3c.dom.Text
import java.math.BigDecimal
import java.text.NumberFormat
import network.o3.o3wallet.R.id.view
import android.content.Context.INPUT_METHOD_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.util.Log
import android.view.inputmethod.InputMethodManager
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onLongClick
import java.text.DecimalFormatSymbols
import kotlin.math.floor
import android.app.Activity
import android.support.v4.content.ContextCompat.getSystemService




class SendWhatFragment : Fragment() {
    private lateinit var mView: View
    private lateinit var amountEditText: EditText
    private lateinit var amountCurrencyEditText: EditText
    private lateinit var otherAmountTextView: TextView
    private lateinit var reviewButton: Button
    private lateinit var decimalButton: ImageButton

    var currentAssetFilters: Array<InputFilter>? = null
    var currentAssetInputType =  (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL)
    var firstLoad = true

    var ownedAssets: ArrayList<TransferableAsset> = arrayListOf()
    private var pricingData =  O3RealTimePrice("NEO", PersistentStore.getCurrency(), 0.0, 0)
    var enteredCurrencyDouble = 0.0

    fun setupFiatEntrySwap() {
        otherAmountTextView = mView.find<TextView>(R.id.otherAmountTextView)
        otherAmountTextView.text = 0.0.formattedFiatString()
    }

    fun digitTapped(digit: String) {
        resetPercentButtonSelections()
        amountEditText.text = SpannableStringBuilder(amountEditText.text.toString() + digit)
    }

    fun initiatePinPadButtons() {
        mView.find<Button>(R.id.button0).setOnClickListener {
            val curVal = amountEditText.text.toString()
            if (curVal.isNotBlank()) {
                amountEditText.text = SpannableStringBuilder(curVal + "0")
            } else if ((activity as SendV2Activity).sendViewModel.selectedAssetDecimals > 0) {
                amountEditText.text = SpannableStringBuilder(curVal + "0" + DecimalFormatSymbols().decimalSeparator)
            }
            resetPercentButtonSelections()
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
            resetPercentButtonSelections()
        }

        mView.find<ImageButton>(R.id.buttonBackSpace).onLongClick {
            amountEditText.text = SpannableStringBuilder("")
        }

        decimalButton = mView.find<ImageButton>(R.id.buttonDecimal)
        if (DecimalFormatSymbols().decimalSeparator == ',') {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_comma)
        } else {
            decimalButton.image = context!!.getDrawable(R.drawable.ic_decimal)
        }

        decimalButton.setOnClickListener {
            if ((activity as SendV2Activity).sendViewModel.selectedAssetDecimals == 0) {
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

    fun resetPercentButtonSelections() {
        mView.find<Button>(R.id.twentyFivePercentButton).textColor = context!!.getColor(R.color.colorSubtitleGrey)
        mView.find<Button>(R.id.fiftyPercentButton).textColor = context!!.getColor(R.color.colorSubtitleGrey)
        mView.find<Button>(R.id.seventyFivePercentButton).textColor = context!!.getColor(R.color.colorSubtitleGrey)
        mView.find<Button>(R.id.oneHundredPercentButton).textColor = context!!.getColor(R.color.colorSubtitleGrey)
    }

    fun updateAmountPercentButton(ratio: Double, selectedButton: Button) {
        val selectedAsset = (activity as SendV2Activity).sendViewModel.getSelectedAsset().value
        if (selectedAsset == null) {
            return
        }

        var cryptoAmount = ratio * selectedAsset.value.toDouble()
        if (cryptoAmount == 0.0) {
            return
        }

        if (selectedAsset.symbol == "ONG") {
            cryptoAmount = cryptoAmount - 0.01
        }

        selectedButton.textColor = context!!.getColor(R.color.colorAccent)

        if (selectedAsset.decimals == 0) {
            cryptoAmount = floor(cryptoAmount)
        }
        var formatter = NumberFormat.getNumberInstance()
        formatter.maximumFractionDigits = selectedAsset.decimals
        formatter.isGroupingUsed = false
        amountEditText.text = SpannableStringBuilder(formatter.format(cryptoAmount))
    }

    fun setupPercentageButtons() {
        val percent25Button = mView.find<Button>(R.id.twentyFivePercentButton)
        val percent50Button = mView.find<Button>(R.id.fiftyPercentButton)
        val percent75Button = mView.find<Button>(R.id.seventyFivePercentButton)
        val percent100Button = mView.find<Button>(R.id.oneHundredPercentButton)

        percent25Button.setOnClickListener {
            resetPercentButtonSelections()
            updateAmountPercentButton(0.25, percent25Button)
        }

        percent50Button.setOnClickListener {
            resetPercentButtonSelections()
            updateAmountPercentButton(0.50, percent50Button)
        }

        percent75Button.setOnClickListener {
            resetPercentButtonSelections()
            updateAmountPercentButton(0.75, percent75Button)
        }

        percent100Button.setOnClickListener {
            resetPercentButtonSelections()
            updateAmountPercentButton(1.00, percent100Button)
        }
    }

    fun initiateAssetSelector() {
        val assetContainer = mView.find<ConstraintLayout>(R.id.assetSelectorContainer)
        val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", "NEO")
        Glide.with(this).load(imageURL).into(mView.find(R.id.assetLogoImageView))
        var formatter = NumberFormat.getNumberInstance()
        (activity as SendV2Activity).sendViewModel.getOwnedAssets(true).observe ( this, Observer { ownedAssets ->
            if((activity as SendV2Activity).sendViewModel.selectedAsset?.value == null) {
                val neoAsset = ownedAssets?.find { it.symbol.toUpperCase() == "NEO" }
                formatter.maximumFractionDigits = 0
                find<TextView>(R.id.assetBalanceTextView).text = formatter.format(neoAsset?.value ?: 0)
                if (neoAsset != null) {
                    (activity as SendV2Activity).sendViewModel.setSelectedAsset(neoAsset)
                }
            }
        })
        assetContainer.setNoDoubleClickListener(View.OnClickListener { v ->
            val assetSelectorSheet = AssetSelectionBottomSheet()
            (activity as SendV2Activity).sendViewModel.getOwnedAssets(false).observe ( this, Observer { ownedAssets ->
                //weird bug where this can be added twice potentially dont add until it is fully dismissed and destoryed
                if (assetSelectorSheet.isAdded) {
                    return@Observer
                } else {
                    assetSelectorSheet.assets = ownedAssets!!
                    assetSelectorSheet.show(activity!!.supportFragmentManager, assetSelectorSheet.tag)
                }
            })
        })

        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            formatter.maximumFractionDigits = selectedAsset!!.decimals
            if (selectedAsset.decimals > 0) {
                decimalButton.visibility = View.VISIBLE
            } else {
                decimalButton.visibility = View.INVISIBLE
            }

            find<TextView>(R.id.assetBalanceTextView).text = formatter.format(selectedAsset.value)
            find<TextView>(R.id.assetNameTextView).text = selectedAsset.symbol

            if (firstLoad) {
                if ((activity as SendV2Activity).sendViewModel.selectedAsset?.value != null) {
                    (activity as SendV2Activity).sendViewModel.setSelectedAsset((activity as SendV2Activity).sendViewModel.selectedAsset?.value!!)
                }
                val toSendAmount = (activity as SendV2Activity).sendViewModel.toSendAmount
                if (toSendAmount != BigDecimal.ZERO) {
                    var formatter = NumberFormat.getNumberInstance()
                    formatter.maximumFractionDigits = selectedAsset.decimals
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
        (activity as SendV2Activity).sendViewModel.getRealTimePrice(true).observe(this, Observer { realTimePrice ->
            if (realTimePrice == null) {
                pricingData = O3RealTimePrice("NEO", PersistentStore.getCurrency(), 0.0, 0)
                val preloadedAsset = (activity as SendV2Activity).sendViewModel.getSelectedAsset().value?.symbol?.toUpperCase()
                if (preloadedAsset != null) {
                   pricingData = O3RealTimePrice(preloadedAsset, PersistentStore.getCurrency(), 0.0, 0)
                }

                otherAmountTextView.visibility = View.INVISIBLE
                mView.find<TextView>(R.id.sendPricingUnavailableTextView).visibility = View.VISIBLE
            } else {
                pricingData = realTimePrice
                otherAmountTextView.visibility = View.VISIBLE
                mView.find<TextView>(R.id.sendPricingUnavailableTextView).visibility = View.INVISIBLE
            }
            assetBalanceTextView.textColor = context!!.getColor(R.color.colorSubtitleGrey)
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
        if (activity == null) {
            return
        }
        var displayedString = amountEditText.text.toString()
        val usFormattedString = displayedString.replace(',', '.')

        if (usFormattedString == "" || usFormattedString.isEmpty() || BigDecimal(usFormattedString) == BigDecimal.ZERO) {
            amountEditText.isCursorVisible = false
            assetBalanceTextView.textColor = context!!.getColor(R.color.colorSubtitleGrey)
            otherAmountTextView.visibility = View.INVISIBLE
            enteredCurrencyDouble = 0.0
            reviewButton.isEnabled = false
            return
        }
        reviewButton.isEnabled  = true
        amountEditText.isCursorVisible = true
        val amount = pricingData.price *  usFormattedString.toDouble()
        enteredCurrencyDouble = amount
        var assetBalance = mView.find<TextView>(R.id.assetBalanceTextView).text.toString()
        if (assetBalance.isNotEmpty()) {
            val balance = NumberFormat.getInstance().parse(assetBalance).toDouble()
            val underlineVuew = mView.find<View>(R.id.underlineView)
            if (usFormattedString.toDouble() > balance) {
                assetBalanceTextView.textColor = context!!.getColor(R.color.colorLoss)
                amountEditText.textColor = context!!.getColor(R.color.colorLoss)
                reviewButton.isEnabled = false
            } else {
                assetBalanceTextView.textColor = context!!.getColor(R.color.colorSubtitleGrey)
                amountEditText.textColor = context!!.getColorFromAttr(R.attr.defaultTextColor)
                reviewButton.isEnabled = true
            }
            mView.find<TextView>(R.id.otherAmountTextView).text = amount.formattedFiatString()
            otherAmountTextView.visibility = View.VISIBLE
        }


        (activity as SendV2Activity).sendViewModel.setSelectedSendAmount(BigDecimal(usFormattedString))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.send_what_fragment, container, false)
        amountEditText = mView.find(R.id.amountEditText)
        reviewButton = mView.find(R.id.placeOrderButton)
        reviewButton.isEnabled = false
        reviewButton.setOnClickListener {
            mView.findNavController().navigate(R.id.action_sendWhatFragment_to_sendReviewFragment)
        }

        listenForNewPricingData()
        mView.find<EditText>(R.id.amountEditText).afterTextChanged { calculateAndDisplaySendAmount() }
        setupFiatEntrySwap()
        initiateAssetSelector()
        setupPercentageButtons()
        initiatePinPadButtons()
        amountEditText.isCursorVisible = false

        return mView
    }
}

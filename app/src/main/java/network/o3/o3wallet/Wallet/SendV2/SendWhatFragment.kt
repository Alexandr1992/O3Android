package network.o3.o3wallet.Wallet.SendV2


import android.arch.lifecycle.Observer
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.renderscript.ScriptGroup
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import org.jetbrains.anko.find
import com.xw.repo.BubbleSeekBar
import kotlinx.android.synthetic.main.onboarding_verify_paper_key_activity.*
import kotlinx.android.synthetic.main.send_what_fragment.*
import kotlinx.android.synthetic.main.tabbar_activity_main_tabbed.*
import kotlinx.coroutines.experimental.channels.Send
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.O3RealTimePrice
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.API.O3Platform.TransferableBalance
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.sdk15.coroutines.textChangedListener
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.textColor
import org.w3c.dom.Text
import java.math.BigDecimal
import java.text.NumberFormat
import network.o3.o3wallet.R.id.view
import android.content.Context.INPUT_METHOD_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.view.inputmethod.InputMethodManager
import kotlin.math.floor


class SendWhatFragment : Fragment() {
    private lateinit var mView: View
    private lateinit var amountEditText: EditText
    private lateinit var amountCurrencyEditText: EditText
    private lateinit var otherAmountTextView: TextView
    private lateinit var reviewButton: Button
    private var pricingData =  O3RealTimePrice("NEO", PersistentStore.getCurrency(), 0.0, 0)
    var currentAssetFilters: Array<InputFilter>? = null
    var currentAssetInputType =  (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL)
    var firstLoad = true

    var ownedAssets: ArrayList<TransferableAsset> = arrayListOf()
    var enteredCurrencyDouble = 0.0


    fun setupFiatEntrySwap() {
        otherAmountTextView = mView.find<TextView>(R.id.otherAmountTextView)
        otherAmountTextView.text = 0.0.formattedFiatString()
    }

    fun setUpSeekBar() {
        val mBubbleSeekBar = mView.find<BubbleSeekBar>(R.id.bubbleSeekBar)
        mBubbleSeekBar.setCustomSectionTextArray(BubbleSeekBar.CustomSectionTextArray { sectionCount, array ->
            array.clear()
            array.put(0, "0%")
            array.put(1, "25%")
            array.put(2, "50%")
            array.put(3, "75%")
            array.put(4, "MAX")
            array
        })

        mBubbleSeekBar.onProgressChangedListener = object : BubbleSeekBar.OnProgressChangedListenerAdapter() {
            override fun onProgressChanged(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(activity!!, Observer { selectedAsset ->
                    var ratio = progressFloat / 100
                    var cryptoAmount = ratio * selectedAsset!!.value.toDouble()
                    if (selectedAsset.decimals == 0) {
                        cryptoAmount = floor(cryptoAmount)
                    }

                    amountEditText.text = SpannableStringBuilder((cryptoAmount).toString())
                })
            }

            override fun getProgressOnActionUp(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float) {
            }

            override fun getProgressOnFinally(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
            }
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
                assetSelectorSheet.assets = ownedAssets!!
                assetSelectorSheet.show(activity!!.supportFragmentManager, assetSelectorSheet.tag)
            })
        })

        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            formatter.maximumFractionDigits = selectedAsset!!.decimals
            find<TextView>(R.id.assetBalanceTextView).text = formatter.format(selectedAsset.value)
            find<TextView>(R.id.assetNameTextView).text = selectedAsset!!.symbol
            var displayedDecimals = selectedAsset.decimals
            if (selectedAsset.decimals == 0) {
                currentAssetInputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL)
                currentAssetFilters = arrayOf<InputFilter>()
            } else {
                currentAssetInputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                currentAssetFilters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8, displayedDecimals))
            }
            amountEditText.inputType = currentAssetInputType
            amountEditText.filters = currentAssetFilters
            if (firstLoad) {
                if((activity as SendV2Activity).sendViewModel.selectedAsset?.value != null) {
                    (activity as SendV2Activity).sendViewModel.setSelectedAsset((activity as SendV2Activity).sendViewModel.selectedAsset?.value!!)
                }
                val toSendAmount = (activity as SendV2Activity).sendViewModel.toSendAmount
                if (toSendAmount != BigDecimal.ZERO) {
                    mView.find<EditText>(R.id.amountEditText).text = SpannableStringBuilder(toSendAmount.toDouble().toString())
                }
                firstLoad = false
            } else {
                amountEditText.text.clear()
            }

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
                pricingData = realTimePrice!!
                otherAmountTextView.visibility = View.VISIBLE
                mView.find<TextView>(R.id.sendPricingUnavailableTextView).visibility = View.INVISIBLE
            }
            assetBalanceTextView.textColor = resources.getColor(R.color.colorSubtitleGrey)
            otherAmountTextView.text = 0.0.formattedFiatString()
        })
    }

    fun calculateAndDisplaySendAmount() {
        if (activity == null) {
            return
        }
        var displayedString = amountEditText.text.toString()
        if (displayedString == ".") {
            amountEditText.isCursorVisible = true
            amountEditText.text = SpannableStringBuilder("0.")
            amountEditText.setSelection(amountEditText.text.length)
            return
        }

        if (displayedString == "" || displayedString.isEmpty() || BigDecimal(displayedString) == BigDecimal.ZERO) {
            amountEditText.isCursorVisible = false
            otherAmountTextView.text = 0.0.formattedFiatString()
            enteredCurrencyDouble = 0.0
            reviewButton.isEnabled = false
            return
        }
        reviewButton.isEnabled  = true
        amountEditText.isCursorVisible = true
        val amount = pricingData.price *  displayedString.toDouble()
        enteredCurrencyDouble = amount
        var assetBalance = find<TextView>(R.id.assetBalanceTextView).text.toString()
        if (assetBalance.isNotEmpty()) {
            val balance = NumberFormat.getInstance().parse(assetBalance).toDouble()
            val underlineVuew = mView.find<View>(R.id.underlineView)
            if (displayedString.toDouble() > balance) {
                assetBalanceTextView.textColor = resources.getColor(R.color.colorLoss)
                amountEditText.textColor = resources.getColor(R.color.colorLoss)
                reviewButton.isEnabled = false
            } else {
                assetBalanceTextView.textColor = resources.getColor(R.color.colorSubtitleGrey)
                amountEditText.textColor = resources.getColor(R.color.colorBlack)
                reviewButton.isEnabled = true
            }
            mView.find<TextView>(R.id.otherAmountTextView).text = amount.formattedFiatString()
        }
        (activity as SendV2Activity).sendViewModel.setSelectedSendAmount(BigDecimal(displayedString))
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.send_what_fragment, container, false)
        amountEditText = mView.find(R.id.amountEditText)
        reviewButton = mView.find(R.id.sendWhereButton)
        reviewButton.isEnabled = false
        reviewButton.setOnClickListener {
            //some devices you ahve to force keyboard down
            val imm = (activity as SendV2Activity).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm!!.hideSoftInputFromWindow(view?.getWindowToken(), 0)
            mView.findNavController().navigate(R.id.action_sendWhatFragment_to_sendReviewFragment)
        }
        listenForNewPricingData()
        mView.find<EditText>(R.id.amountEditText).afterTextChanged { calculateAndDisplaySendAmount() }
        setupFiatEntrySwap()
        initiateAssetSelector()
        setUpSeekBar()
        amountEditText.isCursorVisible = false

        return mView
    }
}

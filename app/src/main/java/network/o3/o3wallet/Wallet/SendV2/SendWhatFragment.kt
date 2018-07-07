package network.o3.o3wallet.Wallet.SendV2


import android.arch.lifecycle.Observer
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
import org.jetbrains.anko.sdk15.coroutines.textChangedListener
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.find
import org.w3c.dom.Text
import java.text.NumberFormat


class SendWhatFragment : Fragment() {
    private lateinit var mView: View
    private lateinit var amountEditText: EditText
    private lateinit var amountCurrencyEditText: EditText
    private lateinit var otherAmountTextView: TextView
    private var pricingData =  O3RealTimePrice("NEO", PersistentStore.getCurrency(), 0.0, 0)
    var currentAssetFilters: Array<InputFilter>? = null
    var currentAssetInputType =  (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NULL)

    var ownedAssets: ArrayList<TransferableAsset> = arrayListOf()
    var enteredCurrencyDouble = 0.0


    fun setupFiatEntrySwap() {
        otherAmountTextView = mView.find<TextView>(R.id.otherAmountTextView)
        otherAmountTextView.text = 0.0.formattedFiatString()

        mView.find<ImageButton>(R.id.swapFiatEntryType).setOnClickListener {
            val isFiatEntry = (activity as SendV2Activity).sendViewModel.toggleFiatEntryType()
            //revert the values back to zero on flip for ease of use
            val currentEditText = amountEditText.text.toString()
            val currentSubEditText = otherAmountTextView.text
            amountEditText.text = SpannableStringBuilder(currentSubEditText)
            otherAmountTextView.text = currentEditText
            if(!isFiatEntry) {
                amountEditText.inputType = currentAssetInputType
                amountEditText.filters = currentAssetFilters
            } else {
                amountEditText.inputType = (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                amountEditText.filters = arrayOf()
            }


            if (currentEditText == "") {
                if (!isFiatEntry) {
                    otherAmountTextView.text = 0.0.formattedFiatString()
                } else {
                    otherAmountTextView.text = 0.0.toString()
                }
            }
        }
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
                    val cryptoAmount = ratio * selectedAsset!!.value.toDouble()
                    if ( (activity as SendV2Activity).sendViewModel.isFiatEntryType) {
                        val amountFiat = cryptoAmount * pricingData.price
                        amountEditText.text = SpannableStringBuilder(amountFiat.formattedFiatString())
                    } else {
                        amountEditText.text = SpannableStringBuilder((ratio * cryptoAmount).toString())
                    }
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
        (activity as SendV2Activity).sendViewModel.getOwnedAssets(false).observe ( this, Observer { ownedAssets ->
            val neoAsset = ownedAssets?.find { it.symbol.toUpperCase() == "NEO" }
            formatter.maximumFractionDigits = 0
            find<TextView>(R.id.assetBalanceTextView).text = formatter.format(neoAsset?.value ?: 0)
            if (neoAsset != null) {
                (activity as SendV2Activity).sendViewModel.setSelectedAsset(neoAsset)
            }
        })
        assetContainer.setOnClickListener {
            val assetSelectorSheet = AssetSelectionBottomSheet()
            (activity as SendV2Activity).sendViewModel.getOwnedAssets(false).observe ( this, Observer { ownedAssets ->
                assetSelectorSheet.assets = ownedAssets!!
                assetSelectorSheet.show(activity!!.supportFragmentManager, assetSelectorSheet.tag)
            })
        }

        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            formatter.maximumFractionDigits = selectedAsset!!.decimals
            find<TextView>(R.id.assetBalanceTextView).text = formatter.format(selectedAsset.value)
            find<TextView>(R.id.assetNameTextView).text = selectedAsset!!.symbol
            if ((activity as SendV2Activity).sendViewModel.isFiatEntryType) {
                amountEditText.filters = null
            } else {
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
                amountEditText.text.clear()
            }

            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset.symbol)
            Glide.with(this).load(imageURL).into(find(R.id.assetLogoImageView))
        })
    }

    fun listenForNewPricingData() {
        (activity as SendV2Activity).sendViewModel.getRealTimePrice(true).observe(this, Observer { realTimePrice ->
            if (realTimePrice == null) {
                if((activity as SendV2Activity).sendViewModel.isFiatEntryType) {
                    (activity as SendV2Activity).sendViewModel.toggleFiatEntryType()
                }

                pricingData = O3RealTimePrice("NEO", PersistentStore.getCurrency(), 0.0, 0)
                mView.find<ImageButton>(R.id.swapFiatEntryType).visibility = View.INVISIBLE
                otherAmountTextView.visibility = View.INVISIBLE
                mView.find<TextView>(R.id.sendPricingUnavailableTextView).visibility = View.VISIBLE
            } else {
                pricingData = realTimePrice!!
                mView.find<ImageButton>(R.id.swapFiatEntryType).visibility = View.VISIBLE
                otherAmountTextView.visibility = View.VISIBLE
                mView.find<TextView>(R.id.sendPricingUnavailableTextView).visibility = View.INVISIBLE
            }
            amountEditText.text.clear()
            otherAmountTextView.text = 0.0.formattedFiatString()
        })
    }

    fun calculateAndDisplaySendAmount() {
        val displayedString = amountEditText.text.toString()
        if ((activity as SendV2Activity).sendViewModel.isFiatEntryType) {
            if (displayedString == "") {
                amountEditText.text = SpannableStringBuilder(0.0.formattedFiatString())
                otherAmountTextView.text = 0.0.toString()
                return
            }
            if (displayedString.length == 1) {
                otherAmountTextView.text = 0.0.toString()
                return
            }

            val regex = Regex("[^0-9.]")
            val cleanString = displayedString.replace(regex, "")
            val doubleValue = cleanString.toDouble()
            var cryptoAmount = 0.0
            if (pricingData.price != 0.0 && doubleValue != 0.0 ) {
                cryptoAmount = doubleValue / pricingData.price
                (activity as SendV2Activity).sendViewModel.setSelectedSendAmount(cryptoAmount)
                otherAmountTextView.text = cryptoAmount.removeTrailingZeros()
            } else {
                (activity as SendV2Activity).sendViewModel.setSelectedSendAmount(0.0)
                otherAmountTextView.text = cryptoAmount.removeTrailingZeros()
            }
        } else {
            if (displayedString == "") {
                amountEditText.isCursorVisible = false
                otherAmountTextView.text = 0.0.formattedFiatString()
                enteredCurrencyDouble = 0.0
                return
            }
            amountEditText.isCursorVisible = true
            val amount = pricingData.price *  displayedString.toDouble()
            (activity as SendV2Activity).sendViewModel.setSelectedSendAmount(displayedString.toDouble())
            enteredCurrencyDouble = amount
            mView.find<TextView>(R.id.otherAmountTextView).text = amount.formattedFiatString()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.send_what_fragment, container, false)
        amountEditText = mView.find(R.id.amountEditText)
        mView.find<Button>(R.id.sendWhereButton).setOnClickListener {
            mView.findNavController().navigate(R.id.action_sendWhatFragment_to_sendReviewFragment)
        }
        listenForNewPricingData()
        mView.find<EditText>(R.id.amountEditText).afterTextChanged { calculateAndDisplaySendAmount() }

        setupFiatEntrySwap()
        initiateAssetSelector()
        setUpSeekBar()
        return mView
    }
}

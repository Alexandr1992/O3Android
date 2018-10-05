package network.o3.o3wallet.NativeTrade


import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.amplitude.api.Amplitude
import com.bumptech.glide.Glide
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toMap
import network.o3.o3wallet.*
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import network.o3.o3wallet.NativeTrade.DepositWithdrawal.DepositWithdrawalResultDialog
import org.jetbrains.anko.find
import org.jetbrains.anko.noButton
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.yesButton
import org.json.JSONObject
import org.w3c.dom.Text

class ReviewOrderFragment : Fragment() {
    private lateinit var mView: View
    private lateinit var vm: NativeTradeViewModel

    fun initiateOrderAssetDetails() {
        mView.find<TextView>(R.id.reviewTotalAmountTextView).text = vm.orderAssetAmount.value!!.removeTrailingZeros()
        mView.find<TextView>(R.id.reviewOrderNameTextView).text = vm.orderAsset
        Glide.with(this).load(String.format("https://cdn.o3.network/img/neo/%s.png", vm.orderAsset))
                .into(mView.find(R.id.reviewOrderLogoImageView))
    }

    fun initiatePricing() {
        mView.find<TextView>(R.id.reviewCryptoPriceTextView).text = vm.selectedPrice!!.value!!.second.removeTrailingZeros()
        mView.find<TextView>(R.id.reviewFiatPriceTextView).text = vm.selectedPrice!!.value!!.first.formattedFiatString()

        val marketRatePercent = (vm.marketRateDifference.value!! - 1.0) * 100
        if(marketRatePercent > 0.0) {
            mView.find<TextView>(R.id.reviewPercentAboveMedianTextView).text =
                    String.format(resources.getString(R.string.NATIVE_Trade_percent_above_market),
                            marketRatePercent.formattedPercentString())
        } else {
            mView.find<TextView>(R.id.reviewPercentAboveMedianTextView).text =
                    String.format(resources.getString(R.string.NATIVE_Trade_percent_below_market),
                            marketRatePercent.formattedPercentString())
        }

        //TODO: Readd instant fill in the future maybe
        mView.find<TextView>(R.id.reviewEstimatedFillTextView).visibility = View.GONE
       /* mView.find<TextView>(R.id.reviewEstimatedFillTextView).text =
                String.format(resources.getString(R.string.NATIVE_TRADE_instant_fill_with_amount),
                       (vm.estimatedFillAmount.value!! * 100).formattedPercentString())
        */
    }

    fun initiateBaseAssetDetails() {
        mView.find<TextView>(R.id.reviewBaseAssetAmountTextView).text = vm.selectedBaseAssetAmount.value!!.removeTrailingZeros()
        mView.find<TextView>(R.id.reviewBaseAssetNameTextView).text = vm.selectedBaseAsset!!.value
        val newTotalFiat = vm.orderAssetAmount.value!! * vm.selectedPrice!!.value!!.first
        mView.find<TextView>(R.id.reviewBaseAssetFiatAmountTextView).text = newTotalFiat.formattedFiatString()

        Glide.with(this).load(String.format("https://cdn.o3.network/img/neo/%s.png", vm.selectedBaseAsset!!.value!!))
                .into(mView.find(R.id.reviewBaseAssetLogoImageView))
    }

    fun initiatePlaceOrderButton() {
        val placeOrderButton = mView.find<Button>(R.id.finalizeOrderButton)
        if ((activity as NativeTradeRootActivity).viewModel.isBuyOrder) {
            placeOrderButton.background = ContextCompat.getDrawable(this.activity!!, R.drawable.buy_button_background)
        } else {
            placeOrderButton.background = ContextCompat.getDrawable(this.activity!!, R.drawable.sell_button_background)
        }

        val vm = (activity as NativeTradeRootActivity).viewModel
        vm.getIsOrdering().observe (this, Observer {
            if (it == true) {
                placeOrderButton.isEnabled = false
            }
        })

        placeOrderButton.onClick {

            val pair = vm.orderAsset + "_" + vm.selectedBaseAsset?.value!!
            val price = vm.selectedPrice!!.value!!.second.removeTrailingZeros().decimalNoGrouping()
            var side = "buy"
            var wantAmount = (vm.orderAssetAmount.value!! * 100000000.0).toLong().toString()
            if (!vm.isBuyOrder) {
                side = "sell"
                wantAmount = (vm.selectedBaseAssetAmount.value!! * 100000000.0).toLong().toString()
            }

            val orderType = "limit"
            vm.setIsOrdering(true)

            val resultFragment = OrderResultDialog.newInstance()
            resultFragment.show(activity!!.supportFragmentManager, "depositResult")
            SwitcheoAPI().singleStepOrder(pair, side, price, wantAmount, orderType) {
                onUiThread {
                    vm.setIsOrdering(false)
                    if (it.first == null) {
                        resultFragment.showFailure()
                    } else {
                        resultFragment.showSuccess(it.first!!)
                        val loggedJson = jsonObject(
                                "order_id" to it.first!!.id,
                                "datetime" to it.first!!.created_at,
                                "side" to side,
                                "pair" to pair,
                                "base_currency" to vm.selectedBaseAsset.value!!,
                                "quantity" to vm.orderAssetAmount.value!!,
                                "price_selection" to (activity as NativeTradeRootActivity).viewModel.priceSelectionType)
                        Amplitude.getInstance().logEvent("Native_Order_Placed", JSONObject(loggedJson.toMap()))
                    }
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.native_trade_review_order_fragment, container, false)
        vm = (activity as NativeTradeRootActivity).viewModel
        initiateOrderAssetDetails()
        initiatePricing()
        initiateBaseAssetDetails()
        initiatePlaceOrderButton()
        return mView
    }
}

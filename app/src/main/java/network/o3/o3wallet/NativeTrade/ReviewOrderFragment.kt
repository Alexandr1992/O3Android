package network.o3.o3wallet.NativeTrade


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.R
import network.o3.o3wallet.formattedFiatString
import org.jetbrains.anko.find
import org.w3c.dom.Text

class ReviewOrderFragment : Fragment() {
    private lateinit var mView: View
    private lateinit var vm: NativeTradeViewModel

    fun initiateOrderAssetDetails() {
        mView.find<TextView>(R.id.reviewTotalAmountTextView).text = vm.orderAssetAmount.value!!.toString()
        mView.find<TextView>(R.id.reviewOrderNameTextView).text = vm.orderAsset
        Glide.with(this).load(String.format("https://cdn.o3.network/img/neo/%s.png", vm.orderAsset))
                .into(mView.find(R.id.reviewOrderLogoImageView))
    }

    fun initiatePricing() {
        mView.find<TextView>(R.id.reviewCryptoPriceTextView).text = vm.selectedPrice!!.value!!.second.toString()
        mView.find<TextView>(R.id.reviewFiatPriceTextView).text = vm.selectedPrice!!.value!!.first.formattedFiatString()

        mView.find<TextView>(R.id.reviewEstimatedFillTextView).text = vm.estimatedFillAmount!!.value.toString()
        mView.find<TextView>(R.id.reviewPercentAboveMedianTextView).text = vm.marketRateDifference.value.toString()

    }

    fun initiateBaseAssetDetails() {
        mView.find<TextView>(R.id.reviewBaseAssetAmountTextView).text = vm.selectedBaseAssetAmount.value!!.toString()
        mView.find<TextView>(R.id.reviewBaseAssetNameTextView).text = vm.selectedBaseAsset!!.value
        val newTotalFiat = vm.orderAssetAmount.value!! * vm.selectedPrice!!.value!!.first
        mView.find<TextView>(R.id.reviewBaseAssetFiatAmountTextView).text = newTotalFiat.formattedFiatString()

        Glide.with(this).load(String.format("https://cdn.o3.network/img/neo/%s.png", vm.selectedBaseAsset!!.value!!))
                .into(mView.find(R.id.reviewBaseAssetLogoImageView))
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.native_trade_review_order_fragment, container, false)
        vm = (activity as NativeTradeRootActivity).viewModel
        initiateOrderAssetDetails()
        initiatePricing()
        initiateBaseAssetDetails()
        return mView
    }
}

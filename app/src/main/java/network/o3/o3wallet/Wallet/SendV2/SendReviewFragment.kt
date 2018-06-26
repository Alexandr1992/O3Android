package network.o3.o3wallet.Wallet.SendV2


import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.send_review_fragment.*
import network.o3.o3wallet.R
import network.o3.o3wallet.format
import network.o3.o3wallet.formattedFiatString
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find
import org.w3c.dom.Text

class SendReviewFragment : Fragment() {
    private lateinit var mView: View


    fun initiateSelectedAssetDetails() {
        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset!!.symbol)
            Glide.with(this).load(imageURL).into(find(R.id.reviewAssetLogoImageView))
            find<TextView>(R.id.reviewAssetSymbolTextView).text = selectedAsset.symbol
        })
    }

    fun initateSelectedBalanceDetails() {
        mView.find<TextView>(R.id.reviewAmountTextView).text = (activity as SendV2Activity)
                .sendViewModel.getSelectedSendAmount().removeTrailingZeros()

        (activity as SendV2Activity).sendViewModel.getRealTimePrice().observe(this, Observer { realTimePrice ->
            val fiatAmount = realTimePrice!!.price * (activity as SendV2Activity).sendViewModel.getSelectedSendAmount()
            mView.find<TextView>(R.id.reviewFiatAmountTextView).text = fiatAmount.formattedFiatString()
        })
    }

    fun initiateSelectedRecipientDetails() {
        val contact = (activity as SendV2Activity).sendViewModel.getSelectedContact().value
        if (contact != null) {
            mView.find<TextView>(R.id.reviewSelectedAddressTextView).text = contact.address
            mView.find<TextView>(R.id.reviewNicknameTextView).text = contact.nickname
        } else {
            val address = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!
            mView.find<TextView>(R.id.reviewSelectedAddressTextView).text = address
            (activity as SendV2Activity).sendViewModel.getVerifiedAddress(true, address).observe(this, Observer { verifiedAddress ->
                if (verifiedAddress != null) {
                    mView.find<TextView>(R.id.reviewNicknameTextView).text = verifiedAddress.displayName
                    mView.find<ImageView>(R.id.reviewVerifiedBadge).visibility = View.VISIBLE
                }
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.send_review_fragment, container, false)
        initateSelectedBalanceDetails()
        initiateSelectedAssetDetails()
        initiateSelectedRecipientDetails()
        mView.findViewById<Button>(R.id.sendOverHangButton).bringToFront()
        return mView
    }
}

package network.o3.o3wallet.Wallet.SendV2


import android.arch.lifecycle.Observer
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.R
import network.o3.o3wallet.Settings.AddContact
import network.o3.o3wallet.formattedFiatString
import network.o3.o3wallet.removeTrailingZeros
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find

class SendSuccessFragment : Fragment() {
    lateinit var mView: View
    lateinit var addToContactCheckbox: CheckBox

    fun initateSelectedBalanceDetails() {
        mView.find<TextView>(R.id.receiptAmountTextView).text = (activity as SendV2Activity)
                .sendViewModel.getSelectedSendAmount().removeTrailingZeros()

        (activity as SendV2Activity).sendViewModel.getRealTimePrice().observe(this, Observer { realTimePrice ->
            val fiatAmount = realTimePrice!!.price * (activity as SendV2Activity).sendViewModel.getSelectedSendAmount()
            mView.find<TextView>(R.id.receiptFiatAmountTextView).text = fiatAmount.formattedFiatString()
        })

    }

    fun initiateSelectedRecipientDetails() {
        val contact = (activity as SendV2Activity).sendViewModel.getSelectedContact().value
        if (contact != null) {
            mView.find<TextView>(R.id.receiptSelectedAddressTextView).text = contact.address
            mView.find<TextView>(R.id.receiptNicknameTextView).text = contact.nickname
        } else {
            val address = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!
            mView.find<TextView>(R.id.receiptSelectedAddressTextView).text = address
            (activity as SendV2Activity).sendViewModel.getVerifiedAddress(true, address).observe(this, Observer { verifiedAddress ->
                if (verifiedAddress != null) {
                    mView.find<TextView>(R.id.receiptNicknameTextView).text = verifiedAddress.displayName
                    mView.find<ImageView>(R.id.receiptVerifiedBadge).visibility = View.VISIBLE
                }
            })
        }
    }

    fun initiateSelectedAssetDetails() {
        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset!!.symbol)
            Glide.with(this).load(imageURL).into(find(R.id.receiptAssetLogoImageView))
            mView.find<TextView>(R.id.receiptAmountTextView).text = (activity as SendV2Activity)
                    .sendViewModel.getSelectedSendAmount().removeTrailingZeros() + " " + selectedAsset.symbol
        })
    }

    fun initiateActionButtons() {
        mView.find<Button>(R.id.sendSuccessCloseButton).setOnClickListener {
            if (addToContactCheckbox.isChecked) {
                val contactIntent = Intent(context, AddContact::class.java)
                contactIntent.putExtra("address", mView.find<TextView>(R.id.receiptSelectedAddressTextView).text)
                startActivity(contactIntent)
            }
            activity?.finish()
        }

        mView.find<CheckBox>(R.id.addToContactCheckbox).setOnClickListener {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.send_success_fragment, container, false)
        initateSelectedBalanceDetails()
        initiateSelectedRecipientDetails()
        initiateSelectedAssetDetails()
        initiateActionButtons()
        addToContactCheckbox = mView.find(R.id.addToContactCheckbox)
        return mView
    }
}

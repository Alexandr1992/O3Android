package network.o3.o3wallet.Wallet.SendV2


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.TokenListing
import network.o3.o3wallet.API.O3Platform.TransactionHistoryEntry
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Settings.AddContact
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find

class SendSuccessFragment : Fragment() {
    lateinit var mView: View
    lateinit var addToContactCheckbox: CheckBox

    fun initateSelectedBalanceDetails() {
        mView.find<TextView>(R.id.receiptAmountTextView).text = (activity as SendV2Activity)
                .sendViewModel.getSelectedSendAmount().toDouble().removeTrailingZeros()

        (activity as SendV2Activity).sendViewModel.getRealTimePrice(false).observe(this, Observer { realTimePrice ->
            if (realTimePrice != null) {
                val fiatAmount = realTimePrice.price * (activity as SendV2Activity).sendViewModel.getSelectedSendAmount().toDouble()
                mView.find<TextView>(R.id.receiptFiatAmountTextView).text = fiatAmount.formattedFiatString()
            }
        })

    }

    fun initiateSelectedRecipientDetails() {
        val contact = (activity as SendV2Activity).sendViewModel.getSelectedContact().value
        if (contact != null) {
            mView.find<TextView>(R.id.receiptSelectedAddressTextView).text = contact.address
            mView.find<TextView>(R.id.receiptNicknameTextView).text = contact.nickname
            addToContactCheckbox.visibility = View.INVISIBLE
        } else if ((activity as SendV2Activity).sendViewModel.nnsName != "") {
            mView.find<TextView>(R.id.receiptNicknameTextView).text = (activity as SendV2Activity).sendViewModel.nnsName
            mView.find<TextView>(R.id.receiptSelectedAddressTextView).text = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!
        }else {
            val address = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!
            mView.find<TextView>(R.id.receiptNicknameTextView).visibility = View.INVISIBLE
            mView.find<TextView>(R.id.receiptSelectedAddressTextView).text = address
            (activity as SendV2Activity).sendViewModel.getVerifiedAddress(true, address).observe(this, Observer { verifiedAddress ->
                if (verifiedAddress != null) {
                    mView.find<TextView>(R.id.receiptNicknameTextView).text = verifiedAddress.displayName
                    mView.find<ImageView>(R.id.receiptVerifiedBadge).visibility = View.VISIBLE
                }
            })
        }
    }

    fun setPendingEntry(selectedAsset: TransferableAsset) {
        var amount = (activity as SendV2Activity).sendViewModel.getSelectedSendAmount().toDouble().removeTrailingZeros()
        if ((activity as SendV2Activity).sendViewModel.getSelectedAddress().value!! == Account.getWallet().address) {
            amount = "0"
        }
        val pendingTxEntry = TransactionHistoryEntry(blockchain = "", txid = (activity as SendV2Activity).sendViewModel.txID.toLowerCase(),
                time = System.currentTimeMillis() / 1000, blockHeight = 0,
                asset = TokenListing(logoURL =  String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset.symbol),
                        symbol = selectedAsset.symbol, decimal = selectedAsset.decimals, name = selectedAsset.name,
                        logoSVG = "", url = "", tokenHash = selectedAsset.id, totalSupply = 0),
                amount = amount,
                to = (activity as SendV2Activity).sendViewModel.getSelectedAddress().value!!,
                from = Account.getWallet().address)

        // sending tokens to yourself wont be picked up by the notification server
        // so dont add them to the pending transaction list
        if ((activity as SendV2Activity).sendViewModel.isNEOTokenAsset()) {
            if ((activity as SendV2Activity).sendViewModel.getSelectedAddress().value!! == Account.getWallet().address) {
                return
            }
        }

        if ((activity as SendV2Activity).sendViewModel.isOntAsset()) {
            return
        }




        var currentPending = PersistentStore.getPendingTransactions()
        currentPending.add(pendingTxEntry)
        PersistentStore.setPendingTransactions(currentPending)
    }

    fun initiateSelectedAssetDetails() {
        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset!!.symbol)
            Glide.with(activity).load(imageURL).into(find(R.id.receiptAssetLogoImageView))
            mView.find<TextView>(R.id.receiptAmountTextView).text = (activity as SendV2Activity)
                    .sendViewModel.getSelectedSendAmount().toDouble().removeTrailingZeros() + " " + selectedAsset.symbol
            setPendingEntry(selectedAsset)

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
        mView.find<TextView>(R.id.transactionIdSubtitleLabel).text = (activity as SendV2Activity).sendViewModel.txID.toLowerCase()
        addToContactCheckbox = mView.find(R.id.addToContactCheckbox)
        initateSelectedBalanceDetails()
        initiateSelectedRecipientDetails()
        initiateSelectedAssetDetails()
        initiateActionButtons()

        return mView
    }
}

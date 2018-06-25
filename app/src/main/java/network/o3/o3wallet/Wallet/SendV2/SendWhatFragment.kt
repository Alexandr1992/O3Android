package network.o3.o3wallet.Wallet.SendV2


import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import org.jetbrains.anko.find
import com.xw.repo.BubbleSeekBar
import kotlinx.coroutines.experimental.channels.Send
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Account
import network.o3.o3wallet.R
import org.jetbrains.anko.support.v4.find
import java.text.NumberFormat


class SendWhatFragment : Fragment() {
    private lateinit var mView: View
    var ownedAssets: ArrayList<TransferableAsset> = arrayListOf()

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

        assetContainer.setOnClickListener {
            val assetSelectorSheet = AssetSelectionBottomSheet()
            (activity as SendV2Activity).sendViewModel.getOwnedAssets(false).observe ( this, Observer { ownedAssets ->
                assetSelectorSheet.assets = ownedAssets!!
                assetSelectorSheet.show(activity!!.supportFragmentManager, assetSelectorSheet.tag)
            })
        }

        (activity as SendV2Activity).sendViewModel.getSelectedAsset().observe(this, Observer { selectedAsset ->
            var formatter = NumberFormat.getNumberInstance()
            formatter.maximumFractionDigits = selectedAsset!!.decimals
            find<TextView>(R.id.assetBalanceTextView).text = formatter.format(selectedAsset.value)
            find<TextView>(R.id.assetNameTextView).text = selectedAsset!!.symbol
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", selectedAsset.symbol)
            Glide.with(this).load(imageURL).into(find(R.id.assetLogoImageView))
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.send_what_fragment, container, false)
        mView.find<Button>(R.id.sendWhereButton).setOnClickListener {
            mView.findNavController().navigate(R.id.action_sendWhatFragment_to_sendReviewFragment)
        }

        initiateAssetSelector()
        return mView
    }
}

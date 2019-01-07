package network.o3.o3wallet.Portfolio

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.portfolio_fragment_portfolio_header.*
import network.o3.o3wallet.*
import network.o3.o3wallet.MultiWallet.ManageMultiWallet.SwapWalletBottomSheet
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk15.coroutines.onClick
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.view
import org.w3c.dom.Text
import java.util.*

class PortfolioHeader: Fragment {
    private val titles = O3Wallet.appContext!!.resources.getStringArray(R.array.PORTFOLIO_headers)
    var position: Int = 0
    var unscrubbedDisplayedAmount = 0.0
    lateinit var mView: View
    lateinit var fundSourceTextView: TextView
    lateinit var walletTypeIcon: ImageView
    lateinit var walletStatusIcon: ImageView

    enum class WalletType {
        Default,
        WatchOnly,
        Wallet,
        Combined
    }

    constructor() : super()

    companion object {
        fun newInstance(position: Int): PortfolioHeader {
            val args = Bundle()
            args.putInt("position", position)
            val fragment = PortfolioHeader()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.portfolio_fragment_portfolio_header, container, false)
        position = arguments!!.getInt("position")
        fundSourceTextView = mView.findViewById(R.id.fundSourceTextView)

        val fundChangeTextView = view?.findViewById<TextView>(R.id.fundChangeTextView)
        fundChangeTextView?.text = ""

        walletTypeIcon = mView.find(R.id.walletTypeIconImageView)
        walletTypeIcon.image = null

        walletStatusIcon = mView.find<ImageView>(R.id.lockIconImageView)
        walletStatusIcon.image = null

        configureArrows()

        return mView
    }

    fun setHeaderInfo(amount: String, percentChange: Double, interval: String,
                      initialDate: Date, title: String, walletType: WalletType, account: NEP6.Account?) {
        onUiThread {
            fundSourceTextView.text = title
            fundChangeTextView.text = percentChange.formattedPercentString() +
                    " " +  initialDate.intervaledString(interval)
            fundAmountTextView.text = amount

            walletTypeIcon.image = when(walletType) {
                WalletType.Default -> ContextCompat.getDrawable(context!!, R.drawable.ic_wallet)
                WalletType.WatchOnly -> ContextCompat.getDrawable(context!!, R.drawable.ic_eye)
                WalletType.Wallet -> ContextCompat.getDrawable(context!!, R.drawable.ic_wallet)
                WalletType.Combined -> ContextCompat.getDrawable(context!!, R.drawable.ic_wallet_combined)
            }

            if (account?.isDefault ?: false) {
                walletStatusIcon.image = ContextCompat.getDrawable(context!!, R.drawable.ic_unlocked)
            } else {
                walletStatusIcon.image = ContextCompat.getDrawable(context!!, R.drawable.ic_locked)
            }

            if (percentChange < 0) {
                fundChangeTextView?.setTextColor(context!!.getColor(R.color.colorLoss))
            } else {
                fundChangeTextView?.setTextColor(context!!.getColor(R.color.colorGain))
            }
        }
    }

    fun configureArrows() {
        val pager = activity?.findViewById<ViewPager>(R.id.portfolioHeaderFragment)
        val leftArrow = mView?.findViewById<ImageView>(R.id.leftArrowImageView)
        val rightArrow = mView?.findViewById<ImageView>(R.id.rightArrowImageView)

        mView?.findViewById<TextView>(R.id.fundAmountTextView)!!.setOnClickListener {
            var pFragment = (parentFragment as HomeFragment)
            if (pFragment.homeModel.getCurrency() == CurrencyType.FIAT) {
                pFragment.homeModel.setCurrency(CurrencyType.BTC)
            } else {
                pFragment.homeModel.setCurrency(CurrencyType.FIAT)
            }
            pFragment.homeModel.loadPortfolioValue(pFragment.assetListAdapter?.assets ?: arrayListOf())
        }

        val walletSwap = {
            if (NEP6.getFromFileSystem().getNonDefaultAccounts().isNotEmpty() ) {
                val bottomSheet = SwapWalletBottomSheet()
                bottomSheet.show(activity!!.supportFragmentManager, "swapWallet")
            }
        }

        if (position == 0) {
            fundSourceTextView.onClick { walletSwap() }
            walletTypeIcon.onClick { walletSwap() }
        }

        if (position == 0) {
            leftArrow?.visibility = View.INVISIBLE
        } else {
            leftArrow?.visibility = View.VISIBLE
        }

        leftArrow?.setOnClickListener {
            pager?.currentItem = position - 1
        }

        var lastPosition = 1
        if (NEP6.getFromFileSystem().getNonDefaultAccounts().count() > 0) {
            lastPosition = NEP6.getFromFileSystem().getNonDefaultAccounts().count() + 1
        }

        if (position == lastPosition) {
            rightArrow?.visibility = View.INVISIBLE
            walletStatusIcon.visibility = View.INVISIBLE
        } else {
            rightArrow?.visibility = View.VISIBLE
            walletStatusIcon.visibility = View.VISIBLE
        }

        rightArrow?.setOnClickListener {
            pager?.currentItem = position + 1
        }
    }
}

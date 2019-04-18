package network.o3.o3wallet.Portfolio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import com.airbnb.lottie.LottieAnimationView
import com.commit451.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import com.commit451.modalbottomsheetdialogfragment.Option
import com.robinhood.spark.SparkView
import com.robinhood.spark.animation.MorphSparkAnimator
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.Dapp.DappContainerActivity
import network.o3.o3wallet.MultiWallet.Activate.MultiwalletActivateActivity
import network.o3.o3wallet.MultiWallet.AddNewMultiWallet.AddNewMultiwalletRootActivity
import network.o3.o3wallet.Wallet.MyAddressFragment
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.onUiThread


interface HomeViewModelProtocol {
    fun showPortfolioLoadingIndicator()
    fun hidePortfolioLoadingIndicator()
    fun hideAssetLoadingIndicator()
    fun updatePortfolioData(portfolio: Portfolio)
}

class HomeFragment : Fragment(), HomeViewModelProtocol, ModalBottomSheetDialogFragment.Listener {
    var selectedButton: Button? = null
    lateinit var homeModel: HomeViewModelV2
    var viewPager: ViewPager? = null
    var chartDataAdapter = PortfolioDataAdapter(FloatArray(0))
    var assetListAdapter: AssetListAdapter? = null
    var sparkView: SparkView? = null
    lateinit var recyclerView: RecyclerView
    lateinit var mView: View

    var hasWatchAddress = NEP6.hasMultipleAccounts()

    val needReloadWatchAddressReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            homeModel.hasWatchAddress = NEP6.hasMultipleAccounts()
            viewPager?.adapter?.notifyDataSetChanged()
            val name = "android:switcher:" + viewPager?.id + ":" + viewPager?.currentItem
            val header = childFragmentManager.findFragmentByTag(name) as PortfolioHeader
            header.configureArrows()
            if (intent.getBooleanExtra("reset", false)) {
                viewPager?.setCurrentItem(0, true)
            }
            homeModel.reloadDisplayedAssets()
        }
    }

    val needReloadPortfolioReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            homeModel.loadPortfolioValue(assetListAdapter?.assets ?: arrayListOf())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeModel = HomeViewModelV2()
        homeModel.getBestNode()
        homeModel.delegate = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadWatchAddressReciever,
                IntentFilter("need-update-watch-address-event"))
        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadPortfolioReciever,
                IntentFilter("need-update-currency-event"))
        mView =  inflater.inflate(R.layout.portfolio_fragment_home, container, false)

        recyclerView = mView.findViewById(R.id.assetListView)
        val itemDecorator = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.vertical_divider)!!)
        recyclerView.addItemDecoration(itemDecorator)

        val layoutManager = LinearLayoutManager(this.activity)
        layoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = layoutManager
        return mView
    }

    fun initiateBalanceListeners() {
        homeModel.getPortfolio().observe(this, Observer { portfolio ->
            hidePortfolioLoadingIndicator()
            updatePortfolioData(portfolio!!)
        })

        homeModel.getDisplayedAssets(false).observe(this, Observer { displayedAssets ->
            //This is a hack to force an update where the displayed assets are the same
            //but we are on a different page
            val name = "android:switcher:" + viewPager?.id + ":" + viewPager?.currentItem
            val header = childFragmentManager.findFragmentByTag(name) as PortfolioHeader?
            val amountView = header?.view?.findViewById<TextView>(R.id.fundAmountTextView)
            //

            if (displayedAssets?.equals(assetListAdapter?.assets) == false || amountView?.text == "") {
                assetListAdapter?.assets = displayedAssets ?: arrayListOf()
                assetListAdapter?.notifyDataSetChanged()
                homeModel.loadPortfolioValue(displayedAssets ?: arrayListOf())
            } else if (homeModel.getCurrentPortfolioValue() == 0.0) {
                updateHeader(0.0.formattedCurrencyString(homeModel.getCurrency()), 0.0)

            }
        })
    }

    fun initiateGraph() {

        sparkView = mView.findViewById(R.id.sparkview)
        sparkView?.sparkAnimator = MorphSparkAnimator()
        sparkView?.adapter = chartDataAdapter
        sparkView?.scrubListener = SparkView.OnScrubListener { value ->
            val name = "android:switcher:" + viewPager?.id + ":" + viewPager?.currentItem
            val header = childFragmentManager.findFragmentByTag(name) as PortfolioHeader

            val amountView = header.view?.findViewById<TextView>(R.id.fundAmountTextView)
            val percentView = header.view?.findViewById<TextView>(R.id.fundChangeTextView)
            if (value == null) { //return to original state
                updateHeader(homeModel.getCurrentPortfolioValue().formattedCurrencyString(homeModel.getCurrency()),
                        homeModel.getPercentChange())
                return@OnScrubListener
            } else {
                val scrubbedAmount = (value as Float).toDouble()
                val percentChange = (scrubbedAmount - homeModel.getInitialPortfolioValue()) /
                        homeModel.getInitialPortfolioValue() * 100
                if (percentChange < 0) {
                    percentView?.setTextColor(context!!.getColor(R.color.colorLoss))
                } else {
                    percentView?.setTextColor(context!!.getColor(R.color.colorGain))
                }
                percentView?.text = percentChange.formattedPercentString() +
                        " " +  homeModel.getInitialDate().intervaledString(homeModel.getInterval())
                amountView?.text = scrubbedAmount.formattedCurrencyString(homeModel.getCurrency())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        assetListAdapter = AssetListAdapter(this.context!!, this)
        recyclerView.adapter = assetListAdapter

        initiateBalanceListeners()

        initiateGraph()
        initiateViewPager(view)
        initiateIntervalButtons()
        view.find<SwipeRefreshLayout>(R.id.portfolioSwipeRefresh).setColorSchemeResources(R.color.colorPrimary)
        view.find<SwipeRefreshLayout>(R.id.portfolioSwipeRefresh).setProgressBackgroundColorSchemeColor(context!!.getColorFromAttr(R.attr.secondaryBackgroundColor))
        view.find<SwipeRefreshLayout>(R.id.portfolioSwipeRefresh).onRefresh {
            view.find<SwipeRefreshLayout>(R.id.portfolioSwipeRefresh).isRefreshing = true
            homeModel.getDisplayedAssets(true)
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this.context!!)
                .unregisterReceiver(needReloadWatchAddressReciever)
        LocalBroadcastManager.getInstance(this.context!!)
                .unregisterReceiver(needReloadPortfolioReciever)
        super.onDestroy()
    }

    fun initiateViewPager(view: View) {
        viewPager = view.findViewById<ViewPager>(R.id.portfolioHeaderFragment)
        val portfolioHeaderAdapter = PortfolioHeaderPagerAdapter(childFragmentManager)
        viewPager?.adapter = portfolioHeaderAdapter
        viewPager?.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                Handler().postDelayed({
                    homeModel.setPosition(position)
                }, 200)
            }
        })
    }

    fun initiateIntervalButtons() {
        val sixHourButton = mView.findViewById<Button>(R.id.sixHourInterval)
        val oneDayButton = mView.findViewById<Button>(R.id.oneDayInterval)
        val oneWeekButton = mView.findViewById<Button>(R.id.oneWeekInterval)
        val oneMonthButton = mView.findViewById<Button>(R.id.oneMonthInterval)
        val threeMonthButton = mView.findViewById<Button>(R.id.threeMonthInterval)
        val allButton = mView.findViewById<Button>(R.id.allInterval)

        sixHourButton.setOnClickListener { tappedIntervalButton(sixHourButton) }
        oneDayButton.setOnClickListener { tappedIntervalButton(oneDayButton) }
        oneWeekButton.setOnClickListener { tappedIntervalButton(oneWeekButton) }
        oneMonthButton.setOnClickListener { tappedIntervalButton(oneMonthButton) }
        threeMonthButton.setOnClickListener { tappedIntervalButton(threeMonthButton) }
        allButton.setOnClickListener { tappedIntervalButton(allButton) }

        selectedButton = oneDayButton
    }

    fun tappedIntervalButton(button: Button) {
        selectedButton?.setTextAppearance(R.style.IntervalButtonText_NotSelected)
        button.setTextAppearance(R.style.IntervalButtonText_Selected)
        selectedButton = button
        homeModel.setInterval(button.tag.toString())
        homeModel.loadPortfolioValue(assetListAdapter?.assets ?: arrayListOf())
    }

    fun setEmptyOrGraph(portfolio: Portfolio) {
        val emptyWalletView = view?.find<ConstraintLayout>(R.id.emptyWalletContainer)
        val emptyPortfolioActionButton = view?.find<Button>(R.id.emptyPortfolioActionButton)
        val emptyPortfolioActionButtonTwo = view?.find<Button>(R.id.emptyPortfolioActionButtonTwo)
        val emptyPortfolioTextView = view?.find<TextView>(R.id.emptyPortfolioTextView)
        val emptyActionsDivider = view?.find<View>(R.id.emptyActionsDivider)

        if ((portfolio.data.first().averageBTC == 0.0 && homeModel.getPosition() == 0) ||
                homeModel.getPosition() == 1 && NEP6.getFromFileSystem().accounts.count() <= 1) {
            sparkView?.visibility = View.INVISIBLE
            mView.find<LinearLayout>(R.id.intervalButtonLayout).visibility = View.INVISIBLE
            emptyWalletView?.visibility = View.VISIBLE
            emptyPortfolioActionButton?.visibility = View.VISIBLE
            emptyPortfolioActionButtonTwo?.visibility = View.VISIBLE
            emptyPortfolioActionButton?.setCompoundDrawablesRelativeWithIntrinsicBounds(ContextCompat.getDrawable(context!!, R.drawable.ic_qrcode_button), null, null, null)
            emptyActionsDivider?.visibility = View.VISIBLE
        } else {
            sparkView?.visibility = View.VISIBLE
            mView.find<LinearLayout>(R.id.intervalButtonLayout).visibility = View.VISIBLE
            emptyWalletView?.visibility = View.INVISIBLE
            emptyPortfolioActionButton?.visibility = View.GONE
        }

        if (homeModel.getPosition() > 0) {
            emptyActionsDivider?.visibility = View.GONE
            emptyPortfolioTextView?.text = resources.getString(R.string.MULTIWALLET_no_additional_wallets)
            emptyPortfolioActionButtonTwo?.visibility = View.GONE
            if (!NEP6.nep6HasActivated()) {
                emptyPortfolioActionButton?.text = resources.getString(R.string.MULTIWALLET_activate_multiwallet)
            } else {
                emptyPortfolioActionButton?.text = resources.getString(R.string.MULTIWALLET_add_additional_wallets)
                emptyPortfolioActionButton?.setCompoundDrawablesRelativeWithIntrinsicBounds(ContextCompat.getDrawable(context!!, R.drawable.ic_wallet), null, null, null)
            }


            emptyPortfolioActionButton?.setOnClickListener {
                if (!NEP6.nep6HasActivated()) {
                    val intent = Intent(context, MultiwalletActivateActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(context, AddNewMultiwalletRootActivity::class.java)
                    startActivity(intent)
                }
            }
        } else {
            emptyPortfolioActionButton?.text = resources.getString(R.string.PORTFOLIO_deposit_tokens)
            emptyPortfolioTextView?.text = resources.getString(R.string.PORTOFOLIO_wallet_is_empty)
            emptyPortfolioActionButton?.setOnClickListener {
                val addressBottomSheet = MyAddressFragment()
                addressBottomSheet.show(activity!!.supportFragmentManager, "myaddress")
            }
            emptyPortfolioActionButtonTwo?.setOnClickListener {
                ModalBottomSheetDialogFragment.Builder()
                        .add(R.menu.buy_menu)
                        .show(childFragmentManager, "buy_options")
            }
        }
    }

    override fun onModalOptionSelected(tag: String?, option: Option) {
        if (option.id == R.id.buy_with_crypto) {
            val intent = Intent(this.context, DappContainerActivity::class.java)
            intent.putExtra("url", "https://o3.network/swap")
            startActivity(intent)
        } else {
            val intent = Intent(this.context, DappContainerActivity::class.java)
            intent.putExtra("url", "https://buy.o3.network/?c=NEO")
            startActivity(intent)
        }
    }

    override fun updatePortfolioData(portfolio: Portfolio) {
        onUiThread {
            assetListAdapter?.portfolio = portfolio
            assetListAdapter?.referenceCurrency = homeModel.getCurrency()
            assetListAdapter?.notifyDataSetChanged()
            chartDataAdapter.setData(homeModel.getPriceFloats())
            setEmptyOrGraph(portfolio)
            updateHeader(homeModel.getCurrentPortfolioValue().formattedCurrencyString(homeModel.getCurrency()),
                    homeModel.getPercentChange())
        }
    }

    fun getHeaderTitle(position: Int): String {
        if (position == 0 && NEP6.getFromFileSystem().accounts.isEmpty()) {
            return resources.getString(R.string.WALLET_my_o3_wallet)
        } else if (position == 1 && NEP6.getFromFileSystem().accounts.count() <= 1) {
            return resources.getString(R.string.PORTFOLIO_total)
        }

        if (position < NEP6.getFromFileSystem().accounts.count()) {
            return NEP6.getFromFileSystem().accounts[position].label
        } else {
            return resources.getString(R.string.PORTFOLIO_total)
        }
    }

    fun updateHeader(amount: String, percentChange: Double) {
        viewPager?.currentItem = viewPager?.currentItem!!
        val name = "android:switcher:" + viewPager?.id + ":" + viewPager?.currentItem
        val header = childFragmentManager.findFragmentByTag(name) as PortfolioHeader
        val isDefault = (homeModel.getPosition() == 0)
        var account: NEP6.Account? = null

        var walletType = PortfolioHeader.WalletType.Default
        if (NEP6.getFromFileSystem().accounts.count() <= 1) {
            walletType = PortfolioHeader.WalletType.Combined
        }

        if (NEP6.getFromFileSystem().accounts.count() > 1 &&
                homeModel.getPosition() < NEP6.getFromFileSystem().accounts.count()) {
            account = NEP6.getFromFileSystem().accounts[homeModel.getPosition()]
            if (account.key == null) {
                walletType = PortfolioHeader.WalletType.WatchOnly
            } else {
                walletType = PortfolioHeader.WalletType.Wallet
            }

            if (homeModel.getPosition() == NEP6.getFromFileSystem().accounts.count()) {
                walletType = PortfolioHeader.WalletType.Combined
            }
        }

        header.setHeaderInfo(amount, percentChange, homeModel.getInterval(),
                homeModel.getInitialDate(), getHeaderTitle(homeModel.getPosition()), walletType, account)
    }

    override fun showPortfolioLoadingIndicator() {
        onUiThread {
            sparkView?.visibility = View.INVISIBLE
            mView.findViewById<LottieAnimationView>(R.id.progressBar)?.visibility = View.VISIBLE
        }
    }

    override fun hidePortfolioLoadingIndicator() {
        onUiThread {
            sparkView?.visibility = View.VISIBLE
            mView.findViewById<LottieAnimationView>(R.id.progressBar)?.visibility = View.GONE
        }
    }

    override fun hideAssetLoadingIndicator() {
        onUiThread {
            view?.find<SwipeRefreshLayout>(R.id.portfolioSwipeRefresh)?.isRefreshing = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        assetListAdapter?.notifyDataSetChanged()
    }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
package network.o3.o3wallet.Portfolio

import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import com.robinhood.spark.SparkView
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager.*
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.robinhood.spark.animation.MorphSparkAnimator
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.Onboarding.CreateKey.Backup.DialogBackupKeyFragment
import network.o3.o3wallet.Settings.WatchAddressFragment
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

class HomeFragment : Fragment(), HomeViewModelProtocol {
    var selectedButton: Button? = null
    lateinit var homeModel: HomeViewModelV2
    var viewPager: ViewPager? = null
    var chartDataAdapter = PortfolioDataAdapter(FloatArray(0))
    var assetListAdapter: AssetListAdapter? = null
            //  var sparkView: SparkView? = null
    lateinit var recyclerView: RecyclerView

    lateinit var mView: View

    val needReloadWatchAddressReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
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
        homeModel.delegate = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadWatchAddressReciever,
                IntentFilter("need-update-watch-address-event"))
        LocalBroadcastManager.getInstance(this.context!!).registerReceiver(needReloadPortfolioReciever,
                IntentFilter("need-update-currency-event"))
        mView =  inflater.inflate(R.layout.portfolio_fragment_home, container, false)
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
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        assetListAdapter = AssetListAdapter(this.context!!, this)
        initiateBalanceListeners()
        recyclerView = view.findViewById<RecyclerView>(R.id.assetListView)
        val layoutManager = LinearLayoutManager(this.activity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = assetListAdapter
        initiateViewPager(view)
        view.find<SwipeRefreshLayout>(R.id.portfolioSwipeRefresh).setColorSchemeResources(R.color.colorPrimary)
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
        viewPager?.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                val displayType = when {
                    position == 0 -> HomeViewModelV2.DisplayType.HOT
                    position == 1 -> HomeViewModelV2.DisplayType.COLD
                    position == 2 -> HomeViewModelV2.DisplayType.COMBINED
                    else -> return
                }

                Handler().postDelayed({
                    homeModel.setDisplayType(displayType)
                }, 200)

            }
        })
    }

    fun setEmptyOrGraph(portfolio: Portfolio) {
        val emptyWalletView = view?.find<ConstraintLayout>(R.id.emptyWalletContainer)
        val emptyPortfolioActionButton = view?.find<Button>(R.id.emptyPortfolioActionButton)
        val emptyPortfolioTextView = view?.find<TextView>(R.id.emptyPortfolioTextView)
        if (portfolio.data.first().averageBTC == 0.0) {
            recyclerView.find<SparkView>(R.id.sparkview)?.visibility = View.INVISIBLE
            recyclerView?.find<LinearLayout>(R.id.intervalButtonLayout)?.visibility = View.INVISIBLE
            emptyWalletView?.visibility = View.VISIBLE
            emptyPortfolioActionButton?.visibility = View.VISIBLE
        } else {
            recyclerView.find<SparkView>(R.id.sparkview)?.visibility = View.VISIBLE
            recyclerView?.find<LinearLayout>(R.id.intervalButtonLayout)?.visibility = View.VISIBLE
            emptyWalletView?.visibility = View.INVISIBLE
            emptyPortfolioActionButton?.visibility = View.INVISIBLE
        }

        if (homeModel.getDisplayType() == HomeViewModelV2.DisplayType.COLD) {
            emptyPortfolioActionButton?.text = resources.getString(R.string.PORTFOLIO_add_watch_address)
            emptyPortfolioTextView?.text = resources.getString(R.string.PORTFOLIO_no_watch_addresses)
            emptyPortfolioActionButton?.setOnClickListener {
                val watchAddressModal = WatchAddressFragment.newInstance()
                watchAddressModal.show(activity!!.supportFragmentManager, watchAddressModal.tag)
            }
        } else {
            emptyPortfolioActionButton?.text = resources.getString(R.string.PORTFOLIO_deposit_tokens)
            emptyPortfolioTextView?.text = resources.getString(R.string.PORTOFOLIO_wallet_is_empty)
            emptyPortfolioActionButton?.setOnClickListener {
                val addressBottomSheet = MyAddressFragment()
                addressBottomSheet.show(activity!!.supportFragmentManager, "myaddress")
            }
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
            if (PersistentStore.getFirstTokenAppeared() && homeModel.getCurrentPortfolioValue() != 0.0
                    && homeModel.getDisplayType() == HomeViewModelV2.DisplayType.HOT) {
                val backupKeyCheck = DialogBackupKeyFragment.newInstance()
                backupKeyCheck.show(this.fragmentManager, "backupkey")
                PersistentStore.setFirstTokenAppeared(false)
            }
        }
    }


    fun updateHeader(amount: String, percentChange: Double) {
        viewPager?.currentItem = viewPager?.currentItem!!
        val name = "android:switcher:" + viewPager?.id + ":" + viewPager?.currentItem
        val header = childFragmentManager.findFragmentByTag(name) as PortfolioHeader
        header.setHeaderInfo(amount, percentChange, homeModel.getInterval(), homeModel.getInitialDate())
    }

    override fun showPortfolioLoadingIndicator() {
        onUiThread {
            recyclerView.find<SparkView>(R.id.sparkview)?.visibility = View.INVISIBLE
            recyclerView?.findViewById<LottieAnimationView>(R.id.progressBar)?.visibility = View.VISIBLE
        }
    }

    override fun hidePortfolioLoadingIndicator() {
        onUiThread {
            recyclerView.find<SparkView>(R.id.sparkview)?.visibility = View.VISIBLE
            recyclerView?.findViewById<LottieAnimationView>(R.id.progressBar)?.visibility = View.GONE
        }
    }

    override fun hideAssetLoadingIndicator() {
        onUiThread {
            view?.find<SwipeRefreshLayout>(R.id.portfolioSwipeRefresh)?.isRefreshing = false
        }
    }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
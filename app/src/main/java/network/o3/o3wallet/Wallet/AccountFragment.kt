package network.o3.o3wallet.Wallet
import android.animation.Animator
import android.animation.ObjectAnimator
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.support.v4.app.Fragment
import android.widget.*
import android.support.v4.widget.SwipeRefreshLayout
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewCompat
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.*
import network.o3.o3wallet.TokenSales.TokenSalesActivity
import org.jetbrains.anko.support.v4.onUiThread
import network.o3.o3wallet.Wallet.Send.SendActivity
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton


class AccountFragment : Fragment() {

    private lateinit var menuButton: FloatingActionMenu
    private lateinit var myQrButton: FloatingActionButton
    private lateinit var sendButton: FloatingActionButton
    private lateinit var tokenSaleButton: FloatingActionButton
    private lateinit var unclaimedGASTicker: TickerView
    private lateinit var syncButton: Button
    private lateinit var claimButton: Button
    private lateinit var learnMoreClaimButton: Button
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var assetListView: ListView
    private lateinit var accountViewModel: AccountViewModel
    private var claimAmount: Double = 0.0
    private var firstLoad = true
    private var tickupHandler = Handler()
    private lateinit var tickupRunnable: Runnable
    private var claimSucceeded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.wallet_fragment_account, container, false)
    }

    fun setupActionButton(view: View) {
        menuButton = view.findViewById(R.id.menuActionButton)
        myQrButton = view.findViewById(R.id.fab_my_address)
        sendButton = view.findViewById(R.id.fab_send)
        tokenSaleButton = view.findViewById(R.id.fab_token_sale)

        tokenSaleButton.colorNormal = resources.getColor(R.color.colorAccent)
        tokenSaleButton.colorPressed = resources.getColor(R.color.colorAccentLight)
        tokenSaleButton.setOnClickListener { showTokenSale() }

        myQrButton.colorNormal = resources.getColor(R.color.colorAccent)
        myQrButton.colorPressed = resources.getColor(R.color.colorAccentLight)
        myQrButton.setOnClickListener { showMyAddress() }

        sendButton.colorNormal = resources.getColor(R.color.colorAccent)
        sendButton.colorPressed = resources.getColor(R.color.colorAccentLight)
        sendButton.setOnClickListener { menuButtonTapped() }

        menuButton.menuButtonColorNormal = resources.getColor(R.color.colorAccent)
        menuButton.menuButtonColorPressed = resources.getColor(R.color.colorAccentLight)
        menuButton.transitionName = "reveal"
        menuButton.bringToFront()
    }

    fun reloadAllData() {
        accountViewModel.getAssets(true).observe(this, Observer<TransferableAssets?> {
            if (it == null) {
                context?.toast(accountViewModel.getLastError().localizedMessage)
            } else {
                showAssets(it)
                accountViewModel.getBlock(true).observe(this, Observer<Int?> {
                    accountViewModel.getClaims(true).observe(this, Observer<ClaimData?> {
                        if (it == null) {
                            this.syncButton.isEnabled = false
                            context?.toast(accountViewModel.getLastError().localizedMessage)
                        } else {

                            if (it.data.claims.isNotEmpty() && !claimSucceeded) {
                                showReadyToClaim()
                            } else {
                                claimSucceeded = false
                                showClaims(it)
                                if (firstLoad) {
                                    beginTickup()
                                    firstLoad = false
                                }
                            }
                        }
                    })
                })
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountViewModel = AccountViewModel()

        syncButton = view.findViewById(R.id.syncButton)
        claimButton = view.find(R.id.claimButton)
        learnMoreClaimButton = view.find(R.id.learnMoreClaimButton)
        unclaimedGASTicker = view.findViewById(R.id.unclaimedGasTicker)
        assetListView = view.findViewById(R.id.assetListView)
        unclaimedGASTicker.setCharacterList(TickerUtils.getDefaultNumberList())

        swipeContainer = view.findViewById(R.id.swipeContainer)
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary)

        swipeContainer.setOnRefreshListener {
            swipeContainer.isRefreshing = true
            reloadAllData()
        }

        setupActionButton(view)

        unclaimedGASTicker.text = "0.00000000"
        unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
        syncButton.setOnClickListener {
            syncTapped()
        }

        claimButton.setOnClickListener {
            claimTapped()
        }

        learnMoreClaimButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
            startActivity(browserIntent)
        }

        activity?.title = "Account"
        reloadAllData()
    }


    private fun tickup() {
        var current = unclaimedGASTicker.text.toDouble()
        val addIntervalAmount = (accountViewModel.getNeoBalance() * 7 / 100000000.0)
        unclaimedGASTicker.text = "%.8f".format(current + addIntervalAmount)
    }

    private fun beginTickup() {
        tickupRunnable = object : Runnable {
            override fun run() {
                tickup()
                tickupHandler.postDelayed(this, 15000)
            }
        }
        tickupHandler.postDelayed(tickupRunnable, 15000)
    }

    private fun showMyAddress() {
        val addressBottomSheet = MyAddressFragment()
        addressBottomSheet.show(activity!!.supportFragmentManager, "myaddress")
    }

    private fun showTokenSale() {
        val tokenSaleIntent = Intent(context, TokenSalesActivity::class.java)
        startActivity(tokenSaleIntent)
    }

    private fun showAssets(data: TransferableAssets) {
        swipeContainer.isRefreshing = false
        val adapter = AccountAssetsAdapter(this,context!!, Account.getWallet()!!.address,
                data.assets)
        assetListView.adapter = adapter
    }


    fun showClaims(claims: ClaimData) {
        val amount = claims.data.gas.toDouble()
        unclaimedGASTicker.text =  "%.8f".format(accountViewModel.getEstimatedGas(claims))
        claimAmount = amount
        learnMoreClaimButton.visibility = View.VISIBLE
        syncButton.visibility = View.VISIBLE

        if (accountViewModel.getClaimingStatus()) {
            this.syncButton.isEnabled = false
        } else {
            this.syncButton.isEnabled = !(amount == 0.0 || unclaimedGASTicker.visibility == View.GONE)
        }
    }

    fun showSyncingInProgress() {
        onUiThread {
            view?.find<ImageView>(R.id.syncingProgress)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.syncingSubtitle)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.syncingTitle)?.visibility = View.VISIBLE
            view?.find<View>(R.id.gasClaimDivider)?.visibility = View.GONE

            view?.find<TextView>(R.id.gasStateTitle)?.visibility = View.GONE
            view?.find<TextView>(R.id.claimableGasHeader)?.visibility = View.GONE
            view?.find<TickerView>(R.id.unclaimedGasTicker)?.visibility = View.GONE
            view?.find<ImageView>(R.id.claimableGasImageView)?.visibility = View.GONE
            learnMoreClaimButton.visibility = View.GONE
            syncButton.visibility = View.GONE
        }
    }

    fun showReadyToClaim() {
        onUiThread {
            unclaimedGASTicker.text = accountViewModel.getStoredClaims().data.gas
            unclaimedGASTicker.textColor = resources.getColor(R.color.colorBlack)
            learnMoreClaimButton.visibility = View.GONE
            syncButton.visibility = View.GONE

            view?.find<ImageView>(R.id.syncingProgress)?.visibility = View.GONE
            view?.find<TextView>(R.id.syncingSubtitle)?.visibility = View.GONE
            view?.find<TextView>(R.id.syncingTitle)?.visibility = View.GONE
            view?.find<View>(R.id.gasClaimDivider)?.visibility = View.VISIBLE

            view?.find<ImageView>(R.id.claimableGasImageView)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.gasStateTitle)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.gasStateTitle)?.text = getString(R.string.WALLET_confirmed_gas)

            view?.find<TextView>(R.id.claimableGasHeader)?.visibility = View.VISIBLE
            view?.find<TickerView>(R.id.unclaimedGasTicker)?.visibility = View.VISIBLE
            claimButton.visibility = View.VISIBLE
        }
    }

    fun showClaimSucceeded() {
        onUiThread {
            view?.find<TextView>(R.id.gasStateTitle)?.visibility = View.GONE
            view?.find<TextView>(R.id.claimableGasHeader)?.visibility = View.GONE
            view?.find<TickerView>(R.id.unclaimedGasTicker)?.visibility = View.GONE
            view?.find<ImageView>(R.id.claimableGasImageView)?.visibility = View.GONE
            view?.find<View>(R.id.gasClaimDivider)?.visibility = View.GONE
            claimButton.visibility = View.GONE

            view?.find<TextView>(R.id.successfulClaimAmountTextView)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.successfulClaimTitleTextView)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.successfulClaimSubtitle)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.successfulClaimAmountTextView)?.text = unclaimedGASTicker.text
            view?.find<ImageView>(R.id.coinsImageView)?.visibility = View.VISIBLE


            unclaimedGASTicker.text = "0.00000000"
            progressBarBegin(60000, true)

        }
    }

    fun progressBarBegin(millis: Long, claimComplete: Boolean) {
        val progressBar = view?.find<ProgressBar>(R.id.canClaimAgainProgress)
        progressBar?.visibility = View.VISIBLE

        progressBar?.max = 10000
        val animation = ObjectAnimator.ofInt(progressBar, "progress" , 0, 10000)
        animation.setDuration(millis)
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                swipeContainer.setOnRefreshListener {
                    swipeContainer.isRefreshing = false
                }
            }
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                swipeContainer.setOnRefreshListener {
                    swipeContainer.isRefreshing = true
                    reloadAllData()
                }
                if (claimComplete) {
                    showUnsyncedClaim(true)
                }
            }
            override fun onAnimationCancel(animation: Animator?) {}
        })
        animation.start()
    }

    fun showUnsyncedClaim(reload: Boolean) {
        onUiThread {
            view?.find<ImageView>(R.id.syncingProgress)?.visibility = View.GONE
            view?.find<TextView>(R.id.syncingSubtitle)?.visibility = View.GONE
            view?.find<TextView>(R.id.syncingTitle)?.visibility = View.GONE
            view?.find<TextView>(R.id.successfulClaimAmountTextView)?.visibility = View.GONE
            view?.find<TextView>(R.id.successfulClaimTitleTextView)?.visibility = View.GONE
            view?.find<TextView>(R.id.successfulClaimSubtitle)?.visibility = View.GONE
            view?.find<ImageView>(R.id.coinsImageView)?.visibility = View.GONE
            view?.find<ProgressBar>(R.id.canClaimAgainProgress)?.visibility = View.GONE

            view?.find<TextView>(R.id.gasStateTitle)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.gasStateTitle)?.text = getString(R.string.WALLET_estimated_gas)
            view?.find<TextView>(R.id.claimableGasHeader)?.visibility = View.VISIBLE
            view?.find<TickerView>(R.id.unclaimedGasTicker)?.visibility = View.VISIBLE
            view?.find<ImageView>(R.id.claimableGasImageView)?.visibility = View.VISIBLE
            view?.find<View>(R.id.gasClaimDivider)?.visibility = View.VISIBLE
            unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
            learnMoreClaimButton.visibility = View.VISIBLE
            syncButton.visibility = View.VISIBLE

            if (reload) {
                firstLoad = true
                reloadAllData()
            }
        }
    }


    fun syncTapped() {
        tickupHandler.removeCallbacks(tickupRunnable)
        showSyncingInProgress()
        progressBarBegin(45000, false)
        accountViewModel.syncChain {
            onUiThread {
                view?.find<ProgressBar>(R.id.canClaimAgainProgress)?.visibility = View.GONE
            }
            if (it) {
                showReadyToClaim()
            } else {
                onUiThread {
                    alert (getString(R.string.WALLET_sync_failed)) {
                        yesButton { getString(R.string.ALERT_OK_Confirm_Button) }
                    }.show()
                }
                showUnsyncedClaim(false)
            }
        }
    }

    fun claimTapped() {
        accountViewModel.performClaim { succeeded, error ->
            if (error != null || succeeded == false) {
                onUiThread {
                    alert (getString(R.string.WALLET_claim_error)) {
                        yesButton { getString(R.string.ALERT_OK_Confirm_Button) }
                    }.show()
                }
               return@performClaim
            } else {
                claimSucceeded = true
                showClaimSucceeded()
            }
        }
    }

    private fun menuButtonTapped() {
        val intent: Intent = Intent(
                context,
                SendActivity::class.java
        )
        intent.putExtra("address", "")
        val option = ActivityOptionsCompat.makeSceneTransitionAnimation(this.activity!!, menuButton, ViewCompat.getTransitionName(menuButton))
        ActivityCompat.startActivity(context!!, intent, option.toBundle())
    }

    companion object {
        fun newInstance(): AccountFragment {
            return AccountFragment()
        }
    }
}
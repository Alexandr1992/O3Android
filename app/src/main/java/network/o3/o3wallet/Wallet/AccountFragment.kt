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
import com.bumptech.glide.Glide
import com.google.zxing.integration.android.IntentIntegrator
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import kotlinx.android.synthetic.main.wallet_fragment_account.*
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.*
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import org.jetbrains.anko.support.v4.onUiThread
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.yesButton
import java.text.NumberFormat
import java.util.*


class AccountFragment : Fragment() {

    private lateinit var myQrButton: Button
    private lateinit var sendButton: Button
    private lateinit var scanButton: Button
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

    fun setupActionButtons(view: View) {
        myQrButton = view.findViewById(R.id.requestButton)
        sendButton = view.findViewById(R.id.sendButton)
        scanButton = view.findViewById(R.id.scanButton)

        myQrButton.setOnClickListener { showMyAddress() }
        sendButton.setOnClickListener { sendButtonTapped("") }
        scanButton.setOnClickListener { scanAddressTapped() }

    }

    fun setUpInboxData() {
        accountViewModel.getInboxItem(true).observe(this, Observer<O3InboxItem?>{
            if (it == null){
                tokenSwapCard.visibility = View.GONE
            } else {
                tokenSwapCard.visibility = View.VISIBLE
                find<TextView>(R.id.tokenSwapTitleView).text = it.title
                find<TextView>(R.id.tokenSwapDescriptionView).text = it.description
                find<TextView>(R.id.tokenSwapSubtitleLabel).text = it.subtitle
                find<Button>(R.id.tokenSwapLearnmoreButton).text = it.readmoreTitle
                find<Button>(R.id.tokenSwapActionButton).text = it.actionTitle

                if (tokenSwapSubtitleLabel.text.isBlank()) {
                    tokenSwapSubtitleLabel.visibility = View.GONE
                } else {
                    tokenSwapSubtitleLabel.visibility = View.VISIBLE
                }

                if (tokenSwapDescriptionView.text.isBlank()) {
                    tokenSwapDescriptionView.visibility = View.GONE
                } else {
                    tokenSwapDescriptionView.visibility = View.VISIBLE
                }
                Glide.with(context).load(it.iconURL).into(find(R.id.tokenSwapLogoImageView))

                val nep9 = it.actionURL
                find<Button>(R.id.tokenSwapActionButton).setOnClickListener {
                    sendButtonTapped(nep9)
                }

                val learnURL = it.readmoreURL
                find<Button>(R.id.tokenSwapLearnmoreButton).setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(learnURL))
                    startActivity(intent)
                }
            }
        })
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
        setUpInboxData()
        tickupRunnable = object : Runnable {
            override fun run() {
                tickup()
                tickupHandler.postDelayed(this, 15000)
            }
        }

        //find<ConstraintLayout>(R.id.toolBarContainer).bringToFront()

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

        setupActionButtons(view)

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
        val format = NumberFormat.getInstance()
        val number = format.parse(unclaimedGASTicker.text)
        val current = number.toDouble()
        val addIntervalAmount = (accountViewModel.getNeoBalance() * 7 / 100000000.0)
        unclaimedGASTicker.text = "%.8f".format(current + addIntervalAmount)
    }

    private fun beginTickup() {
        tickupHandler.postDelayed(tickupRunnable, 15000)
    }

    private fun showMyAddress() {
        val addressBottomSheet = MyAddressFragment()
        addressBottomSheet.show(activity!!.supportFragmentManager, "myaddress")
    }


    private fun showAssets(data: TransferableAssets) {
        swipeContainer.isRefreshing = false
        val adapter = AccountAssetsAdapter(this,context!!, Account.getWallet()!!.address,
                data.assets)
        assetListView.adapter = adapter
    }


    fun showClaims(claims: ClaimData) {
        //Claim data always comes as us number
        val format = NumberFormat.getInstance(Locale.US)
        val number = format.parse(claims.data.gas)
        val amount = number.toDouble()

        unclaimedGASTicker.text =  "%.8f".format(accountViewModel.getEstimatedGas(claims))
        claimAmount = amount
        //learnMoreClaimButton.visibility = View.VISIBLE
        if (claimAmount > 0) {
            syncButton.visibility = View.VISIBLE
        }

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
            //learnMoreClaimButton.visibility = View.GONE
            syncButton.visibility = View.GONE
        }
    }

    fun showReadyToClaim() {
        onUiThread {
            unclaimedGASTicker.text = accountViewModel.getStoredClaims().data.gas
            unclaimedGASTicker.textColor = resources.getColor(R.color.colorBlack)
            //learnMoreClaimButton.visibility = View.GONE
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
            override fun onAnimationCancel(animation: Animator?) {
                swipeContainer.setOnRefreshListener {
                    swipeContainer.isRefreshing = true
                    reloadAllData()
                }
            }
        })
        animation.start()
    }

    fun showUnsyncedClaim(reload: Boolean) {
        //the thread progress could have finished earlier
        if (activity == null) {
            return
        }
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
           // learnMoreClaimButton.visibility = View.VISIBLE
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

    fun scanAddressTapped() {
        val integrator = IntentIntegrator(activity)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
        integrator.setPrompt(resources.getString(R.string.SEND_scan_prompt_qr))
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            Toast.makeText(this.context, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
        } else {
            sendButtonTapped(result.contents.trim())
        }
    }

    private fun sendButtonTapped(payload: String) {
        val intent = Intent(activity, SendV2Activity::class.java)
        intent.putExtra("uri", payload)
        startActivity(intent)
    }

    companion object {
        fun newInstance(): AccountFragment {
            return AccountFragment()
        }
    }
}

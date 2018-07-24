package network.o3.o3wallet.Wallet
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
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.zxing.integration.android.IntentIntegrator
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import kotlinx.android.synthetic.main.wallet_fragment_account.*
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.*
import network.o3.o3wallet.API.Ontology.OntologyClient
import org.jetbrains.anko.support.v4.onUiThread
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.textColor
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
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var assetListView: ListView

    //Ontology stuff
    private lateinit var ontologyTicker: TickerView

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

    fun setupOntologyClaimListener() {
        accountViewModel.getOntologyClaims().observe(this, Observer<OntologyClaimableGas?> {
            val doubleValue = it!!.ong.toLong() / OntologyClient().DecimalDivisor
            ontologyTicker.text = "%.8f".format(doubleValue)
            ontologyTicker.textColor = resources.getColor(R.color.colorBlack)
        })
    }

    fun reloadAllData() {
        accountViewModel.loadOntologyClaims()
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
        unclaimedGASTicker = view.findViewById(R.id.unclaimedGasTicker)
        assetListView = view.findViewById(R.id.assetListView)

        //Ontology stuff
        ontologyTicker = view.find(R.id.unclaimedGasTickerOntology)
        ontologyTicker.setCharacterList(TickerUtils.getDefaultNumberList())
        ontologyTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
        setupOntologyClaimListener()


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
        if (claimAmount > 0) {
            syncButton.visibility = View.VISIBLE
            view?.find<LottieAnimationView>(R.id.neoGasProgress)?.visibility = View.GONE

        }

        if (accountViewModel.getClaimingStatus()) {
            this.syncButton.isEnabled = false
        } else {
            this.syncButton.isEnabled = !(amount == 0.0 || unclaimedGASTicker.visibility == View.GONE)
        }
    }

    fun showSyncingInProgress() {
        onUiThread {
            view?.find<LottieAnimationView>(R.id.neoGasProgress)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.gasStateTitle)?.text = resources.getString(R.string.WALLET_syncing_title)


            syncButton.visibility = View.GONE
        }
    }

    fun showReadyToClaim() {
        onUiThread {
            if (accountViewModel.getStoredClaims() != null) {
                unclaimedGASTicker.text = accountViewModel.getStoredClaims()!!.data.gas
                claimButton.visibility = View.VISIBLE
            } else {
                claimButton.visibility = View.INVISIBLE
            }
            unclaimedGASTicker.textColor = resources.getColor(R.color.colorBlack)
            syncButton.visibility = View.GONE

            view?.find<LottieAnimationView>(R.id.neoGasProgress)?.visibility = View.GONE
            view?.find<View>(R.id.gasClaimDivider)?.visibility = View.VISIBLE

            view?.find<ImageView>(R.id.claimableGasImageView)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.gasStateTitle)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.gasStateTitle)?.text = getString(R.string.WALLET_ready_to_claim_gas)

            view?.find<TextView>(R.id.claimableGasHeader)?.visibility = View.VISIBLE
            view?.find<TickerView>(R.id.unclaimedGasTicker)?.visibility = View.VISIBLE

        }
    }

    fun showClaimSucceeded() {
        onUiThread {
            gasStateTitle.textColor = resources.getColor(R.color.colorGain)
            view?.find<LottieAnimationView>(R.id.neoGasSuccess)?.visibility = View.VISIBLE
            view?.find<LottieAnimationView>(R.id.neoGasSuccess)?.playAnimation()
            Handler().postDelayed(Runnable{
                view?.find<TextView>(R.id.gasStateTitle)?.text = getString(R.string.WALLET_estimated_gas)
                view?.find<TextView>(R.id.gasStateTitle)?.textColor = resources.getColor(R.color.colorSubtitleGrey)
                view?.find<LottieAnimationView>(R.id.neoGasSuccess)?.visibility = View.GONE
                reloadAllData()
            }, 60000)
        }
    }

    fun progressBarBegin(millis: Long, claimComplete: Boolean) {
        /*val progressBar = view?.find<ProgressBar>(R.id.canClaimAgainProgress)
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
        animation.start()*/
    }

    fun showUnsyncedClaim(reload: Boolean) {
        //the thread progress could have finished earlier
        if (activity == null) {
            return
        }
        onUiThread {
            view?.find<LottieAnimationView>(R.id.neoGasProgress)?.visibility = View.GONE

            view?.find<TextView>(R.id.gasStateTitle)?.visibility = View.VISIBLE
            view?.find<TextView>(R.id.gasStateTitle)?.text = getString(R.string.WALLET_estimated_gas)
            view?.find<TextView>(R.id.claimableGasHeader)?.visibility = View.VISIBLE
            view?.find<TickerView>(R.id.unclaimedGasTicker)?.visibility = View.VISIBLE
            view?.find<ImageView>(R.id.claimableGasImageView)?.visibility = View.VISIBLE
            view?.find<View>(R.id.gasClaimDivider)?.visibility = View.VISIBLE
            unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
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
        view?.find<LottieAnimationView>(R.id.neoGasProgress)?.visibility = View.VISIBLE
        view?.find<Button>(R.id.claimButton)?.visibility = View.GONE
        accountViewModel.performClaim { succeeded, error ->
            if (error != null || succeeded == false) {
                onUiThread {
                    view?.find<LottieAnimationView>(R.id.neoGasProgress)?.visibility = View.GONE
                    view?.find<Button>(R.id.claimButton)?.visibility = View.VISIBLE
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

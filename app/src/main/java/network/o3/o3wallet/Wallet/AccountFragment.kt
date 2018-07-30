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
import kotlinx.android.synthetic.main.onboarding_passcode_request_activity.*
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

    // tool narr
    private lateinit var myQrButton: Button
    private lateinit var sendButton: Button
    private lateinit var scanButton: Button
    private lateinit var syncButton: Button
    private lateinit var claimButton: Button

    //assets list
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var assetListView: ListView

    //Gas Claim Card NEO
    private lateinit var neoGasProgress: LottieAnimationView
    private lateinit var neoGasSuccess: LottieAnimationView
    private lateinit var neoGasClaimingStateTitle: TextView
    private lateinit var unclaimedGASTicker: TickerView

    //Gas Claim Card Ontology
    private lateinit var ontologyTicker: TickerView

    //Ontology stuff


    private lateinit var accountViewModel: AccountViewModel
    private var claimAmount: Double = 0.0
    private var firstLoad = true
    private var tickupHandler = Handler()
    private var claimReloadHandler = Handler()
    private lateinit var tickupRunnable: Runnable
    private var claimSucceeded = false

    private var waitingForClaimProcess = false

    override fun onPause() {
        super.onPause()
        claimReloadHandler.removeCallbacksAndMessages(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.wallet_fragment_account, container, false)
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

    fun setupActionButtons(view: View) {
        myQrButton = view.findViewById(R.id.requestButton)
        sendButton = view.findViewById(R.id.sendButton)
        scanButton = view.findViewById(R.id.scanButton)

        myQrButton.setOnClickListener { showMyAddress() }
        sendButton.setOnClickListener { sendButtonTapped("") }
        scanButton.setOnClickListener { scanAddressTapped() }

    }

    fun setupNeoGasClaimViews(view: View) {
        neoGasProgress = view.find(R.id.neoGasProgress)
        neoGasSuccess = view.find(R.id.neoGasSuccess)
        neoGasClaimingStateTitle = view.find(R.id.gasStateTitle)
        unclaimedGASTicker = view.findViewById(R.id.unclaimedGasTicker)
        unclaimedGASTicker.setCharacterList(TickerUtils.getDefaultNumberList())

        syncButton = view.find(R.id.syncButton)
        claimButton = view.find(R.id.claimButton)
        unclaimedGASTicker.text = "0.00000000"
        unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)

        tickupRunnable = object : Runnable {
            override fun run() {
                tickup()
                tickupHandler.postDelayed(this, 15000)
            }
        }
    }

    fun setupOntologyGasClaimViews(view: View) {
        ontologyTicker = view.find(R.id.unclaimedGasTickerOntology)
        ontologyTicker.setCharacterList(TickerUtils.getDefaultNumberList())
        ontologyTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
        setupOntologyClaimListener()

        syncButton.setOnClickListener { syncTapped() }
        claimButton.setOnClickListener { claimTapped() }
    }

    fun setupAssetList(view: View) {
        assetListView = view.findViewById(R.id.assetListView)
        swipeContainer = view.findViewById(R.id.swipeContainer)
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary)

        swipeContainer.setOnRefreshListener {
            if (neoGasProgress.visibility == View.VISIBLE ||
                    neoGasSuccess.visibility == View.VISIBLE) {
                swipeContainer.isRefreshing = false
            } else {
                swipeContainer.isRefreshing = true
                if (!waitingForClaimProcess) {
                    reloadAllData()
                } else {
                    reloadAssets()
                }
            }
        }
    }

    fun setupOntologyClaimListener() {
        accountViewModel.getOntologyClaims().observe(this, Observer<OntologyClaimableGas?> {
            val doubleValue = it!!.ong.toLong() / OntologyClient().DecimalDivisor
            ontologyTicker.text = "%.8f".format(doubleValue)
            ontologyTicker.textColor = resources.getColor(R.color.colorBlack)
        })
    }

    fun reloadAssets() {
        accountViewModel.getAssets(true).observe(this, Observer<TransferableAssets?> {
            if (it == null) {
                context?.toast(accountViewModel.getLastError().localizedMessage)
            } else {
                showAssets(it)
            }
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

        setupNeoGasClaimViews(view)
        setupOntologyGasClaimViews(view)
        setupAssetList(view)
        setupActionButtons(view)
        setUpInboxData()

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
            neoGasProgress.visibility = View.GONE

        }

        if (accountViewModel.getClaimingStatus()) {
            this.syncButton.isEnabled = false
        } else {
            this.syncButton.isEnabled = !(amount == 0.0 || unclaimedGASTicker.visibility == View.GONE)
        }
    }

    fun showSyncingInProgress() {
        onUiThread {
            neoGasProgress.visibility = View.VISIBLE
            neoGasClaimingStateTitle.text = resources.getString(R.string.WALLET_syncing_title)
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
            neoGasProgress.visibility = View.GONE
            neoGasClaimingStateTitle.visibility = View.VISIBLE
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_ready_to_claim_gas)
            unclaimedGASTicker.visibility = View.VISIBLE
        }
    }

    fun showClaimSucceeded() {
        onUiThread {
            gasStateTitle.textColor = resources.getColor(R.color.colorGain)
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_confirmed_gas)
            neoGasSuccess.visibility = View.VISIBLE
            neoGasSuccess.playAnimation()
            Handler().postDelayed(Runnable{
                neoGasClaimingStateTitle.text = getString(R.string.WALLET_estimated_gas)
                neoGasClaimingStateTitle.textColor = resources.getColor(R.color.colorSubtitleGrey)
                unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
                neoGasSuccess.visibility = View.GONE
                reloadAllData()
            }, 30000)
        }
    }

    fun showUnsyncedClaim(reload: Boolean) {
        //the thread progress could have finished earlier
        if (activity == null) {
            return
        }
        onUiThread {
            neoGasProgress.visibility = View.GONE
            neoGasClaimingStateTitle.visibility = View.VISIBLE
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_estimated_gas)
            unclaimedGASTicker.visibility = View.VISIBLE
            unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
            syncButton.visibility = View.VISIBLE

            if (reload) {
                firstLoad = true
                reloadAllData()
            }
        }
    }

    fun syncTapped() {
        waitingForClaimProcess = true
        claimReloadHandler.postDelayed(Runnable {
            waitingForClaimProcess = false
        }, 200000)

        tickupHandler.removeCallbacks(tickupRunnable)
        showSyncingInProgress()
        accountViewModel.syncChain {
            if (it) {
                showReadyToClaim()
            } else {
                onUiThread {
                    if (accountViewModel.needsSync == false) {
                        alert (getString(R.string.WALLET_sync_failed)) {
                            yesButton { getString(R.string.ALERT_OK_Confirm_Button) }
                        }.show()
                    } else {
                        alert (getString(R.string.WALLET_sync_failed)) {
                            yesButton { getString(R.string.ALERT_OK_Confirm_Button) }
                        }.show()
                    }
                }
                showUnsyncedClaim(false)
            }
        }
    }

    fun claimTapped() {
        neoGasProgress.visibility = View.VISIBLE
        claimButton.visibility = View.GONE
        accountViewModel.performClaim { succeeded, error ->
            if (error != null || succeeded == false) {
                onUiThread {
                    neoGasProgress.visibility = View.GONE
                    claimButton.visibility = View.VISIBLE
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

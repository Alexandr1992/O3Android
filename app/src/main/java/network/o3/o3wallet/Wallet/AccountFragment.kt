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
import android.support.v7.widget.CardView
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
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor
import org.jetbrains.anko.yesButton
import java.text.NumberFormat
import java.util.*


class AccountFragment : Fragment() {

    // toolbar items
    private lateinit var myQrButton: Button
    private lateinit var sendButton: Button
    private lateinit var scanButton: Button

    //assets list
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var assetListView: ListView

    //Gas Claim Card NEO
    private lateinit var neoSyncButton: Button
    private lateinit var neoClaimButton: Button
    private lateinit var neoGasProgress: LottieAnimationView
    private lateinit var neoGasSuccess: LottieAnimationView
    private lateinit var neoGasClaimingStateTitle: TextView
    private lateinit var unclaimedGASTicker: TickerView

    //Gas Claim Card Ontology
    private lateinit var ontologyTicker: TickerView
    private lateinit var ontologySyncButton: Button
    private lateinit var ontologyClaimButton: Button
    private lateinit var ontologyGasProgress: LottieAnimationView
    private lateinit var ontologyGasSuccess: LottieAnimationView
    private lateinit var ontologyClaimingStateTitle: TextView

    private lateinit var accountViewModel: AccountViewModel
    private var firstLoad = true
    private var tickupHandler = Handler()
    private lateinit var tickupRunnable: Runnable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.wallet_fragment_account, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountViewModel = AccountViewModel()

        setupNeoGasClaimViews(view)
        setupOntologyGasClaimViews(view)
        setupAssetList(view)
        setupActionButtons(view)
        setUpInboxData()
        setupAssetListener()
        setupNeoClaimsListener()

        activity?.title = "Account"

    }


    //region UI Binding
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
        neoGasClaimingStateTitle = view.find(R.id.neoGasStateTitle)
        unclaimedGASTicker = view.findViewById(R.id.neoUnclaimedGasTicker)
        unclaimedGASTicker.setCharacterList(TickerUtils.getDefaultNumberList())

        neoSyncButton = view.find(R.id.neoSyncButton)
        neoClaimButton = view.find(R.id.neoClaimButton)
        unclaimedGASTicker.text = "0.00000000"
        unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
        neoSyncButton.setOnClickListener { neoSyncTapped() }
        neoClaimButton.setOnClickListener { neoClaimTapped() }
        tickupRunnable = object : Runnable {
            override fun run() {
                neoTickup()
                tickupHandler.postDelayed(this, 15000)
            }
        }
    }

    fun setupOntologyGasClaimViews(view: View) {
        ontologyTicker = view.find(R.id.ontologyUnclaimedGasTicker)
        ontologyTicker.setCharacterList(TickerUtils.getDefaultNumberList())
        ontologyTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)

        ontologySyncButton = view.find(R.id.ontologySyncButton)
        ontologyClaimButton = view.find(R.id.ontologyClaimButton)
        ontologyGasProgress = view.find(R.id.ontologyGasProgress)
        ontologyGasSuccess = view.find(R.id.ontologyGasSuccess)
        ontologyClaimingStateTitle = view.find(R.id.ontologyGasStateTitle)

        ontologyClaimButton.setOnClickListener { ontologyClaimTapped() }
        ontologySyncButton.setOnClickListener {ontologySyncTapped() }

        setupOntologyClaimListener()
    }

    fun setupAssetList(view: View) {
        assetListView = view.findViewById(R.id.assetListView)
        swipeContainer = view.findViewById(R.id.swipeContainer)
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary)

        swipeContainer.setOnRefreshListener {
            accountViewModel.loadAssets()
        }
    }
    //endregion

    //region ViewModel Listeners
    fun setupOntologyClaimListener() {
        accountViewModel.getOntologyClaims().observe(this, Observer<OntologyClaimableGas?> {
            val doubleValue = it!!.ong.toLong() / OntologyClient().DecimalDivisor
            ontologyTicker.textColor = resources.getColor(R.color.colorBlack)
            // ready to claim
            if (it.calculated == false) {
                showOntologyGasReadyToClaim(doubleValue)
            } else {
                showOntologyGasReadyToSync(doubleValue)
            }
        })
    }

    fun setupAssetListener() {
        accountViewModel.getAssets().observe(this, Observer<TransferableAssets?> {
            if (it == null) {
                context?.toast(accountViewModel.getLastError().localizedMessage)
            } else {
                showAssets(it)
            }
        })
    }

    fun setupNeoClaimsListener() {
        accountViewModel.getClaims().observe(this, Observer<ClaimData?> {
            if (it == null) {
                this.neoSyncButton.isEnabled = false
                context?.toast(accountViewModel.getLastError().localizedMessage)
            } else {
                val format = NumberFormat.getInstance(Locale.US)
                val number = format.parse(it.data.gas)
                val current = number.toDouble()
                unclaimedGASTicker.text =  "%.8f".format(current)
                if (it.data.claims.isNotEmpty()) {
                    showNeoReadyToClaim()
                } else {
                    showNeoClaims(it)
                    if (firstLoad) {
                        beginTickup()
                        firstLoad = false
                    }
                }
            }
        })
    }
    //endregion

    //region Inbox
    fun setUpInboxData() {
        accountViewModel.getInboxItem().observe(this, Observer<O3InboxItem?>{
            if (it == null){
                find<CardView>(R.id.tokenSwapCard).visibility = View.GONE
            } else {
                find<CardView>(R.id.tokenSwapCard).visibility = View.VISIBLE
                find<TextView>(R.id.tokenSwapTitleView).text = it.title
                find<TextView>(R.id.tokenSwapDescriptionView).text = it.description
                find<TextView>(R.id.tokenSwapSubtitleLabel).text = it.subtitle
                find<Button>(R.id.tokenSwapLearnmoreButton).text = it.readmoreTitle
                find<Button>(R.id.tokenSwapActionButton).text = it.actionTitle

                if (find<TextView>(R.id.tokenSwapSubtitleLabel).text.isBlank()) {
                    find<TextView>(R.id.tokenSwapSubtitleLabel).visibility = View.GONE
                } else {
                    find<TextView>(R.id.tokenSwapSubtitleLabel).visibility = View.VISIBLE
                }

                if (find<TextView>(R.id.tokenSwapDescriptionView).text.isBlank()) {
                    find<TextView>(R.id.tokenSwapDescriptionView).visibility = View.GONE
                } else {
                    find<TextView>(R.id.tokenSwapDescriptionView).visibility = View.VISIBLE
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
    // endregion

    //region Assets
    private fun showAssets(data: TransferableAssets) {
        swipeContainer.isRefreshing = false
        val adapter = AccountAssetsAdapter(this,context!!, Account.getWallet()!!.address,
                data.assets)
        assetListView.adapter = adapter
    }
    //endregion

    //region NEO claiming
    private fun neoTickup() {
        val format = NumberFormat.getInstance()
        val number = format.parse(unclaimedGASTicker.text)
        val current = number.toDouble()
        val addIntervalAmount = (accountViewModel.getNeoBalance() * 7 / 100000000.0)
        unclaimedGASTicker.text = "%.8f".format(current + addIntervalAmount)
    }

    private fun beginTickup() {
        tickupHandler.postDelayed(tickupRunnable, 15000)
    }

    fun showNeoClaims(claims: ClaimData) {
        //Claim data always comes as us number
        val format = NumberFormat.getInstance(Locale.US)
        val number = format.parse(claims.data.gas)
        val amount = number.toDouble()

        if (amount > 0) {
            neoSyncButton.visibility = View.VISIBLE
            neoGasProgress.visibility = View.GONE
        }

        this.neoSyncButton.isEnabled = !(amount == 0.0 || unclaimedGASTicker.visibility == View.GONE)
    }

    fun showNeoUnsyncedClaim(reload: Boolean) {
        //the thread progress could have finished earlier
        if (activity == null) {
            return
        }
        onUiThread {
            neoGasProgress.visibility = View.GONE
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_estimated_gas)
            unclaimedGASTicker.visibility = View.VISIBLE
            unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
            neoSyncButton.visibility = View.VISIBLE

            if (reload) {
                firstLoad = true
                accountViewModel.loadClaims()
            }
        }
    }

    fun showNeoSyncingInProgress() {
        onUiThread {
            neoGasProgress.visibility = View.VISIBLE
            neoGasClaimingStateTitle.text = resources.getString(R.string.WALLET_syncing_title)
            neoSyncButton.visibility = View.GONE
        }
    }

    fun showNeoReadyToClaim() {
        onUiThread {
            if (accountViewModel.getStoredClaims() != null) {
                unclaimedGASTicker.text = accountViewModel.getStoredClaims()!!.data.gas
                neoClaimButton.visibility = View.VISIBLE
            } else {
                neoClaimButton.visibility = View.INVISIBLE
            }

            unclaimedGASTicker.textColor = resources.getColor(R.color.colorBlack)
            neoSyncButton.visibility = View.GONE
            neoGasProgress.visibility = View.GONE
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_ready_to_claim_gas)
            unclaimedGASTicker.visibility = View.VISIBLE
        }
    }

    fun showNeoClaimSucceeded() {
        onUiThread {
            neoGasStateTitle.textColor = resources.getColor(R.color.colorGain)
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_confirmed_gas)
            neoGasSuccess.visibility = View.VISIBLE
            neoGasSuccess.playAnimation()
            Handler().postDelayed(Runnable{
                if (activity == null) {
                    return@Runnable
                }
                neoGasClaimingStateTitle.text = getString(R.string.WALLET_estimated_gas)
                neoGasClaimingStateTitle.textColor = resources.getColor(R.color.colorSubtitleGrey)
                unclaimedGASTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
                neoGasSuccess.visibility = View.GONE
                accountViewModel.loadClaims()
            }, 60000)
        }
    }

    fun neoSyncTapped() {
        tickupHandler.removeCallbacks(tickupRunnable)
        showNeoSyncingInProgress()
        accountViewModel.syncChain {
            if (it) {
                showNeoReadyToClaim()
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
                showNeoUnsyncedClaim(false)
            }
        }
    }

    fun neoClaimTapped() {
        neoGasProgress.visibility = View.VISIBLE
        neoClaimButton.visibility = View.GONE
        accountViewModel.performClaim { succeeded, error ->
            if (error != null || succeeded == false) {
                onUiThread {
                    neoGasProgress.visibility = View.GONE
                    neoClaimButton.visibility = View.VISIBLE
                    alert (getString(R.string.WALLET_claim_error)) {
                        yesButton { getString(R.string.ALERT_OK_Confirm_Button) }
                    }.show()
                }
                return@performClaim
            } else {
                showNeoClaimSucceeded()
            }
        }
    }
    // endregion

    //region Ontology Claiming
    fun showOntologyGasReadyToClaim(amount: Double? = null) {
        onUiThread {
            if (amount != null) {
                ontologyTicker.text = "%.8f".format(amount)
            }
            ontologyTicker.textColor = resources.getColor(R.color.colorBlack)
            ontologySyncButton.visibility = View.GONE
            ontologyClaimButton.visibility = View.VISIBLE
            ontologyGasProgress.visibility = View.GONE
            ontologyClaimingStateTitle.text = getString(R.string.WALLET_ready_to_claim_gas)
            ontologyTicker.visibility = View.VISIBLE
        }
    }

    fun showOntologyGasReadyToSync(amount: Double? = null) {
        onUiThread {
            if (amount != null) {
                ontologyTicker.text = "%.8f".format(amount)
            } else {
                ontologyTicker.text = "%.8f".format(0.0)
            }
            ontologyClaimingStateTitle.text = getString(R.string.WALLET_unbound_ong)
            ontologyClaimingStateTitle.textColor = resources.getColor(R.color.colorSubtitleGrey)
            ontologyGasSuccess.visibility = View.GONE
            ontologyTicker.textColor = resources.getColor(R.color.colorSubtitleGrey)
            ontologySyncButton.visibility = View.VISIBLE
            ontologyGasProgress.visibility = View.GONE
        }
    }

    fun showOntologyClaimSucceeded() {
        onUiThread {
            ontologyGasStateTitle.textColor = resources.getColor(R.color.colorGain)
            ontologyGasStateTitle.text = getString(R.string.WALLET_confirmed_gas)
            ontologyGasSuccess.visibility = View.VISIBLE
            ontologyGasSuccess.playAnimation()
            Handler().postDelayed(Runnable{
                if (activity == null) {
                    return@Runnable
                }
                showOntologyGasReadyToSync()
                accountViewModel.loadOntologyClaims()
            }, 30000)
        }
    }

    fun showOntologyLoadingInProgress() {
        onUiThread {
            ontologyGasStateTitle.text = resources.getString(R.string.WALLET_syncing_title)
            ontologyGasProgress.visibility = View.VISIBLE
            ontologySyncButton.visibility = View.GONE
            ontologyClaimButton.visibility = View.GONE
        }
    }

    fun ontologySyncTapped() {
        alert (resources.getString(R.string.WALLET_sync_ontology_gas)) {
            noButton {}
            yesButton {
                showOntologyLoadingInProgress()
                accountViewModel.syncOntologyChain { amount, error ->
                    if (amount != null) {
                        showOntologyGasReadyToClaim(amount)
                    } else {
                        showOntologyGasReadyToClaim(null)
                    }
                }
            }
        }.show()
    }

    fun ontologyClaimTapped() {
        alert (resources.getString(R.string.WALLET_claim_ontology_gas)) {
            noButton {}
            yesButton {
                showOntologyLoadingInProgress()
                OntologyClient().claimOntologyGas {
                    if(it.first) {
                        showOntologyClaimSucceeded()
                    } else {
                        showOntologyGasReadyToClaim()
                    }
                }
            }
        }.show()
    }
    //endregion

    //region Toolbar Action Items
    private fun showMyAddress() {
        val addressBottomSheet = MyAddressFragment()
        addressBottomSheet.show(activity!!.supportFragmentManager, "myaddress")
    }

    fun scanAddressTapped() {
        val integrator = IntentIntegrator(activity)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
        integrator.setPrompt(resources.getString(R.string.SEND_scan_prompt_qr))
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }


    private fun sendButtonTapped(payload: String) {
        val intent = Intent(activity, SendV2Activity::class.java)
        intent.putExtra("uri", payload)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {
            Toast.makeText(this.context, resources.getString(R.string.ALERT_cancelled), Toast.LENGTH_LONG).show()
        } else {
            sendButtonTapped(result.contents.trim())
        }
    }

    companion object {
        fun newInstance(): AccountFragment {
            return AccountFragment()
        }
    }
    //endregion
}

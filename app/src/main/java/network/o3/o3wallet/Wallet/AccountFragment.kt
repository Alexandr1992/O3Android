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
import android.os.Handler
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.zxing.integration.android.IntentIntegrator
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3Platform.*
import network.o3.o3wallet.API.Ontology.OntologyClient
import org.jetbrains.anko.support.v4.onUiThread
import network.o3.o3wallet.Wallet.SendV2.SendV2Activity
import org.jetbrains.anko.find
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.textColor
import org.jetbrains.anko.yesButton
import java.text.NumberFormat
import java.util.*
import android.support.v7.widget.LinearLayoutManager
import android.util.TypedValue
import org.jetbrains.anko.support.v4.find

class AccountFragment : Fragment() {
    // toolbar items
    private lateinit var myQrButton: Button
    private lateinit var sendButton: Button
    private lateinit var scanButton: Button

    //assets list
    private lateinit var swipeContainer: SwipeRefreshLayout
    lateinit var assetListView: RecyclerView

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

    private lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        mView = inflater.inflate(R.layout.wallet_fragment_account, container, false)
        accountViewModel = AccountViewModel()
        setupAssetList()
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAssetListener()
        setupNeoClaimsListener()
        setupNeoGasClaimViews()
        setupOntologyGasClaimViews()
        setupActionButtons()
        setupOntologyClaimListener()
        activity?.title = "Account"
    }


    //region UI Binding
    fun setupActionButtons() {
        myQrButton = mView.findViewById(R.id.requestButton)
        sendButton = mView.findViewById(R.id.sendButton)
        scanButton = mView.findViewById(R.id.scanButton)

        myQrButton.setOnClickListener { showMyAddress() }
        sendButton.setOnClickListener { sendButtonTapped("") }
        scanButton.setOnClickListener { scanAddressTapped() }
        activity!!.find<ImageButton>(R.id.rightNavButton).setOnClickListener { scanAddressTapped() }

    }

    fun setupNeoGasClaimViews() {
        neoGasProgress = mView.find(R.id.neoGasProgress)
        neoGasSuccess = mView.find(R.id.neoGasSuccess)
        neoGasClaimingStateTitle = mView.find(R.id.neoGasStateTitle)
        unclaimedGASTicker = mView.findViewById(R.id.neoUnclaimedGasTicker)
        unclaimedGASTicker.setCharacterList(TickerUtils.getDefaultNumberList())

        neoSyncButton = mView.find(R.id.neoSyncButton)
        neoClaimButton = mView.find(R.id.neoClaimButton)
        unclaimedGASTicker.text = "0.00000000"
        unclaimedGASTicker.textColor = context!!.getColor(R.color.colorSubtitleGrey)
        neoSyncButton.setOnClickListener { neoSyncTapped() }
        neoClaimButton.setOnClickListener { neoClaimTapped() }
        tickupRunnable = object : Runnable {
            override fun run() {
                neoTickup()
                tickupHandler.postDelayed(this, 15000)
            }
        }
    }

    fun setupOntologyGasClaimViews() {
        ontologyTicker = mView.find(R.id.ontologyUnclaimedGasTicker)
        ontologyTicker.setCharacterList(TickerUtils.getDefaultNumberList())
        ontologyTicker.textColor = context!!.getColor(R.color.colorSubtitleGrey)

        ontologySyncButton = mView.find(R.id.ontologySyncButton)
        ontologyClaimButton = mView.find(R.id.ontologyClaimButton)
        ontologyGasProgress = mView.find(R.id.ontologyGasProgress)
        ontologyGasSuccess = mView.find(R.id.ontologyGasSuccess)
        ontologyClaimingStateTitle = mView.find(R.id.ontologyGasStateTitle)

        ontologyClaimButton.setOnClickListener { ontologyClaimTapped() }
        ontologySyncButton.setOnClickListener {ontologySyncTapped() }

        setupOntologyClaimListener()
    }

    fun setupAssetList() {
        assetListView = mView.findViewById(R.id.assetListView)
        assetListView.itemAnimator?.changeDuration = 0
        val itemDecorator = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(context!!.getDrawable(R.drawable.vertical_divider))
        assetListView.addItemDecoration(itemDecorator)

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        assetListView.setLayoutManager(layoutManager)
        swipeContainer = mView.findViewById(R.id.swipeContainer)
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary)

        swipeContainer.setProgressBackgroundColorSchemeColor(context!!.getColorFromAttr(R.attr.secondaryBackgroundColor))
        swipeContainer.setOnRefreshListener {
            accountViewModel.loadAssets()
            accountViewModel.loadTradingAccountAssets()
        }
        assetListView.adapter = AccountAssetsAdapter(this)
    }
    //endregion

    //region ViewModel Listeners
    fun setupOntologyClaimListener() {
        accountViewModel.getOntologyClaims().observe(this, Observer<OntologyClaimableGas?> {
            val doubleValue = it!!.ong.toLong() / OntologyClient().DecimalDivisor
            accountViewModel.ontologyCanNotSync = doubleValue <= 0.02
            val typedValue = TypedValue()
            activity!!.getTheme().resolveAttribute(R.attr.defaultTextColor, typedValue, true)
            ontologyTicker.textColor = context!!.getColor(typedValue.resourceId)
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
            onUiThread {
                swipeContainer.isRefreshing = false
                if (it == null) {
                    context?.toast(accountViewModel.getLastError().localizedMessage)
                } else {
                     (assetListView.adapter as AccountAssetsAdapter).setAssetsArray(it.assets)
                }
                accountViewModel.loadWalletAccountPriceData(it!!.assets)
                accountViewModel.getInboxItem().observe(this, Observer<List<O3InboxItem>?>{
                    onUiThread {
                        (assetListView.adapter as AccountAssetsAdapter).setInboxList(it ?: listOf())
                    }
                })
            }
        })

        accountViewModel.getTradingAccountAssets().observe(this, Observer<List<TransferableAsset>> {
            onUiThread {
                (assetListView.adapter as AccountAssetsAdapter).setTradingAccountAssets(it!!)
            }
            accountViewModel.loadTradingAccountPriceData(it!!)
        })

        accountViewModel.getTradingAccountPriceData().observe(this, Observer { priceData ->
            onUiThread {
                (assetListView.adapter as AccountAssetsAdapter).setTradingAccountPriceData(priceData!!)
            }
        })

        accountViewModel.getWalletAccountPriceData().observe(this, Observer { priceData ->
            onUiThread {
                (assetListView.adapter as AccountAssetsAdapter).setWalletAccountPriceData(priceData!!)
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
            unclaimedGASTicker.textColor = context!!.getColor(R.color.colorSubtitleGrey)
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
            val typedValue = TypedValue()
            activity!!.getTheme().resolveAttribute(R.attr.defaultTextColor, typedValue, true)

            unclaimedGASTicker.textColor = context!!.getColor(typedValue.resourceId)
            neoSyncButton.visibility = View.GONE
            neoGasProgress.visibility = View.GONE
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_ready_to_claim_gas)
            unclaimedGASTicker.visibility = View.VISIBLE
        }
    }

    fun showNeoClaimSucceeded() {
        onUiThread {
            neoGasClaimingStateTitle.textColor = context!!.getColor(R.color.colorGain)
            neoGasClaimingStateTitle.text = getString(R.string.WALLET_confirmed_gas)
            neoGasSuccess.visibility = View.VISIBLE
            neoGasSuccess.playAnimation()
            Handler().postDelayed(Runnable{
                if (activity == null) {
                    return@Runnable
                }
                neoGasClaimingStateTitle.text = getString(R.string.WALLET_estimated_gas)
                neoGasClaimingStateTitle.textColor = context!!.getColor(R.color.colorSubtitleGrey)
                unclaimedGASTicker.textColor = context!!.getColor(R.color.colorSubtitleGrey)
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
            val typedValue = TypedValue()
            activity!!.getTheme().resolveAttribute(R.attr.defaultTextColor, typedValue, true)
            ontologyTicker.textColor = context!!.getColor(typedValue.resourceId)
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
            ontologyClaimingStateTitle.textColor = context!!.getColor(R.color.colorSubtitleGrey)
            ontologyGasSuccess.visibility = View.GONE
            ontologyTicker.textColor = context!!.getColor(R.color.colorSubtitleGrey)
            ontologySyncButton.visibility = View.VISIBLE
            ontologyGasProgress.visibility = View.GONE
        }
    }

    fun showOntologyClaimSucceeded() {
        onUiThread {
            ontologyClaimingStateTitle.textColor = context!!.getColor(R.color.colorGain)
            ontologyClaimingStateTitle.text = getString(R.string.WALLET_confirmed_gas)
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
            ontologyClaimingStateTitle.text = resources.getString(R.string.WALLET_syncing_title)
            ontologyGasProgress.visibility = View.VISIBLE
            ontologySyncButton.visibility = View.GONE
            ontologyClaimButton.visibility = View.GONE
        }
    }

    fun ontologySyncTapped() {
        if (accountViewModel.ontologyCanNotSync) {
            alert (resources.getString(R.string.WALLET_sync_ontology_error)) {
                yesButton {  }
            }.show()
            return
        }

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
    fun showMyAddress() {
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


    fun sendButtonTapped(payload: String, assetId: String? = null) {
        val intent = Intent(activity, SendV2Activity::class.java)
        intent.putExtra("uri", payload)
        intent.putExtra("assetID", assetId ?: "")
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


package network.o3.o3wallet.Dapp


import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.skydoves.powermenu.CustomPowerMenu
import com.skydoves.powermenu.MenuAnimation
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.runOnUiThread
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.*

class DAPPBrowser : Fragment() {

    lateinit var dappViewModel: DAPPViewModel
    lateinit var webView: WebView
    lateinit var jsInterface: DappBrowserJSInterfaceV2
    lateinit var searchBar: EditText
    lateinit var progressBar: ProgressBar
    lateinit var mView: View

    var legacyInterface: DappBrowserJSInterface? = null
    var previousWasRedirect = false
    val doNotShowAuthorities = arrayOf("analytics.o3.network")

    var pendingDappMessage: DappMessage? = null
    var lastClickTime: Long  = 0

    data class ResourceObject(val url: String, val mimeType: String, val resourceID: Int, val encoding: String)

    val localResources = arrayOf(
            ResourceObject("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css", "text/css", R.raw.bootstrap, "UTF-8"),
            ResourceObject("https://d3js.org/d3.v4.min.js", "text/javascript", R.raw.d3, "UTF-8"),
            ResourceObject("https://cdn.jsdelivr.net/npm/vue", "text/javascript", R.raw.vue, "UTF-8"),
            ResourceObject("https://unpkg.com/axios/dist/axios.min.js", "text/javascript", R.raw.axios, "UTF-8"),
            ResourceObject("https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.22.2/moment.min.js", "text/javascript", R.raw.moment, "UTF-8"),
            ResourceObject("https://cdn.o3.network/assets/fa/js/fontawesome-all.min.js", "text/javascript", R.raw.font_awesome, "UTF-8"),
            ResourceObject("https://cdnjs.cloudflare.com/ajax/libs/bodymovin/4.13.0/bodymovin.min.js", "text/javascript", R.raw.bodymovin, "UTF-8")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        updateNode()
        mView = inflater.inflate(R.layout.fragment_dapp_fragment, container, false)

        dappViewModel = (activity as DappContainerActivity).dappViewModel
        webView = mView.findViewById(R.id.dapp_browser_webview)
        searchBar = mView.find<EditText>(R.id.dappSearch)
        progressBar = mView.find(R.id.dappViewProgressBar)
        activity?.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        setupWebClients()

        webView.visibility = View.INVISIBLE
        webView.loadUrl(dappViewModel.url)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        WebView.setWebContentsDebuggingEnabled(true)

        if (URL(dappViewModel.url).authority == "switcheo.exchange" || URL(dappViewModel.url).authority == "legacy.switcheo.exchange" ||
                URL(dappViewModel.url).authority == "mnns.nel.group" || dappViewModel.legacy) {
            dappViewModel.legacyInterface = DappBrowserJSInterface(context!!, webView)
            webView.addJavascriptInterface(dappViewModel.legacyInterface, "O3AndroidInterface")
        } else {
            dappViewModel.dapiInterface = DappBrowserJSInterfaceV2(dappViewModel)
            webView.addJavascriptInterface(dappViewModel.dapiInterface, "_o3dapi")
        }

        listenForDappResponses()
        listenForAuthorizeInvoke()
        listenForAuthorizeSend()
        listenForAuthorizeWalletInfo()
        listenForUnlockStatus()

        //can allow for context based footer bars
        initateBrowserHeader(dappViewModel.allowSearch)
        initiateTradeFooter(Uri.parse((activity as DappContainerActivity).dappViewModel.url))
        showDappBrowserWarning()

        return mView
    }

    fun updateNode() {
        O3PlatformClient().getChainNetworks {
            if (it.first == null) {
                return@getChainNetworks
            } else {
                PersistentStore.setOntologyNodeURL(it.first!!.ontology.best)
                PersistentStore.setNodeURL(it.first!!.neo.best)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as DappContainerActivity).setStylingForURLText(dappViewModel.url)
    }


    fun setupWebClients() {
        webView.webChromeClient = object: WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams):Boolean {
                dappViewModel.mUploadMessage = filePathCallback
                val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
                fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
                fileIntent.type = "image/*"

                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val tmpDir = File(activity?.filesDir?.absolutePath + "/tmp")
                tmpDir.mkdirs()
                val fileImage = File(tmpDir, Calendar.getInstance().timeInMillis.toString() +  "_tmp.jpg")
                dappViewModel.photoURI = FileProvider.getUriForFile(this@DAPPBrowser.context!!, "network.o3.o3wallet", fileImage)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, dappViewModel.photoURI)

                var chooser = Intent.createChooser(fileIntent, "Upload File")
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

                startActivityForResult(chooser, dappViewModel.FILECHOOSER_RESULTCODE)
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val urlToLoad = request.url.toString()
                //we are in our own app, open a new browser

                if (!urlToLoad.startsWith("http") && !urlToLoad.startsWith("https")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToLoad))
                    val activityToUse = intent.resolveActivity(activity?.packageManager)
                    if (activityToUse == null) {
                        //webLoader.visibility = View.INVISIBLE
                        return false
                    } else {
                        startActivity(intent)
                        return true
                    }
                }

                (activity as DappContainerActivity).setStylingForURLText(urlToLoad)
                if (previousWasRedirect) {
                    return false
                }
                previousWasRedirect = (doNotShowAuthorities.contains(request.url.authority))

                view.loadUrl(urlToLoad)
                return false // then it is not handled by default action
            }


            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val localResource = localResources.find { it.url == request!!.url.toString() }
                if (localResource != null) {
                    val inputStream: InputStream = resources.openRawResource(localResource.resourceID)
                    val statusCode = 200
                    val reasonPhase = "OK"
                    val responseHeaders = mutableMapOf<String, String>()
                    responseHeaders.put("Access-Control-Allow-Origin", "*")
                    return WebResourceResponse(localResource.mimeType, localResource.encoding, statusCode, reasonPhase, responseHeaders, inputStream)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
                progressBar.progress = 0
                if (legacyInterface == null) {
                    dappViewModel.fireEvent(DappBrowserJSInterfaceV2.EVENT.READY)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
        }
    }

    /*
    fun verifyPassCodeAndSign(message: DappMessage? = null) {
        val mKeyguardManager = webView.context!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (message != null) {
                pendingDappMessage = message
            }
            if (intent != null) {
                (webView.context as Activity).startActivityForResult(intent, 1)
            }
        }
    }*/

    //view model listeners
    fun listenForDappResponses() {
        dappViewModel.getDappResponse()?.observe(this, Observer { response ->
            val mainHandler = Handler(O3Wallet.appContext!!.mainLooper)
            val myRunnable = Runnable {
                val script = "_o3dapi.receiveMessage(" + response.toString() + ")"
                webView.evaluateJavascript(script) { value ->
                    Log.d("javascript", value)
                }
            }
            mainHandler.post(myRunnable)
        })
    }

    fun listenForAuthorizeWalletInfo() {
        dappViewModel.getWalletInfo().observe(this, Observer { message ->
            runOnUiThread{
                val bottomSheet = DappConnectionRequestBottomSheet()
                bottomSheet.dappMessage = message
                val bundle = Bundle()
                bundle.putString("url", webView.url)
                bottomSheet.arguments = bundle
                bottomSheet.show((this.activity as DappContainerActivity).supportFragmentManager, bottomSheet.tag)
            }
        })

    }

    fun listenForAuthorizeSend() {
        dappViewModel.getSendRequest().observe(this, Observer { message ->
            runOnUiThread {
                val bottomSheet = DappRequestSendBottomSheet()
                bottomSheet.dappMessage = message
                val bundle = Bundle()
                bundle.putString("send_request", Gson().toJson(Gson().fromJson<NeoDappProtocol.SendRequest>(Gson().toJson(message.data))))
                bundle.putString("url", webView.url)
                bottomSheet.arguments = bundle
                bottomSheet.show((this.activity as DappContainerActivity).supportFragmentManager, bottomSheet.tag)
            }
        })
    }

    fun listenForAuthorizeInvoke() {
        dappViewModel.getInvokeRequest().observe(this, Observer { message ->
            runOnUiThread {
                val bottomSheet = DappBrowserContractRequestBottomSheet.newInstance()
                bottomSheet.dappMessage = message
                val bundle = Bundle()
                bundle.putString("url", webView.url)
                bottomSheet.arguments = bundle
                bottomSheet.show((this.activity as DappContainerActivity).supportFragmentManager, bottomSheet.tag)
            }
        })
    }

    fun listenForUnlockStatus() {
        dappViewModel.getLockStatus().observe(this, Observer { isLocked ->
            runOnUiThread {
                var walletStatusView = activity!!.find<ImageView>(R.id.walletStatusImageView)

                if (isLocked == false) {
                    walletStatusView.image = resources.getDrawable(R.drawable.ic_dapp_wallet_active)
                    walletStatusView.onClick {
                        val customPowerMenu = CustomPowerMenu.Builder(this@DAPPBrowser.activity, DappPopupMenuAdapter())
                                .setWidth(800)
                                .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
                                .setMenuRadius(10f)
                                .setMenuShadow(10f)
                                .build()

                        var headerView = layoutInflater.inflate(R.layout.dapp_popup_header, null, false)
                        headerView.find<TextView>(R.id.walletAddressTitle).text = dappViewModel.walletForSession?.address
                        headerView.find<TextView>(R.id.walletNameTitle).text = dappViewModel.walletForSessionName
                        headerView.find<Button>(R.id.swapButton).onClick {
                            val swapWalletSheet = DappWalletForSessionBottomSheet.newInstance()
                            swapWalletSheet.needsAuth = false
                            swapWalletSheet.show((this@DAPPBrowser.activity as DappContainerActivity).supportFragmentManager, swapWalletSheet.tag)
                            customPowerMenu.dismiss()
                        }
                        customPowerMenu.setHeaderView(headerView)
                        customPowerMenu.showAsDropDown(walletStatusView)
                    }
                } else {
                    walletStatusView.image = resources.getDrawable(R.drawable.ic_walletitem)
                    walletStatusView.onClick {  }

                }
            }
        })
    }

    fun showDappBrowserWarning() {
        if (PersistentStore.getHasAgreedDappDisclaimer() == false) {
            val warningBottomSheet = DappDisclaimerBottomSheet.newInstance()
            warningBottomSheet.show((this@DAPPBrowser.activity as DappContainerActivity).supportFragmentManager, warningBottomSheet.tag)
        }
    }
}

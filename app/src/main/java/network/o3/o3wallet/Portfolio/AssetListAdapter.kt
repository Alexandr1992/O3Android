package network.o3.o3wallet.Portfolio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.glxn.qrgen.android.QRCode
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.Portfolio
import network.o3.o3wallet.API.O3Platform.TransferableAsset
import network.o3.o3wallet.Dapp.DappContainerActivity
import network.o3.o3wallet.MultiWallet.ManageMultiWallet.MultiwalletManageWallet
import network.o3.o3wallet.R.*
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat

/**
 * Created by drei on 12/15/17.
 */

class AssetListAdapter(context: Context, fragment: HomeFragment): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    data class TableCellData(var assetName: String, var assetSymbol: String, var assetAmount: Double,
                             var assetPrice: Double, var totalValue: Double, var percentChange: Double)

    private val mContext: Context
    private val mfragment: HomeFragment
    var assets = ArrayList<TransferableAsset>()
    var portfolio: Portfolio? = null
    var referenceCurrency: CurrencyType = CurrencyType.FIAT
    init {
        mContext = context
        mfragment = fragment
    }
    companion object {
        val ASSETROW = 0
        val NOTIFICATIONROW = 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0 && !PersistentStore.getHasDismissedBackup()) {
            return NOTIFICATIONROW
        } else {
            return ASSETROW
        }
    }

    override fun getItemCount(): Int {
        if (PersistentStore.getHasDismissedBackup()) {
            return assets.count()
        } else {
            return assets.count() + 1
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        if (viewType == NOTIFICATIONROW) {
            val view = layoutInflater.inflate(layout.portfolio_notification_row, viewGroup, false)
            return NotificationViewHolder(view, mfragment, this)
        } else {
            val view = layoutInflater.inflate(layout.portfolio_asset_card, viewGroup, false)
            return PortfolioAssetViewHolder(view)
        }
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        if (position == 0 && !PersistentStore.getHasDismissedBackup()) {
            (vh as NotificationViewHolder).bindNotification(this)
            return
        } else if (PersistentStore.getHasDismissedBackup()) {
            (vh as PortfolioAssetViewHolder).bindPortfolioAsset(assets[position], portfolio, referenceCurrency)
            return
        } else {
            (vh as PortfolioAssetViewHolder).bindPortfolioAsset(assets[position - 1], portfolio, referenceCurrency)
            return
        }
    }

    class PortfolioAssetViewHolder(v: View): RecyclerView.ViewHolder(v) {
        data class TableCellData(var assetName: String, var assetSymbol: String, var assetAmount: Double,
                                 var assetPrice: Double, var totalValue: Double, var percentChange: Double)

        val view = v
        val assetNameView = view.findViewById<TextView>(id.assetNameTextView)
        val assetPriceView = view.findViewById<TextView>(id.assetPriceTextView)
        val assetAmountView = view.findViewById<TextView>(id.assetAmountTextView)
        val assetTotalValueView = view.findViewById<TextView>(id.totalValueTextView)
        val assetPercentChangeView = view.findViewById<TextView>(id.percentChangeTextView)
        val logoView = view.find<ImageView>(id.portfolioAssetLogoView)

        private var asset: TransferableAsset? = null

        fun getItem(asset: TransferableAsset, portfolio: Portfolio?, referenceCurrency: CurrencyType): TableCellData {
            var assetData = TableCellData("", "", 0.0, 0.0, 0.0, 0.0)
            assetData.assetName = asset.symbol
            assetData.assetAmount = asset.value.toDouble()
            assetData.assetSymbol = asset.symbol
            //TODO: HARDCODED FOR ONTOLOGY, FIND IMPROVED WAY TO DO THIS SOON
            if (asset.id.contains("000000000000000")) {
                assetData.assetName = asset.symbol + " (M)"
            }

            if (portfolio != null) {
                if (referenceCurrency == CurrencyType.FIAT) {
                    val latestPrice = portfolio.price[asset.symbol]?.averageUSD ?: 0.0
                    val firstPrice = portfolio.firstPrice[asset.symbol]?.averageUSD ?: 0.0
                    assetData.assetPrice = latestPrice
                    assetData.totalValue = latestPrice * assetData.assetAmount
                    if (firstPrice == 0.0 ) {
                        assetData.percentChange = 0.0
                    } else {
                        assetData.percentChange = (latestPrice - firstPrice) / firstPrice * 100
                    }

                } else {
                    val latestPrice = portfolio.price[asset.symbol]?.averageBTC ?: 0.0
                    val firstPrice = portfolio.firstPrice[asset.symbol]?.averageBTC ?: 0.0
                    assetData.assetPrice = latestPrice
                    assetData.totalValue = latestPrice * assetData.assetAmount
                    if (firstPrice == 0.0 ) {
                        assetData.percentChange = 0.0
                    } else {
                        assetData.percentChange = (latestPrice - firstPrice) / firstPrice * 100
                    }
                }
            }

            return assetData
        }


        fun bindPortfolioAsset(asset: TransferableAsset, portfolio: Portfolio?, referenceCurrency: CurrencyType) {

            val tableCellData = getItem(asset, portfolio, referenceCurrency)
            assetNameView.text = tableCellData.assetName
            assetPriceView.text = tableCellData.assetPrice.formattedCurrencyString(referenceCurrency)
            assetTotalValueView.text = tableCellData.totalValue.formattedCurrencyString(referenceCurrency)
            assetPercentChangeView.text = tableCellData.percentChange.formattedPercentString()
            val imageURL = String.format("https://cdn.o3.network/img/neo/%s.png", tableCellData.assetSymbol)
            Glide.with(view.context).load(imageURL).into(logoView)

            assetPercentChangeView.visibility = View.VISIBLE
            assetPriceView.visibility = View.VISIBLE
            assetTotalValueView.visibility = View.VISIBLE
            view.find<TextView>(id.pricingNotAvailableTextView).visibility = View.GONE

            view.setOnClickListener {
                var detailURL = "https://public.o3.network/neo/assets/" + tableCellData.assetSymbol + "?address=" + Account.getWallet().address + "&theme=" + PersistentStore.getTheme().toLowerCase()
                if (asset.id.contains("00000000000")) {
                    detailURL = "https://public.o3.network/ont/assets/" + tableCellData.assetSymbol + "?address=" + Account.getWallet().address + "&theme=" + PersistentStore.getTheme().toLowerCase()
                }

                val tokenDetailsAttrs = mapOf("asset" to asset.symbol, "source" to "portfolio_row")
                AnalyticsService.Navigation.logTokenDetailsSelected(JSONObject(tokenDetailsAttrs))
                val intent = Intent(view.context, DappContainerActivity::class.java)
                intent.putExtra("url", detailURL)
                view.context.startActivity(intent)
            }


            if (tableCellData.percentChange == 0.0) {
                assetPercentChangeView.setTextColor(view.context.getColorFromAttr(R.attr.defaultSubtitleTextColor))
            } else if (tableCellData.percentChange < 0) {
                assetPercentChangeView.setTextColor(ContextCompat.getColor(view.context, color.colorLoss))
            } else {
                assetPercentChangeView.setTextColor(ContextCompat.getColor(view.context, color.colorGain))
            }

            var formatter = NumberFormat.getNumberInstance()
            formatter.maximumFractionDigits = asset.decimals
            assetAmountView.text = formatter.format(tableCellData.assetAmount)
        }
    }

    class NotificationViewHolder(v: View, fragment: HomeFragment, adapter: AssetListAdapter): RecyclerView.ViewHolder(v) {
        val view = v
        val mFragment = fragment
        val mAdapter = adapter

        fun backupAction() {
            val key = NEP6.getFromFileSystem().getDefaultAccount().key
            val tmpDir = File(view.context?.filesDir?.absolutePath + "/tmp")
            tmpDir.mkdirs()
            val fileImage = File(tmpDir, "o3wallet.png")
            val fileJson = NEP6.getFromFileSystemAsFile()
            val fout = FileOutputStream(fileImage)
            val bitmap = QRCode.from(key).withSize(2000, 2000).bitmap()
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fout)

            val imageUri = FileProvider.getUriForFile(view.context!!, "network.o3.o3wallet", fileImage)
            val contentUri = FileProvider.getUriForFile(view.context!!, "network.o3.o3wallet", fileJson)

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "message/rfc822"
            intent.putExtra(Intent.EXTRA_SUBJECT, "O3 Wallet Encrypted Backup")
            intent.putExtra(Intent.EXTRA_TEXT, String.format(view.context.resources.getString(R.string.ONBOARDING_backup_email_body), key))
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(imageUri, contentUri))

            val imm = view.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            mFragment.startActivityForResult(Intent.createChooser(intent, "Send Email using:"), 101)

            fout.flush()
            fout.close()
            tmpDir.delete()
        }

        fun manualBackupAction() {
            val nep6Account = NEP6.getFromFileSystem().getDefaultAccount()
            val intent = Intent(mFragment.context, MultiwalletManageWallet::class.java)
            intent.putExtra("address", nep6Account.address)
            intent.putExtra("key", nep6Account.key ?: "")
            intent.putExtra("name", nep6Account.label)
            intent.putExtra("isDefault", nep6Account.isDefault)
            intent.putExtra("shouldNavToManual", true)
            mFragment.context?.startActivity(intent, null)
        }

        fun bindNotification(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
            val notificationTitleView = view.find<TextView>(R.id.notificationTitleView)
            val notificationDescriptionView = view.find<TextView>(R.id.notificationDescriptionView)

            val primaryActionButton = view.find<Button>(R.id.notificationActionButton)
            primaryActionButton.onClick {
                if (PersistentStore.getHasInitiatedBackup() == false) {
                    PersistentStore.setHasInitiatedBackup(true)
                    backupAction()
                } else {
                   manualBackupAction()
                }
            }
          
            val secondaryActionButton = view.find<Button>(R.id.notificationSecondaryActionButton)
            secondaryActionButton.text = view.context.resources.getString(R.string.BACKUP_dismiss)
            if (!PersistentStore.getHasInitiatedBackup()) {
                primaryActionButton.text = view.context.resources.getString(R.string.MULTIWALLET_backup)
                secondaryActionButton.visibility = View.GONE
                notificationTitleView.text = view.context.resources.getString(R.string.BACKUP_initial_warning_title)
                notificationDescriptionView.text = view.context.resources.getString(R.string.BACKUP_initial_warning_description)
            } else {
                primaryActionButton.text = view.context.resources.getString(R.string.BACKUP_secondary_warning_action)
                secondaryActionButton.visibility = View.VISIBLE
                notificationTitleView.text = view.context.resources.getString(R.string.BACKUP_secondary_warning_title)
                notificationDescriptionView.text = view.context.resources.getString(R.string.BACKUP_secondary_warning_description)
            }

            secondaryActionButton.onClick {
                PersistentStore.setHasDismissedBackup(true)
                mAdapter.notifyDataSetChanged()
            }
        }
    }
}
package network.o3.o3wallet.Portfolio

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.robinhood.spark.SparkView
import android.arch.lifecycle.Observer
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import com.robinhood.spark.animation.MorphSparkAnimator
import network.o3.o3wallet.*
import network.o3.o3wallet.API.O3.PriceHistory

/**
 * Created by drei on 12/8/17.
 */

class AssetGraph : AppCompatActivity() {
    private var selectedButton: Button? = null
    private var symbol: String? = null
    private var assetGraphModel: AssetGraphViewModel? = null
    private var chartDataAdapter = PortfolioDataAdapter(FloatArray(0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.portfolio_activity_asset_graph)
        symbol = intent.getStringExtra("SYMBOL")
        title = resources.getString(R.string.PORTFOLIO_price_history, symbol!!)
        assetGraphModel = ViewModelProviders.of(this).get(AssetGraphViewModel::class.java)

        initiateGraph()
        initiateIntervalButtons()
        initiatePriceView()
    }

    fun setHeaderToInitialValues() {
        val priceView = findViewById<TextView>(R.id.currentPriceTextView)
        val percentView = findViewById<TextView>(R.id.percentChangeTextView)
        priceView.text = assetGraphModel?.getLatestPriceFormattedString()
        val percentChange = assetGraphModel?.getPercentChange()
        if (percentChange!! < 0) {
            percentView?.setTextColor(resources.getColor(R.color.colorLoss))
        } else {
            percentView?.setTextColor(resources.getColor(R.color.colorGain))
        }
        percentView.text = percentChange.formattedPercentString() +
                " " +  assetGraphModel?.getInitialDate()?.intervaledString(assetGraphModel?.getInterval() ?: "24H")
    }

    private fun initiateGraph() {
        val sparkView = findViewById<SparkView>(R.id.sparkview)
        sparkView.sparkAnimator = MorphSparkAnimator()
        sparkView.adapter = chartDataAdapter
        loadGraph(true)

        sparkView.scrubListener = SparkView.OnScrubListener { value ->
            if (value == null) { //return to original state
                setHeaderToInitialValues()
                return@OnScrubListener
            } else {
                val priceView = findViewById<TextView>(R.id.currentPriceTextView)
                val percentView = findViewById<TextView>(R.id.percentChangeTextView)
                val price = (value as Float).toDouble()
                val percentChange = ((price - assetGraphModel?.getInitialPrice()!!) /
                        assetGraphModel?.getInitialPrice()!! * 100)

                if (percentChange < 0) {
                    percentView?.setTextColor(resources.getColor(R.color.colorLoss))
                } else {
                    percentView?.setTextColor(resources.getColor(R.color.colorGain))
                }
                priceView.text = price.formattedCurrencyString(assetGraphModel!!.getCurrency())
                percentView.text = percentChange.formattedPercentString() +
                        " " +  assetGraphModel?.getInitialDate()?.intervaledString(assetGraphModel?.getInterval() ?: "24H")
            }
        }
    }

    private fun initiatePriceView() {
        val priceView = findViewById<TextView>(R.id.currentPriceTextView)
        priceView.setOnClickListener {
            if (assetGraphModel?.getCurrency() == CurrencyType.FIAT) {
                assetGraphModel?.setCurrency(CurrencyType.BTC)
            } else {
                assetGraphModel?.setCurrency(CurrencyType.FIAT)
            }
            loadGraph(true)
        }
    }

    private fun initiateIntervalButtons() {
        val sixHourButton = findViewById<Button>(R.id.sixHourInterval)
        val oneDayButton = findViewById<Button>(R.id.oneDayInterval)
        val oneWeekButton = findViewById<Button>(R.id.oneWeekInterval)
        val oneMonthButton = findViewById<Button>(R.id.oneMonthInterval)
        val threeMonthButton = findViewById<Button>(R.id.threeMonthInterval)
        val allButton = findViewById<Button>(R.id.allInterval)

        selectedButton = oneDayButton

        sixHourButton.setOnClickListener { tappedIntervalButton(sixHourButton) }
        oneDayButton.setOnClickListener { tappedIntervalButton(oneDayButton) }
        oneWeekButton.setOnClickListener { tappedIntervalButton(oneWeekButton) }
        oneMonthButton.setOnClickListener { tappedIntervalButton(oneMonthButton) }
        threeMonthButton.setOnClickListener { tappedIntervalButton(threeMonthButton) }
        allButton.setOnClickListener { tappedIntervalButton(allButton) }
    }

    private fun loadGraph(refresh: Boolean) {
        val priceView = findViewById<TextView>(R.id.currentPriceTextView)
        val percentView = findViewById<TextView>(R.id.percentChangeTextView)
        val progress = findViewById<ProgressBar>(R.id.progressBar)
        progress?.visibility = View.VISIBLE
        assetGraphModel?.getHistoryFromModel(symbol!!, refresh)?.observe(this, Observer<PriceHistory> { data ->
            progress?.visibility = View.GONE
            chartDataAdapter.setData(assetGraphModel?.getPriceFloats())
            priceView.text = assetGraphModel?.getLatestPriceFormattedString()

            val percentChange = assetGraphModel?.getPercentChange()
            if (percentChange!! < 0) {
                percentView?.setTextColor(ContextCompat.getColor(applicationContext,R.color.colorLoss))
            } else {
                percentView?.setTextColor(ContextCompat.getColor(applicationContext,R.color.colorGain))
            }
            percentView.text = percentChange.formattedPercentString() +
                    " " +  assetGraphModel?.getInitialDate()?.intervaledString(assetGraphModel?.getInterval() ?: "24H")
        })
    }

    fun tappedIntervalButton(button: Button) {
        selectedButton?.setTextAppearance(R.style.IntervalButtonText_NotSelected)
        button.setTextAppearance(R.style.IntervalButtonText_Selected)
        selectedButton = button
        assetGraphModel?.setInterval(button.tag.toString())
        loadGraph(true)
    }
}

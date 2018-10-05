package network.o3.o3wallet.NativeTrade

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import network.o3.o3wallet.API.Switcheo.SwitcheoOrders
import network.o3.o3wallet.API.Switcheo.calculatePercentFilled
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class OrderResultDialog() : DialogFragment() {


    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var animationView: LottieAnimationView
    private lateinit var finishButton: Button
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.dialog_animated_fragment, container, false)

        animationView = view.find(R.id.animatedDialogAnimationView)
        titleView = view.find(R.id.animatedDialogTitle)
        subtitleView = view.find(R.id.animatedDialogSubtitle)
        finishButton = view.find(R.id.animatedDialogConfirmButton)

        subtitleView.visibility = View.INVISIBLE
        subtitleView.text = "placeholder text"
        finishButton.isEnabled = false
        titleView.text = resources.getString(R.string.NATIVE_TRADE_order_in_progress)

        view.find<Button>(R.id.animatedDialogConfirmButton).setOnClickListener {
            dismiss()
        }

        animationView.setAnimation(R.raw.loader_portfolio)
        animationView.playAnimation()
        return view
    }

    fun showSuccess(order: SwitcheoOrders) {
        val percentFilled = order.calculatePercentFilled()
        if (percentFilled.first.toInt() == 0) {
            subtitleView.text = resources.getString(R.string.NATIVE_TRADE_order_success_subtitle_no_fill)
        } else if (percentFilled.first.toInt() == 100) {
            subtitleView.text = resources.getString(R.string.NATIVE_TRADE_order_success_subtitle_all_fill)
        } else {
            subtitleView.text = String.format(resources.getString(R.string.NATIVE_TRADE_order_success_subtitle_partial_fill), percentFilled.first.toInt().toString())
        }
        titleView.text = resources.getString(R.string.NATIVE_TRADE_order_success_title)
        subtitleView.visibility = View.VISIBLE
        animationView.setAnimation(R.raw.claim_success)
        animationView.playAnimation()
        finishButton.isEnabled = true
    }

    fun showFailure() {
        titleView.text = resources.getString(R.string.NATIVE_TRADE_order_fail_title)
        subtitleView.text = resources.getString(R.string.NATIVE_TRADE_order_failure_subtitle)
        subtitleView.visibility = View.VISIBLE
        animationView.setAnimation(R.raw.task_failed)
        animationView.playAnimation()
        finishButton.isEnabled = true
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        activity?.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        if (PersistentStore.getTheme() == "Dark") {
            setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog_Dark)
        } else {
            setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog_Light)
        }
    }

    companion object {
        fun newInstance(): OrderResultDialog {
            return OrderResultDialog()
        }
    }
}
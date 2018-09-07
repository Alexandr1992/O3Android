package network.o3.o3wallet.NativeTrade.OrdersList


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import network.o3.o3wallet.API.Switcheo.SwitcheoAPI

import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import org.jetbrains.anko.find

class OrderUpdateOptionsBottomSheet : RoundedBottomSheetDialogFragment() {

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.native_trade_order_update_bottom_sheet, null)
        dialog.setContentView(contentView)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val id = arguments!!.getString("id")
        val view = inflater.inflate(R.layout.native_trade_order_update_bottom_sheet, container, false)
        view.find<Button>(R.id.cancelOrderButton).setOnClickListener {
            SwitcheoAPI().singleStepCancel(id) {
                print(it.first)
            }
        }
        return view
    }

    companion object {
        fun newInstance(): OrderUpdateOptionsBottomSheet {
            return OrderUpdateOptionsBottomSheet()
        }
    }
}

package network.o3.o3wallet

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class DappRequestSendBottomSheet : Fragment() {

    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.dapp_send_request_bottom_sheet, container, false)
        return mView
    }

    companion object {
        fun newInstance(): DappRequestSendBottomSheet {
            return DappRequestSendBottomSheet()
        }
    }
}

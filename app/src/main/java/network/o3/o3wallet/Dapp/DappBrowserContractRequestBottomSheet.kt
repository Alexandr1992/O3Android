package network.o3.o3wallet.Dapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment

class DappBrowserContractRequestBottomSheet: RoundedBottomSheetDialogFragment() {

    lateinit var mView: View
    lateinit var dappMessage: DappMessage

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.dapp_contract_request_bottom_sheet, container, false)
        return mView
    }

    companion object {
        fun newInstance(): DappBrowserContractRequestBottomSheet {
           return DappBrowserContractRequestBottomSheet()
        }
    }
}
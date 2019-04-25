package network.o3.o3wallet.Inbox


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment

class InboxOptInBottomSheet : RoundedBottomSheetDialogFragment() {

    lateinit var mView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.inbox_opt_in_bottom_sheet_fragment, container, false)
        return mView
    }


    companion object {
        fun newInstance(): InboxOptInBottomSheet {
            return InboxOptInBottomSheet()
        }
    }
}

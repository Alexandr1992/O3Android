package network.o3.o3wallet.Identity


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import network.o3.o3wallet.API.O3Platform.ReverseLookupNNS

import network.o3.o3wallet.R
import network.o3.o3wallet.RoundedBottomSheetDialogFragment
import org.jetbrains.anko.find

// TODO: Rename parameter arguments, choose names that match
class NNSBottomSheet : RoundedBottomSheetDialogFragment() {
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.identity_nnsbottom_sheet, container, false)
        val domainsJson = arguments!!.getString("domains")
        val domains = Gson().fromJson<List<ReverseLookupNNS>>(domainsJson)
        val adapter = NNSAdapter(context!!, domains)
        val listView = mView.find<ListView>(R.id.nnsListView)
        listView.adapter = adapter
        return mView
    }

    companion object {
        fun newInstance(): NNSBottomSheet {
            return NNSBottomSheet()
        }
    }
}

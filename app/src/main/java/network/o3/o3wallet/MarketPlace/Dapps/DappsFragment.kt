package network.o3.o3wallet.MarketPlace.Dapps


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.MarketPlace.NEP5Tokens.TokensFragment

import network.o3.o3wallet.R
import org.jetbrains.anko.Orientation
import org.jetbrains.anko.find

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class DappsFragment : Fragment() {

    private lateinit var mView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.marketplace_dapps_fragment, container, false)
        val dappsRecycler = mView.find<RecyclerView>(R.id.dappsRecyclerView)
        dappsRecycler.layoutManager = LinearLayoutManager(context)
        (dappsRecycler.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.VERTICAL
        dappsRecycler.adapter = DappsAdapter(arrayListOf())
        return mView
    }

    companion object {
        fun newInstance(): DappsFragment {
            return DappsFragment()
        }
    }
}

package network.o3.o3wallet.MarketPlace.Dapps


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import network.o3.o3wallet.API.O3Platform.O3PlatformClient
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.onUiThread

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
        (dappsRecycler.layoutManager as LinearLayoutManager).orientation = RecyclerView.VERTICAL
        O3PlatformClient().getDapps {
            onUiThread {
                dappsRecycler.adapter = DappsAdapter(it.first!!)
            }
        }
        return mView
    }

    companion object {
        fun newInstance(): DappsFragment {
            return DappsFragment()
        }
    }
}

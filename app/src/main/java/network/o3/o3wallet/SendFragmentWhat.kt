package network.o3.o3wallet


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController
import org.jetbrains.anko.find

class SendFragmentWhat : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.send_what_fragment, container, false)
        view.find<Button>(R.id.sendWhereButton).setOnClickListener {
            view.findNavController().navigate(R.id.action_sendFragmentWhat_to_sendWhereFragment)
        }
        return view
    }


}

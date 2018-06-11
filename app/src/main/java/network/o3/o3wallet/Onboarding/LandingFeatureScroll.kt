package network.o3.o3wallet.Onboarding

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.R
import org.jetbrains.anko.find


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LandingFeatureScroll.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LandingFeatureScroll.newInstance] factory method to
 * create an instance of this fragment.
 */

class LandingFeatureScroll: Fragment() {
    var titles = O3Wallet.appContext!!.resources.getStringArray(R.array.ONBOARDING_landing_titles)
    var subtitles = O3Wallet.appContext!!.resources.getStringArray(R.array.ONBOARDING_landing_subtitles)

    companion object {
        fun newInstance(position: Int): LandingFeatureScroll {
            val args = Bundle()
            args.putInt("position", position)
            val fragment = LandingFeatureScroll()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.onboarding_fragment_landing_feature_scroll, container, false)
        val position = arguments!!.getInt("position")
        val featureSubtitleTextView = view.findViewById<TextView>(R.id.featureSubtitle)
        val featureTitleTextView = view.find<TextView>(R.id.fetaureTitle)

        featureSubtitleTextView.text = subtitles[position]
        featureTitleTextView.text = titles[position]
        return view
    }
}
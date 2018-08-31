package network.o3.o3wallet.Onboarding.CreateKey


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import network.o3.o3wallet.O3Wallet
import network.o3.o3wallet.Onboarding.LandingFeatureScroll
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor

class TutorialCard : Fragment() {

    var titles = O3Wallet.appContext!!.resources.getStringArray(R.array.ONBOARDING_tutorial_titles)
    var infos = O3Wallet.appContext!!.resources.getStringArray(R.array.ONBOARDING_tutorial_infos)
    var subinfos = O3Wallet.appContext!!.resources.getStringArray(R.array.ONBOARDING_tutorial_sub_infos)
    var emphasises = O3Wallet.appContext!!.resources.getStringArray(R.array.ONBOARDING_tutorial_emphasis_texts)

    val MAX_POSITION = 5

    companion object {
        fun newInstance(position: Int): TutorialCard {
            val args = Bundle()
            args.putInt("position", position)
            val fragment = TutorialCard()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.onboarding_tutorial_card_fragment, container, false)
        val position = arguments!!.getInt("position") - 1

        view.find<TextView>(R.id.tutorialTitleTextView).text = titles[position]
        view.find<TextView>(R.id.tutorialInfoTextView).text = infos[position]
        view.find<TextView>(R.id.tutorialSubInfoTextView).text = subinfos[position]
        view.find<TextView>(R.id.emphasisTextView).text = emphasises[position]


        view.find<Button>(R.id.tutorialBackButton).setOnClickListener {
            (activity as CreateNewWalletActivity).progressTutorialBackward()
        }

        if (position == MAX_POSITION - 1) {
            view.findViewById<Button>(R.id.tutorialForwardButton).text = activity?.getString(R.string.ONBOARDING_done_action)
            view.find<TextView>(R.id.emphasisTextView).textColor = context!!.getColor(R.color.colorGain)
        }

        view.find<Button>(R.id.tutorialForwardButton).setOnClickListener {
            (activity as CreateNewWalletActivity).progressTutorialForward()
        }


        return view
    }


}

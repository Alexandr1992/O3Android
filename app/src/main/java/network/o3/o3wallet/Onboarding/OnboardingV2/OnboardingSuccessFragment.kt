package network.o3.o3wallet.Onboarding.OnboardingV2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import network.o3.o3wallet.Onboarding.SelectingBestNode
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

class OnboardingSuccessFragment: Fragment() {
    lateinit var mView: View
    lateinit var finishButton: Button
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.onboarding_success_fragment, null)
        finishButton = mView.find(R.id.finishButton)

        finishButton.onClick {
            val intent = Intent(activity, SelectingBestNode::class.java)
            startActivity(intent)
        }
        return mView
    }
}
package network.o3.o3wallet.Onboarding.OnboardingV2

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import com.airbnb.lottie.LottieAnimationView
import network.o3.o3wallet.Account
import network.o3.o3wallet.NEP6
import network.o3.o3wallet.Onboarding.LandingPagerAdapter
import network.o3.o3wallet.Onboarding.LoginNEP6.LoginNEP6Activity
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.yesButton

class LandingFragment: Fragment() {
    private lateinit var pager: ViewPager
    private lateinit var nextButton: Button
    private lateinit var animationView: LottieAnimationView
    private lateinit var mView: View
    private var deepLink: String? = null


    val maxPages = 5
    val minPages = 0
    var currentPage = 0
    var userDidInteract = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mView = inflater.inflate(R.layout.onboarding_landing_fragment, container, false)
        animationView = mView.find(R.id.landing_animation_view)
        animationView.useHardwareAcceleration(true)
        pager = mView.find(R.id.landingViewPager)
        initiateViewPager()


        mView.find<Button>(R.id.loginButton).setOnClickListener {
            loginTapped()
        }

        mView.find<Button>(R.id.createNewWalletButton).setOnClickListener  {
            findNavController().navigate(R.id.action_landingFragment_to_onboardingNewWalletFragment)
        }

        if (Account.isEncryptedWalletPresent() || Account.isDefaultEncryptedNEP6PassPresent()) {
            authenticateEncryptedWallet()
        } else {
            autoPlayAnimation()
        }
        return mView
    }

    fun initiateViewPager() {
        pager.adapter = LandingPagerAdapter(activity!!.supportFragmentManager)
        pager.setOnClickListener {
            pager.setCurrentItem(currentPage + 1, true)
        }

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    userDidInteract = true
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (currentPage < position) {
                    if (userDidInteract) {
                        animateForward()
                        currentPage += 1
                        setDotColor(currentPage)
                    }
                } else if (currentPage > position){
                    if(userDidInteract) {
                        animateBackward()
                        currentPage -= 1
                        setDotColor(currentPage)
                    }
                }
            }
            override fun onPageSelected(position: Int) { }

        })
    }

    fun animateForward() {
        animationView.speed = 1.3f
        animationView.setMinAndMaxProgress(currentPage.toFloat() / (maxPages.toFloat() - 1),
                (currentPage + 1).toFloat() / (maxPages.toFloat() - 1) )
        animationView.playAnimation()

        if (!userDidInteract) {
            currentPage += 1
            setDotColor(currentPage)
            pager.setCurrentItem(currentPage, true)
        }
    }

    fun animateBackward () {
        animationView.speed = -1.3f
        //animationView.setMinAndMaxProgress(0.0f, 0.2f)
        animationView.setMinAndMaxProgress((currentPage.toFloat() - 1) / (maxPages.toFloat() - 1),
                (currentPage).toFloat() / (maxPages.toFloat() - 1) )
        animationView.playAnimation()
    }


    fun autoPlayAnimation() {
        val handler = Handler()
        val delay: Long = 5000 //milliseconds

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!userDidInteract && currentPage != maxPages - 1) {
                    animateForward()
                    handler.postDelayed(this, delay)
                } else {
                    return
                }
            }
        }, delay)
    }


    fun setDotColor(currentPosition: Int) {
        val dotOne = mView.find<ImageView>(R.id.landingDotOne)
        val dotTwo = mView.find<ImageView>(R.id.landingDotTwo)
        val dotThree = mView.find<ImageView>(R.id.landingDotThree)
        val dotFour = mView.find<ImageView>(R.id.landingDotFour)
        val dotFive = mView.find<ImageView>(R.id.landingDotFive)

        val dotArray = arrayOf(dotOne, dotTwo, dotThree, dotFour, dotFive)
        for (dot in dotArray) {
            dot.image = activity?.getDrawable(R.drawable.ic_inactive_dot)
        }
        dotArray[currentPosition].image = activity?.getDrawable(R.drawable.ic_active_dot)
    }

    fun authenticateEncryptedWallet() {
        val mKeyguardManager =  activity!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.

            toast(R.string.ALERT_no_passcode_setup)
            return
        } else {
            val intent = Intent(activity, LoginNEP6Activity::class.java)
            startActivity(intent)
        }
    }



    fun loginTapped() {
        val mKeyguardManager =  activity!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            toast(resources.getString(R.string.ALERT_no_passcode_setup))
            return
        }
        if (NEP6.getFromFileSystem().accounts.isNotEmpty()) {
            alert(resources.getString(R.string.ONBOARDING_remove_wallet)) {
                yesButton {
                    findNavController().navigate(R.id.action_landingFragment_to_restoreExistingWalletFragment)
                }
                noButton {}
            }.show()
        } else {
            findNavController().navigate(R.id.action_landingFragment_to_restoreExistingWalletFragment)
        }
    }
}
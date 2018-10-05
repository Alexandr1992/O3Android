package network.o3.o3wallet.Onboarding.CreateKey

import android.graphics.Paint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import kotlinx.android.synthetic.main.onboarding_create_new_wallet_activity.*
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import android.text.style.UnderlineSpan
import android.text.SpannableString
import android.graphics.Paint.UNDERLINE_TEXT_FLAG

class CreateNewWalletActivity : AppCompatActivity() {
    lateinit var animationView: LottieAnimationView
    lateinit var tutorialPager: ViewPager

    lateinit var createWalletTitle: TextView
    lateinit var createWalletInfo: TextView
    lateinit var createWalletSubInfo: TextView
    lateinit var learnMoreButton: Button


    var currentPage = 0
    var wif = ""
    val MAX_POSITION = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_create_new_wallet_activity)
        wif = intent.getStringExtra("wif")

        supportActionBar?.hide()
        animationView = find(R.id.learnMoreAnimation)
        animationView.useHardwareAcceleration(true)

        createWalletTitle = find(R.id.createWalletTitle)
        createWalletInfo = find(R.id.createWalletInfo)
        createWalletSubInfo = find(R.id.createWalletSubInfo)
        learnMoreButton = find(R.id.learnMoreButton)

        learnMoreButton.setOnClickListener {
            progressTutorialForward()
        }

        learnMoreButton.paintFlags = learnMoreButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG


        initiateViewPager()
        val walletGeneratedFragment = DialogWalletGeneratedFragment.newInstance()
        walletGeneratedFragment.showNow(this.supportFragmentManager, "walletGenerated")



    }

    fun initiateViewPager() {
        tutorialPager = find<ViewPager>(R.id.tutorialCardPager)
        tutorialPager.adapter = TutorialCardPagerAdapter(supportFragmentManager)

        tutorialPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (currentPage < position) {
                    animateForward()
                    currentPage += 1
                    if (currentPage == 1) {
                        hideElements()
                    }
                } else if (currentPage > position){
                    animateBackward()
                    currentPage -= 1
                    if (currentPage == 0) {
                        showElements()
                    }
                }
            }
            override fun onPageSelected(position: Int) { }
        })
    }

    fun animateForward() {
        animationView.speed = 1.4f
        animationView.setMinAndMaxProgress(currentPage.toFloat() / (MAX_POSITION.toFloat() - 1),
                (currentPage + 1).toFloat() / (MAX_POSITION.toFloat() - 1) )
        animationView.playAnimation()
    }

    fun animateBackward() {
        animationView.speed = -1.4f
        animationView.setMinAndMaxProgress((currentPage.toFloat() - 1) / (MAX_POSITION.toFloat() - 1),
                (currentPage).toFloat() / (MAX_POSITION.toFloat() - 1) )
        animationView.playAnimation()
    }

    fun animateToBeginning() {
        animationView.speed = -20f
        animationView.setMinAndMaxProgress(0.0f, 1.0f)
        animationView.playAnimation()
    }

    fun progressTutorialForward() {
        if (currentPage == MAX_POSITION - 1) {
            tutorialPager.setCurrentItem(0, true)
        } else {
            tutorialPager.setCurrentItem(currentPage + 1, true)
        }
    }

    fun progressTutorialBackward() {
        tutorialPager.setCurrentItem(currentPage - 1, true)
    }


    fun hideElements() {
        createWalletTitle.animate().alpha(0f).duration = 500
        createWalletSubInfo.animate().alpha(0f).duration = 500
        createWalletInfo.animate().alpha(0f).duration = 500
        learnMoreButton.animate().alpha(0f).duration = 500
    }

    fun showElements() {
        createWalletTitle.animate().alpha(1f).duration = 500
        createWalletSubInfo.animate().alpha(1f).duration = 500
        createWalletInfo.animate().alpha(1f).duration = 500
        learnMoreButton.animate().alpha(1f).duration = 500
    }
}

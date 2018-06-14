package network.o3.o3wallet.Onboarding

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.airbnb.lottie.LottieAnimationView
import network.o3.o3wallet.R
import android.support.v4.view.ViewPager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.google.zxing.integration.android.IntentIntegrator
import io.fabric.sdk.android.Fabric
import neoutils.Neoutils
import network.o3.o3wallet.Account
import network.o3.o3wallet.BuildConfig
import network.o3.o3wallet.Onboarding.CreateKey.CreateNewWalletActivity
import network.o3.o3wallet.SelectingBestNode
import org.jetbrains.anko.*

class LandingActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager
    private lateinit var nextButton: Button
    private lateinit var animationView: LottieAnimationView
    val maxPages = 5
    val minPages = 0
    var currentPage = 0
    var userDidInteract = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
        }

        supportActionBar?.hide()
        setContentView(R.layout.onboarding_landing_activity)
        animationView = find(R.id.landing_animation_view)
        animationView.useHardwareAcceleration(true)
        pager = find(R.id.landingViewPager)
        initiateViewPager()


        findViewById<Button>(R.id.loginButton).setOnClickListener {
            loginTapped()
        }

        findViewById<Button>(R.id.createNewWalletButton).setOnClickListener  {
            createWalletTapped()
        }

        autoPlayAnimation()
        if (Account.isEncryptedWalletPresent()) {
            authenticateEncryptedWallet()
        }
    }

    fun initiateViewPager() {
        pager.adapter = LandingPagerAdapter(supportFragmentManager)
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
        val dotOne = find<ImageView>(R.id.landingDotOne)
        val dotTwo = find<ImageView>(R.id.landingDotTwo)
        val dotThree = find<ImageView>(R.id.landingDotThree)
        val dotFour = find<ImageView>(R.id.landingDotFour)
        val dotFive = find<ImageView>(R.id.landingDotFive)

        val dotArray = arrayOf(dotOne, dotTwo, dotThree, dotFour, dotFive)
        for (dot in dotArray) {
            dot.image = getDrawable(R.drawable.ic_inactive_dot)
        }
        dotArray[currentPosition].image = getDrawable(R.drawable.ic_active_dot)
    }

    fun authenticateEncryptedWallet() {
        val mKeyguardManager =  getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.

            Toast.makeText(this,
                    R.string.ALERT_no_passcode_setup,
                    Toast.LENGTH_LONG).show()
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                startActivityForResult(intent, 1)
            }
        }
    }

    fun createWalletTapped() {
        val mKeyguardManager =  getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            Toast.makeText(this, resources.getString(R.string.ALERT_no_passcode_setup), Toast.LENGTH_LONG).show()
            return
        } else {
            val generatedWIF = Neoutils.newWallet().wif
            val intent = Intent(this@LandingActivity, CreateNewWalletActivity::class.java)
            intent.putExtra("wif", generatedWIF)
            startActivity(intent)
        }
    }

    fun authenticateReplaceWallet() {
        val mKeyguardManager =  getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(this, resources.getString(R.string.ALERT_no_passcode_setup), Toast.LENGTH_LONG).show()
            return
        } else {
            val intent = mKeyguardManager.createConfirmDeviceCredentialIntent(resources.getString(R.string.ONBOARDING_Login_To_Existing), null)
            if (intent != null) {
                startActivityForResult(intent, 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents == null) {

        } else {
            if (resultCode == -1) {
                Account.restoreWalletFromDevice()
                val intent = Intent(this, SelectingBestNode::class.java)
                startActivity(intent)
            }
        }
    }

    fun loginTapped() {
        val mKeyguardManager =  getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!mKeyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(this, resources.getString(R.string.ALERT_no_passcode_setup), Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}




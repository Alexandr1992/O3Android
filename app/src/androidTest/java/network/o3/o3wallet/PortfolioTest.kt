package network.o3.o3wallet

import android.support.test.espresso.DataInteraction
import android.support.test.rule.ActivityTestRule
import com.agoda.kakao.*
import org.junit.Rule
import org.junit.Test

open class TestHomeFragment: Screen<TestHomeFragment>() {
    val assetsList  = KListView(builder = { withId(R.id.assetListView)}) {
        itemType(::PortfolioAssetCell)
    }
    class PortfolioAssetCell(i: DataInteraction) : KAdapterItem<PortfolioAssetCell>(i) {
        val assetName = KTextView(i) { withId(R.id.assetNameTextView)}
        val assetAmount = KTextView(i) { withId(R.id.assetAmountTextView)}
    }
}

class TestHomeFragmentWithBalance {
    @Rule
    @JvmField
    val rule = ActivityTestRule(MainTabbedActivity::class.java)

    val account = Account.fromWIF("Kz3dZoCXU8SsmE67GLoGZKaghD1bG1kbePY72LVKpuchMqmRwKer")
    val netType =  PersistentStore.setNetworkType("Private")
    val netURL =  PersistentStore.setNodeURL("https://privatenet.o3.network:30333")
    private val screen = TestHomeFragment()

    @Test
    fun testHasNativeAssetBalance() {
        screen {
            assetsList {
                childAt<TestHomeFragment.PortfolioAssetCell>(0) {
                    assetName.hasText("NEO")
                    assetAmount.hasText("1,000")
                }
            }
            assetsList {
                childAt<TestHomeFragment.PortfolioAssetCell>(1) {
                    assetName.hasText("GAS")
                    assetAmount.hasText("1,000")
                }
            }
        }
    }
}

class TestHomeFragmentWitNoBalance {
    @Rule
    @JvmField
    val rule = ActivityTestRule(MainTabbedActivity::class.java)

    val account = Account.fromWIF("L3dNEe2LNejBxMmcvQkCTZ5XR11RwX7KLbkCqdi9HnrUxV5qk9mt")
    val netType =  PersistentStore.setNetworkType("Private")
    val netURL =  PersistentStore.setNodeURL("https://privatenet.o3.network:30333")
    private val screen = TestHomeFragment()

    @Test
    fun testHasNativeAssetBalance() {
        screen {
            assetsList {
                childAt<TestHomeFragment.PortfolioAssetCell>(0) {
                    assetName.hasText("NEO")
                    assetAmount.hasText("0")
                }
            }
            assetsList {
                childAt<TestHomeFragment.PortfolioAssetCell>(1) {
                    assetName.hasText("GAS")
                    assetAmount.hasText("0")
                }
            }
        }
    }
}


package network.o3.o3wallet

import network.o3.o3wallet.API.Switcheo.SwitcheoAPI
import org.junit.Test
import java.util.concurrent.CountDownLatch

class SwitcheoTest {
    @Test
    fun getTickersTest() {
        val latch = CountDownLatch(1)
        SwitcheoAPI().getDailyTickers {
            for (elem in it.first!!) {
                System.out.println(elem)
            }
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun getOffersTest() {
        val latch = CountDownLatch(1)
        SwitcheoAPI().getOffersForPair("SWTH_NEO") {
            for (elem in it.first!!) {
                System.out.println(elem)
            }
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun submitDepositTest() {
    }
}
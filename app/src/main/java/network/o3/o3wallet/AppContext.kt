package network.o3.o3wallet

import android.app.Application
import android.content.Context


/**
 * Created by drei on 11/29/17.
 */

class O3Wallet : Application() {
    override fun onCreate() {
        super.onCreate()
        O3Wallet.appContext = applicationContext
        O3Wallet.version = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName
    }

    companion object {
        var appContext: Context? = null
            private set
        var version: String? = null
            private set
    }
}
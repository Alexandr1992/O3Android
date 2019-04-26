package network.o3.o3wallet.Inbox

import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import network.o3.o3wallet.PersistentStore
import network.o3.o3wallet.R

class InboxRootActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.inbox_root_activity)
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (PersistentStore.getTheme() == "Dark") {
            theme.applyStyle(R.style.AppTheme_Dark_NoTopBar, true)
        } else {
            theme.applyStyle(R.style.AppTheme_White_NoTopBar, true)
        }
        return theme
    }

    override fun onStop() {
        super.onStop()
        PersistentStore.updateLastInboxOpen()
    }
}

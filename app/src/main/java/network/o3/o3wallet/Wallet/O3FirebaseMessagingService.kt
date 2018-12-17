package network.o3.o3wallet.Wallet

import android.app.*
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import network.o3.o3wallet.R
import android.support.v4.app.NotificationManagerCompat
import android.os.Build
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tapadoo.alerter.Alerter
import network.o3.o3wallet.MainTabbedActivity
import org.jetbrains.anko.activityManager
import android.support.v4.content.LocalBroadcastManager




class O3FirebaseMessagingService: FirebaseMessagingService() {
    private var broadcaster: LocalBroadcastManager? = null

    override fun onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this)
    }

    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        val intent = Intent(this, MainTabbedActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = "Default"
        val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_blue_logo)
                .setStyle(NotificationCompat.BigTextStyle().bigText(p0!!.notification!!.title))
                .setAutoCancel(true).setContentIntent(pendingIntent)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        manager.notify(0, builder.build())

        val alertIntent = Intent("Alert")
        alertIntent.putExtra("alert_title", p0.notification?.title ?: "")
        alertIntent.putExtra("alert_message", p0.notification?.body ?: "")
        broadcaster?.sendBroadcast(alertIntent)
    }

}
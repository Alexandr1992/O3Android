package network.o3.o3wallet

import android.util.Log
//import co.getchannel.channel.Channel
//import co.getchannel.channel.callback.ChannelCallback
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class MyFirebaseInstanceIDService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Log.e("token", refreshedToken)
//        Channel.saveDeviceToken(refreshedToken, object : ChannelCallback {
//            override fun onSuccess() {}
//
//            override fun onFail(message: String) {}
//        })
    }

}
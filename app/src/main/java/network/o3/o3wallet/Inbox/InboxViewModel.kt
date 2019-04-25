package network.o3.o3wallet.Inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import network.o3.o3wallet.API.O3.Message
import network.o3.o3wallet.API.O3.MessageAction
import network.o3.o3wallet.API.O3.MessageChannel

class InboxViewModel: ViewModel() {
    var inboxItems: MutableLiveData<List<Message>>? = null
    var isLoading: MutableLiveData<Boolean>? = null
    var currentPage = 1

    val dummyMessage1 = Message(id="abcxyz", title="O3 Labs",
            timestamp = "1556090601", channel = MessageChannel(service="O3 Labs", topic= "General"),
            action= MessageAction(type="browser", title = "Click this button", url="https://www.o3.network"))
    val dummyMessage2 = Message(id="abcxyz", title="O3 Labs",
            timestamp = "1556090601", channel = MessageChannel(service="O3 Labs", topic= "General"),
            action= MessageAction(type="browser", title = "Click this button", url="https://www.o3.network"))


    fun loadInboxItems(page: Int) {
        isLoading?.postValue(true)
        inboxItems?.postValue(listOf(dummyMessage1, dummyMessage2, dummyMessage1,
                dummyMessage2, dummyMessage1, dummyMessage1))
        isLoading?.postValue(false)
    }

    fun getInboxItems(): LiveData<List<Message>> {
        if (inboxItems == null) {
            inboxItems = MutableLiveData()
            loadInboxItems(page = 1)
        }
        return inboxItems!!
    }

    fun getLoadingStatus(): LiveData<Boolean> {
        if (isLoading == null) {
            isLoading = MutableLiveData()
        }
        return isLoading!!
    }
}
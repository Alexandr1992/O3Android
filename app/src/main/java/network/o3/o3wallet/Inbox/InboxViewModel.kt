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

    val dummyMessage1 = Message(id="abcxyz", title="This is a message from O3 Labs that is something we want to share from the inbox",
            timestamp = "1556090601", channel = MessageChannel(service="O3 Labs", topic= "General"),
            action= MessageAction(type="browser", title = "Check out the new feature", url="https://www.o3.network"))
    val dummyMessage2 = Message(id="abcxyz", title="This is a really really long message that is delivered from O3 Labs." +
            " However the table cells should be able to dynamically resize themselves even if the messages are along so there" +
            " should be no issue. Also this message does have an action associated with it.",
            timestamp = "1556090601", channel = MessageChannel(service="O3 Labs", topic= "General"))


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
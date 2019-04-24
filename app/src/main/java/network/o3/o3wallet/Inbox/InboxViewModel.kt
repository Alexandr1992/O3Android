package network.o3.o3wallet.Inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import network.o3.o3wallet.API.O3.Message

class InboxViewModel: ViewModel() {
    var inboxItems: MutableLiveData<List<Message>>? = null
    var currentPage = 1


    fun loadInboxItems() {
        //todo
    }

    fun loadFirstPage() {

    }

    fun getInboxItems(): LiveData<List<Message>> {
        if (inboxItems == null) {
            inboxItems = MutableLiveData()
            loadInboxItems()
        }
        return return inboxItems!!
    }
}
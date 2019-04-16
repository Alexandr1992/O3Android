package network.o3.o3wallet.Feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import network.o3.o3wallet.API.O3.Feature
import network.o3.o3wallet.API.O3.FeedData
import network.o3.o3wallet.API.O3.O3API

/**
 * Created by drei on 12/21/17.
 */

class NewsFeedViewModel: ViewModel() {
    var feedData: MutableLiveData<FeedData>? = null
    var featureData: MutableLiveData<Array<Feature>>? = null


    fun getFeedData(refresh: Boolean): LiveData<FeedData> {
        if (feedData == null || refresh) {
            feedData = MutableLiveData()
            loadFeedData()
        }
        return feedData!!
    }

    fun getFeatureData(refresh: Boolean): LiveData<Array<Feature>> {
        if (featureData == null || refresh) {
            featureData = MutableLiveData()
            loadFeatureData()
        }
        return featureData!!
    }

    fun loadFeedData() {
        O3API().getNewsFeed {
            if (it.second != null) return@getNewsFeed
            feedData?.postValue(it.first!!)
        }
    }

    fun loadFeatureData() {
        O3API().getFeatures {
            if (it.second != null) return@getFeatures
            featureData?.postValue(it.first!!)
        }
    }
}
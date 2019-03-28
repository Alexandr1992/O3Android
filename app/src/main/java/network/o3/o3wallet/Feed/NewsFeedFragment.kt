package network.o3.o3wallet.Feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import network.o3.o3wallet.R

class NewsFeedFragment : Fragment() {
    var model: NewsFeedViewModel? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        model = NewsFeedViewModel()

        val view =  inflater.inflate(R.layout.news_fragment_news_feed, container, false)
        val listView = view?.findViewById<ListView>(R.id.newsList)
        val featureView = layoutInflater.inflate(R.layout.news_fragment_features, null)
        listView?.adapter = NewsFeedAdapter(context!!, this)

        model?.getFeatureData(true)?.observe(this, Observer {features ->
            val featuredRecycler = featureView.findViewById<RecyclerView>(R.id.featuredList)
            featuredRecycler?.adapter = FeaturesAdapter(features = features?.toCollection(ArrayList())!!)
            model?.getFeedData(true)?.observe(this, Observer { feed ->
                (listView!!.adapter as NewsFeedAdapter).setData(feed!!)
                if (features?.isNotEmpty() == true) {
                    listView.addHeaderView(featureView)
                }
            })
        })

        return view
    }


    companion object {
        fun newInstance(): NewsFeedFragment {
            return NewsFeedFragment()
        }
    }
}

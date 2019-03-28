package network.o3.o3wallet.Feed

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import network.o3.o3wallet.API.O3.FeedData
import network.o3.o3wallet.API.O3.FeedItem
import network.o3.o3wallet.API.O3.NewsImage
import network.o3.o3wallet.Dapp.DAppBrowserActivityV2
import network.o3.o3wallet.R
import java.text.SimpleDateFormat


/**
 * Created by drei on 12/21/17.
 */

class NewsFeedAdapter(context: Context, fragment: NewsFeedFragment): BaseAdapter() {
    var mContext: Context
    private val mFragment: NewsFeedFragment
    private var feedData: FeedData? = null

    init {
        mContext = context
        mFragment = fragment
    }

    fun setData(feed: FeedData) {
        this.feedData = feed
        this.notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): FeedItem {
        return feedData?.items?.get(position) ?:
                FeedItem("","","","","", arrayOf<NewsImage>())
    }

    override fun getCount(): Int {
        return feedData?.items?.size ?: 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val view = layoutInflater.inflate(R.layout.news_feed_row, parent, false)
        val feedItem = getItem(position)
        view.findViewById<TextView>(R.id.titleTextView).text = feedItem.title

        val pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        val date = SimpleDateFormat(pattern).parse(feedItem.published)

        view.findViewById<TextView>(R.id.dateTextView).text = SimpleDateFormat("yyyy-MM-dd").format(date)
        val imageView = view.findViewById<ImageView>(R.id.newImageView)
        Glide.with(mContext).load(feedItem.images[0].url).apply(RequestOptions().centerCrop()).into(imageView)

        view?.setOnClickListener {
            Answers().logContentView(ContentViewEvent()
                    .putContentType("NEO News Today")
                    .putContentId(feedItem.title)
                    .putContentName("NewsFeed Item View"))

            val url = feedItem.link
            val i = Intent(view.context, DAppBrowserActivityV2::class.java)
            i.putExtra("url", url)
            view.context.startActivity(i)
        }
        return view
    }
}


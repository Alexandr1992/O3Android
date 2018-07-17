package network.o3.o3wallet.Feed

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import network.o3.o3wallet.API.O3.Feature
import network.o3.o3wallet.R
import android.content.Intent
import android.net.Uri
import android.widget.Button
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import network.o3.o3wallet.Dapp.DAppBrowserActivity


/**
 * Created by drei on 3/22/18.
 */

class FeaturesAdapter(private val features: ArrayList<Feature>): RecyclerView.Adapter<FeaturesAdapter.FeatureHolder>() {

    override fun getItemCount(): Int {
        return features.count()
    }

    override fun onBindViewHolder(holder: FeatureHolder, position: Int) {
        holder?.bindFeature(features[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val view = layoutInflater.inflate(R.layout.news_feature_item, parent, false)
        return FeatureHolder(view)
    }

    class FeatureHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private var feature: Feature? = null

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            if (feature != null) {
                Answers().logContentView(ContentViewEvent()
                        .putContentType(feature?.category )
                        .putContentId(feature?.title)
                        .putContentName("Featured Item View"))
                val browserIntent = Intent(view.context, DAppBrowserActivity::class.java)
                browserIntent.putExtra("url", feature?.actionURL)
                view.context.startActivity(browserIntent)
            }
        }

        companion object {
            private val FEATURE_KEY = "FEATURE"
        }

        fun bindFeature(feature: Feature?) {
            this.feature = feature
            view.findViewById<TextView>(R.id.featureTitle).text = feature?.title?.toUpperCase() ?: ""
            view.findViewById<TextView>(R.id.featureSubtitle).text = feature?.subtitle ?: ""
            view.findViewById<Button>(R.id.featureBadge).text = feature?.actionTitle ?: ""
            view.findViewById<Button>(R.id.featureBadge).setOnClickListener {
                val browserIntent = Intent(view.context, DAppBrowserActivity::class.java)
                browserIntent.putExtra("url", feature?.actionURL)
                view.context.startActivity(browserIntent)
            }
            val imageView = view.findViewById<ImageView>(R.id.featuredImage)
            Glide.with(view.context).load(feature?.imageURL).into(imageView)
        }
    }
}

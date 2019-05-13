package network.o3.o3wallet.Settings.Help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import network.o3.o3wallet.Dapp.DappContainerActivity
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk27.coroutines.onClick
import zendesk.support.request.RequestActivity

class HelpOptionsFragment: Fragment() {
    lateinit var mView: View
    lateinit var recycler: RecyclerView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.help_options_fragment, null)
        val adapter = HelpRecyclerAdapter(this)
        recycler = mView.find(R.id.helpRecycler)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter

        return mView
    }

    class HelpRecyclerAdapter(fragment: HelpOptionsFragment): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val mFragment = fragment

        companion object {
            val NUM_SECTIONS = 2
            val NUM_SUPPORT_METHODS = 2
            val NUM_GUIDES = 1

            val HEADERROW = 0
            val SUPPORTROW = 1
            val GUIDEROW = 2
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0 || position == NUM_SUPPORT_METHODS + 1) {
                return HEADERROW
            } else if (position < NUM_SUPPORT_METHODS + 1) {
                return SUPPORTROW
            } else {
                return GUIDEROW
            }
        }

        override fun getItemCount(): Int {
            return NUM_GUIDES + NUM_SECTIONS + NUM_SUPPORT_METHODS
        }

        override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
            if (vh is HeaderViewHolder) {
                vh.bindHeader(position)
            } else if (vh is GuideViewHolder) {
                vh.bindGuide(position)
            } else if (vh is SupportOptionViewHolder) {
                vh.bindSupportOption(position)
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layoutInflater = LayoutInflater.from(viewGroup.context)
            if (viewType == HEADERROW) {
                val view = layoutInflater.inflate(R.layout.settings_header_row, viewGroup, false)
                return HeaderViewHolder(view)
            } else if (viewType == SUPPORTROW) {
                val view = layoutInflater.inflate(R.layout.settings_row_layout, viewGroup, false)
                return SupportOptionViewHolder(view)
            } else {
                val view = layoutInflater.inflate(R.layout.settings_version_row_layout, viewGroup, false)
                return GuideViewHolder(view, mFragment)

            }
        }

        class HeaderViewHolder(v: View): RecyclerView.ViewHolder(v) {
            val mView = v
            fun bindHeader(position: Int) {
                if (position == 0) {
                    mView.find<TextView>(R.id.headerTextView).text =
                            mView.context.resources.getString(R.string.SETTINGS_get_support)
                } else {
                    mView.find<TextView>(R.id.headerTextView).text =
                            mView.context.resources.getString(R.string.SETTINGS_help_guides)
                }
            }
        }

        class SupportOptionViewHolder(v: View): RecyclerView.ViewHolder(v) {
            val mView = v
            var titles = arrayOf("Community", "Contact")
            var images = arrayOf(ContextCompat.getDrawable(mView.context, R.drawable.ic_community),
                    ContextCompat.getDrawable(mView.context, R.drawable.ic_envelope))
            fun bindSupportOption(position: Int) {
                var sectionedPosition = position - 1
                mView.find<TextView>(R.id.titleTextView).text = titles[sectionedPosition]
                mView.find<ImageView>(R.id.settingsIcon).image = images[sectionedPosition]
                mView.onClick {
                    if (sectionedPosition == 0 ) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://community.o3.network/"))
                        startActivity(mView.context, browserIntent, null)
                    } else {
                        RequestActivity.builder()
                                .withTags("Android", mView.context.packageManager.getPackageInfo(mView.context.packageName, 0).versionName)
                                .withRequestSubject("Android Support Ticket")
                                .show(mView.context)
                    }
                }
            }
        }

        class GuideViewHolder(v: View, fragment: HelpOptionsFragment): RecyclerView.ViewHolder(v) {
            val mView = v
            val mFragment = fragment
            var titles = arrayOf("Crypto 101")
            var subtitles = arrayOf("New to crypto? Get started with the basics")
            fun bindGuide(position: Int) {
                val sectionedPosition = position - HelpRecyclerAdapter.NUM_SUPPORT_METHODS - 2
                mView.find<TextView>(R.id.titleTextView).text = titles[sectionedPosition]
                mView.find<TextView>(R.id.subtitleTextView).text = subtitles[sectionedPosition]
                mView.find<ImageView>(R.id.settingsIcon).image = ContextCompat.getDrawable(mView.context, R.drawable.ic_guide)
                mView.onClick {
                    val intent = Intent(mFragment.context, DappContainerActivity::class.java)

                    intent.putExtra("url", "https://docs.o3.network/docs/privateKeysAddressesAndSignatures/?mode=embed")
                    mFragment.startActivity(intent)

                    //mFragment.findNavController().navigate(R.id.action_helpOptionsFragment_to_helpGuideFragment)
                    //mFragment.activity?.find<TextView>(R.id.mytext)?.text = titles[sectionedPosition]
                }
            }
        }
    }
}
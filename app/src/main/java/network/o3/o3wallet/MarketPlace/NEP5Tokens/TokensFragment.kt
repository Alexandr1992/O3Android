package network.o3.o3wallet.MarketPlace.NEP5Tokens


import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.onUiThread


class TokensFragment : Fragment() {

    var model: TokensViewModel? = null
    private lateinit var searchView: SearchView
    private lateinit var tokensGridRecycler: RecyclerView

    fun getSizeName(context: Context): String {
        var screenLayout = context.resources.configuration.screenLayout
        screenLayout = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        when (screenLayout) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> return "small"
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> return "normal"
            Configuration.SCREENLAYOUT_SIZE_LARGE -> return "large"
            4 -> return "xlarge"
            else -> return "undefined"
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        model = TokensViewModel()

        val view =  inflater.inflate(R.layout.marketplace_tokens_fragment, container, false)
        tokensGridRecycler = view.find(R.id.tokensGridView)
        searchView = view.find(R.id.searchView)

        val v = searchView.find<View>(R.id.search_plate)
        v.setBackgroundColor(context!!.getColor(R.color.zxing_transparent))

        view.find<CardView>(R.id.searchContainerCardView).setOnClickListener {
            searchView.isIconified = false
            (activity as MainTabbedActivity).find<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE

        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                (tokensGridRecycler.adapter as TokensAdapter).setData(model!!.filteredTokens(newText).toCollection(ArrayList()))
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                (tokensGridRecycler.adapter as TokensAdapter).setData(model!!.filteredTokens(query).toCollection(ArrayList()))
                return true
            }
        })


        var spanCount = 3
        if (getSizeName(this.context!!) == "small") {
            spanCount = 2
        }

        val layoutManager = GridLayoutManager(this.context, spanCount)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0 ) 3 else 1
            }
        }
        tokensGridRecycler.layoutManager = layoutManager
        tokensGridRecycler.adapter = TokensAdapter(arrayListOf())
        model?.getListingData(true)?.observe(this, Observer { tokens ->
            onUiThread {
                (tokensGridRecycler.adapter as TokensAdapter).
                        setData(tokens?.toCollection(ArrayList())!!)
            }
        })

        model?.getSwitcheoTokenData(true)?.observe(this, Observer { tokens ->
            onUiThread {
                (tokensGridRecycler.adapter as TokensAdapter).
                        setSwitcheoData(tokens)
            }
        })

        return view
    }

    companion object {
        fun newInstance(): TokensFragment {
            return TokensFragment()
        }
    }
}

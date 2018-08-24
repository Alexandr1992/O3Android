package network.o3.o3wallet.MarketPlace.NEP5Tokens


import android.app.ActionBar
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import android.view.Gravity
import android.support.v4.view.MenuItemCompat.getActionView
import android.graphics.Color.parseColor
import android.support.design.internal.BottomNavigationMenu
import android.support.design.widget.BottomNavigationView
import android.support.v7.widget.CardView
import android.widget.Button
import network.o3.o3wallet.Dapp.DAppBrowserActivity
import network.o3.o3wallet.MainTabbedActivity
import network.o3.o3wallet.R.id.searchView
import org.jetbrains.anko.sdk25.coroutines.onFocusChange


class TokensFragment : Fragment() {

    var model: TokensViewModel? = null
    private lateinit var searchView: SearchView
    private lateinit var tokensGridRecycler: RecyclerView

    fun getSizeName(context: Context): String {
        var screenLayout = context.getResources().getConfiguration().screenLayout
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

        val v = searchView.find<View>(android.support.v7.appcompat.R.id.search_plate)
        v.setBackgroundColor(resources.getColor(R.color.zxing_transparent))

        view.find<CardView>(R.id.searchContainerCardView).setOnClickListener {
            searchView.isIconified = false
            (activity as MainTabbedActivity).find<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE

        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                tokensGridRecycler.adapter = TokensAdapter(model!!.filteredTokens(newText).toCollection(ArrayList()))
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                tokensGridRecycler.adapter = TokensAdapter(model!!.filteredTokens(query).toCollection(ArrayList()))
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
        tokensGridRecycler.setLayoutManager(layoutManager)

        model?.getListingData(true)?.observe(this, Observer { tokens ->
            tokensGridRecycler.adapter = TokensAdapter(tokens?.toCollection(ArrayList())!!)
        })

        return view
    }

    companion object {
        fun newInstance(): TokensFragment {
            return TokensFragment()
        }
    }
}

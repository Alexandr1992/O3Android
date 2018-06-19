package network.o3.o3wallet.MarketPlace.NEP5Tokens


import android.arch.lifecycle.Observer
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import network.o3.o3wallet.R
import org.jetbrains.anko.find


class TokensFragment : Fragment() {

    var model: TokensViewModel? = null

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
        val tokensGridRecycler = view.find<RecyclerView>(R.id.tokensGridView)
        var spanCount = 3
        if (getSizeName(this.context!!) == "small") {
            spanCount = 2
        }

        val layoutManager = GridLayoutManager(this.context, spanCount)
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

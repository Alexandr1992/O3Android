package network.o3.o3wallet.Settings.Help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.tiagohm.markdownview.MarkdownView
import br.tiagohm.markdownview.css.styles.Github
import network.o3.o3wallet.R
import org.jetbrains.anko.find

class HelpGuideFragment: Fragment() {

    lateinit var mView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.help_guide_fragment, null)

        //mView.find<MarkdownView>(R.id.markdown_view).addStyleSheet(ExternalStyleSheet.fromAsset("github.css", null))
        var css = Github()
        css.removeRule(".scrollup")
        mView.find<MarkdownView>(R.id.markdown_view).addStyleSheet(css)
        mView.find<MarkdownView>(R.id.markdown_view).loadMarkdownFromUrl("https://docs.o3.network/docs/privateKeysAddressesAndSignatures/?mode=embed")
        return mView
    }
}
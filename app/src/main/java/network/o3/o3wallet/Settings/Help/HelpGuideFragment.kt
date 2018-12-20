package network.o3.o3wallet.Settings.Help

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import br.tiagohm.markdownview.MarkdownView
import br.tiagohm.markdownview.css.ExternalStyleSheet
import br.tiagohm.markdownview.css.styles.Github
import network.o3.o3wallet.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find
import java.net.URL

class HelpGuideFragment: Fragment() {

    lateinit var mView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.help_guide_fragment, null)

        //mView.find<MarkdownView>(R.id.markdown_view).addStyleSheet(ExternalStyleSheet.fromAsset("github.css", null))
        mView.find<MarkdownView>(R.id.markdown_view).addStyleSheet(Github())
        mView.find<MarkdownView>(R.id.markdown_view).loadMarkdownFromUrl("https://raw.githubusercontent.com/saltyskip/slate/master/guide.md")

        return mView
    }
}
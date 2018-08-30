package network.o3.o3wallet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.support.annotation.AttrRes
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText

/**
 * Created by drei on 4/18/18.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun View.setNoDoubleClickListener(listener: View.OnClickListener, waitMillis : Long = 1000) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        if (System.currentTimeMillis() > lastClickTime + waitMillis) {
            listener.onClick(view)
            lastClickTime = System.currentTimeMillis()
        }
    }
}

fun Context.getColorFromAttr(
        @AttrRes attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}


package eu.faircode.netguard

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

// This requires list view items with equal heights
class ExpandedListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ListView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 4, MeasureSpec.AT_MOST),
        )
    }
}

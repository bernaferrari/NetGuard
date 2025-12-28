package eu.faircode.netguard

import android.content.Context
import android.util.AttributeSet

// https://code.google.com/p/android/issues/detail?id=26194
class SwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.switchPreferenceStyle,
) : android.preference.SwitchPreference(context, attrs, defStyle)

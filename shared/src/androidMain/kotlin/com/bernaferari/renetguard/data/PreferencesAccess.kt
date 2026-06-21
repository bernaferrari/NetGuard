package com.bernaferari.renetguard.data

import android.content.Context
import org.koin.core.context.GlobalContext

fun Context.preferences(): PreferencesRepository = GlobalContext.get().get()
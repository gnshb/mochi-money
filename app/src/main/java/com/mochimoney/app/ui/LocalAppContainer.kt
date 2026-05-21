package com.mochimoney.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.mochimoney.app.data.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer was not provided")
}

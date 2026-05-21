package com.mochimoney.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.mochimoney.app.ui.LocalAppContainer
import com.mochimoney.app.ui.MochiMoneyApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as MochiMoneyApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                MochiMoneyApp()
            }
        }
    }
}

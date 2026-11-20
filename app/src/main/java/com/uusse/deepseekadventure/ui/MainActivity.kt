package com.uusse.deepseekadventure.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.uusse.deepseekadventure.App
import com.uusse.deepseekadventure.domain.repository.AppSettings
import com.uusse.deepseekadventure.ui.navigation.AppRoot
import com.uusse.deepseekadventure.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsState(AppSettings())
            AppTheme(settings.themeMode) {
                AppRoot(container)
            }
        }
    }
}

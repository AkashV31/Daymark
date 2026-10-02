package com.daymark.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableStateFlow
import com.daymark.app.ui.DaymarkApp

class MainActivity : ComponentActivity() {
    private val notificationTarget = MutableStateFlow<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyNotificationTarget(intent)
        val app = application as DaymarkApplication
        setContent {
            val viewModel: com.daymark.app.ui.DaymarkViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.daymark.app.ui.DaymarkViewModel.Factory(app, app.repository)
            )
            DaymarkApp(viewModel = viewModel, notificationTarget = notificationTarget)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyNotificationTarget(intent)
    }

    private fun applyNotificationTarget(intent: Intent?) {
        val type = intent?.getStringExtra("daymark.owner_type") ?: return
        val id = intent.getStringExtra("daymark.owner_id") ?: return
        notificationTarget.value = type to id
    }
}

package dev.daniilborovskiy.yandextvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.daniilborovskiy.yandextvremote.ui.RemoteApp
import dev.daniilborovskiy.yandextvremote.ui.theme.YandexTvRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YandexTvRemoteTheme {
                val remoteViewModel: RemoteViewModel = viewModel()
                RemoteApp(remoteViewModel)
            }
        }
    }
}

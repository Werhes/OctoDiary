package org.bxkr.octodiary.ui.screen.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import org.bxkr.octodiary.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthWebView(
    webViewParams: AuthViewModel.AdditionalPageContent.WebView,
    viewModel: AuthViewModel = koinViewModel()
) {
    val webViewState = rememberWebViewState(webViewParams.url) {
        androidWebSettings.apply {
            useWideViewPort = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
        }
    }
    val currentUrl = webViewState.lastLoadedUrl
    LaunchedEffect(currentUrl) {
        if (currentUrl != null && webViewParams.webViewListener(currentUrl)) {
            viewModel.catchWebViewUrl(currentUrl)
        }
    }
    Box(Modifier.fillMaxSize()) {
        WebView(webViewState, Modifier.fillMaxSize())
    }
}

/**
 * МЭШ-авторизация через токен. Приложение просто открывает ссылку вида
 * `school.mos.ru/?backUrl=.../v2/token/refresh...`. Пользователь авторизуется на school.mos.ru,
 * после чего сайт сам переводит его на страницу с токеном. Токен нужно скопировать
 * и вставить в приложение.
 */
@Composable
fun MesTokenAuthWebView(
    params: AuthViewModel.AdditionalPageContent.MesTokenWebView,
    viewModel: AuthViewModel = koinViewModel()
) {
    val webViewState = rememberWebViewState(params.url) {
        androidWebSettings.apply {
            useWideViewPort = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
        }
    }
    val currentUrl = webViewState.lastLoadedUrl

    params.webViewListener?.let { listener ->
        LaunchedEffect(currentUrl) {
            if (currentUrl != null && listener(currentUrl)) viewModel.catchWebViewUrl(currentUrl)
        }
    }

    val onTokenPage = currentUrl?.contains("/v2/token/refresh") == true

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            WebView(webViewState, Modifier.fillMaxSize())
        }
        Text(
            text = if (onTokenPage) {
                "Скопируйте токен из этой страницы и вставьте его в приложение"
            } else {
                "Войдите в аккаунт на school.mos.ru — после входа вы попадёте на страницу с токеном"
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (onTokenPage) {
            Button(
                onClick = { viewModel.goToTokenPrompt() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Ввести токен в приложении")
            }
        }
    }
}
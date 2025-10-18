package ai.lmarena.webapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ai.lmarena.webapp.ui.OrbitLoader
import ai.lmarena.webapp.ui.theme.AppTheme
import kotlinx.coroutines.delay

private const val START_URL = "https://lmarena.ai"

class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    WebAppScreen(url = START_URL)
                }
            }
        }
    }
}

@Composable
fun WebAppScreen(url: String) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var httpError by remember { mutableStateOf<WebError?>(null) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportZoom(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val u = request?.url?.toString().orEmpty()
                    val scheme = request?.url?.scheme.orEmpty()

                    if (scheme in listOf("http", "https")) {
                        // Keep navigation in-app
                        return false
                    }
                    // Handle intents like tel:, sms:, mailto:, etc.
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(u))
                        context.startActivity(intent)
                        true
                    } catch (_: ActivityNotFoundException) {
                        true
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                    progress = 0
                    httpError = null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    canGoBack = view?.canGoBack() == true
                    // Ensure loader dismisses even if progress callback is quirky
                    progress = 100
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true) {
                        httpError = WebError(
                            code = errorResponse?.statusCode ?: -1,
                            message = errorResponse?.reasonPhrase ?: "HTTP error"
                        )
                        isLoading = false
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        httpError = WebError(
                            code = error?.errorCode ?: -1,
                            message = error?.description?.toString() ?: "Load error"
                        )
                        isLoading = false
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    httpError = WebError(code = -1200, message = "SSL error")
                    handler?.cancel()
                    isLoading = false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress
                }

                // Handle target=_blank / window.open
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    // Load into same WebView
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = view
                    resultMsg?.sendToTarget()
                    return true
                }
            }
        }
    }

    // Smoothly hide the loader once progress hits 100%
    LaunchedEffect(progress) {
        if (progress >= 100) {
            delay(200) // tiny delay for polish
            isLoading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = {
                if (it.url == null) {
                    it.loadUrl(url)
                }
                canGoBack = it.canGoBack()
            }
        )

        // Top progress bar while loading
        AnimatedVisibility(visible = isLoading) {
            Column(Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Animated loader overlay
        AnimatedVisibility(visible = isLoading, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OrbitLoader(
                        sizeDp = 96.dp,
                        color = MaterialTheme.colorScheme.primary,
                        secondary = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Loading lmarena.ai",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Error overlay
        if (httpError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Couldn’t display the site (${httpError?.code}).",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        httpError?.message ?: "",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(onClick = {
                            httpError = null
                            isLoading = true
                            webView.reload()
                        }) { Text("Retry") }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = {
                            openInCustomTab(context, url)
                        }) { Text("Open in Browser") }
                    }
                }
            }
        }
    }

    // Back button: go back within WebView history
    BackHandler(enabled = canGoBack && !isLoading) {
        if (webView.canGoBack()) webView.goBack()
    }
}

data class WebError(val code: Int, val message: String)

private fun openInCustomTab(context: android.content.Context, url: String) {
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()
            .launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        // Fallback to default browser if Custom Tabs not available
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) { }
    }
}
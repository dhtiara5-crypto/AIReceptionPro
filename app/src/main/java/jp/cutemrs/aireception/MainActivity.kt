package jp.cutemrs.aireception

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import jp.cutemrs.aireception.databinding.ActivityMainBinding
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    companion object {
        const val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxhVCwZm4SUssHLntGEfSfeoLfl2POcTIJW_kgwOKmBOXehlvseQTzRSJpqNYQdz14B/exec"
    }

    private lateinit var binding: ActivityMainBinding
    private var currentPhone: String = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { loadFromIntent(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureWebView()
        requestRequiredPermissions()
        loadFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadFromIntent(intent)
    }

    private fun requestRequiredPermissions() {
        val wanted = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            wanted += Manifest.permission.READ_PHONE_STATE
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            wanted += Manifest.permission.READ_CALL_LOG
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            wanted += Manifest.permission.POST_NOTIFICATIONS
        }
        if (wanted.isNotEmpty()) permissionLauncher.launch(wanted.toTypedArray())
    }

    private fun configureWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = false
            useWideViewPort = false
            textZoom = 115
            userAgentString = "$userAgentString AIReceptionPro/1.0"
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.subStatusText.text = if (currentPhone.isBlank()) {
                    "電話番号を待機しています"
                } else {
                    "顧客情報を表示しています"
                }
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.visibility = if (newProgress < 100) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun loadFromIntent(sourceIntent: Intent?) {
        val deepLinkPhone = sourceIntent?.data?.getQueryParameter("phone")
        val extraPhone = sourceIntent?.getStringExtra("phone")
        val savedPhone = getSharedPreferences("ai_reception", MODE_PRIVATE).getString("last_phone", "")
        val phone = normalizePhone(deepLinkPhone ?: extraPhone ?: savedPhone.orEmpty())
        showPhone(phone)
        loadReceptionPage(phone)
    }

    private fun showPhone(phone: String) {
        currentPhone = phone
        binding.phoneText.text = if (phone.isBlank()) "番号待機中" else formatPhone(phone)
        binding.subStatusText.text = if (phone.isBlank()) {
            "MacroDroidの着信番号を待機しています"
        } else {
            "顧客情報を自動検索中…"
        }
    }

    private fun loadReceptionPage(phone: String) {
        val encoded = URLEncoder.encode(phone, StandardCharsets.UTF_8.toString())
        val url = "$WEB_APP_URL?mode=android&phone=$encoded&source=apk&v=1"
        binding.webView.loadUrl(url)
    }

    private fun normalizePhone(value: String): String {
        var p = value.filter(Char::isDigit)
        if (p.startsWith("81") && p.length in 11..12) p = "0" + p.drop(2)
        return p
    }

    private fun formatPhone(p: String): String = when (p.length) {
        11 -> "${p.substring(0,3)}-${p.substring(3,7)}-${p.substring(7)}"
        10 -> "${p.substring(0,2)}-${p.substring(2,6)}-${p.substring(6)}"
        else -> p
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) binding.webView.goBack() else super.onBackPressed()
    }
}

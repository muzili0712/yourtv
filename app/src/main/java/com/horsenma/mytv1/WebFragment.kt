package com.horsenma.mytv1

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient as AndroidWebChromeClient
import android.webkit.WebResourceRequest as AndroidWebResourceRequest
import android.webkit.WebResourceResponse as AndroidWebResourceResponse
import android.webkit.WebView as AndroidWebView
import android.webkit.WebViewClient as AndroidWebViewClient
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.horsenma.yourtv.databinding.PlayerBinding
import com.horsenma.mytv1.models.TVModel
import android.net.http.SslError as AndroidSslError
import android.webkit.SslErrorHandler as AndroidSslErrorHandler
import com.tencent.smtt.sdk.WebChromeClient as X5WebChromeClient
import com.tencent.smtt.sdk.WebView as X5WebView
import com.tencent.smtt.sdk.WebViewClient as X5WebViewClient
import com.tencent.smtt.export.external.interfaces.SslError as X5SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler as X5SslErrorHandler
import com.tencent.smtt.export.external.interfaces.WebResourceRequest as X5WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse as X5WebResourceResponse
import com.tencent.smtt.export.external.interfaces.ConsoleMessage as X5ConsoleMessage
import com.horsenma.yourtv.R
import com.horsenma.yourtv.YourTVApplication
import java.io.ByteArrayInputStream
import androidx.constraintlayout.widget.ConstraintLayout

@Suppress("DEPRECATION")
class WebFragment : Fragment() {
    private lateinit var mainActivity: MainActivity
    private lateinit var webView: View // 兼容现有代码
    private var tvModel: TVModel? = null
    private var _binding: PlayerBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.myLooper()!!)
    private val delayHideVolume = 2 * 1000L

    private val scriptMap = mapOf(
        "live.kankanews.com" to R.raw.ahtv1,
        "www.cbg.cn" to R.raw.ahtv1,
        "www.sxrtv.com" to R.raw.sxrtv1,
        "www.xjtvs.com.cn" to R.raw.xjtv1,
        "www.yb983.com" to R.raw.ahtv1,
        "www.yntv.cn" to R.raw.ahtv1,
        "www.nmtv.cn" to R.raw.nmgtv1,
        "live.snrtv.com" to R.raw.ahtv1,
        "www.btzx.com.cn" to R.raw.ahtv1,
        "static.hntv.tv" to R.raw.ahtv1,
        "www.hljtv.com" to R.raw.ahtv1,
        "www.qhtb.cn" to R.raw.ahtv1,
        "www.qhbtv.com" to R.raw.ahtv1,
        "v.iqilu.com" to R.raw.ahtv1,
        "www.jlntv.cn" to R.raw.ahtv1,
        "www.cztv.com" to R.raw.ahtv1,
        "www.gzstv.com" to R.raw.ahtv1,
        "www.jxntv.cn" to R.raw.jxtv1,
        "www.hnntv.cn" to R.raw.ahtv1,
        "live.mgtv.com" to R.raw.ahtv1,
        "www.hebtv.com" to R.raw.ahtv1,
        "tc.hnntv.cn" to R.raw.ahtv1,
        "live.fjtv.net" to R.raw.ahtv1,
        "tv.gxtv.cn" to R.raw.ahtv1,
        "www.nxtv.com.cn" to R.raw.ahtv1,
        "www.ahtv.cn" to R.raw.ahtv2,
        "news.hbtv.com.cn" to R.raw.ahtv1,
        "www.sztv.com.cn" to R.raw.ahtv1,
        "www.setv.sh.cn" to R.raw.ahtv1,
        "www.gdtv.cn" to R.raw.ahtv1,
        "tv.cctv.com" to R.raw.ahtv1,
        "www.yangshipin.cn" to R.raw.ahtv1,
        "www.brtn.cn" to R.raw.xjtv1,
        "www.kangbatv.com" to R.raw.ahtv1,
        "live.jstv.com" to R.raw.xjtv1,
        "www.wfcmw.cn" to R.raw.xjtv1,
    )

    private val blockMap = mapOf(
        "央视甲" to listOf(
            "jweixin",
            "daohang",
            "dianshibao.js",
            "dingtalk.js",
            "configtool",
            "qrcode",
            "shareindex.js",
            "zhibo_shoucang.js",
            "gray",
            "cntv_Advertise.js",
            "top2023newindex.js",
            "indexPC.js",
            "getEpgInfoByChannelNew",
            "epglist",
            "epginfo",
            "getHandDataList",
            "2019whitetop/index.js",
            "pc_nav/index.js",
            "shareindex.js",
            "mapjs/index.js",
            "bottomjs/index.js",
            "top2023newindex.js",
            "2019dlbhyjs/index.js"
        ),
    )

    private var finished = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mainActivity = activity as MainActivity
        super.onActivityCreated(savedInstanceState)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBinding.inflate(inflater, container, false)
        val application = requireActivity().applicationContext as YourTVApplication
        val webViewContainer = binding.webView // 布局中的 WebView 或容器

        // 动态创建 WebView
        if (SP.useX5WebView && YourTVApplication.getInstance().isX5Available()) {
            Log.i(TAG, "Creating X5 WebView")
            webView = X5WebView(requireContext()).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
                }
                webChromeClient = object : X5WebChromeClient() {
                    override fun getDefaultVideoPoster(): Bitmap {
                        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    }
                    override fun onConsoleMessage(consoleMessage: X5ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            if (consoleMessage.message() == "success") {
                                Log.i(TAG, "${tvModel?.tv?.title} success")
                                tvModel?.tv?.finished?.let {
                                    (webView as X5WebView).evaluateJavascript(it) { res ->
                                        Log.i(TAG, "${tvModel?.tv?.title} finished: $res")
                                    }
                                }
                                tvModel?.setErrInfo("web ok")
                            }
                        }
                        return true
                    }
                    override fun onShowCustomView(view: View, callback: com.tencent.smtt.export.external.interfaces.IX5WebChromeClient.CustomViewCallback) {
                        Log.i(TAG, "X5 onShowCustomView")
                        binding.root.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    }
                    override fun onHideCustomView() {
                        Log.i(TAG, "X5 onHideCustomView")
                    }
                }
                webViewClient = object : X5WebViewClient() {
                    override fun onReceivedSslError(view: X5WebView?, handler: X5SslErrorHandler, error: X5SslError?) {
                        handler.proceed()
                    }
                    override fun shouldInterceptRequest(view: X5WebView?, request: X5WebResourceRequest?): X5WebResourceResponse? {
                        val uri = request?.url
                        Log.d(TAG, "${request?.method} ${uri.toString()} ${request?.requestHeaders}")
                        blockMap[tvModel?.tv?.group]?.let {
                            for (i in it) {
                                if (uri?.path?.contains(i) == true) {
                                    Log.i(TAG, "block path ${uri.path}")
                                    return X5WebResourceResponse("text/plain", "utf-8", null)
                                }
                            }
                        }
                        tvModel?.tv?.block?.let {
                            for (i in it) {
                                if (uri?.path?.contains(i) == true) {
                                    Log.i(TAG, "block path ${uri.path}")
                                    return X5WebResourceResponse("text/plain", "utf-8", null)
                                }
                            }
                        }
                        if (uri?.path?.endsWith(".css") == true) {
                            return X5WebResourceResponse("text/css", "utf-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        if (!request?.isForMainFrame!! && (uri?.path?.endsWith(".jpg") == true || uri?.path?.endsWith(".jpeg") == true ||
                                    uri?.path?.endsWith(".png") == true || uri?.path?.endsWith(".gif") == true ||
                                    uri?.path?.endsWith(".webp") == true || uri?.path?.endsWith(".svg") == true)) {
                            return X5WebResourceResponse("image/png", "utf-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        return null
                    }
                    override fun onPageStarted(view: X5WebView?, url: String?, favicon: Bitmap?) {
                        Log.i(TAG, "X5 onPageStarted $url")
                        val jsCode = """
                    (() => {
                        const style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = `
                        body {
                            background-color: #000;
                        }
                        img:not([role="presentation"]) {
                            display: none;
                        }
                        video, iframe, object, embed {
                            display: block !important;
                            position: relative !important;
                            width: 100% !important;
                            height: auto !important;
                            background-color: transparent !important;
                        }
                        `;
                        document.head.appendChild(style);
                    })();
                    """.trimIndent()
                        (webView as X5WebView).evaluateJavascript(jsCode, null)
                    }
                    override fun onPageFinished(view: X5WebView?, url: String?) {
                        if ((webView as X5WebView).progress < 100) {
                            super.onPageFinished(view, url)
                            return
                        }
                        finished++
                        if (finished < 1) {
                            super.onPageFinished(view, url)
                            return
                        }
                        Log.i(TAG, "X5 onPageFinished $finished $url")
                        tvModel?.tv?.started?.let {
                            (webView as X5WebView).evaluateJavascript(it, null)
                            Log.i(TAG, "started")
                        }
                        tvModel?.tv?.script?.let {
                            (webView as X5WebView).evaluateJavascript(it, null)
                            Log.i(TAG, "script")
                        }
                        val uri = Uri.parse(url)
                        var script = scriptMap[uri?.host]
                        if (script == null) {
                            script = R.raw.ahtv1
                        }
                        var s = requireContext().resources.openRawResource(script)
                            .bufferedReader()
                            .use { it.readText() }
                        tvModel?.tv?.id?.let {
                            s = s.replace("{id}", "$it")
                        }
                        tvModel?.tv?.selector?.let {
                            s = s.replace("{selector}", it)
                        }
                        tvModel?.tv?.index?.let {
                            s = s.replace("{index}", "$it")
                        }
                        Log.d(TAG, "s: $s")
                        (webView as X5WebView).evaluateJavascript(s, null)
                        Log.i(TAG, "default")
                    }
                }
            }
            // 移除布局中的 WebView，添加 X5WebView
            webViewContainer.visibility = View.GONE
            binding.root.addView(webView, ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            ).apply {
                topToTop = R.id.player_fragment
                bottomToBottom = R.id.player_fragment
                startToStart = R.id.player_fragment
                endToEnd = R.id.player_fragment
            })
        } else {
            Log.i(TAG, "Creating System WebView")
            webView = binding.webView // 使用布局中的 WebView
            (webView as AndroidWebView).settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
            }
            (webView as AndroidWebView).layoutParams.width = application.shouldWidthPx()
            (webView as AndroidWebView).layoutParams.height = application.shouldHeightPx()
            (webView as AndroidWebView).webChromeClient = object : AndroidWebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    if (consoleMessage != null) {
                        Log.d(TAG, "Console: ${consoleMessage?.message()} [${consoleMessage?.lineNumber()}]")
                        if (consoleMessage.message() == "success") {
                            Log.i(TAG, "${tvModel?.tv?.title} success")
                            tvModel?.tv?.finished?.let {
                                (webView as AndroidWebView).evaluateJavascript(it) { res ->
                                    Log.i(TAG, "${tvModel?.tv?.title} finished: $res")
                                }
                            }
                            tvModel?.setErrInfo("web ok")
                        }
                    }
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            (webView as AndroidWebView).webViewClient = object : AndroidWebViewClient() {
                override fun onReceivedSslError(view: AndroidWebView?, handler: AndroidSslErrorHandler?, error: AndroidSslError?) {
                    handler?.proceed()
                }
                override fun shouldInterceptRequest(
                    view: AndroidWebView?,
                    request: AndroidWebResourceRequest?
                ): AndroidWebResourceResponse? {
                    val uri = request?.url
                    Log.d(TAG, "${request?.method} ${uri.toString()} ${request?.requestHeaders}")
                    blockMap[tvModel?.tv?.group]?.let {
                        for (i in it) {
                            if (uri?.path?.contains(i) == true) {
                                Log.i(TAG, "block path ${uri.path}")
                                return AndroidWebResourceResponse("text/plain", "utf-8", null)
                            }
                        }
                    }
                    tvModel?.tv?.block?.let {
                        for (i in it) {
                            if (uri?.path?.contains(i) == true) {
                                Log.i(TAG, "block path ${uri.path}")
                                return AndroidWebResourceResponse("text/plain", "utf-8", null)
                            }
                        }
                    }
                    if (uri?.path?.endsWith(".css") == true) {
                        return AndroidWebResourceResponse("text/css", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    }
                    if (!request?.isForMainFrame!! && (uri?.path?.endsWith(".jpg") == true || uri?.path?.endsWith(".jpeg") == true ||
                                uri?.path?.endsWith(".png") == true || uri?.path?.endsWith(".gif") == true ||
                                uri?.path?.endsWith(".webp") == true || uri?.path?.endsWith(".svg") == true)) {
                        return AndroidWebResourceResponse("image/png", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    }
                    return null
                }
                override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: Bitmap?) {
                    Log.i(TAG, "System onPageStarted $url")
                    super.onPageStarted(view, url, favicon)
                    val jsCode = """
                    (() => {
                        const style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = `
                        body {
                            position: 'fixed';
                            left: '100%';
                            background-color: '#000';
                        }
                        img {
                            display: none;
                        }
                        * {
                            font-size: 0 !important;
                            color: black !important;
                            background-color: black !important;
                            border-color: black !important;
                            outline-color: black !important;
                            text-shadow: none !important;
                            box-shadow: none !important;
                            fill: black !important;
                            stroke: black !important;
                            width: 0;
                        }
                        `;
                        document.head.appendChild(style);
                    })();
                    """.trimIndent()
                    (webView as AndroidWebView).evaluateJavascript(jsCode, null)
                }
                override fun onPageFinished(view: AndroidWebView?, url: String?) {
                    if ((webView as AndroidWebView).progress < 100) {
                        super.onPageFinished(view, url)
                        return
                    }
                    finished++
                    if (finished < 1) {
                        super.onPageFinished(view, url)
                        return
                    }
                    Log.i(TAG, "System onPageFinished $finished $url")
                    tvModel?.tv?.started?.let {
                        (webView as AndroidWebView).evaluateJavascript(it, null)
                        Log.i(TAG, "started")
                    }
                    super.onPageFinished(view, url)
                    tvModel?.tv?.script?.let {
                        (webView as AndroidWebView).evaluateJavascript(it, null)
                        Log.i(TAG, "script")
                    }
                    val uri = Uri.parse(url)
                    var script = scriptMap[uri?.host]
                    if (script == null) {
                        script = R.raw.ahtv1
                    }
                    var s = requireContext().resources.openRawResource(script)
                        .bufferedReader()
                        .use { it.readText() }
                    tvModel?.tv?.id?.let {
                        s = s.replace("{id}", "$it")
                    }
                    tvModel?.tv?.selector?.let {
                        s = s.replace("{selector}", it)
                    }
                    tvModel?.tv?.index?.let {
                        s = s.replace("{index}", "$it")
                    }
                    Log.d(TAG, "s: $s")
                    (webView as AndroidWebView).evaluateJavascript(s, null)
                    Log.i(TAG, "default")
                }
            }
        }

        webView.isClickable = false
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
        webView.setOnTouchListener { v, event ->
            if (event != null) {
                (activity as MainActivity).gestureDetector.onTouchEvent(event)
            }
            true
        }

        (activity as MainActivity).ready(TAG)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun play(tvModel: TVModel) {
        finished = 0
        this.tvModel = tvModel
        val url = tvModel.videoUrl.value as String
        Log.i(TAG, "play ${tvModel.tv.id} ${tvModel.tv.title} $url")

        val isX5 = webView is X5WebView
        if (isX5) {
            (webView as X5WebView).loadUrl(url)
        } else {
            (webView as AndroidWebView).loadUrl(url)
        }

        //binding.playerView.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }

    fun showVolume(visibility: Int) {
        binding.icon.visibility = visibility
        binding.volume.visibility = visibility
        hideVolume()
    }

    fun setVolumeMax(volume: Int) {
        binding.volume.max = volume
    }

    fun setVolume(progress: Int, volume: Boolean = false) {
        val context = requireContext()
        binding.volume.progress = progress
        binding.icon.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                if (volume) {
                    if (progress > 0) R.drawable.volume_up_24px else R.drawable.volume_off_24px
                } else {
                    R.drawable.light_mode_24px
                }
            )
        )
    }

    fun hideVolume() {
        handler.removeCallbacks(hideVolumeRunnable)
        handler.postDelayed(hideVolumeRunnable, delayHideVolume)
    }

    fun hideVolumeNow() {
        handler.removeCallbacks(hideVolumeRunnable)
        handler.postDelayed(hideVolumeRunnable, 0)
    }

    private val hideVolumeRunnable = Runnable {
        binding.icon.visibility = View.GONE
        binding.volume.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (webView is X5WebView) {
            (webView as X5WebView).destroy()
        } else {
            (webView as AndroidWebView).destroy()
        }
        _binding = null
    }

    companion object {
        private const val TAG = "WebFragment"
    }
}
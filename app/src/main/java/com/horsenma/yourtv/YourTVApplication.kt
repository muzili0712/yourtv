package com.horsenma.yourtv

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import java.util.Locale

class YourTVApplication : Application() {

    companion object {
        private const val TAG = "YourTVApplication"
        private lateinit var instance: YourTVApplication

        @JvmStatic
        fun getInstance(): YourTVApplication {
            return instance
        }
    }

    private var isX5Initialized = false
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var realDisplayMetrics: DisplayMetrics

    private var width = 0
    private var height = 0
    private var shouldWidth = 0
    private var shouldHeight = 0
    private var ratio = 1.0
    private var density = 2.0f
    private var scale = 1.0f

    lateinit var imageHelper: ImageHelper

    override fun onCreate() {
        super.onCreate()
        instance = this
        SP.init(this)
        SP.compactMenu = true
        initX5()

        displayMetrics = DisplayMetrics()
        realDisplayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        windowManager.defaultDisplay.getRealMetrics(realDisplayMetrics)

        if (realDisplayMetrics.heightPixels > realDisplayMetrics.widthPixels) {
            width = realDisplayMetrics.heightPixels
            height = realDisplayMetrics.widthPixels
        } else {
            width = realDisplayMetrics.widthPixels
            height = realDisplayMetrics.heightPixels
        }

        density = Resources.getSystem().displayMetrics.density
        scale = displayMetrics.scaledDensity

        if ((width.toDouble() / height) < (16.0 / 9.0)) {
            ratio = width * 2 / 1920.0 / density
            shouldWidth = width
            shouldHeight = (width * 9.0 / 16.0).toInt()
        } else {
            ratio = height * 2 / 1080.0 / density
            shouldHeight = height
            shouldWidth = (height * 16.0 / 9.0).toInt()
        }

        Thread.setDefaultUncaughtExceptionHandler(YourTVExceptionHandler(this))

        imageHelper = ImageHelper(this)
    }

    private fun initX5() {
        val settings = mutableMapOf<String, Any>(
            TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER to true,
            TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE to true
        )
        QbSdk.initTbsSettings(settings)
        QbSdk.initX5Environment(this, object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
                Log.i(TAG, "X5 Core initialized")
            }

            override fun onViewInitFinished(isX5: Boolean) {
                Log.i(TAG, "X5 View initialized, isX5=$isX5")
                isX5Initialized = isX5
                if (isX5) {
                    Log.d(TAG, "X5 kernel is available on device, no need to download")
                } else {
                    Log.w(TAG, "X5 kernel not available, triggering download")
                }
            }
        })
    }

    fun isX5Available(): Boolean {
        return isX5Initialized
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return displayMetrics
    }

    fun toast(message: CharSequence = "", duration: Int = Toast.LENGTH_SHORT) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, duration).show()
        }
    }

    fun shouldWidthPx(): Int {
        return shouldWidth
    }

    fun shouldHeightPx(): Int {
        return shouldHeight
    }

    fun dp2Px(dp: Int): Int {
        return (dp * ratio * density + 0.5f).toInt()
    }

    fun px2Px(px: Int): Int {
        return (px * ratio + 0.5f).toInt()
    }

    fun px2PxFont(px: Float): Float {
        return (px * ratio / scale).toFloat()
    }

    fun sp2Px(sp: Float): Float {
        return (sp * ratio * scale).toFloat()
    }

    override fun attachBaseContext(base: Context) {
        try {
            val locale = Locale.TRADITIONAL_CHINESE
            val config = Configuration()
            config.setLocale(locale)
            super.attachBaseContext(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    base.createConfigurationContext(config)
                } else {
                    val resources = base.resources
                    resources.updateConfiguration(config, resources.displayMetrics)
                    base
                }
            )
        } catch (_: Exception) {
            super.attachBaseContext(base)
        }
    }
}
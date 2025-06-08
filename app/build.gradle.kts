plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.horsenma.yourtv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.horsenma.yourtv"
        minSdk = 23
        targetSdk = 35
        versionCode = getVersionCode()
        versionName = getVersionName()
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_LOG", "false")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_LOG", "true")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/io.netty.versions.properties" // 新增排除
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                val appName = "yourtv"
                val newName = "${appName}_v1.7.5.apk"
                outputFileName = newName
            }
        }
    }
}

fun getVersionName(): String {
    return "1.7.5"
}

fun getVersionCode(): Int {
    val parts = getVersionName().split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return major * 100 + minor
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.activity.ktx)
    implementation(libs.appcompat.v161)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx.v1160)
    implementation(libs.coroutines)
    implementation(libs.exoplayer)
    implementation(libs.fragment.ktx.v184)
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx.v290)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.datasource.rtmp)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.exoplayer.v111)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.v111)
    implementation(libs.nanohttpd)
    implementation(libs.okhttp)
    implementation(libs.recyclerview)
    implementation(libs.security.crypto)
    implementation(libs.viewbinding)
    implementation(libs.webkit)
    implementation(libs.zxing)
    implementation("com.tencent.tbs:tbssdk:44286")
}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

fun loadLocalProperties(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    val props = mutableMapOf<String, String>()
    file.readLines().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank() || line.startsWith("#") || line.startsWith("!")) return@forEach
        val sep = if (line.contains("=")) line.indexOf("=") else line.indexOf(":")
        if (sep > 0) {
            val key = line.substring(0, sep).trim()
            val value = line.substring(sep + 1).trim()
            props[key] = value
        }
    }
    return props
}

base {
    archivesName.set("NeedAIChat")
}

android {
    namespace = "com.needai.chat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.needai.chat"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        // 从 local.properties（已 gitignore）读取内置 API Key，通过 BuildConfig 注入
        // 避免在 APK assets 中明文存储密钥
        val localProps = loadLocalProperties(rootProject.file("local.properties"))
        buildConfigField("String", "BUILTIN_CHAT_API_KEY",
            "\"${localProps["builtin.chat.api.key"] ?: ""}\"")
        buildConfigField("String", "BUILTIN_TTS_API_KEY",
            "\"${localProps["builtin.tts.api.key"] ?: ""}\"")
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // VAD — 排除它的旧版 onnxruntime 传递依赖，避免旧 .so 打进 APK
    implementation(libs.vad.silero) {
        exclude(group = "com.microsoft.onnxruntime", module = "onnxruntime-android")
    }
    // 强制 onnxruntime ≥1.25.0（libonnxruntime4j_jni.so 才有 16 KB LOAD 段对齐）
    implementation("com.microsoft.onnxruntime:onnxruntime-android") {
        version { strictly("[1.25.0,)") }
    }
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)

    // Security
    implementation(libs.security.crypto)

    // JSON
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // NUI SDK (阿里云 CosyVoice TTS)
    implementation(files("libs/nuisdk-release.aar"))
    implementation("com.alibaba:fastjson:1.2.83")

    // VAD
    implementation(libs.vad.silero)

    // WorkManager
    implementation(libs.workmanager.ktx)

    // QR Code
    implementation(libs.zxing.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

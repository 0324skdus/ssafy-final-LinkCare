import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}
val googleWebClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")
val kakaoNativeAppKey = localProps.getProperty("KAKAO_NATIVE_APP_KEY", "")

android {
    namespace = "com.a307.linkcare"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.a307.linkcare"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${(localProps["GOOGLE_WEB_CLIENT_ID"] ?: "").toString()}\""
        )

        resValue("string", "kakao_native_app_key", (localProps["KAKAO_NATIVE_APP_KEY"] ?: "").toString())
        resValue("string", "kakao_scheme", "kakao${(localProps["KAKAO_NATIVE_APP_KEY"] ?: "").toString()}")
        resValue("string", "web_client_id", googleWebClientId)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.transportation.consumer)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Kakao SDK 추가
    implementation("com.kakao.sdk:v2-all:2.15.0")
    implementation("com.kakao.sdk:v2-user")
    implementation("com.kakao.sdk:v2-common")
    implementation("com.kakao.sdk:v2-auth")

    // Retrofit (백엔드 API 호출)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // Firebase Cloud Messaging (BoM 사용 시 버전 명시 안 함)
    implementation("com.google.firebase:firebase-messaging")

    // Firebase Analytics
    implementation("com.google.firebase:firebase-analytics")

    // image
    implementation("io.coil-kt:coil-compose:2.6.0")
    // character
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
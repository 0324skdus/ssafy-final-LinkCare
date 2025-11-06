plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
//    // Retrofit
//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    // JSON 변환을 위해 GSON 컨버터
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//    // OkHttp
//    implementation("com.squareup.okhttp3:okhttp:4.9.3")
//    // Google Sign-In
//    implementation("com.google.android.gms:play-services-auth:21.2.0")
//    // Kakao SDK
//    implementation("com.kakao.sdk:v2-user:2.20.6")
    // image
    implementation("io.coil-kt:coil-compose:2.6.0")
    // character
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
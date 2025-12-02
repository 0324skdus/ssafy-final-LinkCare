plugins {
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.parcelize)
    id("com.google.gms.google-services")  // 추가!
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ssafy.sdk.health"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
        versionCode = 1

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.core.ktx)

    //compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    //tests
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    testImplementation(libs.slf4j)
    implementation(libs.junit.ktx)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    implementation(libs.gson)

    //hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")
}

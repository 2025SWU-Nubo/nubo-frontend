plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    // 대시보드 렌더링
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.nubo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nubo"
        minSdk = 24
        targetSdk = 35
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
//    compileOptions { isCoreLibraryDesugaringEnabled = true }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

}

hilt {
    enableAggregatingTask = false
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.coil.compose)
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.play.services.auth)
    implementation(libs.javapoet)
//    implementation(libs.richtext.commonmark) // 마크다운
//    implementation(libs.richtext.ui.material3) // M3 스타일
    implementation(libs.coil.compose)
    implementation(libs.richtext.ui.material3.v100alpha03)
    implementation(libs.richtext.commonmark.v100alpha03)
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")
    implementation ("io.coil-kt:coil-svg:2.6.0")

    // Firebase BOM으로 버전 통합 관리
    implementation( platform("com.google.firebase:firebase-bom:33.3.0"))

    // FCM
    implementation ("com.google.firebase:firebase-messaging-ktx")

    //
    implementation ("com.google.firebase:firebase-analytics-ktx")

    // navhost 페이지 이동 애니메이션 커스텀
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation(libs.accompanist.navigation.animation)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // 대시보드 렌더링
    // Sceneform & Filament for 3D rendering
    implementation("io.github.sceneview:sceneview:2.2.0")
}


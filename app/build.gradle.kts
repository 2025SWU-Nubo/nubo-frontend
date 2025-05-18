plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.nubo"
    compileSdk = 35

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
        //noinspection DataBindingWithoutKapt
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    // Android 기본 라이브러리
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // 테스트 라이브러리
    testImplementation(libs.junit)                          // JUnit 단위 테스트
    androidTestImplementation(libs.androidx.junit)          // Android 테스트용 JUnit 확장
    androidTestImplementation(libs.androidx.espresso.core)  // UI 테스트용 Espresso

    // Jetpack Compose 관련
    implementation(libs.androidx.ui)                        // Compose UI 기본 구성
    implementation(libs.androidx.material3)                 // Material Design 3 컴포넌트
    implementation(libs.androidx.activity.compose)          // Compose 지원 Activity
    implementation(libs.androidx.lifecycle.runtime.compose) // Compose에서 Lifecycle 상태 관리를 위한 라이브러리
    implementation(libs.androidx.material.icons.extended)   // Material 아이콘 확장 (filled, outlined 등)

    // Compose Preview 및 디버깅용 도구
    debugImplementation(libs.androidx.ui.tooling)           // Compose UI 툴링 지원 (디버깅용)
    debugImplementation(libs.androidx.ui.tooling.preview)   // Compose 프리뷰 렌더링 도구

    //coil
    implementation(libs.coil.compose)
    //composeViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

}

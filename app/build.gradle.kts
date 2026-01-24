plugins {

    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // ВАЖНО: Эта строка включает поддержку kapt. Без неё слово kapt в dependencies работать не будет.
    id("kotlin-kapt")
}

android {
    namespace = "com.example.shop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.shop"
        minSdk = 26
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
}

dependencies {
// Базовые библиотеки Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ROOM (SQLite)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion") // kapt теперь будет работать

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
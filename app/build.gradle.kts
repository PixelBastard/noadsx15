plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ro.noadsx15.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "ro.noadsx15.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 30
        versionName = "3.0.0"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
kotlin { jvmToolchain(17) }

dependencies { testImplementation("junit:junit:4.13.2") }

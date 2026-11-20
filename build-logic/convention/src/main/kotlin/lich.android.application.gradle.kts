import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
}

extensions.configure<ApplicationExtension> {
    namespace = LichBuild.BASE_NAMESPACE
    compileSdk = LichBuild.COMPILE_SDK
    defaultConfig {
        minSdk = LichBuild.MIN_SDK
        targetSdk = LichBuild.TARGET_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

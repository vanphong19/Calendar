import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.library")
}

extensions.configure<LibraryExtension> {
    namespace = derivedNamespace
    compileSdk = LichBuild.COMPILE_SDK
    defaultConfig {
        minSdk = LichBuild.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    "testImplementation"(versionCatalog.lib("junit"))
    "testImplementation"(versionCatalog.lib("truth"))
    "androidTestImplementation"(versionCatalog.lib("androidx-test-runner"))
    "androidTestImplementation"(versionCatalog.lib("androidx-test-ext-junit"))
    "androidTestImplementation"(versionCatalog.lib("truth"))
}

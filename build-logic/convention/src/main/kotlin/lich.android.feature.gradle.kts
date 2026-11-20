plugins {
    id("lich.android.library")
    id("lich.android.compose")
    id("lich.android.hilt")
    id("lich.kotlin.serialization")
}

dependencies {
    "implementation"(project(":core:model"))
    "implementation"(project(":core:domain"))
    "implementation"(project(":core:designsystem"))
    "implementation"(versionCatalog.lib("androidx-lifecycle-runtime-compose"))
    "implementation"(versionCatalog.lib("androidx-lifecycle-viewmodel-compose"))
    "implementation"(versionCatalog.lib("androidx-hilt-lifecycle-viewmodel-compose"))
    "implementation"(versionCatalog.lib("kotlinx-coroutines-android"))
    "implementation"(versionCatalog.lib("androidx-navigation3-runtime"))
    "implementation"(versionCatalog.lib("androidx-compose-material3-adaptive"))
    "testImplementation"(versionCatalog.lib("kotlinx-coroutines-test"))
    "testImplementation"(versionCatalog.lib("turbine"))
}

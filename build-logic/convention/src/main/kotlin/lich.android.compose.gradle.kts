import com.android.build.api.dsl.CommonExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure(CommonExtension::class.java) {
    buildFeatures.compose = true
}

dependencies {
    val bom = platform(versionCatalog.lib("androidx-compose-bom"))
    "implementation"(bom)
    "androidTestImplementation"(bom)
    "implementation"(versionCatalog.lib("androidx-compose-ui"))
    "implementation"(versionCatalog.lib("androidx-compose-ui-graphics"))
    "implementation"(versionCatalog.lib("androidx-compose-foundation"))
    "implementation"(versionCatalog.lib("androidx-compose-material3"))
    "implementation"(versionCatalog.lib("androidx-compose-ui-tooling-preview"))
    "debugImplementation"(versionCatalog.lib("androidx-compose-ui-tooling"))
    "debugImplementation"(versionCatalog.lib("androidx-compose-ui-test-manifest"))
    "androidTestImplementation"(versionCatalog.lib("androidx-compose-ui-test-junit4"))
}

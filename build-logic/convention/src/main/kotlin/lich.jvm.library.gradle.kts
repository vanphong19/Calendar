plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(LichBuild.JVM_TARGET)
}

dependencies {
    "testImplementation"(versionCatalog.lib("junit"))
    "testImplementation"(versionCatalog.lib("truth"))
}

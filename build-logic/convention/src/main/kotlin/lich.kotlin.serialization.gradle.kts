plugins {
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    "implementation"(versionCatalog.lib("kotlinx-serialization-json"))
}

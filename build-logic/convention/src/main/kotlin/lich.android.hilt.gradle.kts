plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    "implementation"(versionCatalog.lib("hilt-android"))
    "ksp"(versionCatalog.lib("hilt-compiler"))
}

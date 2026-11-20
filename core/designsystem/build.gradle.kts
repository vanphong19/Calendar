plugins {
    id("lich.android.library")
    id("lich.android.compose")
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}

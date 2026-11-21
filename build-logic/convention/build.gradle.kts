import org.gradle.util.GradleVersion

plugins {
    `kotlin-dsl`
}

// VS Code can import this included build with a different Gradle distribution.
// Keep its generated Kotlin DSL accessors separate from the app wrapper's outputs.
layout.buildDirectory.set(layout.projectDirectory.dir("build/gradle-${GradleVersion.current().version}"))

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.serialization.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
}

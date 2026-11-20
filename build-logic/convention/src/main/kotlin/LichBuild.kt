import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** Shared build constants for every module. */
object LichBuild {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    const val JVM_TARGET = 17
    const val BASE_NAMESPACE = "com.app.calendar"
}

internal val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("Missing library alias '$alias' in libs.versions.toml") }

/** ":core:data" -> "com.app.calendar.core.data" */
internal val Project.derivedNamespace: String
    get() = LichBuild.BASE_NAMESPACE + path.replace(':', '.').replace('-', '_')

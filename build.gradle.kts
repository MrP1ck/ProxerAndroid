import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.impl.VariantOutputImpl
import io.gitlab.arturbosch.detekt.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)

    alias(libs.plugins.aboutlibraries)

    alias(libs.plugins.versions)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.roborazzi)
}

val isCI = System.getenv("CI")?.toBoolean() == true

val secrets = Properties().apply {
    layout.projectDirectory.file("secrets.properties").asFile.takeIf { it.exists() }?.reader()?.use { load(it) }
}

fun getFromSecrets(key: String): String {
    if (secrets.isEmpty && isCI) return "dummy"

    if (secrets.isEmpty) {
        throw GradleException("Please add a secrets.properties file with a value for $key to perform this action.")
    }

    return secrets.getProperty(key)
        ?: throw GradleException("Please include a value for $key in your secrets.properties file.")
}

val shouldSign = listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
    .all { secrets.containsKey(it) }

val gitHash = providers.exec { commandLine("git", "rev-parse", "--verify", "--short", "HEAD") }
    .standardOutput.asText.map { it.trim() }

val appVersionMajor = libs.versions.appVersionMajor.get().toInt()
val appVersionMinor = libs.versions.appVersionMinor.get().toInt()
val appVersionPatch = libs.versions.appVersionPatch.get().toInt()

android {
    namespace = "me.proxer.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "me.proxer.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionMajor * 1000000 + appVersionMinor * 10000 + appVersionPatch * 100
        versionName = "$appVersionMajor.$appVersionMinor.$appVersionPatch"

        vectorDrawables.useSupportLibrary = true
    }

    androidResources {
        localeFilters += "de"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (shouldSign) {
            create("release") {
                storeFile = file(getFromSecrets("RELEASE_STORE_FILE"))
                storePassword = getFromSecrets("RELEASE_STORE_PASSWORD")
                keyAlias = getFromSecrets("RELEASE_KEY_ALIAS")
                keyPassword = getFromSecrets("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        if (!shouldSign && !isCI) {
            logger.warn(
                "This build will not be signed because it is missing the keystore info. Please add " +
                    "values for \"RELEASE_STORE_FILE\", \"RELEASE_STORE_PASSWORD\", \"RELEASE_KEY_ALIAS\" and " +
                    "\"RELEASE_KEY_PASSWORD\" to your secrets.properties file if you want the apk to be signed."
            )
        }

        debug {
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }

            versionNameSuffix = "-debug"
            buildConfigField("String", "PROXER_API_KEY", "\"${getFromSecrets("PROXER_API_KEY")}\"")
            buildConfigField("boolean", "LOG", "Boolean.parseBoolean(\"true\")")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "config/shrinker/shrinker-rules.pro")

            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }

            buildConfigField("String", "PROXER_API_KEY", "\"${getFromSecrets("PROXER_API_KEY")}\"")
            buildConfigField("boolean", "LOG", "Boolean.parseBoolean(\"false\")")

            if (shouldSign) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        create("logRelease") {
            initWith(getByName("release"))
            matchingFallbacks += "release"

            versionNameSuffix = "-logRelease-${gitHash.get()}"
            buildConfigField("boolean", "LOG", "Boolean.parseBoolean(\"true\")")
        }
    }

    lint {
        warningsAsErrors = true
        checkReleaseBuilds = false

        lintConfig = file("$rootDir/config/lint/lint.xml")
        xmlOutput = layout.buildDirectory.file("reports/lint/lint-results.xml").get().asFile
        htmlOutput = layout.buildDirectory.file("reports/lint/lint-results.html").get().asFile
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }

        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/proguard/**",
                "META-INF/README.md",
                "META-INF/nanohttpd/**",
                "kotlin/**",
                "kotlinx/**",
                "third_party/**",
                "jsr305_annotations/**",
                "error_prone/**",
                "auto/**",
                "build-data.properties",
                "androidsupportmultidexversion.txt",
                "play-services-*.properties"
            )
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

androidComponents {
    // The test needs the activity of ui-test-manifest, which is only added to debug builds.
    beforeVariants(selector().all()) { variantBuilder ->
        if (variantBuilder.buildType != "debug") {
            (variantBuilder as HasHostTestsBuilder).hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
        }
    }

    onVariants { variant ->
        if (variant.buildType == "logRelease") {
            // logRelease is release with logging, so it uses the sources and resources of release.
            variant.sources.java?.addStaticSourceDirectory("src/release/kotlin")
            variant.sources.res?.addStaticSourceDirectory("src/release/res")
        }

        val suffix = if (variant.buildType == "release") "-release" else ""

        variant.outputs.forEach { output ->
            (output as VariantOutputImpl).outputFileName.set("app-${output.versionName.get()}$suffix.apk")
        }
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlin.stdlib)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)
    implementation(libs.telephoto.zoomable.image.coil3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.play.app.update)
    implementation(libs.play.services.cast.framework)
    implementation(libs.play.services.ads.identifier)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.sharetarget)
    implementation(libs.androidx.security.crypto)

    implementation(libs.material)
    implementation(libs.aboutlibraries.compose)
    implementation(libs.customtabshelper)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.smoothstreaming)
    implementation(libs.media3.exoplayer.ima)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.ui)
    implementation(libs.media3.cast)

    implementation(libs.proxerlib)
    implementation(libs.threetenabp)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.logging)
    implementation(libs.okio)
    implementation(libs.moshi)
    implementation(libs.moshi.adapters)
    implementation(libs.jsoup)

    implementation(libs.strictmodecompat)
    implementation(libs.timber)
    implementation(libs.anrwatchdog)

    implementation(libs.leakcanary.plumber)
    debugImplementation(libs.leakcanary)

    releaseImplementation(libs.customactivityoncrash)
    "logReleaseImplementation"(libs.customactivityoncrash)

    ksp(libs.androidx.room.compiler)
    ksp(libs.moshi.codegen)

    compileOnly(libs.javax.annotation)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        // The jitpack builds of these artifacts and the jcenter artifacts below are no longer available.
        substitute(module("com.squareup.moshi:moshi"))
            .using(module("com.squareup.moshi:moshi:${libs.versions.moshi.get()}"))
        substitute(module("com.google.android:flexbox"))
            .using(module("com.google.android.flexbox:flexbox:${libs.versions.flexbox.get()}"))
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true

    config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"

    reports {
        xml.required = true
        xml.outputLocation = layout.buildDirectory.file("reports/detekt/detekt.xml")
        html.required = true
        html.outputLocation = layout.buildDirectory.file("reports/detekt/detekt.html")
    }
}

ktlint {
    version = libs.versions.ktlint.get()
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf {
        candidate.version.contains("alpha")
    }
}

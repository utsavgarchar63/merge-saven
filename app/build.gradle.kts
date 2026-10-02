import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Local builds must not publish symbols or mapping files to an external account.
tasks.configureEach {
    if (name.startsWith("uploadCrashlytics")) enabled = false
}

// One source of truth for manifest and all ad formats, including release QA.
val adMobSettings = Properties().apply {
    rootProject.file("admob.properties").inputStream().use { load(it) }
}
val adMobProfile = adMobSettings.getProperty("profile")
require(adMobProfile in setOf("test", "production")) { "admob.properties: profile must be test or production" }
fun adMobId(format: String, profile: String = adMobProfile): String = requireNotNull(adMobSettings.getProperty("$profile.$format")) {
    "Missing $profile.$format in admob.properties"
}.also { require(it.startsWith("ca-app-pub-")) { "Invalid AdMob ID for $format" } }

android {
    namespace = "com.mergeseven.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mergeseven.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.5.0"

        // The app currently ships English only; omit unused library translations.
        resourceConfigurations += listOf("en")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "admob_app_id", adMobId("app"))
        resValue("string", "admob_rewarded_unit_id", adMobId("rewarded"))
        resValue("string", "admob_interstitial_unit_id", adMobId("interstitial"))
        resValue("string", "admob_banner_unit_id", adMobId("banner"))
        resValue("string", "admob_demo_rewarded_unit_id", adMobId("rewarded", "test"))
        resValue("string", "admob_demo_interstitial_unit_id", adMobId("interstitial", "test"))
        resValue("string", "admob_demo_banner_unit_id", adMobId("banner", "test"))
        buildConfigField("boolean", "TEST_ADS", (adMobProfile == "test").toString())
        buildConfigField("boolean", "QA_AD_FALLBACK", "false")
        buildConfigField("String", "AD_TEST_DEVICE_IDS", "\"${adMobSettings.getProperty("test.devices", "")}\"")

    }

    sourceSets {
        // Lets MigrationTestHelper read the exported schemas at runtime.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    signingConfigs {
        getByName("debug") {
            val debugKeystoreFile = file("debug.keystore")
            val rootDebugKeystore = file("${project.rootDir}/debug.keystore")
            if (debugKeystoreFile.exists()) {
                storeFile = debugKeystoreFile
            } else if (rootDebugKeystore.exists()) {
                storeFile = rootDebugKeystore
            }
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            isV1SigningEnabled = true
            isV2SigningEnabled = true
        }
        create("release") {
            val keystoreFile = file("release.keystore")
            val debugKeystoreFile = file("debug.keystore")
            val rootDebugKeystore = file("${project.rootDir}/debug.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "mergeseven123"
                keyAlias = "releasekey"
                keyPassword = "mergeseven123"
            } else if (debugKeystoreFile.exists()) {
                storeFile = debugKeystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            } else if (rootDebugKeystore.exists()) {
                storeFile = rootDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            } else {
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            isV1SigningEnabled = true
            isV2SigningEnabled = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "QA_AD_FALLBACK", adMobSettings.getProperty("qa.fallback", "false").toBooleanStrict().toString())
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        // Compress APK bytecode for smaller direct-download files. Android extracts
        // it during installation; this does not change Play-generated bundle APKs.
        dex { useLegacyPackaging = true }
    }

    bundle {
        // Keep device-specific delivery enabled without dropping device support.
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time is used for daily streaks/quests and is API 26+ without this.
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

// Room schema JSON is committed to app/schemas and read by the migration tests.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

// AF6-01: keep real google-services.json out of git; copy example stub if missing so CI compiles.
val ensureGoogleServicesJson by tasks.registering {
    val example = file("google-services.json.example")
    val target = file("google-services.json")
    onlyIf { !target.exists() && example.exists() }
    doLast {
        example.copyTo(target, overwrite = false)
    }
}
tasks.named("preBuild").configure { dependsOn(ensureGoogleServicesJson) }

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Firebase (AF6)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    // Ads + UMP (AF9)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)


    // Java 8+ API desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}

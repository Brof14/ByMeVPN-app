import java.util.Base64

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

// Signed device-bound entitlements are opt-in until the matching private key
// is confirmed on the production API. Public releases intentionally remain on
// the same legacy API contract as the working Windows client by default.
val signedEntitlementsEnabled = providers.gradleProperty("enableSignedEntitlements").orNull == "true"
val configuredServerEntitlementPublicKey = providers.gradleProperty("serverEntitlementPublicKey")
    .orElse(providers.environmentVariable("SERVER_ES256_PUBLIC_KEY"))
    .getOrElse("")
val serverEntitlementPublicKey = if (signedEntitlementsEnabled) {
    configuredServerEntitlementPublicKey
} else {
    ""
}
val escapedEntitlementPublicKey = serverEntitlementPublicKey
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\r", "\\r")
    .replace("\n", "\\n")
require(!signedEntitlementsEnabled ||
    (serverEntitlementPublicKey.startsWith("-----BEGIN PUBLIC KEY-----") &&
        serverEntitlementPublicKey.trimEnd().endsWith("-----END PUBLIC KEY-----"))) {
    "Set enableSignedEntitlements=true and provide a PEM SERVER_ES256_PUBLIC_KEY to opt in."
}

android {
    namespace = "com.example.bymevpn"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bymevpn.app"
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 58
        versionName = "1.0.57"
        buildConfigField("String", "SERVER_ES256_PUBLIC_KEY", "\"$escapedEntitlementPublicKey\"")
    }

    // Enable per-ABI splits (+ universal APK) for release builds so CI and
    // direct-APK update flows receive compact ABI packages, while keeping
    // assembleDebug outputting the standard single app-debug.apk.
    val enableAbiSplits = gradle.startParameter.taskNames.any {
        it.contains("Release", ignoreCase = true)
    } || providers.gradleProperty("enableAbiSplits").orNull == "true"

    splits {
        abi {
            isEnable = enableAbiSplits
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        val rootDebugKeystore = file("${rootDir}/debug.keystore")
        val debugKeystoreBase64 = file("${rootDir}/debug.keystore.base64")
        val resolvedDebugKeystore = when {
            rootDebugKeystore.exists() -> rootDebugKeystore
            debugKeystoreBase64.exists() -> layout.buildDirectory.file("signing/debug.keystore").get().asFile.apply {
                parentFile.mkdirs()
                val encoded = debugKeystoreBase64.readText().replace(Regex("\\s+"), "")
                writeBytes(Base64.getDecoder().decode(encoded))
            }
            else -> null
        }
        if (resolvedDebugKeystore != null && resolvedDebugKeystore.exists()) {
            create("debugConfig") {
                storeFile = resolvedDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (System.getenv("KEYSTORE_PATH") != null && System.getenv("KEYSTORE_PASSWORD") != null &&
            System.getenv("KEY_ALIAS") != null && System.getenv("KEY_PASSWORD") != null) {
            create("release") {
                storeFile = file(System.getenv("KEYSTORE_PATH"))
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "EXTERNAL_CHECKOUT_ENABLED", "true")
            if (providers.gradleProperty("sideBySideDebug").orNull == "true") {
                applicationIdSuffix = ".${providers.gradleProperty("previewSuffix").orElse("preview").get()}"
            }
            signingConfigs.findByName("debugConfig")?.let {
                signingConfig = it
            }
        }
        release {
            buildConfigField("boolean", "EXTERNAL_CHECKOUT_ENABLED", "true")
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("play") {
            initWith(getByName("release"))
            buildConfigField("boolean", "EXTERNAL_CHECKOUT_ENABLED", "true")
            matchingFallbacks += listOf("release")
        }
    }
    val hasLibv2rayAar = file("libs/libv2ray.aar").exists()
    val requireNativeLibv2ray = providers.gradleProperty("requireNativeLibv2ray").orNull == "true"
    require(!requireNativeLibv2ray || hasLibv2rayAar) {
        "Native app/libs/libv2ray.aar is required when -PrequireNativeLibv2ray=true is set."
    }
    if (!hasLibv2rayAar) {
        logger.warn("WARNING: app/libs/libv2ray.aar is missing; building with stub Libv2ray (VPN tunnel disabled).")
    }
    sourceSets.getByName("debug").java.srcDir("src/direct/kotlin")
    sourceSets.getByName("release").java.srcDir("src/direct/kotlin")
    if (!hasLibv2rayAar) {
        sourceSets.getByName("main").java.srcDir("src/stub/kotlin")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    if (file("libs/libv2ray.aar").exists()) {
        implementation(files("libs/libv2ray.aar"))
    }
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.11.00"))
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

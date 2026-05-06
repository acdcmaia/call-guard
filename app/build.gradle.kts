import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val buildDate: String = SimpleDateFormat("yyMMdd.HHmm").format(Date())

android {
    signingConfigs {
        create("releaseConfig") {
            storeFile = file("C:\\Users\\maia\\callguard.jks")
            storePassword = "callguard123!"
            keyAlias = "callguard"
            keyPassword = "callguard123!"
        }
    }
    namespace = "com.acdcmaia.callguard"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.acdcmaia.callguard"
        minSdk = 29
        targetSdk = 36
        versionCode = 8
        versionName = "0.1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")

        buildConfigField("String", "DEVELOPER", "\"acdcmaia\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("releaseConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

tasks.whenTaskAdded {
    if (name.startsWith("assemble") && (name.endsWith("Debug") || name.endsWith("Release"))) {
        doLast {
            val variant = name.removePrefix("assemble").lowercase()
            val named = "CallGuard-v${android.defaultConfig.versionName}-b$buildDate.$variant.apk"
            val buildApkDir = layout.buildDirectory.dir("outputs/apk/$variant").get().asFile
            val releaseDir = File(projectDir, "release")

            // Procura o APK nas duas localizações possíveis
            val apk = listOf(buildApkDir, releaseDir)
                .flatMap { it.listFiles()?.toList() ?: emptyList() }
                .firstOrNull { it.name.startsWith("app-") && it.name.contains(variant) && it.extension == "apk" }

            if (apk != null) {
                val destDir = if (variant == "release") { releaseDir.also { it.mkdirs() } } else buildApkDir
                apk.copyTo(File(destDir, named), overwrite = true)
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

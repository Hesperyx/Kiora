import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

/**
 * 取构建时的 Git 信息（提交哈希 / 提交时间）。
 *
 * 打包机没有 git 或不在仓库里时返回 unknown，不阻断构建 ——
 * 构建信息只用于展示，缺了不该让打包失败。
 */
fun gitValue(vararg args: String): String = runCatching {
    val process = ProcessBuilder("git", *args)
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    val exitCode = process.waitFor()
    if (exitCode == 0) output else "unknown"
}.getOrDefault("unknown")

extensions.configure<ApplicationExtension> {
    namespace = "cn.hxy.kiora"
    compileSdk = 37

    defaultConfig {
        applicationId = "cn.hxy.kiora"
        minSdk = 26
        targetSdk = 37
        versionCode = 28
        versionName = "1.3.6"

        buildConfigField("String", "GIT_COMMIT", "\"${gitValue("rev-parse", "HEAD")}\"")
        buildConfigField("String", "GIT_COMMIT_TIME", "\"${gitValue("log", "-1", "--format=%cI")}\"")

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
            storePassword = keystoreProperties.getProperty("storePassword") ?: ""
            val storeFileName = keystoreProperties.getProperty("storeFile") ?: ""
            if (storeFileName.isNotEmpty()) {
                storeFile = file(storeFileName)
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    androidResources {
        additionalParameters += listOf(
            "--allow-reserved-package-id",
            "--package-id",
            "0x44",
        )
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF*.proto"
            )
            pickFirsts += setOf(
                "META-INF/xposed/**",
                "META-INF/services/**"
            )
        }
    }

}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.dalvik.dx)
    implementation(libs.libxposed.service)
    implementation(projects.annotation)
    implementation(libs.dexkit)
    implementation(libs.protobuf.java)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kyant0.backdrop)
    implementation(libs.kyant0.shapes)
    implementation(libs.composablehorizons.material.symbols.outlined)
    implementation(libs.composablehorizons.material.symbols.filled)

    ksp(projects.processor)

    compileOnly(libs.libxposed.api)
    compileOnly(libs.xposed)
    compileOnly(projects.qqinterface)
    compileOnly(projects.wxinterface)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.36.1"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
val packageName = "com.tencent.mobileqq"
// adb shell am force-stop com.tencent.mobileqq
val killQQ = tasks.register<Exec>("killQQ") {
    description = ""
    group = "kiora"
    commandLine(adb, "shell", "am", "force-stop", packageName)
    isIgnoreExitValue = true
}

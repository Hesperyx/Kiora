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
                "META-INF*.proto",
                // Monet 用 ARSCLib 读宿主资源表、用 apksig + bouncycastle 签 overlay APK。
                // bouncycastle 三件套（bcprov/bcpkix/bcutil）各自带同名许可与 jar 签名文件，
                // 不排会直接撞 mergeReleaseJavaResource；versions/** 是 JDK9+ 的 MR-JAR 覆盖层，Android 不读。
                "META-INF/LICENSE.md",
                "META-INF/INDEX.LIST",
                "META-INF/BCRSA204.SF",
                "META-INF/BCRSA204.RSA",
                "META-INF/versions/**",
                // Monet 读宿主资源表时禁用了框架默认加载。
                "frameworks/android/**",
                // Monet 用 RSA 签名，Picnic 的后量子查表用不到。
                "org/bouncycastle/pqc/crypto/picnic/**"
            )
            pickFirsts += setOf(
                "META-INF/xposed/**",
                "META-INF/services/**"
            )
        }
    }

}

// ARSCLib 打包了桌面版 android/** 与 org/xmlpull/v1/** 实现。若把它当 program class 交给 R8，
// 连构造函数查询里的 AttributeSet::class 都会被改写成它自带（混淆后）的副本，从而不再匹配 Android 版；
// 因此把这两棵树从 jar 中剔除，只留纯资源表解析部分给宿主 APK 用。
val arsclibSource = configurations.create("arsclibSource") {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

val prepareAndroidArsclib = tasks.register<Jar>("prepareAndroidArsclib") {
    from(provider { arsclibSource.map { zipTree(it) } })
    exclude("android/**", "org/xmlpull/v1/**")
    archiveFileName.set("arsclib-android.jar")
    destinationDirectory.set(layout.buildDirectory.dir("generated/arsclib"))
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
    implementation(libs.materialkolor)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp3.okhttp)
    // coil-android pulls appcompat-resources 1.7.1, which is not in the offline cache; 1.8.0 is.
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.composablehorizons.material.symbols.outlined)
    implementation(libs.composablehorizons.material.symbols.filled)
    implementation(libs.androidx.room.runtime)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.shader)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.squircle)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.mcp.server)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.html)
    // WeKit 血统的公共层需要：ProxyBuilder 用于运行期生成代理类，osmdroid 用于地图选点。
    implementation(libs.dexmaker)
    implementation(libs.osmdroid.android)
    // FingerprintPay 用 BiometricPrompt 做指纹支付解锁。
    implementation(libs.androidx.biometric)
    // TransparentActivity 用 FragmentActivity 作基类（BiometricPrompt 的 Activity 构造器要求）。
    implementation(libs.androidx.fragment)
    // 主题引擎（MonetEngineModuleGenerator）用 ARSCLib 读宿主资源表，用 apksig/bouncycastle 签出 overlay APK。
    add(arsclibSource.name, libs.arsclib)
    implementation(files(prepareAndroidArsclib))
    implementation(libs.apksig)
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)

    // biometric 1.2.0-alpha05 的传递依赖里 customview/drawerlayout 仍指向 1.0.0，
    // 而离线缓存里只有 1.2.0 / 1.1.1（同大版本内的向上对齐），这里显式提版。
    constraints {
        implementation("androidx.customview:customview:1.2.0") {
            because("biometric -> appcompat 1.8.0 -> drawerlayout 1.0.0 -> customview 1.0.0 不在离线缓存")
        }
        implementation("androidx.drawerlayout:drawerlayout:1.1.1") {
            because("biometric -> appcompat 1.8.0 -> drawerlayout 1.0.0 不在离线缓存")
        }
    }

    ksp(projects.processor)
    ksp(libs.androidx.room.compiler)

    compileOnly(libs.libxposed.api)
    compileOnly(libs.xposed)
    compileOnly(projects.qqinterface)
    compileOnly(projects.wxinterface)
}

ksp {
    // Room schema export for migration diffing
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
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

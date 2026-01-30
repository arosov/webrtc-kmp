import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.maven.publish)
    kotlin("native.cocoapods")
}

val localProperties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

group = "io.github.arosov"

version = System.getenv("VERSION") ?: "0.0.0"

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    cocoapods {
        version = project.version.toString()
        summary = "WebRTC Kotlin Multiplatform SDK"
        homepage = "https://github.com/arosov/webrtc-kmp"
        ios.deploymentTarget = "13.0"

        noPodspec()

        pod("WebRTC-SDK") {
            version =
                libs.versions.webrtc.ios.sdk
                    .get()
            moduleName = "WebRTC"
            packageName = "WebRTC"
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    androidTarget {
        publishAllLibraryVariants()
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
        }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js {
        useCommonJs()
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        static =
                            (static ?: mutableListOf()).apply {
                                // Serve sources to debug inside browser
                                add(project.rootDir.path)
                            }
                    }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jsAndWasmJs") {
                withJs()
                withWasmJs()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.coroutines)
        }

        androidMain.dependencies {
            api(libs.webrtc.sdk)
            implementation(libs.kotlin.coroutines.android)
            implementation(libs.androidx.coreKtx)
            implementation(libs.androidx.startup)
        }

        jsMain.dependencies {
            implementation(npm("webrtc-adapter", "8.1.1"))
        }

        wasmJsMain.dependencies {
            implementation(libs.kotlin.browser)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-annotations-common"))
            implementation(libs.kotlin.coroutines.test)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.test.rules)
        }

        val iosX64AndSimulatorArm64Main by creating {
            dependsOn(iosMain.get())
        }

        val iosX64Main by getting
        iosX64Main.dependsOn(iosX64AndSimulatorArm64Main)
        val iosSimulatorArm64Main by getting
        iosSimulatorArm64Main.dependsOn(iosX64AndSimulatorArm64Main)
    }
}

android {
    namespace = "com.shepeliev.webrtckmp"

    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDir("src/androidMain/res")

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
    }

    dependencies {
        androidTestImplementation(libs.androidx.test.core)
        androidTestImplementation(libs.androidx.test.runner)
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/arosov/webrtc-kmp")
            credentials {
                username = localProperties.getProperty("GITHUB_ACTOR") ?: System.getenv("GITHUB_ACTOR")
                password = localProperties.getProperty("GITHUB_TOKEN") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

mavenPublishing {
    coordinates(project.group.toString(), project.name, "${project.version}")

    pom {
        name = "WebRTC KMP"
        description = "WebRTC Kotlin Multiplatform SDK."
        url = "https://github.com/arosov/webrtc-kmp"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "arosov"
                name = "arosov"
                email = "arosov@users.noreply.github.com"
            }
        }

        scm {
            url = "https://github.com/arosov/webrtc-kmp"
            connection = "scm:git:https://github.com/arosov/webrtc-kmp.git"
            developerConnection = "scm:git:https://github.com/arosov/webrtc-kmp.git"
        }
    }
}
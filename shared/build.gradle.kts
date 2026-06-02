plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("app.cash.sqldelight") version "2.0.1"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("io.ktor:ktor-client-core:2.3.8")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
                
                // Phase 2: SQLDelight
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
                implementation("com.benasher44:uuid:0.8.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                
                // Phase 3: Koin & Settings
                implementation("io.insert-koin:koin-core:3.5.3")
                implementation("com.russhwolf:multiplatform-settings:1.1.1")
                implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
                implementation("com.russhwolf:multiplatform-settings-coroutines:1.1.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.8")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
                implementation("app.cash.sqldelight:android-driver:2.0.1")
                implementation("io.insert-koin:koin-android:3.5.3")
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("androidx.biometric:biometric:1.2.0-alpha05")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.8")
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }
    }
}

android {
    namespace = "com.henryliu.cbtreframe.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.henryliu.cbtreframe.shared.db")
        }
    }
}

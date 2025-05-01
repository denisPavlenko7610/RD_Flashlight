plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rdragon.rd_flashlightautoshutdown"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rdragon.rd_flashlightautoshutdown"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.1"
    }

    packaging {
        resources {
            excludes += "kotlin/collections/collections.kotlin_builtins"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.material.v190)
    androidTestImplementation (libs.androidx.appcompat.v161)
    androidTestImplementation( libs.material.v1110)
    androidTestImplementation( libs.androidx.constraintlayout)

}